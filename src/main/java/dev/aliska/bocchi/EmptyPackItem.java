package dev.aliska.bocchi;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Пустой смятый тетрапак от они-напитка.
 * Можно бросить как снежок или переработать на бумагу.
 */
public final class EmptyPackItem extends Item implements PolymerItem {
    private final int model;

    public EmptyPackItem() {
        super(new Item.Settings().maxCount(16));
        this.model = PolymerResourcePackUtils.requestModel(
            Items.PAPER,
            BocchiMod.id("item/empty_pack")
        ).value();
    }

    @Override
    public Item getPolymerItem(ItemStack stack, ServerPlayerEntity player) {
        return Items.PAPER;
    }

    @Override
    public int getPolymerCustomModelData(ItemStack stack, ServerPlayerEntity player) {
        return model;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
            SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!world.isClient()) {
            SnowballEntity snowballEntity = new SnowballEntity(world, user);
            snowballEntity.setItem(new ItemStack(Items.PAPER));
            snowballEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.2F, 1.0F);
            world.spawnEntity(snowballEntity);
        }
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        if (!user.getAbilities().creativeMode) {
            itemStack.decrement(1);
        }
        return TypedActionResult.success(itemStack, world.isClient());
    }
}
