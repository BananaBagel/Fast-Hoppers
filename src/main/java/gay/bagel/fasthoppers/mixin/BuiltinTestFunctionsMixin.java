package gay.bagel.fasthoppers.mixin;

//? fabric && >= 1.21.11 {

/*import gay.bagel.fasthoppers.test.FastHoppersGameTests;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunctionLoader;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/^*
 * Gets this mod's gametests into the {@code minecraft:test_function} registry on Fabric.
 *
 * <p>
 * From 1.21.11 that registry is a {@code BuiltInRegistries} entry populated exactly once, when
 * {@code BuiltinTestFunctions.bootstrap} drains {@code TestFunctionLoader}'s loader list during
 * {@code Bootstrap.bootStrap()}. That happens well before any mod initializer runs, so calling
 * {@code registerLoader} from mod init is silently too late — the list has already been emptied
 * and the registry frozen.
 *
 * <p>
 * NeoForge solves this with {@code RegisterEvent}; Fabric API has no equivalent, having dropped
 * its server-side gametest module once the datapack-driven system landed. Injecting immediately
 * ahead of the drain is the remaining hook that is guaranteed to be early enough.
 *
 * <p>
 * Fabric-only: the NeoForge builds register through
 * {@code NeoforgeEntrypoint} instead, so this mixin is compiled out for them and fletching-table
 * leaves it out of the generated mixin config.
 ^/
@Mixin(BuiltinTestFunctions.class)
public class BuiltinTestFunctionsMixin {

	/^*
	 * Adds this mod's loader before vanilla runs them all.
	 *
	 * @param registry the registry being bootstrapped, unused — the loader receives its own sink
	 * @param cir      mixin callback, never cancelled
	 ^/
	@Inject(method = "bootstrap", at = @At("HEAD"))
	private static void fasthoppers$registerTestFunctions(
			Registry<Consumer<GameTestHelper>> registry,
			CallbackInfoReturnable<Consumer<GameTestHelper>> cir) {
		TestFunctionLoader.registerLoader(new TestFunctionLoader() {
			@Override
			public void load(BiConsumer<ResourceKey<Consumer<GameTestHelper>>, Consumer<GameTestHelper>> register) {
				FastHoppersGameTests.registerFunctions((id, function) ->
						register.accept(ResourceKey.create(Registries.TEST_FUNCTION, id), function));
			}
		});
	}
}
*///?}