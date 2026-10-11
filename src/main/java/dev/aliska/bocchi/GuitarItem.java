package dev.aliska.bocchi;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Гитара: при использовании отбрасывает враждебных мобов в радиусе 4 блоков
 * и проигрывает гитарный аккорд с перезарядкой 5 секунд.
 */
public class GuitarItem extends ModelItem {
    public GuitarItem() {
        super("guitar", new Item.Settings().maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (!world.isClient && !player.getItemCooldownManager().isCoolingDown(this)) {
            boolean hit = false;
            var hostiles = world.getEntitiesByClass(
                HostileEntity.class,
                player.getBoundingBox().expand(4),
                Entity::isAlive
            );

            for (HostileEntity mob : hostiles) {
                hit = true;
                Vec3d direction = mob.getPos().subtract(player.getPos()).normalize();
                mob.addVelocity(direction.x * 0.8, 0.25, direction.z * 0.8);
                mob.velocityModified = true;
            }

            if (hit) {
                BtrAdvancements.grant(player, "power_chord");
            }

            world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_NOTE_BLOCK_GUITAR.value(),
                SoundCategory.PLAYERS,
                0.8F,
                1.2F
            );

            ((ServerWorld) world).spawnParticles(
                ParticleTypes.NOTE,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                8,
                1.0,
                0.2,
                1.0,
                0.0
            );

            player.getItemCooldownManager().set(this, 100);
        }

        return TypedActionResult.success(player.getStackInHand(hand));
    }
}
