package dev.aliska.bocchi;

import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Утилиты для выделения визуальных состояний блоков Polymer и тестирования исчерпания пулов.
 */
final class BlockVisuals {
    private static final Logger LOGGER = LoggerFactory.getLogger("bocchi");

    private BlockVisuals() {}

    /**
     * Запрашивает клиентское состояние блока через Polymer.
     * При исчерпании пула состояний переключается на безопасный ванильный fallback
     * (белая шерсть для сплошных блоков, воздух для декора).
     */
    static BlockState allocate(String id, PolymerBlockModel model, boolean solid) {
        var primary = solid ? BlockModelType.FULL_BLOCK : BlockModelType.TRIPWIRE_BLOCK_FLAT;
        BlockState state = PolymerBlockResourceUtils.requestBlock(primary, model);

        if (state == null && !solid) {
            state = PolymerBlockResourceUtils.requestBlock(BlockModelType.TRIPWIRE_BLOCK, model);
        }

        if (state != null) {
            return state;
        }

        LOGGER.warn(
            "No Polymer model states available for bocchi:{} ({}). Using safe vanilla fallback; custom model unavailable. Other Polymer mods share this pool.",
            id,
            primary
        );

        return solid ? Blocks.WHITE_WOOL.getDefaultState() : Blocks.AIR.getDefaultState();
    }

    /**
     * Искусственно исчерпывает пулы состояний Polymer при тестовом прогоне,
     * проверяя корректность работы механизма безопасного fallback.
     */
    static void exhaustForTest() {
        if (!Boolean.getBoolean("bocchi.testExhaustedBlockStates")) {
            return;
        }

        BlockModelType[] types = {
            BlockModelType.FULL_BLOCK,
            BlockModelType.TRIPWIRE_BLOCK_FLAT,
            BlockModelType.TRIPWIRE_BLOCK
        };

        for (var type : types) {
            int count = PolymerBlockResourceUtils.getBlocksLeft(type);
            for (int i = 0; i < count; i++) {
                PolymerBlockResourceUtils.requestBlock(
                    type,
                    PolymerBlockModel.of(Identifier.of(
                        "bocchi",
                        "block/exhaustion_probe_" + type.name().toLowerCase() + "_" + i
                    ))
                );
            }
        }
    }
}
