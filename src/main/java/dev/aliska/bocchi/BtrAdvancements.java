package dev.aliska.bocchi;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Утилиты для выдачи достижений BTR World игрокам.
 */
public final class BtrAdvancements {
    private BtrAdvancements() {}

    /**
     * Выдаёт достижение со стандартным критерием "done".
     */
    public static void grant(PlayerEntity player, String id) {
        grant(player, id, "done");
    }

    /**
     * Выдаёт указанный критерий достижения и гарантирует открытие корневого достижения.
     */
    public static void grant(PlayerEntity player, String id, String criterion) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            var loader = serverPlayer.getServer().getAdvancementLoader();
            var root = loader.get(BocchiMod.id("root"));
            if (root != null) {
                serverPlayer.getAdvancementTracker().grantCriterion(root, "done");
            }
            var entry = loader.get(BocchiMod.id(id));
            if (entry != null) {
                serverPlayer.getAdvancementTracker().grantCriterion(entry, criterion);
            }
        }
    }
}
