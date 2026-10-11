package dev.aliska.bocchi;

import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Предмет блока для декоративных блоков палитры и фигурок.
 */
public class PaletteItem extends PolymerBlockItem {
    private final int model;

    public PaletteItem(String id, Block block) {
        super(block, new Item.Settings(), Items.PAPER);
        this.model = PolymerResourcePackUtils.requestModel(
            Items.PAPER,
            Identifier.of("bocchi", "item/" + id)
        ).value();
    }

    @Override
    public int getPolymerCustomModelData(ItemStack stack, ServerPlayerEntity player) {
        return model;
    }
}
