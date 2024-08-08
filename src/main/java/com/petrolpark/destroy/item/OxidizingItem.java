package com.petrolpark.destroy.item;

import java.util.function.Supplier;

import com.simibubi.create.foundation.config.ConfigBase.ConfigInt;

import net.minecraft.world.item.ItemStack;

public class OxidizingItem extends ConfiguredDecayingItem {

    public OxidizingItem(Properties properties, Supplier<ItemStack> decayProduct, Supplier<ConfigInt> lifetime) {
        super(properties, decayProduct, lifetime);
    };
    
};
