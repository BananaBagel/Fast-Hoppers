package com.banana.fasthoppers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.block.entity.HopperBlockEntity;

@Mixin(HopperBlockEntity.class)
public interface HopperInterface {
    @Accessor
    int getTransferCooldown();

    @Accessor("transferCooldown")
    public void setTransferCooldownAccessor(int cooldown);
}
