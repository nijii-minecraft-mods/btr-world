package dev.aliska.bocchi;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Equipment;
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
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/**
 * Уникальные трусики для каждой героини BTR World.
 * Надеваются в слот поножей/ног (на попу, как обычное бельё) при обычном ПКМ.
 * При Shift + ПКМ можно надеть на голову (как маску без тыквенного оверлея).
 * Дают уникальный бафф героини, проигрывают звук и частицы сердечек.
 */
public final class PantiesItem extends Item implements PolymerItem, Equipment {
    private final CharacterKind kind;
    private final int color;
    private final int model;

    public PantiesItem(CharacterKind kind, int color) {
        super(new Item.Settings()
            .maxCount(1)
            .component(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color, false))
            .component(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT));
        this.kind = kind;
        this.color = color;
        this.model = PolymerResourcePackUtils.requestModel(
            Items.LEATHER_LEGGINGS,
            BocchiMod.id("item/" + kind.id + "_panties")
        ).value();
    }

    public CharacterKind getKind() {
        return kind;
    }

    public int getColor() {
        return color;
    }

    @Override
    public EquipmentSlot getSlotType() {
        return EquipmentSlot.LEGS;
    }

    @Override
    public Item getPolymerItem(ItemStack stack, ServerPlayerEntity player) {
        if (player != null && player.getEquippedStack(EquipmentSlot.HEAD) == stack) {
            return Items.LEATHER_HELMET;
        }
        return Items.LEATHER_LEGGINGS;
    }

    @Override
    public int getPolymerCustomModelData(ItemStack stack, ServerPlayerEntity player) {
        return model;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        // Shift + ПКМ: надеть на голову (как маску/головной убор без тыквенного оверлея)
        if (player.isSneaking()) {
            ItemStack currentHead = player.getEquippedStack(EquipmentSlot.HEAD);
            player.equipStack(EquipmentSlot.HEAD, stack.copyWithCount(1));
            stack.decrement(1);
            if (!currentHead.isEmpty()) {
                if (!player.giveItemStack(currentHead)) {
                    player.dropItem(currentHead, false);
                }
            }
            if (!world.isClient()) {
                applyHeroineBuff(player);
                world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0F, 1.5F);
                player.sendMessage(Text.translatable("message.bocchi.panties.head", kind.name), true);
                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0, player.getZ(), 5, 0.3, 0.3, 0.3, 0.1);
                }
            }
            return TypedActionResult.success(stack, world.isClient());
        }

        // Обычный ПКМ: надеть на ноги / попу (слот поножей)
        TypedActionResult<ItemStack> result = equipAndSwap(this, world, player, hand);
        if (!world.isClient() && result.getResult().isAccepted()) {
            applyHeroineBuff(player);
            world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), SoundCategory.PLAYERS, 1.0F, 1.0F);
            player.sendMessage(Text.translatable("message.bocchi.panties.wear", kind.name), true);
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0, player.getZ(), 5, 0.3, 0.3, 0.3, 0.1);
            }
        }
        return result;
    }

    public void applyHeroineBuff(PlayerEntity player) {
        switch (kind) {
            case BOCCHI -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 400, 1, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 200, 0, false, false, true));
            }
            case NIJIKA -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 400, 1, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 300, 0, false, false, true));
            }
            case RYO -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 600, 2, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 400, 0, false, false, true));
            }
            case KITA -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 400, 0, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 400, 1, false, false, true));
            }
            case SEIKA -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 400, 1, false, false, true));
            }
            case KIKURI -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 400, 1, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, false, false, true));
            }
            case PA_SAN -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 400, 0, false, false, true));
                Box box = player.getBoundingBox().expand(8.0);
                for (HostileEntity mob : player.getWorld().getEntitiesByClass(HostileEntity.class, box, e -> true)) {
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 1));
                }
            }
        }
    }
}
