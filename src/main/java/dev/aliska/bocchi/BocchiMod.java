package dev.aliska.bocchi;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Главный класс инициализации мода BTR World.
 * Регистрирует сущностей спутниц, блоки палитры, декор, предметы и серверную creative-вкладку Polymer.
 */
public class BocchiMod implements ModInitializer {
    public static final String MOD_ID = "bocchi";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Map<EntityType<?>, CharacterKind> KINDS = new HashMap<>();
    public static final Set<EntityType<?>> PLUSH_TYPES = new HashSet<>();
    public static final Map<String, Item> MODELS = new HashMap<>();
    public static final Map<CharacterKind, Item> FIGURINES = new EnumMap<>(CharacterKind.class);
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final List<Item> ITEMS = new ArrayList<>();
    public static Item BOX_MODEL;

    /**
     * Создаёт идентификатор с пространством имён мода "bocchi".
     */
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    /**
     * Регистрирует предмет в ванильном реестре и добавляет его в список творческой вкладки мода.
     */
    public static <T extends Item> T item(String path, T item) {
        Registry.register(Registries.ITEM, id(path), item);
        ITEMS.add(item);
        return item;
    }

    @Override
    public void onInitialize() {
        // Проверка устойчивости к исчерпанию пулов Polymer при запуске специальных тестов
        BlockVisuals.exhaustForTest();

        // Инициализация маркерных сидений в коробке
        BoxSeats.initialize();

        // Подключение ассетов мода к генератору серверного ресурс-пака Polymer
        PolymerResourcePackUtils.addModAssets(MOD_ID);
        PolymerResourcePackUtils.markAsRequired();

        // Служебная модель картонной коробки для режима пряток Боччи
        BOX_MODEL = Registry.register(
            Registries.ITEM,
            id("hiding_box"),
            new ModelItem("hiding_box", new Item.Settings())
        );

        // Регистрация 8 типов спутниц (4 героини × 2 формы), их выражений лиц и блоков палитры
        for (CharacterKind kind : CharacterKind.values()) {
            for (String form : List.of("plush", "chibi")) {
                String name = kind.id + "_" + form;

                // Базовая нейтральная модель
                MODELS.put(
                    name,
                    Registry.register(Registries.ITEM, id(name + "_model"), new ModelItem(name, new Item.Settings()))
                );

                // Модели выражений лиц (радость, сон, удивление, прищур)
                for (String mood : List.of("happy", "sleepy", "surprised", "squint")) {
                    String variant = name + "_" + mood;
                    MODELS.put(
                        variant,
                        Registry.register(Registries.ITEM, id(variant + "_model"), new ModelItem(variant, new Item.Settings()))
                    );
                }

                // Регистрация сущности спутницы
                EntityType<CompanionEntity> type = Registry.register(
                    Registries.ENTITY_TYPE,
                    id(name),
                    EntityType.Builder.create(CompanionEntity::new, SpawnGroup.CREATURE)
                        .dimensions(0.45F, 0.65F)
                        .maxTrackingRange(8)
                        .build(null)
                );

                KINDS.put(type, kind);
                if (form.equals("plush")) {
                    PLUSH_TYPES.add(type);
                }

                FabricDefaultAttributeRegistry.register(type, WolfEntity.createWolfAttributes());
                PolymerEntityUtils.registerType(type);

                // Токен призыва спутницы
                item(name + "_summoner", new CompanionSummoner(name + "_summoner", type));

                // Естественный спавн на равнинах, в лесах и лугах
                BiomeModifications.addSpawn(
                    BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.FOREST, BiomeKeys.MEADOW),
                    SpawnGroup.CREATURE,
                    type,
                    2,
                    1,
                    1
                );
            }

            // Блоки палитры: 3 оттенка (light, base, dark) × 3 фактуры (matte, tile, lamp)
            for (String shade : List.of("light", "base", "dark")) {
                for (String finish : List.of("matte", "tile", "lamp")) {
                    String name = kind.id + "_" + shade + "_" + finish;
                    PaletteBlock block = new PaletteBlock(
                        name,
                        AbstractBlock.Settings.create()
                            .strength(1.5F)
                            .luminance(state -> finish.equals("lamp") ? 15 : 0)
                    );
                    Registry.register(Registries.BLOCK, id(name), block);
                    BLOCKS.put(name, block);
                    item(name, new PaletteItem(name, block));
                }
            }

            // Коллекционные фигурки героинь
            String figurineName = kind.id + "_figurine";
            DecorationBlock figurineBlock = new DecorationBlock(figurineName);
            Registry.register(Registries.BLOCK, id(figurineName), figurineBlock);
            BLOCKS.put(figurineName, figurineBlock);
            FIGURINES.put(kind, item(figurineName, new PaletteItem(figurineName, figurineBlock)));
        }

        // Музыкальное оборудование и интерактивные напольные декорации
        for (String name : List.of("bocchi_box", "amplifier", "speaker", "drum_kit", "microphone", "guitar_case", "starry_sign")) {
            DecorationBlock block = new DecorationBlock(name);
            Registry.register(Registries.BLOCK, id(name), block);
            BLOCKS.put(name, block);
            item(name, new PaletteItem(name, block));
        }

        // Плюшевые игрушки и стойка для гитары
        for (String name : List.of("jimihen_plush", "mini_slime_plush", "guitar_stand")) {
            DecorationBlock block = new DecorationBlock(name);
            Registry.register(Registries.BLOCK, id(name), block);
            BLOCKS.put(name, block);
            item(name, new PaletteItem(name, block));
        }

        // Настенный декор, светящиеся звёзды и гирлянды
        for (String name : List.of("kessoku_poster", "indie_poster", "festival_poster", "starry_neon_star", "fairy_lights")) {
            int light = name.equals("starry_neon_star") ? 9 : name.equals("fairy_lights") ? 6 : 0;
            boolean ceiling = name.equals("fairy_lights");
            WallDecorBlock block = new WallDecorBlock(name, light, ceiling);
            Registry.register(Registries.BLOCK, id(name), block);
            BLOCKS.put(name, block);
            item(name, new PaletteItem(name, block));
        }

        // Инструменты, аксессуары и костюм Боччи
        item("guitar", new GuitarItem());
        item("rehearsal_bell", new RehearsalBellItem());
        item("mango_box_hat", new DecorativeHatItem("mango_box_hat"));
        item("nijika_ahoge", new DecorativeHatItem("nijika_ahoge"));
        item("bocchi_hairclip", new DecorativeHatItem("bocchi_hairclip"));
        item("nijika_ribbon", new DecorativeHatItem("nijika_ribbon"));
        item("ryo_hairclip", new DecorativeHatItem("ryo_hairclip"));
        item("kita_hairclip", new DecorativeHatItem("kita_hairclip"));
        item("tsuchinoko_hat", new DecorativeHatItem("tsuchinoko_hat"));
        item("stop_sign", new ModelItem("stop_sign", new Item.Settings().maxCount(1)));
        item("pink_hood", new CostumeItem(EquipmentSlot.HEAD, Items.LEATHER_HELMET));
        item("pink_jacket", new CostumeItem(EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE));
        item("pink_pants", new CostumeItem(EquipmentSlot.LEGS, Items.LEATHER_LEGGINGS));
        item("pink_shoes", new CostumeItem(EquipmentSlot.FEET, Items.LEATHER_BOOTS));

        // Регистрация серверной вкладки инвентаря Creative через Polymer
        PolymerItemGroupUtils.registerPolymerItemGroup(
            id("little_stage"),
            PolymerItemGroupUtils.builder()
                .displayName(Text.translatable("itemGroup.bocchi.little_stage"))
                .icon(() -> new ItemStack(FIGURINES.get(CharacterKind.NIJIKA)))
                .entries((context, entries) -> ITEMS.forEach(entries::add))
                .build()
        );

        // Проверочный запуск при валидации окружения в CI
        if (Boolean.getBoolean("bocchi.validateResources")) {
            VisualValidation.run();
            if (!PolymerItemGroupUtils.contains(id("little_stage")) || ITEMS.size() != 113) {
                throw new IllegalStateException("BTR World creative tab registration failed: " + ITEMS.size() + " player-facing entries");
            }
            LOGGER.info("Creative tab BTR World registered with {} player-facing entries.", ITEMS.size());
            boolean built = PolymerResourcePackUtils.buildMain(Path.of("polymer/bocchi-validation-pack.zip"));
            if (!built) {
                throw new IllegalStateException("Bocchi resource pack validation failed");
            }
            LOGGER.info("Resource pack validation passed.");
        }

        LOGGER.info("Little Stage registered: 14 companions, 85 blocks, 113 recipes.");

        // Регистрация операторской команды /bocchi stage
        registerCommands();
    }

    /**
     * Регистрирует команду /bocchi stage для возведения небольшой сцены.
     */
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
            dispatcher.register(
                CommandManager.literal("bocchi").then(
                    CommandManager.literal("stage")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            var player = context.getSource().getPlayerOrThrow();
                            ServerWorld world = player.getServerWorld();
                            BlockPos origin = player.getBlockPos().add(0, 0, 3);

                            // Проверяем, что пространство 7×4×5 свободно от блоков перед размещением
                            for (int x = -3; x <= 3; x++) {
                                for (int z = 0; z <= 4; z++) {
                                    for (int y = 0; y <= 3; y++) {
                                        if (!world.getBlockState(origin.add(x, y, z)).isAir()) {
                                            context.getSource().sendError(Text.literal("Нужно свободное место 7×4×5 перед игроком (по оси +Z)."));
                                            return 0;
                                        }
                                    }
                                }
                            }

                            // Пол сцены
                            for (int x = -3; x <= 3; x++) {
                                for (int z = 0; z <= 4; z++) {
                                    world.setBlockState(origin.add(x, 0, z), BLOCKS.get("ryo_dark_matte").getDefaultState());
                                }
                            }

                            // Светильники заднего плана
                            for (int x = -3; x <= 3; x++) {
                                world.setBlockState(origin.add(x, 1, 4), BLOCKS.get("nijika_base_lamp").getDefaultState());
                            }

                            // Сценическое оборудование
                            world.setBlockState(origin.add(-2, 1, 2), BLOCKS.get("speaker").getDefaultState());
                            world.setBlockState(origin.add(2, 1, 2), BLOCKS.get("amplifier").getDefaultState());
                            world.setBlockState(origin.add(0, 1, 3), BLOCKS.get("drum_kit").getDefaultState());
                            world.setBlockState(origin.add(0, 1, 1), BLOCKS.get("microphone").getDefaultState());
                            world.setBlockState(origin.add(1, 1, 3), BLOCKS.get("starry_sign").getDefaultState());

                            // Фигурки героинь на переднем крае сцены (-3..3)
                            int x = -3;
                            for (CharacterKind kind : CharacterKind.values()) {
                                world.setBlockState(origin.add(x++, 1, 0), BLOCKS.get(kind.id + "_figurine").getDefaultState());
                            }

                            context.getSource().sendFeedback(() -> Text.literal("Маленькая сцена готова!"), false);
                            return 1;
                        })
                )
            );
        });
    }
}
