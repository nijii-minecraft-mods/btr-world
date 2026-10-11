package dev.aliska.bocchi;

import dev.aliska.bocchi.mixin.DisplayAccessor;
import dev.aliska.bocchi.mixin.ItemDisplayAccessor;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registries;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Валидация состояния блоков, моделей, NBT-структур и сущностей мода в среде разработки/CI
 * перед этапом проверки EULA и загрузки игрового мира.
 */
final class VisualValidation {
    private static final Logger LOGGER = LoggerFactory.getLogger("bocchi");

    private VisualValidation() {}

    static void run() {
        // 1. Проверка всех состояний блоков Polymer и инициализации кэшей коллизий
        int checkedStates = 0;
        for (var entry : BocchiMod.BLOCKS.entrySet()) {
            for (var state : entry.getValue().getStateManager().getStates()) {
                try {
                    state.initShapeCache();
                    if (((PolymerTexturedBlock) entry.getValue()).getPolymerBlockState(state) == null) {
                        throw new IllegalStateException("Null client state");
                    }
                    checkedStates++;
                } catch (Exception error) {
                    throw new IllegalStateException("Invalid Polymer mapping: bocchi:" + entry.getKey() + " " + state, error);
                }
            }
        }
        LOGGER.info("Validated {} Polymer block states including collision caches.", checkedStates);

        // 2. Проверка количества моделей (14 нейтральных + 56 эмоций)
        if (BocchiMod.MODELS.size() != 70) {
            throw new IllegalStateException("Expected 14 neutral and 56 expression models");
        }

        var fakeWorld = PolymerCommonUtils.getFakeWorld();

        // 3. Проверка стойки-сиденья в коробке
        var seat = new ArmorStandEntity(EntityType.ARMOR_STAND, fakeWorld);
        BoxSeats.configure(seat);
        if (!seat.isMarker() || !seat.isInvisible() || !seat.hasNoGravity() || !seat.isInvulnerable()) {
            throw new IllegalStateException("Box seat configuration failed");
        }

        // 4. Проверка трансформации отображения и компенсации крепления пассажира
        var display = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, fakeWorld);
        var wolf = new WolfEntity(EntityType.WOLF, fakeWorld);
        float offset = (float) (wolf.getPassengerRidingPos(display).y - wolf.getY());
        if (offset <= 0 || offset > 2) {
            throw new IllegalStateException("Unexpected wolf mount height " + offset);
        }

        ((DisplayAccessor) display).bocchi$setTransformation(new AffineTransformation(
            new Vector3f(0, -offset, 0),
            new Quaternionf(),
            new Vector3f(1, 1, 1),
            new Quaternionf()
        ));
        ((ItemDisplayAccessor) display).bocchi$setMode(ModelTransformationMode.NONE);

        for (var item : BocchiMod.MODELS.values()) {
            ((ItemDisplayAccessor) display).bocchi$setItemStack(new ItemStack(item));
            if (!((ItemDisplayAccessor) display).bocchi$getItemStack().isOf(item)) {
                throw new IllegalStateException("Display expression swap failed");
            }
        }

        var nbt = new NbtCompound();
        display.writeNbt(nbt);
        float saved = nbt.getCompound("transformation").getList("translation", NbtElement.FLOAT_TYPE).getFloat(1);
        if (Math.abs(offset + saved) > 0.00001) {
            throw new IllegalStateException("Mount correction is not synchronized/saved");
        }

        // 5. Проверка сохранения и загрузки семейных NBT-тегов спутницы
        var bocchiType = BocchiMod.KINDS.keySet().stream()
            .filter(t -> BocchiMod.KINDS.get(t) == CharacterKind.BOCCHI)
            .findFirst()
            .orElseThrow();
        var probe = (CompanionEntity) bocchiType.create(fakeWorld);
        var familyNbt = new NbtCompound();
        probe.writeCustomDataToNbt(familyNbt);
        familyNbt.putIntArray("BtrGenes", new int[]{1, 0, 3});
        familyNbt.putInt("BtrGrowth", 1200);
        familyNbt.putInt("BtrBreedCooldown", 4000);
        probe.readCustomDataFromNbt(familyNbt);

        var savedFamily = new NbtCompound();
        probe.writeCustomDataToNbt(savedFamily);
        if (!Arrays.equals(savedFamily.getIntArray("BtrGenes"), new int[]{1, 0, 3})
            || savedFamily.getInt("BtrGrowth") != 1200
            || savedFamily.getInt("BtrBreedCooldown") != 4000) {
            throw new IllegalStateException("Family data did not survive NBT round trip");
        }

        // 6. Проверка NBT-шаблонов статуй героинь
        try {
            var mod = FabricLoader.getInstance().getModContainer("bocchi").orElseThrow();
            for (var kind : CharacterKind.values()) {
                var path = mod.findPath("data/bocchi/structure/statues/" + kind.id + ".nbt").orElseThrow();
                var templateNbt = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());
                for (var tag : templateNbt.getList("palette", NbtElement.COMPOUND_TYPE)) {
                    var name = Identifier.of(((NbtCompound) tag).getString("Name"));
                    if (!Registries.BLOCK.containsId(name)) {
                        throw new IllegalStateException("Unknown statue block " + name);
                    }
                }
                var template = new StructureTemplate();
                template.readNbt(Registries.BLOCK.getReadOnlyWrapper(), templateNbt);
                if (template.getSize().getY() != 18 || templateNbt.getList("blocks", NbtElement.COMPOUND_TYPE).size() < 500) {
                    throw new IllegalStateException("Invalid statue template " + kind.id);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Statue validation failed", e);
        }

        // 7. Проверка миграции старых русских авто-имён и сохранения сценических тегов
        var legacy = new NbtCompound();
        probe.writeNbt(legacy);
        legacy.putString("CustomName", "{\"text\":\"Хитори · чиби\"}");
        probe.readNbt(legacy);
        if (probe.hasCustomName()) {
            throw new IllegalStateException("Automatic legacy name was not migrated");
        }

        legacy.putString("CustomName", "{\"text\":\"Алискина подруга\"}");
        legacy.putBoolean("BtrStagePerformer", true);
        legacy.putIntArray("BtrStageHome", new int[]{4, 65, 9});
        probe.readNbt(legacy);

        var stageSave = new NbtCompound();
        probe.writeNbt(stageSave);
        if (!probe.getName().getString().equals("Алискина подруга")
            || !stageSave.getBoolean("BtrStagePerformer")
            || !Arrays.equals(stageSave.getIntArray("BtrStageHome"), new int[]{4, 65, 9})) {
            throw new IllegalStateException("Stage/name persistence failed: name=" + probe.getName().getString()
                + ", stage=" + stageSave.getBoolean("BtrStagePerformer")
                + ", home=" + Arrays.toString(stageSave.getIntArray("BtrStageHome")));
        }

        // 8. Проверка 9 сценических NBT-структур (STARRY, школа, клуб × 3 типа популяции)
        try {
            var mod = FabricLoader.getInstance().getModContainer("bocchi").orElseThrow();
            for (String style : new String[]{"starry", "school", "indie"}) {
                for (String population : new String[]{"chibi", "tsum", "mixed"}) {
                    var path = mod.findPath("data/bocchi/structure/stages/" + style + "_" + population + ".nbt").orElseThrow();
                    var stage = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());

                    for (var tag : stage.getList("palette", NbtElement.COMPOUND_TYPE)) {
                        var stateNbt = (NbtCompound) tag;
                        var id = Identifier.of(stateNbt.getString("Name"));
                        if (!Registries.BLOCK.containsId(id)) {
                            throw new IllegalStateException("Unknown stage block " + id);
                        }
                        var block = Registries.BLOCK.get(id);
                        var properties = stateNbt.getCompound("Properties");
                        for (String key : properties.getKeys()) {
                            var property = block.getStateManager().getProperty(key);
                            if (property == null || property.parse(properties.getString(key)).isEmpty()) {
                                throw new IllegalStateException("Invalid stage block property " + id + " " + key);
                            }
                        }
                    }

                    var template = new StructureTemplate();
                    template.readNbt(Registries.BLOCK.getReadOnlyWrapper(), stage);

                    if (stage.getList("entities", NbtElement.COMPOUND_TYPE).size() != 4) {
                        throw new IllegalStateException("Stage needs four live performers");
                    }

                    for (var tag : stage.getList("entities", NbtElement.COMPOUND_TYPE)) {
                        var mob = ((NbtCompound) tag).getCompound("nbt");
                        var id = Identifier.of(mob.getString("id"));
                        if (!Registries.ENTITY_TYPE.containsId(id)) {
                            throw new IllegalStateException("Unknown stage mob " + id);
                        }
                        var actor = Registries.ENTITY_TYPE.get(id).create(fakeWorld);
                        if (!(actor instanceof CompanionEntity girl)) {
                            throw new IllegalStateException("Invalid stage mob " + id);
                        }
                        girl.readCustomDataFromNbt(mob);
                        var savedActor = new NbtCompound();
                        girl.writeCustomDataToNbt(savedActor);
                        if (!savedActor.getBoolean("BtrStagePerformer") || girl.hasCustomName()) {
                            throw new IllegalStateException("Invalid performer NBT " + id);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Stage validation failed", e);
        }

        LOGGER.info("Validated 9 stage templates, 36 live performers and legacy name migration.");
        LOGGER.info("Visual validation passed: 70 models, display invokers, mount correction {} blocks.", offset);
    }
}
