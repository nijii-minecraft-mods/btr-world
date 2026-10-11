package dev.aliska.bocchi;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Памятный билет на концерт Kessoku Band в лайвхаусе STARRY.
 * При использовании проигрывает фанфары и запускает праздничные искры.
 */
public final class StarryTicketItem extends Item implements PolymerItem {
    private final int model;

    public StarryTicketItem() {
        super(new Item.Settings().maxCount(16));
        this.model = PolymerResourcePackUtils.requestModel(
            Items.PAPER,
            BocchiMod.id("item/starry_ticket")
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
            world.playSound(null, player.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.7F, 1.2F);
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 1.2, player.getZ(), 8, 0.4, 0.4, 0.4, 0.1);
            }
            player.sendMessage(Text.translatable("message.bocchi.ticket.inspect"), true);
            player.getItemCooldownManager().set(this, 20);
        }
        return TypedActionResult.success(stack, world.isClient());
    }
}
