package dev.aliska.bocchi;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Фирменный медиатор Kessoku Band.
 * При использовании проигрывает гитарные аккорды с нотными частицами.
 */
public final class GuitarPickItem extends Item implements PolymerItem {
    private final int model;

    public GuitarPickItem() {
        super(new Item.Settings().maxCount(16));
        this.model = PolymerResourcePackUtils.requestModel(
            Items.PAPER,
            BocchiMod.id("item/guitar_pick")
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
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.isClient()) {
            float[] pitches = {0.75F, 0.9F, 1.0F, 1.2F, 1.35F, 1.5F};
            float pitch = pitches[player.getRandom().nextInt(pitches.length)];
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_GUITAR.value(), SoundCategory.PLAYERS, 0.9F, pitch);
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.NOTE, player.getX(), player.getY() + 1.2, player.getZ(), 2, 0.3, 0.3, 0.3, pitch / 24.0);
            }
            player.getItemCooldownManager().set(this, 10);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        var pos = context.getBlockPos();
        var block = world.getBlockState(pos).getBlock();
        if (block == BocchiMod.BLOCKS.get("guitar_stand")) {
            if (!world.isClient()) {
                world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_GUITAR.value(), SoundCategory.BLOCKS, 1.2F, 1.0F);
                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.0);
                }
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}
