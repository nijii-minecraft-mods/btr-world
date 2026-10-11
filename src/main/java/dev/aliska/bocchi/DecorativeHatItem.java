package dev.aliska.bocchi;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Декоративные головные уборы (ахоге Нидзики, коробка манго).
 * Надеваются в слот головы аналогично тыкве.
 */
public final class DecorativeHatItem extends ModelItem implements Equipment {
    public DecorativeHatItem(String model) {
        super(model, new Item.Settings().maxCount(1), Items.CARVED_PUMPKIN);
    }

    @Override
    public EquipmentSlot getSlotType() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        return equipAndSwap(this, world, player, hand);
    }
}
