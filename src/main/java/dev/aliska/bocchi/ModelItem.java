package dev.aliska.bocchi;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Базовый предмет с серверной кастомной моделью Polymer на основе ванильного предмета
 * (по умолчанию используется бумага с CustomModelData).
 */
public class ModelItem extends Item implements PolymerItem {
    private final int model;
    private final Item appearance;

    public ModelItem(String id, Settings settings) {
        this(id, settings, Items.PAPER);
    }

    public ModelItem(String id, Settings settings, Item appearance) {
        super(settings);
        this.appearance = appearance;
        this.model = PolymerResourcePackUtils.requestModel(
            appearance,
            Identifier.of("bocchi", "item/" + id)
        ).value();
    }

    @Override
    public Item getPolymerItem(ItemStack stack, ServerPlayerEntity player) {
        return appearance;
    }

    @Override
    public int getPolymerCustomModelData(ItemStack stack, ServerPlayerEntity player) {
        return model;
    }
}
