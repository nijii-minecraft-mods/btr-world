package dev.aliska.bocchi;

import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;

/**
 * Декоративный полноразмерный блок палитры (матовый, плитка или лампа).
 */
public class PaletteBlock extends Block implements PolymerTexturedBlock {
    private final BlockState clientState;

    public PaletteBlock(String id, Settings settings) {
        super(settings);
        this.clientState = BlockVisuals.allocate(
            id,
            PolymerBlockModel.of(Identifier.of("bocchi", "block/" + id)),
            true
        );
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return clientState != null ? clientState : Blocks.WHITE_WOOL.getDefaultState();
    }
}
