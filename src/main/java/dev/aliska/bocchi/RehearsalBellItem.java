package dev.aliska.bocchi;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Репетиционный колокольчик: будит и поднимает всех сидящих приручённых спутниц владельца
 * в радиусе 24 блоков, прерывая режим ожидания.
 */
public final class RehearsalBellItem extends ModelItem {
    public RehearsalBellItem() {
        super("rehearsal_bell", new Item.Settings().maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (!world.isClient && !player.getItemCooldownManager().isCoolingDown(this)) {
            var girls = world.getEntitiesByClass(
                CompanionEntity.class,
                player.getBoundingBox().expand(24),
                girl -> girl.isAlive() && girl.isOwner(player) && girl.squaredDistanceTo(player) <= 576
            );

            for (var girl : girls) {
                girl.recall();
            }

            if (!girls.isEmpty()) {
                BtrAdvancements.grant(player, "rehearsal");
            }

            world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_BELL_USE,
                SoundCategory.PLAYERS,
                0.5F,
                1.3F
            );

            player.getItemCooldownManager().set(this, 60);
        }

        return TypedActionResult.success(player.getStackInHand(hand));
    }
}
