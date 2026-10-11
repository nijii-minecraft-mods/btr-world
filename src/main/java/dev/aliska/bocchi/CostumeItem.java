package dev.aliska.bocchi;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Декоративные костюмы и одежда (костюм Боччи, школьные формы, форма персонала STARRY, костюм горничной).
 * На базе кожаной брони с настраиваемым цветом красителя. Не дают боевых характеристик.
 */
public class CostumeItem extends Item implements PolymerItem, Equipment {
    private final EquipmentSlot slot;
    private final Item appearance;

    public CostumeItem(EquipmentSlot slot, Item appearance, int color) {
        super(new Item.Settings()
            .maxCount(1)
            .component(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color, false))
            .component(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT));
        this.slot = slot;
        this.appearance = appearance;
    }

    public CostumeItem(EquipmentSlot slot, Item appearance) {
        this(slot, appearance, 0xefa1c4);
    }

    @Override
    public EquipmentSlot getSlotType() {
        return slot;
    }

    @Override
    public Item getPolymerItem(ItemStack stack, ServerPlayerEntity player) {
        return appearance;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        return equipAndSwap(this, world, player, hand);
    }
}
