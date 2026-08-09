package gay.bagel.fasthoppers.mixin;

import gay.bagel.fasthoppers.FastHoppersGameRules;
import gay.bagel.fasthoppers.TransferDelay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies {@code hopperTransferDelay} by rewriting the cooldown vanilla is
 * about to store.
 *
 * <p>
 * This is deliberately the mod's only hook into hopper blocks. Transfer
 * logic is left completely untouched, so mods that inject inside
 * {@code ejectItems} or {@code suckInItems} — Copper Hopper's filter veto, for
 * one — keep working exactly as they would unmodded.
 *
 * <p>
 * Targeting the setter rather than its call sites means every path is
 * covered with one injection, including other mods' calls: a mod that sets a
 * hopper cooldown gets the operator's configured speed instead of a hardcoded
 * 8.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperEntityMixin {

	@ModifyVariable(method = "setCooldown(I)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int fasthoppers$applyTransferDelay(int vanillaValue) {
		if (vanillaValue <= 0 || TransferDelay.DISABLED) {
			return vanillaValue;
		}
		Level level = ((BlockEntity) (Object) this).getLevel();
		if (level == null || level.isClientSide()) {
			return vanillaValue;
		}
		int gameruleValue = FastHoppersGameRules.transferDelay(level);
		return TransferDelay.apply(vanillaValue, gameruleValue);
	}


	@Inject(method = "pushItemsTick", at = @At("HEAD"))
	private static void fasthoppers$clampStaleCooldown(
			Level level,
			BlockPos blockPos,
			BlockState blockState,
			HopperBlockEntity hopper,
			CallbackInfo ci) {
		if (TransferDelay.DISABLED) {
			return;
		}
		HopperCooldownAccessor accessor = (HopperCooldownAccessor) hopper;
		int remaining = accessor.fasthoppers$getCooldownTime();
		if (remaining <= 0) {
			return;
		}
		int gameruleValue = FastHoppersGameRules.transferDelay(level);
		if (remaining > gameruleValue) {
			accessor.fasthoppers$setCooldownTime(Math.max(0, gameruleValue));
		}
	}
}
