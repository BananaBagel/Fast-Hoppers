package gay.bagel.fasthoppers.test;

import gay.bagel.fasthoppers.FastHoppers;
import gay.bagel.fasthoppers.FastHoppersGameRules;
import gay.bagel.fasthoppers.test.tests.ChestTests;
import gay.bagel.fasthoppers.test.tests.DropperTests;
import gay.bagel.fasthoppers.test.tests.FurnaceTests;
import gay.bagel.fasthoppers.test.tests.HopperTests;
import gay.bagel.fasthoppers.test.tests.MinecartTests;

import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;
import java.util.function.Consumer;

//? < 1.21.11 {
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;

import java.util.ArrayList;
import java.util.Collection;
//?} >= 1.21.11 {
/*import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;
*///?}

/**
 * Entry point for {@link FastHoppers}' gametests, and the only file that knows how tests are
 * declared on any given version.
 * <p>
 * Tests themselves live in {@code test.tests} and are written against vanilla
 * {@link GameTestHelper}, which is identical on every loader. Each suite exposes a
 * {@code contribute(TestRegistrar)} method and is named once in {@link #SUITES}; everything else
 * here is plumbing that adapts that single list to whatever the current version wants:
 * <ul>
 *   <li><strong>&lt; 1.21.11</strong> — {@code generateTests} turns each entry into a
 *       {@code TestFunction} for {@code GameTestRegistry}.</li>
 *   <li><strong>&gt;= 1.21.11</strong> — {@code registerFunctions} registers each body into
 *       {@code Registries.TEST_FUNCTION}, and a {@code test_instance} datapack entry supplies the
 *       settings the runtime no longer carries.</li>
 * </ul>
 * Because both paths — and, later, the generator for those datapack entries — walk {@link #SUITES}
 * through {@link #collect}, a test cannot be registered on one version and silently missing on
 * another.
 * <p>
 * The legacy era deliberately uses {@code @GameTestGenerator} rather than the more obvious
 * {@code @GameTest} annotation, because {@code @GameTest} cannot name a structure portably.
 * Vanilla rewrites its {@code template} to {@code <lowercased class name>.<template>} in the
 * {@code minecraft} namespace, while NeoForge and Forge instead prepend a namespace of their own —
 * so the same annotation resolves to two different structures depending on the loader. Building
 * {@code TestFunction} by hand passes {@code structureName} through untouched on every loader,
 * which is what lets all of them share one structure file.
 */
public final class FastHoppersGameTests {

	/**
	 * Every test suite this mod contributes. Adding a suite means adding one line here.
	 * <p>
	 * Order is not significant — the runner batches and schedules tests itself.
	 */
	private static final List<Consumer<TestRegistrar>> SUITES = List.of(
			HopperTests::contribute,
			ChestTests::contribute,
			DropperTests::contribute,
			FurnaceTests::contribute,
			MinecartTests::contribute
	);

	/**
	 * Required by the legacy {@code @GameTestGenerator} path, which reflectively calls
	 * {@code getDeclaringClass().newInstance()} before invoking the generator. Kept public and
	 * no-arg for that reason — this class holds no state and is not meant to be instantiated
	 * by hand.
	 */
	public FastHoppersGameTests() {}

	/**
	 * Hopper transfer delays every test is run under, in ticks.
	 * <p>
	 * Eight is vanilla. The rest bracket it deliberately: the bug this mod has to stay clear of is
	 * items vanishing when a shortened delay moves more than one item per transfer, so the fast
	 * values matter most, and the slow ones prove nothing regressed in the other direction.
	 */
	private static final int[] TRANSFER_DELAYS = {1, 3, 8, 12, 23};

	/**
	 * Ticks allowed past a timed test's tolerance before the runner gives up on it.
	 * <p>
	 * Keeps the duration check the thing that reports an overrun, since it can say by how much,
	 * instead of the runner timing out with no detail.
	 */
	private static final int TIMEOUT_MARGIN = 40;

	/**
	 * Offers every test in every suite to {@code registrar}, once per entry in
	 * {@link #TRANSFER_DELAYS}.
	 * <p>
	 * The single point all consumers go through, so they cannot disagree about which tests exist.
	 * Suites declare a test once and know nothing about the delays; this expands each into one test
	 * per delay, named {@code <test>_delay_<ticks>}.
	 * <p>
	 * Each delay gets its own batch because gamerules are per level and tests sharing a batch run
	 * at the same time — so tests wanting different delays must not run together. Batches run one
	 * after another, which makes every test within one agree on the value it sets.
	 *
	 * @param registrar sink to receive each test
	 */
	public static void collect(TestRegistrar registrar) {
		TestRegistrar perDelay = (name, options, body) -> {
			for (int delay : TRANSFER_DELAYS) {
				String scopedName = name + "_delay_" + delay;

				// A suite that named a batch keeps its own group; everything else shares the default
				// one. Separation matters for more than tidiness: tests in a batch run side by side,
				// and the runner's structure cleanup culls entities within a block of a test's
				// bounds. A neighbour finishing next to an arena only one block wide can therefore
				// reach in and kill entities belonging to a test still running — which is exactly
				// what happened to the stacked minecarts, on 1.20.1-forge, where the arenas happen
				// to be packed closest.
				String group = TestOptions.DEFAULT_BATCH.equals(options.batch())
						? "transfer_delay"
						: options.batch();

				TestOptions scoped = options
						.withBatch(group + "_" + delay)
						.withTransferDelay(delay);

				// A timed test sizes its own budget from what it expects to take. The extra margin
				// is so an overrun is reported by the duration check, which can say by how much,
				// rather than by the runner's bare timeout.
				if (scoped.isTimed()) {
					scoped = scoped.withMaxTicks(
							scoped.expectedTicks(delay)
									+ scoped.toleranceTicks(delay)
									+ scoped.settleTicks(delay)
									+ TIMEOUT_MARGIN
					);
				}

				TestOptions forRun = scoped;
				registrar.add(scopedName, scoped, helper -> {
					FastHoppersGameRules.setTransferDelay(helper.getLevel(), delay, helper.getLevel().getServer());
					TestTiming.beginning(scopedName, forRun);
					body.accept(helper);
				});
			}
		};
		SUITES.forEach(suite -> suite.accept(perDelay));
	}

	//? < 1.21.11 {
	/**
	 * Builds this mod's tests for the pre-1.21.11 GameTest framework.
	 * <p>
	 * Invoked reflectively on a fresh instance by {@code GameTestRegistry}; see the class Javadoc
	 * for why generation is used in place of {@code @GameTest}.
	 *
	 * @return every test this mod contributes
	 */
	@GameTestGenerator
	public Collection<TestFunction> generateTests() {
		List<TestFunction> functions = new ArrayList<>();
		collect((name, options, body) -> functions.add(new TestFunction(
				options.batch(),
				name,
				options.structure(),
				options.rotation(),
				options.maxTicks(),
				options.setupTicks(),
				options.required(),
				body
		)));
		return functions;
	}
	//?}

	//? >= 1.21.11 {
	/*/^*
	 * Contributes this mod's test functions to the {@code minecraft:test_function} registry.
	 * <p>
	 * From 1.21.11 that registry is one of the {@code BuiltInRegistries}, bootstrapped by
	 * {@code BuiltinTestFunctions} during {@code Bootstrap.bootStrap()} and frozen immediately
	 * after — which is long before any mod is constructed. {@code TestFunctionLoader.registerLoader}
	 * is therefore unusable from mod init: its loader list has already been drained. Each loader
	 * instead exposes its own hook that runs while the registry is still open, so this method is
	 * called from platform code rather than from {@link #register()}.
	 * <p>
	 * Only the body is registered here. Structure, tick budget and the rest live in the matching
	 * {@code test_instance} datapack entry, so {@code options} is deliberately unused.
	 *
	 * @param register sink accepting the function's id and the function itself
	 ^/
	public static void registerFunctions(BiConsumer<ResourceLocation, Consumer<GameTestHelper>> register) {
		collect((name, options, body) -> register.accept(FastHoppers.id(name), body));
	}
	*///?}

	/**
	 * Registers every test with the game.
	 * <p>
	 * Called unconditionally from {@link FastHoppers#onInitialize()} rather than only in
	 * development. On 1.21.11+ the {@code test_instance} datapack entries ship in the jar and are
	 * parsed on every world load, so the functions they reference must exist in production too or
	 * datapack loading fails.
	 * <p>
	 * From 1.21.11 this does nothing: registration happens through {@code registerFunctions} at
	 * registry-bootstrap time, which is far earlier than mod initialization. The note lives here
	 * rather than in the method body because Stonecutter strips the leading {@code //} from line
	 * comments inside a block it is enabling, turning them into syntax errors.
	 */
	public static void register() {
		//? < 1.21.11 {
		GameTestRegistry.register(FastHoppersGameTests.class);
		//?}
	}
}
