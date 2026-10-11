package dev.aliska.bocchi;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/**
 * Знаменитый напиток Кикури в тетрапаке (сок / саке куража «Онигороси»).
 * Пьётся, даёт бафф Силы II и кратковременное головокружение.
 */
public final class KikuriDrinkItem extends Item implements PolymerItem {
    private final int model;

    public KikuriDrinkItem() {
        super(new Item.Settings()
            .maxCount(16)
            .food(new FoodComponent.Builder()
                .nutrition(4)
                .saturationModifier(0.6F)
                .alwaysEdible()
                .build()));
        this.model = PolymerResourcePackUtils.requestModel(
            Items.POTION,
            BocchiMod.id("item/kikuri_drink")
        ).value();
    }

    @Override
    public Item getPolymerItem(ItemStack stack, ServerPlayerEntity player) {
        return Items.POTION;
    }

    @Override
    public int getPolymerCustomModelData(ItemStack stack, ServerPlayerEntity player) {
        return model;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public SoundEvent getDrinkSound() {
        return SoundEvents.ENTITY_GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatSound() {
        return SoundEvents.ENTITY_GENERIC_DRINK;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack result = super.finishUsing(stack, world, user);
        if (!world.isClient()) {
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 600, 1));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 140, 0));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 400, 0));
        }
        if (user instanceof PlayerEntity player && !player.getAbilities().creativeMode) {
            ItemStack carton = new ItemStack(BocchiMod.EMPTY_PACK != null ? BocchiMod.EMPTY_PACK : Items.PAPER);
            if (!player.giveItemStack(carton)) {
                player.dropItem(carton, false);
            }
        }
        return result;
    }
}
