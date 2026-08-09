package gay.bagel.fasthoppers.mixin;

import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Direct access to a hopper's cooldown field.
 *
 * <p>Needed because writing the field through {@code setCooldown} would be
 * intercepted by {@link HopperEntityMixin} and remapped a second time — a
 * clamp to N would come back out as {@code apply(N, N)}. Clamping has to bypass
 * our own hook and write the field.
 */
@Mixin(HopperBlockEntity.class)
public interface HopperCooldownAccessor {

    /**
     * @return ticks remaining before this hopper may act again
     */
    @Accessor("cooldownTime")
    int fasthoppers$getCooldownTime();

    /**
     * Writes the cooldown without passing through {@code setCooldown}.
     *
     * @param ticks the cooldown to store verbatim
     */
    @Accessor("cooldownTime")
    void fasthoppers$setCooldownTime(int ticks);
}
