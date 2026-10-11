package dev.aliska.bocchi;

import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Напольные декоративные блоки (фигурки героинь, коробка Боччи,
 * барабанная установка, усилители, плюшевые игрушки).
 */
public class DecorationBlock extends Block implements PolymerTexturedBlock {
    private final BlockState visual;
    private final String id;

    public DecorationBlock(String id) {
        super(AbstractBlock.Settings.create()
            .strength(0.7F)
            .nonOpaque()
            .noCollision()
            .sounds(BlockSoundGroup.WOOD));
        this.id = id;
        this.visual = BlockVisuals.allocate(
            id,
            PolymerBlockModel.of(Identifier.of("bocchi", "block/" + id)),
            false
        );
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return visual != null ? visual : Blocks.AIR.getDefaultState();
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return Block.createCuboidShape(2, 0, 2, 14, id.endsWith("figurine") ? 13 : 12, 14);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        // Посадка игрока в картонную коробку Боччи
        if (id.equals("bocchi_box")) {
            if (!BoxSeats.sit((ServerWorld) world, pos, player)) {
                return ActionResult.PASS;
            }
            if (!player.getItemCooldownManager().isCoolingDown(asItem())) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 200, 0, false, false, true));
                player.getItemCooldownManager().set(asItem(), 400);
            }
            return ActionResult.SUCCESS;
        }

        // Интерактивная барабанная установка Нидзики
        if (id.equals("drum_kit")) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 200, 0, true, false, true));
            world.playSound(
                null,
                pos,
                SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value(),
                SoundCategory.BLOCKS,
                0.6F,
                1.0F
            );
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}
