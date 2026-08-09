package gay.bagel.fasthoppers.test;

import net.minecraft.gametest.framework.GameTestHelper;

import java.util.function.Consumer;

/**
 * Sink a test suite hands its tests to.
 * <p>
 * This is the seam that lets a suite be written once and work on every version. Suites call
 * {@link #add} and never learn what happens next; the caller supplies an implementation
 * appropriate to what it is doing:
 * <ul>
 *   <li><strong>&lt; 1.21.11 runtime</strong> — builds {@code TestFunction}s for
 *       {@code GameTestRegistry}.</li>
 *   <li><strong>&gt;= 1.21.11 runtime</strong> — registers the body into
 *       {@code Registries.TEST_FUNCTION}, ignoring the options, which belong to the datapack
 *       entry rather than to the function.</li>
 *   <li><strong>Build time</strong> — writes the {@code test_instance} datapack entries the
 *       1.21.11+ runtime expects, reading the options the runtime ignored.</li>
 * </ul>
 * Because every consumer walks the same suite list, a test cannot exist for one of them and not
 * the others.
 * <p>
 * Version-specific tests are handled with Stonecutter comments inside the suite's contribute
 * method. Gating at compile time rather than at runtime means the suite list compiled for a given
 * version already contains exactly the tests valid there, so generated datapack entries stay
 * correct without evaluating any conditions a second time.
 */
@FunctionalInterface
public interface TestRegistrar {

	/**
	 * Adds a test with explicit settings.
	 *
	 * @param name    test id, unique within the mod, used as the path of its namespaced id
	 * @param options structure, tick budget and placement settings
	 * @param body    the test itself, run against a fresh arena
	 */
	void add(String name, TestOptions options, Consumer<GameTestHelper> body);

	/**
	 * Adds a test using {@link TestOptions#of() default settings} — the shared empty arena and a
	 * 100 tick budget.
	 *
	 * @param name test id, unique within the mod
	 * @param body the test itself, run against a fresh arena
	 */
	default void add(String name, Consumer<GameTestHelper> body) {
		add(name, TestOptions.of(), body);
	}
}
