package gay.bagel.fasthoppers.mixin;

import gay.bagel.fasthoppers.FastHoppersGameRules;
import gay.bagel.fasthoppers.TransferDelay;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? >= 1.21.11 {
/*import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
*///?} < 1.21.11 {
import net.minecraft.world.entity.vehicle.MinecartHopper;
//?}

//? < 1.20.1 {
/*import org.spongepowered.asm.mixin.injection.ModifyVariable;
*///?}

/**
 * Applies {@code hopperMinecartTransferDelay} to hopper minecarts.
 *
 * <p>
 * The delay is tracked here rather than borrowed from vanilla, on every version, because from
 * 1.20 there is nothing left to borrow: the cooldown {@code MinecartHopper} used to carry was
 * deleted when the pickup rate was fixed at one item per tick, and {@code tick} now calls
 * {@code suckInItems} unconditionally. Counting here is the only mechanism that works across
 * the whole supported range, so it is the only one used.
 *
 * <p>
 * 1.19.2 still has that cooldown, and it is actively unhelpful. Vanilla sets it to 4 after a
 * pickup but resets it to 0 whenever the cart changes block position — the "sometimes 4 ticks,
 * sometimes 1" behaviour of the era, and the bug whose fix removed the field. Left alone it
 * would both impose a floor of 4 ticks on a stationary cart and ignore the gamerule entirely on
 * a moving one, which is the common case. So on 1.19.2 the cooldown is flattened to zero and
 * vanilla's scheduling is taken out of the picture, leaving the gamerule authoritative and the
 * behaviour identical to every later version.
 *
 * <p>
 * Transfer logic itself is untouched everywhere. The injections only decide <em>whether</em>
 * {@code suckInItems} runs on a given tick, never what it does, so mods hooking inside it keep
 * behaving as they would unmodded. Gating there rather than in {@code tick} also leaves the
 * cart's movement and every other per-tick behaviour alone.
 */
@Mixin(MinecartHopper.class)
public abstract class HopperMinecartMixin {

	/**
	 * Ticks left before this minecart may pick up again.
	 *
	 * <p>
	 * Held per entity because each cart counts down independently, and left unsaved because a
	 * cooldown of at most a few ticks is not worth persisting: a cart reloaded mid-delay simply
	 * becomes eligible immediately.
	 */
	@Unique
	private int fasthoppers$transferCooldown;

	/**
	 * The level this minecart is in.
	 *
	 * <p>
	 * Isolates the one divergence in this file that is not about cooldowns: 1.19.2 exposes a
	 * {@code level} field, later versions a {@code level()} accessor.
	 *
	 * @return the cart's level
	 */
	@Unique
	private Level fasthoppers$level() {
		//? >= 1.20.1 {
		return ((Entity) (Object) this).level();
		//?} < 1.20.1 {
		/*return ((Entity) (Object) this).level;
		*///?}
	}

	/**
	 * Refuses a pickup while the configured delay is still counting down.
	 *
	 * @param cir callback used to report "picked nothing up" without running vanilla's logic
	 */
	@Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
	private void fasthoppers$holdOffTransfer(CallbackInfoReturnable<Boolean> cir) {
		if (TransferDelay.DISABLED || fasthoppers$transferCooldown <= 0) {
			return;
		}
		fasthoppers$transferCooldown--;
		cir.setReturnValue(false);
	}

	/**
	 * Restarts the delay after a pickup actually happened.
	 *
	 * <p>
	 * Only a successful pickup starts the clock, so an empty cart stays responsive instead of
	 * idling through a delay it never earned.
	 *
	 * @param cir callback carrying whether vanilla moved an item
	 */
	@Inject(method = "suckInItems", at = @At("RETURN"))
	private void fasthoppers$beginCooldown(CallbackInfoReturnable<Boolean> cir) {
		if (TransferDelay.DISABLED || !cir.getReturnValueZ()) {
			return;
		}
		Level level = fasthoppers$level();
		if (level.isClientSide()) {
			return;
		}
		// Minus one because the tick that just transferred is itself part of the delay: a
		// gamerule of 1 has to mean "every tick", which is a cooldown of zero.
		fasthoppers$transferCooldown = Math.max(0, FastHoppersGameRules.minecartTransferDelay(level) - 1);
	}

	//? < 1.20.1 {
	/*/^*
	 * Flattens vanilla's own cooldown so it never competes with the gamerule.
	 *
	 * <p>
	 * Zero rather than the configured delay: this is not where the delay is applied, it is
	 * where vanilla's scheduling is switched off. With the cooldown always clear, 1.19.2 calls
	 * {@code suckInItems} every tick exactly as later versions do, and the gate above is the
	 * only thing deciding when a pickup happens — including for a moving cart, which vanilla
	 * would otherwise let pick up every tick regardless of the rule.
	 *
	 * @param vanillaValue the cooldown vanilla chose, ignored unless the mod is disabled
	 * @return zero, or {@code vanillaValue} when the mod is switched off
	 ^/
	@ModifyVariable(method = "setCooldown(I)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int fasthoppers$neutraliseVanillaCooldown(int vanillaValue) {
		return TransferDelay.DISABLED ? vanillaValue : 0;
	}
	*///?}
}
