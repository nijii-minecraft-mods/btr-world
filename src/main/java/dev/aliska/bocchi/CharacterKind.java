package dev.aliska.bocchi;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * Героини вселенной Bocchi the Rock: идентификатор, имя и любимое лакомство для приручения/кормления.
 */
public enum CharacterKind {
    BOCCHI("bocchi", "Хитори", Items.COOKIE),
    NIJIKA("nijika", "Нидзика", Items.CAKE),
    RYO("ryo", "Рё", Items.WHEAT),
    KITA("kita", "Кита", Items.GLOW_BERRIES),
    SEIKA("seika", "Сейка", Items.SWEET_BERRIES),
    KIKURI("kikuri", "Кикури", Items.HONEY_BOTTLE),
    PA_SAN("pa_san", "ПА-сан", Items.DRIED_KELP);

    public final String id;
    public final String name;
    public final Item treat;

    CharacterKind(String id, String name, Item treat) {
        this.id = id;
        this.name = name;
        this.treat = treat;
    }

    /**
     * Возвращает true для четырёх основных участниц квартета Kessoku Band.
     */
    public boolean isKessokuBand() {
        return this == BOCCHI || this == NIJIKA || this == RYO || this == KITA;
    }
}
