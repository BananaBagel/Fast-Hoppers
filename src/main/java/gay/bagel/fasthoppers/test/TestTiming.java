package gay.bagel.fasthoppers.test;

import gay.bagel.fasthoppers.FastHoppers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Records how long each test took, in game ticks and in real time, and leaves the whole run behind
 * as a CSV so results can be compared across versions and loaders.
 * <p>
 * Every test logs one tab-separated line, readable in a run log:
 * <pre>
 * [fasthoppers-timing] basic_chest  delay=8  items=64  hoppers=1  expected=513  actual=513  drift=0
 * </pre>
 * and contributes one row to {@value #CSV_NAME}, written to the run directory when the game exits.
 * The log line is for reading during a run; the CSV is for aggregating afterwards, because the
 * interesting questions are comparative and no single run answers them.
 * <p>
 * The two durations measure different things and both are wanted. <strong>Ticks</strong> are game
 * time: how long the mod's own behaviour takes, which is what the delay gamerule controls and what
 * the timing model predicts. <strong>Wall milliseconds</strong> are how long the server took to
 * simulate those ticks, which is a property of the version and loader rather than of the mod — a
 * gametest server runs ticks as fast as it can, so a test spanning 1450 ticks can finish in under
 * two seconds. Drift between versions shows up in the first; a version being expensive to run shows
 * up only in the second.
 * <p>
 * Wall time is measured across concurrently running tests, since everything in a batch ticks
 * together, so it reflects each test's share of a shared run rather than its cost in isolation.
 * That is the honest number for comparing one version against another, and it is not additive.
 * <p>
 * Only tests that reach {@link #record} appear. A test that never satisfies its assertions leaves
 * no row, so a missing row means a failure or a timeout.
 * <p>
 * The current test is stashed here rather than threaded through every assertion because a test body
 * is a plain {@code Consumer<GameTestHelper>} with nowhere to carry it. That is safe because bodies
 * are invoked synchronously on the server thread, so {@link TestArena#succeedWhen} captures what it
 * needs into its closure before any other test starts — only the capture has to happen while the
 * values are current, not the later polling.
 */
public final class TestTiming {

	/** Marker every timing line starts with, for grepping a run log. */
	public static final String PREFIX = "[fasthoppers-timing]";

	/** File written to the run directory as the game exits. */
	public static final String CSV_NAME = "fasthoppers-timings.csv";

	/** Column order of {@value #CSV_NAME}. */
	private static final String CSV_HEADER =
			"minecraft,loader,test,delay,items,hoppers,expected_ticks,actual_ticks,drift_ticks,wall_ms,"
			+ "span_ticks,span_items,ticks_per_item";

	/** Suffix the delay fan-out appends to each test name, stripped so names group across delays. */
	private static final String DELAY_SUFFIX = "_delay_";

	private static final List<String> ROWS = Collections.synchronizedList(new ArrayList<>());

	private static String current = "unknown";
	private static TestOptions currentOptions = TestOptions.of();
	private static long startedAtNanos;

	static {
		// A gametest server exits as soon as the run finishes, and there is no completion hook to
		// hang this off. Writing on shutdown also means one file write per run rather than per test.
		Runtime.getRuntime().addShutdownHook(new Thread(TestTiming::writeCsv, "fasthoppers-timings"));
	}

	private TestTiming() {
		throw new AssertionError("No instances.");
	}

	/**
	 * Notes which test is about to run. Called by the fan-out immediately before invoking a body.
	 *
	 * @param name    the test's full name, including its delay suffix
	 * @param options that test's settings, including its timing expectation
	 */
	public static void beginning(String name, TestOptions options) {
		current = name;
		currentOptions = options;
		startedAtNanos = System.nanoTime();
	}

	/**
	 * @return the name of the test currently being set up
	 */
	public static String current() {
		return current;
	}

	/**
	 * @return the settings of the test currently being set up
	 */
	public static TestOptions currentOptions() {
		return currentOptions;
	}

	/**
	 * Reports a completed test's duration, to the log and to the run's CSV.
	 *
	 * @param name    the test's full name
	 * @param options that test's settings
	 * @param ticks   how many ticks elapsed before its assertions first passed
	 */
	public static void record(String name, TestOptions options, long ticks) {
		record(name, options, ticks, -1L, 0);
	}

	/**
	 * Reports a completed test's duration and, where one was observed, its measured transfer rate.
	 * <p>
	 * The rate columns are the trustworthy ones. {@code ticks} is measured from the test's clock,
	 * which from 1.21.11 starts after the structure has already been ticking, so it under-reports
	 * there. The span is measured between two moments the test watched for itself, so it excludes
	 * that window from both endpoints and stays comparable across every version.
	 *
	 * @param name      the test's full name
	 * @param options   that test's settings
	 * @param ticks     how many ticks elapsed before its assertions first passed
	 * @param spanTicks ticks between the first and last observed movement, or -1 if not measured
	 * @param spanItems items moved across that span
	 */
	public static void record(String name, TestOptions options, long ticks, long spanTicks, int spanItems) {
		long wallMillis = (System.nanoTime() - startedAtNanos) / 1_000_000L;
		int delay = options.transferDelay();
		String perItem = spanTicks > 0 && spanItems > 0
				? String.format("%.3f", (double) spanTicks / spanItems)
				: "";

		if (spanItems > 0) {
			FastHoppers.LOGGER.info(
					"{} {}\tdelay={}\tspan={}t/{}items\tticks_per_item={}",
					PREFIX, name, delay, spanTicks, spanItems, perItem
			);
		}

		if (options.isTimed()) {
			int expected = options.expectedTicks(delay);
			FastHoppers.LOGGER.info(
					"{} {}\tdelay={}\titems={}\thoppers={}\texpected={}\tactual={}\tdrift={}\twall={}ms",
					PREFIX, name, delay, options.itemsMoved(), options.hoppers(),
					expected, ticks, ticks - expected, wallMillis
			);
			addRow(name, options, delay, expected, ticks, ticks - expected, wallMillis, spanTicks, spanItems, perItem);
			return;
		}

		FastHoppers.LOGGER.info(
				"{} {}\tdelay={}\tactual={}\twall={}ms", PREFIX, name, delay, ticks, wallMillis
		);
		// Untimed tests have no prediction to compare against, so those columns stay empty rather
		// than carrying a zero that would average in as though it were a measurement.
		addRow(name, options, delay, null, ticks, null, wallMillis, spanTicks, spanItems, perItem);
	}

	/**
	 * Adds one row to the run's CSV.
	 *
	 * @param name       the test's full name, delay suffix included
	 * @param options    that test's settings
	 * @param delay      the delay it ran under
	 * @param expected   predicted ticks, or {@code null} when the test is untimed
	 * @param ticks      measured ticks
	 * @param drift      measured minus predicted, or {@code null} when the test is untimed
	 * @param wallMillis real time the test occupied
	 */
	private static void addRow(
			String name, TestOptions options, int delay,
			Integer expected, long ticks, Long drift, long wallMillis,
			long spanTicks, int spanItems, String perItem
	) {
		ROWS.add(String.join(",",
				// The build-time constant rather than Platform.mcVersion(), which is an unimplemented
				// stub returning "" on both Forge and NeoForge. This is also the more accurate value:
				// what is wanted is the version the jar targets, which is exactly what is substituted.
				FastHoppers.MINECRAFT,
				FastHoppers.xplat().loader().name().toLowerCase(),
				baseName(name),
				String.valueOf(delay),
				String.valueOf(options.itemsMoved()),
				String.valueOf(options.hoppers()),
				expected == null ? "" : String.valueOf(expected),
				String.valueOf(ticks),
				drift == null ? "" : String.valueOf(drift),
				String.valueOf(wallMillis),
				spanTicks > 0 ? String.valueOf(spanTicks) : "",
				spanItems > 0 ? String.valueOf(spanItems) : "",
				perItem
		));
	}

	/**
	 * Strips the delay suffix the fan-out appends, so a test is one name across every delay it ran
	 * under and the delay lives in its own column.
	 *
	 * @param name the test's full name
	 * @return the name the suite declared
	 */
	private static String baseName(String name) {
		int suffix = name.lastIndexOf(DELAY_SUFFIX);
		return suffix < 0 ? name : name.substring(0, suffix);
	}

	/**
	 * Writes every recorded row to {@value #CSV_NAME} in the run directory.
	 * <p>
	 * Failures here are logged rather than thrown: this runs on shutdown, where an exception would
	 * be reported as the run dying instead of as a report going missing.
	 */
	private static void writeCsv() {
		List<String> rows;
		synchronized (ROWS) {
			if (ROWS.isEmpty()) {
				return;
			}
			rows = new ArrayList<>(ROWS);
		}

		List<String> lines = new ArrayList<>(rows.size() + 1);
		lines.add(CSV_HEADER);
		lines.addAll(rows);

		Path target = Paths.get(CSV_NAME).toAbsolutePath();
		try {
			Files.write(target, lines);
			FastHoppers.LOGGER.info("{} wrote {} timings to {}", PREFIX, rows.size(), target);
		} catch (IOException failure) {
			FastHoppers.LOGGER.error("{} could not write {}", PREFIX, target, failure);
		}
	}
}
