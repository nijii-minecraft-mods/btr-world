package dev.aliska.bocchi;

import dev.aliska.bocchi.mixin.DisplayAccessor;
import dev.aliska.bocchi.mixin.ItemDisplayAccessor;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SitGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * Сущность спутницы BTR World (формы тсум и чиби).
 * На стороне сервера базируется на WolfEntity с невидимой моделью,
 * отображая пассажира ItemDisplayEntity через ресурс-пак Polymer.
 */
public class CompanionEntity extends WolfEntity implements PolymerEntity {
    private static final int[][] HUMMING_NOTES = {
        {12, 14, 16, 14, 12, 9, 12, 12}, // Боччи
        {12, 16, 19, 16, 14, 12, 14, 12}, // Нидзика
        {0, 3, 5, 3, 0, 7, 5, 0},         // Рё
        {12, 16, 17, 19, 17, 16, 14, 12}, // Кита
        {14, 16, 17, 16, 14, 12, 14, 14}, // Сейка
        {2, 5, 7, 5, 2, 7, 5, 2},         // Кикури
        {12, 15, 17, 15, 12, 10, 12, 12}  // ПА-сан
    };

    // Таймеры состояний и поведения
    private int hidingTicks;
    private int grassTicks;
    private int happyTicks;
    private int panicTicks;

    // Группа и генеалогия
    private UUID bandLeader;
    private CompanionGenes genes;
    private int pairingTicks;
    private int breedingCooldown;
    private int growthTicks;
    private int courtshipTicks;
    private UUID courtshipMate;

    // Визуальное масштабирование и смещение
    private float lastScale = -1.0F;
    private float visualMountOffset = Float.NaN;

    // Репетиция у инструментов
    private int rehearsalCooldown;
    private boolean performing;
    private BlockPos rehearsalInstrument;

    // Сценические исполнители
    private boolean stagePerformer;
    private BlockPos stageHome;

    // Режим передвижения (0 = следовать при >32 блоков, 1 = держаться рядом, 2 = гулять в радиусе 16)
    private int movementMode;

    // Напевание мелодий
    private boolean hummingMuted;
    private int hummingDelay = 600;
    private int hummingStep = -1;
    private int hummingBeat;

    public CompanionEntity(EntityType<? extends WolfEntity> type, World world) {
        super(type, world);
        setInvisible(true);
        setSilent(true);
        setPersistent();
        hummingDelay = 400 + random.nextInt(800);
    }

    public CompanionGenes genes() {
        if (genes == null) {
            genes = new CompanionGenes(kind().ordinal(), kind().ordinal(), 0);
        }
        return genes;
    }

    /**
     * Прерывает режим ожидания и привлекает внимание спутницы (вызывается колокольчиком).
     */
    public void recall() {
        setSitting(false);
        navigation.stop();
        happyTicks = 40;
    }

    private boolean ready() {
        return isAlive()
            && isTamed()
            && growthTicks == 0
            && breedingCooldown == 0
            && pairingTicks > 0
            && panicTicks == 0
            && hidingTicks == 0;
    }

    private boolean compatible(CompanionEntity other) {
        return other != this
            && other.ready()
            && Objects.equals(getOwnerUuid(), other.getOwnerUuid());
    }

    private CompanionEntity partner(ServerWorld world) {
        if (courtshipMate != null) {
            Entity locked = world.getEntity(courtshipMate);
            if (locked instanceof CompanionEntity girl && compatible(girl) && squaredDistanceTo(girl) < 64) {
                return girl;
            }
        }
        return world.getEntitiesByClass(CompanionEntity.class, getBoundingBox().expand(8), this::compatible)
            .stream()
            .min(Comparator.comparingDouble(this::squaredDistanceTo))
            .orElse(null);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void pushAwayFrom(Entity entity) {
        // Отключаем ванильное жесткое расталкивание
    }

    /**
     * Назначает сущность в команду с отключенными коллизиями, чтобы не толкать игроков.
     */
    private void ensureNoPushTeam(ServerWorld world) {
        var scoreboard = world.getScoreboard();
        String name = getNameForScoreboard();
        if (scoreboard.getScoreHolderTeam(name) != null) {
            return;
        }
        var team = scoreboard.getTeam("btr_pets");
        if (team == null) {
            team = scoreboard.addTeam("btr_pets");
        }
        team.setCollisionRule(AbstractTeam.CollisionRule.NEVER);
        scoreboard.addScoreHolderToTeam(name, team);
    }

    public CharacterKind kind() {
        return BocchiMod.KINDS.getOrDefault(getType(), CharacterKind.BOCCHI);
    }

    public boolean plush() {
        return BocchiMod.PLUSH_TYPES.contains(getType());
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new SitGoal(this));

        // Паника Боччи: убегает от монстров
        goalSelector.add(2, new FleeEntityGoal<HostileEntity>(this, HostileEntity.class, 7.0F, 1.15, 1.4) {
            @Override
            public boolean canStart() {
                return kind() == CharacterKind.BOCCHI && panicTicks > 0 && !isSitting() && super.canStart();
            }

            @Override
            public boolean shouldContinue() {
                return panicTicks > 0 && !isSitting() && super.shouldContinue();
            }
        });

        // Сближение пары для размножения
        goalSelector.add(2, new Goal() {
            private CompanionEntity mate;

            {
                setControls(EnumSet.of(Control.MOVE, Control.LOOK));
            }

            @Override
            public boolean canStart() {
                return ready() && getWorld() instanceof ServerWorld world && (mate = partner(world)) != null;
            }

            @Override
            public boolean shouldContinue() {
                return ready() && mate != null && compatible(mate) && squaredDistanceTo(mate) < 100;
            }

            @Override
            public void tick() {
                faceMate(mate);
                if (squaredDistanceTo(mate) > 0.72) {
                    navigation.startMovingTo(mate, 0.8);
                } else {
                    navigation.stop();
                }
            }

            @Override
            public void stop() {
                mate = null;
                navigation.stop();
            }
        });

        goalSelector.add(3, new RehearsalGoal());
        goalSelector.add(3, new FollowBandGoal());

        // Режим 0: свободное следование на расстоянии (без телепортации при удалении >32 блоков)
        goalSelector.add(3, new Goal() {
            private int delay;

            {
                setControls(EnumSet.of(Control.MOVE, Control.LOOK));
            }

            private boolean available() {
                return movementMode == 0
                    && isTamed()
                    && !isSitting()
                    && getOwner() != null
                    && !getOwner().isSpectator()
                    && panicTicks == 0
                    && hidingTicks == 0
                    && pairingTicks == 0;
            }

            @Override
            public boolean canStart() {
                return available() && squaredDistanceTo(getOwner()) > 32 * 32;
            }

            @Override
            public boolean shouldContinue() {
                return available() && squaredDistanceTo(getOwner()) > 24 * 24;
            }

            @Override
            public void start() {
                delay = 0;
            }

            @Override
            public void tick() {
                var owner = getOwner();
                if (owner == null) {
                    return;
                }
                getLookControl().lookAt(owner, 10.0F, getMaxLookPitchChange());
                if (--delay <= 0) {
                    delay = 10;
                    navigation.startMovingTo(owner, 1.1);
                }
            }

            @Override
            public void stop() {
                navigation.stop();
            }
        });

        // Режим 1: держаться вплотную к хозяину (стандартный FollowOwnerGoal)
        goalSelector.add(3, new FollowOwnerGoal(this, 1.0, 1.5F, 0.8F) {
            @Override
            public boolean canStart() {
                return movementMode == 1 && super.canStart();
            }

            @Override
            public boolean shouldContinue() {
                return movementMode == 1 && super.shouldContinue();
            }
        });

        // Режим 2: гулять вокруг хозяина в радиусе примерно 15 блоков
        goalSelector.add(3, new Goal() {
            private int delay;

            {
                setControls(EnumSet.of(Control.MOVE));
            }

            @Override
            public boolean canStart() {
                return movementMode == 2
                    && isTamed()
                    && !isSitting()
                    && getOwner() != null
                    && panicTicks == 0
                    && hidingTicks == 0
                    && pairingTicks == 0;
            }

            @Override
            public boolean shouldContinue() {
                return canStart();
            }

            @Override
            public void tick() {
                var owner = getOwner();
                if (owner == null) {
                    return;
                }
                if (squaredDistanceTo(owner) > 225) {
                    navigation.startMovingTo(owner, 1.0);
                    return;
                }
                if (--delay <= 0) {
                    delay = 80 + random.nextInt(80);
                    var target = FuzzyTargeting.find(CompanionEntity.this, 10, 4);
                    if (target != null && target.squaredDistanceTo(owner.getPos()) < 225) {
                        navigation.startMovingTo(target.x, target.y, target.z, 0.65);
                    }
                }
            }

            @Override
            public void stop() {
                navigation.stop();
            }
        });

        // Сценические спутницы: не уходить далеко от исходной точки на сцене
        goalSelector.add(3, new Goal() {
            {
                setControls(EnumSet.of(Control.MOVE));
            }

            @Override
            public boolean canStart() {
                return stagePerformer
                    && !isTamed()
                    && !isSitting()
                    && stageHome != null
                    && !performing
                    && squaredDistanceTo(stageHome.getX() + 0.5, stageHome.getY(), stageHome.getZ() + 0.5) > 16;
            }

            @Override
            public boolean shouldContinue() {
                return stagePerformer
                    && !isTamed()
                    && stageHome != null
                    && !isSitting()
                    && squaredDistanceTo(stageHome.getX() + 0.5, stageHome.getY(), stageHome.getZ() + 0.5) > 4;
            }

            @Override
            public void tick() {
                navigation.startMovingTo(stageHome.getX() + 0.5, stageHome.getY(), stageHome.getZ() + 0.5, 0.65);
            }

            @Override
            public void stop() {
                navigation.stop();
            }
        });

        goalSelector.add(4, new WanderAroundFarGoal(this, 0.65) {
            @Override
            public boolean canStart() {
                return !stagePerformer && super.canStart();
            }

            @Override
            public boolean shouldContinue() {
                return !stagePerformer && super.shouldContinue();
            }
        });

        goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        goalSelector.add(6, new LookAroundGoal(this));
    }

    @Override
    public EntityType<?> getPolymerEntityType(ServerPlayerEntity player) {
        return EntityType.WOLF;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack held = player.getStackInHand(hand);
        if (getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        // Приручение любимой едой
        if (!isTamed() && held.isOf(kind().treat)) {
            happyTicks = 100;
            consume(player, held);
            setOwner(player);
            setSitting(false);
            heal(getMaxHealth());
            getWorld().sendEntityStatus(this, (byte) 7);
            BtrAdvancements.grant(player, "first_friend");
            BtrAdvancements.grant(player, "quartet", kind().id);
            return ActionResult.SUCCESS;
        }

        // Взаимодействие прирученной спутницы с владельцем
        if (isOwner(player)) {
            BtrAdvancements.grant(player, "first_friend");
            BtrAdvancements.grant(player, "quartet", kind().id);

            // Переключение режима движения палкой при приседании
            if (held.isOf(Items.STICK) && player.isSneaking()) {
                movementMode = (movementMode + 1) % 3;
                setSitting(false);
                navigation.stop();
                String[] modes = {"follow", "close", "roam"};
                player.sendMessage(Text.translatable("message.bocchi.movement." + modes[movementMode]), false);
                return ActionResult.SUCCESS;
            }

            // Ускорение роста малыша любимой едой
            if (held.isOf(kind().treat) && growthTicks > 0) {
                consume(player, held);
                heal(6.0F);
                happyTicks = 60;
                growthTicks = Math.max(0, growthTicks - Math.max(1, growthTicks / 10));
                ((ServerWorld) getWorld()).spawnParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    getX(),
                    getY() + 0.5,
                    getZ(),
                    5,
                    0.25,
                    0.2,
                    0.25,
                    0.0
                );
                return ActionResult.SUCCESS;
            }

            // Перевод в состояние готовности к паре любимой едой
            if (held.isOf(kind().treat) && growthTicks == 0 && breedingCooldown == 0 && pairingTicks == 0 && getHealth() >= getMaxHealth()) {
                consume(player, held);
                pairingTicks = 600;
                setSitting(false);
                happyTicks = 60;
                ((ServerWorld) getWorld()).spawnParticles(
                    ParticleTypes.HEART,
                    getX(),
                    getY() + 0.8,
                    getZ(),
                    5,
                    0.3,
                    0.2,
                    0.3,
                    0.0
                );
                return ActionResult.SUCCESS;
            }

            // Торговля с Рё: меняет изумруд на случайную фигурку героинь
            if (kind() == CharacterKind.RYO && held.isOf(Items.EMERALD)) {
                happyTicks = 60;
                consume(player, held);
                BtrAdvancements.grant(player, "ryo_trade");
                Item item = BocchiMod.FIGURINES.get(CharacterKind.values()[random.nextInt(CharacterKind.values().length)]);
                if (!player.giveItemStack(new ItemStack(item))) {
                    player.dropItem(new ItemStack(item), false);
                }
                return ActionResult.SUCCESS;
            }

            // Угощение Кикури баночкой мёда с возвратом пустой бутылочки
            if (kind() == CharacterKind.KIKURI && held.isOf(Items.HONEY_BOTTLE)) {
                happyTicks = 100;
                consume(player, held);
                if (!player.isCreative() && !player.giveItemStack(new ItemStack(Items.GLASS_BOTTLE))) {
                    player.dropItem(new ItemStack(Items.GLASS_BOTTLE), false);
                }
                getWorld().playSound(null, getBlockPos(), SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.NEUTRAL, 0.5F, 1.0F);
                heal(8.0F);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 0, true, false, true));
                getWorld().sendEntityStatus(this, (byte) 7);
                return ActionResult.SUCCESS;
            }

            // Лечение любимой едой при повреждении
            if (held.isOf(kind().treat) && getHealth() < getMaxHealth()) {
                happyTicks = 80;
                consume(player, held);
                heal(6.0F);
                getWorld().sendEntityStatus(this, (byte) 7);
                return ActionResult.SUCCESS;
            }

            // Пустая рука: переключение напевания (Shift) или переход в режим ожидания/сидения
            if (held.isEmpty()) {
                if (player.isSneaking()) {
                    hummingMuted = !hummingMuted;
                    hummingStep = -1;
                    hummingDelay = 100;
                    if (!hummingMuted) {
                        ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.NOTE, getX(), getY() + 0.9, getZ(), 1, 0, 0, 0, 0);
                    }
                    return ActionResult.SUCCESS;
                }
                setSitting(!isSitting());
                navigation.stop();
                return ActionResult.SUCCESS;
            }
        }

        // Поддержка ванильных бирок и поводков
        if (held.isOf(Items.NAME_TAG) || held.isOf(Items.LEAD)) {
            return super.interactMob(player, hand);
        }

        return ActionResult.PASS;
    }

    private void consume(PlayerEntity player, ItemStack stack) {
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    @Override
    public WolfEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(getWorld() instanceof ServerWorld world) || !isAlive()) {
            return;
        }

        setInvisible(true);

        // Инициализация позиции сценического артиста на первом тике
        if (stagePerformer && stageHome == null) {
            stageHome = getBlockPos().toImmutable();
        }
        if (stagePerformer && isTamed()) {
            stagePerformer = false;
            stageHome = null;
        }

        if (age % 20 == 0) {
            ensureNoPushTeam(world);
        }

        // Обновление таймеров
        if (rehearsalCooldown > 0) rehearsalCooldown--;
        if (breedingCooldown > 0) breedingCooldown--;
        if (growthTicks > 0) growthTicks--;
        if (pairingTicks > 0) pairingTicks--;

        if (ready()) {
            tryPair(world);
        } else {
            courtshipTicks = 0;
            courtshipMate = null;
        }

        if (happyTicks > 0) happyTicks--;
        if (panicTicks > 0 && --panicTicks == 0) hidingTicks = 60;
        if (hidingTicks > 0) {
            hidingTicks--;
            navigation.stop();
        }
        if (grassTicks > 0) {
            grassTicks--;
            navigation.stop();
        }

        // Особенность Боччи: обнаружение монстров и паника
        if (kind() == CharacterKind.BOCCHI && age % 20 == 0) {
            boolean danger = !world.getEntitiesByClass(HostileEntity.class, getBoundingBox().expand(5), Entity::isAlive).isEmpty();
            if (danger && !isSitting() && panicTicks == 0 && hidingTicks == 0) {
                panicTicks = 60;
            }
        }

        // Особенность Рё: поедание травы
        if (kind() == CharacterKind.RYO && !isSitting() && hidingTicks == 0 && age % 20 == 0) {
            eatGrass(world);
        }

        // Особенность Киты: светящиеся частицы и ночное зрение для владельца
        if (kind() == CharacterKind.KITA && age % 10 == 0) {
            world.spawnParticles(ParticleTypes.END_ROD, getX(), getY() + 0.7, getZ(), 2, 0.3, 0.2, 0.3, 0.01);
            if (isTamed() && getOwner() != null && distanceTo(getOwner()) < 8 && age % 40 == 0) {
                getOwner().addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 240, 0, true, false, true));
            }
        }

        // Особенность Нидзики: спешка владельцу и успокоение паники Боччи
        if (kind() == CharacterKind.NIJIKA && isTamed() && !isSitting() && age % 200 == 0 && getOwner() != null && distanceTo(getOwner()) < 8) {
            getOwner().addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 100, 0, true, false, true));
            world.spawnParticles(ParticleTypes.NOTE, getX(), getY() + 0.9, getZ(), 1, 0, 0, 0, 0);
            world.playSound(null, getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value(), SoundCategory.NEUTRAL, 0.2F, 1.2F);
        }

        if (kind() == CharacterKind.NIJIKA && bandLeader != null && !isSitting() && age % 40 == 0) {
            Entity leader = world.getEntity(bandLeader);
            if (leader instanceof CompanionEntity bocchi && bocchi.kind() == CharacterKind.BOCCHI && bocchi.panicTicks > 0 && squaredDistanceTo(bocchi) < 36) {
                bocchi.panicTicks = Math.max(1, bocchi.panicTicks - 10);
                world.spawnParticles(ParticleTypes.NOTE, getX(), getY() + 0.9, getZ(), 1, 0, 0, 0, 0);
                world.playSound(null, getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value(), SoundCategory.NEUTRAL, 0.15F, 1.4F);
            }
        }

        // Особенность Сейки: заботливая старшая сестра и защита владельца
        if (kind() == CharacterKind.SEIKA && isTamed() && !isSitting() && age % 200 == 0 && getOwner() != null && distanceTo(getOwner()) < 8) {
            getOwner().addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 160, 0, true, false, true));
            world.spawnParticles(ParticleTypes.HEART, getX(), getY() + 0.8, getZ(), 1, 0.2, 0.2, 0.2, 0);
            world.playSound(null, getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.NEUTRAL, 0.2F, 1.2F);
        }

        // Особенность Кикури: шатающаяся рокерская походка и воодушевление владельца
        if (kind() == CharacterKind.KIKURI && age % 50 == 0) {
            world.spawnParticles(ParticleTypes.BUBBLE_POP, getX(), getY() + 0.6, getZ(), 2, 0.2, 0.2, 0.2, 0.02);
            if (isTamed() && getOwner() != null && distanceTo(getOwner()) < 8 && age % 160 == 0) {
                getOwner().addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 120, 0, true, false, true));
            }
        }

        // Особенность ПА-сан: звуковые волны, ночное видение и аура звукорежиссёра
        if (kind() == CharacterKind.PA_SAN && age % 40 == 0) {
            world.spawnParticles(ParticleTypes.PORTAL, getX(), getY() + 0.6, getZ(), 3, 0.2, 0.2, 0.2, 0.05);
            if (isTamed() && getOwner() != null && distanceTo(getOwner()) < 8) {
                getOwner().addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 260, 0, true, false, true));
            }
            // Мягкое замедление приближающихся враждебных мобов
            for (var mob : world.getEntitiesByClass(HostileEntity.class, getBoundingBox().expand(6), Entity::isAlive)) {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, true, false, false));
            }
        }

        // Мягкое дистанцирование спутниц друг от друга без выталкивания через стены
        if (age % 2 == 0 && pairingTicks == 0 && !hasVehicle()) {
            for (var other : world.getEntitiesByClass(
                CompanionEntity.class,
                getBoundingBox().expand(0.7),
                girl -> girl.getId() > getId() && girl.isAlive() && girl.pairingTicks == 0 && !girl.hasVehicle()
            )) {
                double dx = getX() - other.getX();
                double dz = getZ() - other.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                double spacing = (getWidth() + other.getWidth()) * 0.5 + 0.18;
                if (distance >= spacing || Math.abs(getY() - other.getY()) > 0.5) {
                    continue;
                }
                if (distance < 0.0001) {
                    dx = 1.0;
                    dz = 0.0;
                } else {
                    dx /= distance;
                    dz /= distance;
                }
                double correction = Math.min(0.06, (spacing - distance) * 0.5);
                var offset = new Vec3d(dx * correction, 0, dz * correction);
                move(MovementType.SELF, offset);
                other.move(MovementType.SELF, offset.negate());
            }
        }

        // Тихое напевание и обновление визуальной модели
        tickHumming(world);
        updateVisual(world);
    }

    private void tickHumming(ServerWorld world) {
        boolean calm = !performing
            && !hummingMuted
            && panicTicks == 0
            && hidingTicks == 0
            && pairingTicks == 0
            && grassTicks == 0
            && getHealth() >= getMaxHealth()
            && world.getClosestPlayer(this, 8) != null;

        if (!calm) {
            hummingStep = -1;
            hummingDelay = Math.max(hummingDelay, 100);
            return;
        }

        if (hummingStep < 0) {
            if (--hummingDelay > 0) {
                return;
            }
            // Спутницы поблизости напевают по очереди, а не одновременно
            if (!world.getEntitiesByClass(
                CompanionEntity.class,
                getBoundingBox().expand(8),
                girl -> girl != this && girl.hummingStep >= 0
            ).isEmpty()) {
                hummingDelay = 100 + random.nextInt(200);
                return;
            }
            hummingStep = 0;
            hummingBeat = 0;
        }

        if (hummingBeat-- > 0) {
            return;
        }

        int note = HUMMING_NOTES[kind().ordinal()][hummingStep];
        SoundEvent instrument = switch (kind()) {
            case BOCCHI -> SoundEvents.BLOCK_NOTE_BLOCK_HARP.value();
            case NIJIKA -> SoundEvents.BLOCK_NOTE_BLOCK_BELL.value();
            case RYO -> SoundEvents.BLOCK_NOTE_BLOCK_BASS.value();
            case KITA -> SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value();
            case SEIKA -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
            case KIKURI -> SoundEvents.BLOCK_NOTE_BLOCK_GUITAR.value();
            case PA_SAN -> SoundEvents.BLOCK_NOTE_BLOCK_BIT.value();
        };

        world.playSound(
            null,
            getX(),
            getY(),
            getZ(),
            instrument,
            SoundCategory.NEUTRAL,
            0.12F,
            (float) Math.pow(2, (note - 12) / 12.0)
        );
        world.spawnParticles(ParticleTypes.NOTE, getX(), getY() + 0.9, getZ(), 0, note / 24.0, 0, 0, 1);
        hummingBeat = 7;

        if (++hummingStep >= HUMMING_NOTES[kind().ordinal()].length) {
            hummingStep = -1;
            hummingDelay = 800 + random.nextInt(700);
        }
    }

    private void faceMate(CompanionEntity mate) {
        getLookControl().lookAt(mate, 30.0F, 30.0F);
        float facing = (float) (Math.atan2(mate.getZ() - getZ(), mate.getX() - getX()) * 180.0 / Math.PI) - 90.0F;
        setYaw(facing);
        setBodyYaw(facing);
        setHeadYaw(facing);
    }

    private void tryPair(ServerWorld world) {
        CompanionEntity mate = partner(world);
        if (mate == null) {
            courtshipTicks = 0;
            courtshipMate = null;
            return;
        }

        if (getId() > mate.getId()) {
            return;
        }

        if (mate.courtshipMate != null && !mate.courtshipMate.equals(getUuid())) {
            courtshipTicks = 0;
            courtshipMate = null;
            return;
        }

        if (!mate.getUuid().equals(courtshipMate)) {
            courtshipTicks = 0;
        }

        courtshipMate = mate.getUuid();
        mate.courtshipMate = getUuid();

        if (squaredDistanceTo(mate) > 1.0 || !canSee(mate)) {
            courtshipTicks = 0;
            return;
        }

        faceMate(mate);
        mate.faceMate(this);

        happyTicks = Math.max(happyTicks, 20);
        mate.happyTicks = Math.max(mate.happyTicks, 20);

        if (++courtshipTicks % 5 == 0) {
            for (CompanionEntity parent : new CompanionEntity[]{this, mate}) {
                world.spawnParticles(ParticleTypes.HEART, parent.getX(), parent.getY() + 0.75, parent.getZ(), 3, 0.22, 0.18, 0.22, 0);
            }
        }

        if (courtshipTicks < 60) {
            return;
        }

        // Повторяем попытку спавна малыша не чаще 1 раза в секунду, если место заблокировано
        if (courtshipTicks > 60 && courtshipTicks % 20 != 0) {
            return;
        }

        var inheritanceRandom = new Random(random.nextLong());
        CompanionGenes inherited = CompanionGenes.cross(
            kind().ordinal(),
            genes().generation(),
            mate.kind().ordinal(),
            mate.genes().generation()
        );

        CharacterKind childKind = CharacterKind.values()[inherited.heroine(inheritanceRandom)];
        boolean childPlush = random.nextBoolean() ? plush() : mate.plush();

        var type = BocchiMod.KINDS.keySet().stream()
            .filter(t -> BocchiMod.KINDS.get(t) == childKind && BocchiMod.PLUSH_TYPES.contains(t) == childPlush)
            .findFirst()
            .orElseThrow();

        CompanionEntity child = (CompanionEntity) type.create(world);
        if (child == null || !(getOwner() instanceof PlayerEntity owner)) {
            return;
        }

        child.refreshPositionAndAngles((getX() + mate.getX()) / 2.0, getY(), (getZ() + mate.getZ()) / 2.0, getYaw(), 0);
        if (!world.isSpaceEmpty(child)) {
            return;
        }

        child.genes = inherited;
        child.growthTicks = 6000;
        child.setOwner(owner);
        child.setSitting(false);
        child.bandLeader = bandLeader;

        if (!world.spawnEntity(child)) {
            return;
        }

        courtshipTicks = mate.courtshipTicks = 0;
        courtshipMate = mate.courtshipMate = null;
        pairingTicks = mate.pairingTicks = 0;
        breedingCooldown = mate.breedingCooldown = 6000;
        happyTicks = mate.happyTicks = 100;

        world.spawnParticles(ParticleTypes.HEART, child.getX(), child.getY() + 0.5, child.getZ(), 16, 0.4, 0.3, 0.4, 0);
        BtrAdvancements.grant(owner, "new_generation");
        if (kind() != mate.kind() || plush() != mate.plush()) {
            BtrAdvancements.grant(owner, "mixed_duet");
        }
        if (child.genes.generation() >= 3) {
            BtrAdvancements.grant(owner, "family_tree");
        }
    }

    private void eatGrass(ServerWorld world) {
        BlockPos pos = getBlockPos();
        if (world.getBlockState(pos).isOf(Blocks.SHORT_GRASS)) {
            if (world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                world.breakBlock(pos, false, this);
            }
            grassTicks = 30;
            heal(1.0F);
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.4, getZ(), 3, 0.2, 0.1, 0.2, 0);
        } else if (world.getBlockState(pos.down()).isOf(Blocks.GRASS_BLOCK) && age % 100 == 0) {
            if (world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                world.setBlockState(pos.down(), Blocks.DIRT.getDefaultState());
            }
            grassTicks = 30;
            heal(1.0F);
        }
    }

    private void updateVisual(ServerWorld world) {
        DisplayEntity.ItemDisplayEntity display = null;
        for (Entity passenger : getPassengerList()) {
            if (passenger instanceof DisplayEntity.ItemDisplayEntity item) {
                display = item;
                break;
            }
        }

        if (display == null) {
            visualMountOffset = Float.NaN;
            lastScale = -1.0F;
            display = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
            display.setPosition(getPos());
            display.addCommandTag("bocchi_companion_visual");
            ((ItemDisplayAccessor) display).bocchi$setMode(ModelTransformationMode.NONE);
            world.spawnEntity(display);
            display.startRiding(this, true);
        }

        // Компенсация высоты крепления пассажира у волка в NBT трансформации
        if (Float.isNaN(visualMountOffset)) {
            WolfEntity vanillaWolf = new WolfEntity(EntityType.WOLF, world);
            visualMountOffset = (float) (vanillaWolf.getPassengerRidingPos(display).y - vanillaWolf.getY());
        }

        float scale = growthTicks > 0 ? 0.6F : 1.0F;
        if (lastScale != scale || Float.isNaN(visualMountOffset)) {
            lastScale = scale;
            ((DisplayAccessor) display).bocchi$setTransformation(new AffineTransformation(
                new Vector3f(0, -visualMountOffset, 0),
                new Quaternionf(),
                new Vector3f(scale, scale, scale),
                new Quaternionf()
            ));
        }

        String mood = hurtTime > 0 ? "surprised"
            : happyTicks > 0 ? "happy"
            : (grassTicks > 0 || panicTicks > 0) ? "squint"
            : isSitting() ? "sleepy"
            : "";

        String name = kind().id + (plush() ? "_plush" : "_chibi") + (mood.isEmpty() ? "" : "_" + mood);
        Item visual = hidingTicks > 0 ? BocchiMod.BOX_MODEL : BocchiMod.MODELS.get(name);

        if (!((ItemDisplayAccessor) display).bocchi$getItemStack().isOf(visual)) {
            ((ItemDisplayAccessor) display).bocchi$setItemStack(new ItemStack(visual));
        }

        display.setYaw(getYaw());
        display.setPitch(0);
        display.setGlowing(kind() == CharacterKind.KITA);
    }

    @Override
    protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater updater) {
        if (passenger instanceof DisplayEntity.ItemDisplayEntity) {
            updater.accept(passenger, getX(), getY(), getZ());
        } else {
            super.updatePassengerPosition(passenger, updater);
        }
    }

    @Override
    public EntityData initialize(ServerWorldAccess access, LocalDifficulty difficulty, SpawnReason reason, EntityData data) {
        EntityData result = super.initialize(access, difficulty, reason, data);
        // При естественном появлении участницы Kessoku Band спавнятся полным квартетом
        if (reason == SpawnReason.NATURAL && access instanceof ServerWorld world) {
            var members = new ArrayList<CompanionEntity>();
            members.add(this);
            int offset = 0;

            var spawnGroup = kind().isKessokuBand()
                ? java.util.List.of(CharacterKind.BOCCHI, CharacterKind.NIJIKA, CharacterKind.RYO, CharacterKind.KITA)
                : (kind() == CharacterKind.SEIKA ? java.util.List.of(CharacterKind.SEIKA, CharacterKind.PA_SAN) : java.util.List.of(kind()));

            for (CharacterKind wanted : spawnGroup) {
                if (wanted == kind()) {
                    continue;
                }
                EntityType<?> type = BocchiMod.KINDS.keySet().stream()
                    .filter(t -> BocchiMod.KINDS.get(t) == wanted && BocchiMod.PLUSH_TYPES.contains(t) == plush())
                    .findFirst()
                    .orElseThrow();

                CompanionEntity friend = (CompanionEntity) type.create(world);
                if (friend == null) {
                    continue;
                }

                boolean placed = false;
                int[][] spots = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {-2, -2}};
                for (int i = 0; i < spots.length && !placed; i++) {
                    int[] spot = spots[(i + offset) % spots.length];
                    for (int dy : new int[]{0, 1, -1}) {
                        BlockPos pos = getBlockPos().add(spot[0], dy, spot[1]);
                        friend.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, getYaw(), 0);
                        if (world.getBlockState(pos.down()).isSolidBlock(world, pos.down())
                            && world.isSpaceEmpty(friend)
                            && !world.containsFluid(friend.getBoundingBox())) {
                            if (world.spawnEntity(friend)) {
                                members.add(friend);
                                placed = true;
                            }
                            break;
                        }
                    }
                }
                offset++;
            }

            CompanionEntity leader = members.stream()
                .filter(m -> m.kind() == CharacterKind.BOCCHI)
                .findFirst()
                .orElse(this);

            for (CompanionEntity member : members) {
                member.bandLeader = leader.getUuid();
            }
        }
        return result;
    }

    /**
     * Цель поведения: репетиция возле соответствующего инструмента.
     */
    private final class RehearsalGoal extends Goal {
        private BlockPos instrument;
        private int elapsed;
        private int played;
        private int searchDelay;
        private int sessionLength;

        RehearsalGoal() {
            setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        private boolean available() {
            boolean audience = stagePerformer && !isTamed() && getWorld().getClosestPlayer(CompanionEntity.this, 24) != null;
            boolean ownerNearby = isTamed() && getOwner() != null && squaredDistanceTo(getOwner()) < 256;
            return (audience || ownerNearby)
                && growthTicks == 0
                && !isSitting()
                && !hummingMuted
                && panicTicks == 0
                && hidingTicks == 0
                && pairingTicks == 0
                && getHealth() >= getMaxHealth();
        }

        private String instrumentId() {
            return switch (kind()) {
                case NIJIKA -> "drum_kit";
                case KITA -> "microphone";
                case PA_SAN -> "speaker";
                case SEIKA -> "amplifier";
                default -> "guitar_stand";
            };
        }

        @Override
        public boolean canStart() {
            if (!available() || rehearsalCooldown > 0 || --searchDelay > 0) {
                return false;
            }
            searchDelay = 80;
            for (BlockPos candidate : BlockPos.iterateOutwards(getBlockPos(), 8, 3, 8)) {
                if (!getWorld().getBlockState(candidate).isOf(BocchiMod.BLOCKS.get(instrumentId()))) {
                    continue;
                }
                if (getWorld() instanceof ServerWorld world && !world.getEntitiesByClass(
                    CompanionEntity.class,
                    getBoundingBox().expand(10),
                    girl -> girl != CompanionEntity.this && girl.performing && (!stagePerformer || !girl.stagePerformer || candidate.equals(girl.rehearsalInstrument))
                ).isEmpty()) {
                    continue;
                }
                instrument = candidate.toImmutable();
                return true;
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return available()
                && (played > 0 || elapsed < 240)
                && played < sessionLength
                && instrument != null
                && getWorld().getBlockState(instrument).isOf(BocchiMod.BLOCKS.get(instrumentId()));
        }

        @Override
        public void start() {
            elapsed = 0;
            played = 0;
            sessionLength = 1800 + random.nextInt(1201);
            performing = true;
            rehearsalInstrument = instrument;
            hummingStep = -1;
        }

        @Override
        public void tick() {
            elapsed++;
            double x = instrument.getX() + 0.5;
            double y = instrument.getY();
            double z = instrument.getZ() + 0.5;

            if (squaredDistanceTo(x, y, z) > 2.25) {
                navigation.startMovingTo(x, y, z, 0.8);
                return;
            }

            navigation.stop();
            float facing = (float) (Math.atan2(z - getZ(), x - getX()) * 180.0 / Math.PI) - 90.0F;
            setYaw(facing);
            setBodyYaw(facing);
            setHeadYaw(facing);
            getLookControl().lookAt(x, y + 0.6, z);
            happyTicks = Math.max(happyTicks, 20);

            if (played++ % 8 == 0 && getWorld() instanceof ServerWorld world) {
                int note = OriginalMusic.note(kind().ordinal(), played / 8);
                SoundEvent sound = switch (kind()) {
                    case NIJIKA -> played % 16 < 8
                        ? SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value()
                        : SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value();
                    case KITA -> SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value();
                    case RYO -> SoundEvents.BLOCK_NOTE_BLOCK_BASS.value();
                    case BOCCHI -> SoundEvents.BLOCK_NOTE_BLOCK_GUITAR.value();
                    case SEIKA -> SoundEvents.BLOCK_NOTE_BLOCK_HAT.value();
                    case KIKURI -> SoundEvents.BLOCK_NOTE_BLOCK_BASS.value();
                    case PA_SAN -> SoundEvents.BLOCK_NOTE_BLOCK_BIT.value();
                };

                world.playSound(
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    sound,
                    SoundCategory.NEUTRAL,
                    0.2F,
                    (float) Math.pow(2, (note - 12) / 12.0)
                );
                world.spawnParticles(ParticleTypes.NOTE, getX(), getY() + 0.95, getZ(), 0, note / 24.0, 0, 0, 1);
                world.spawnParticles(ParticleTypes.NOTE, x, y + 0.9, z, 0, note / 24.0, 0, 0, 1);
            }
        }

        @Override
        public void stop() {
            navigation.stop();
            performing = false;
            instrument = null;
            rehearsalInstrument = null;
            rehearsalCooldown = 1200 + random.nextInt(1200);
            hummingDelay = 200;
        }
    }

    /**
     * Цель поведения: следование за лидером группы (Боччи) при естественном появлении в мире.
     */
    private final class FollowBandGoal extends Goal {
        private CompanionEntity leader;
        private int repath;

        FollowBandGoal() {
            setControls(EnumSet.of(Control.MOVE));
        }

        private CompanionEntity findLeader() {
            if (bandLeader == null || bandLeader.equals(getUuid()) || !(getWorld() instanceof ServerWorld world)) {
                return null;
            }
            Entity entity = world.getEntity(bandLeader);
            return entity instanceof CompanionEntity girl && girl.isAlive() ? girl : null;
        }

        @Override
        public boolean canStart() {
            leader = findLeader();
            return !isTamed()
                && !isSitting()
                && grassTicks == 0
                && hidingTicks == 0
                && leader != null
                && squaredDistanceTo(leader) > 6.25
                && squaredDistanceTo(leader) < 1024;
        }

        @Override
        public boolean shouldContinue() {
            return !isTamed()
                && !isSitting()
                && grassTicks == 0
                && leader != null
                && leader.isAlive()
                && squaredDistanceTo(leader) > 3.0
                && squaredDistanceTo(leader) < 1024;
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void tick() {
            if (--repath <= 0) {
                repath = 10;
                navigation.startMovingTo(leader, leader.panicTicks > 0 ? 1.25 : 0.9);
            }
            getLookControl().lookAt(leader, 20.0F, 20.0F);
        }

        @Override
        public void stop() {
            navigation.stop();
            leader = null;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy()) {
            for (Entity passenger : getPassengerList()) {
                passenger.discard();
            }
            if (getWorld() instanceof ServerWorld world) {
                var team = world.getScoreboard().getScoreHolderTeam(getNameForScoreboard());
                if (team != null && team.getName().equals("btr_pets")) {
                    world.getScoreboard().removeScoreHolderFromTeam(getNameForScoreboard(), team);
                }
            }
        }
        super.remove(reason);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("BocchiHiding", hidingTicks);
        nbt.putInt("BocchiPanic", panicTicks);
        if (bandLeader != null) {
            nbt.putUuid("BocchiBandLeader", bandLeader);
        }
        nbt.putIntArray("BtrGenes", new int[]{genes().heroineA(), genes().heroineB(), genes().generation()});
        nbt.putBoolean("BtrStagePerformer", stagePerformer);
        if (stageHome != null) {
            nbt.putIntArray("BtrStageHome", new int[]{stageHome.getX(), stageHome.getY(), stageHome.getZ()});
        }
        nbt.putInt("BtrMovementMode", movementMode);
        nbt.putBoolean("BtrHummingMuted", hummingMuted);
        nbt.putInt("BtrGrowth", growthTicks);
        nbt.putInt("BtrBreedCooldown", breedingCooldown);
        nbt.putInt("BtrPairing", pairingTicks);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        hidingTicks = nbt.getInt("BocchiHiding");
        panicTicks = nbt.getInt("BocchiPanic");

        int[] g = nbt.getIntArray("BtrGenes");
        if (g.length == 3) {
            genes = new CompanionGenes(g[0], g[1], g[2]);
        }

        stagePerformer = nbt.getBoolean("BtrStagePerformer");
        int[] home = nbt.getIntArray("BtrStageHome");
        stageHome = home.length == 3 ? new BlockPos(home[0], home[1], home[2]) : null;

        // Миграция старых русских авто-имён на стандартную систему переводов
        var custom = getCustomName();
        if (custom != null
            && custom.getContent() instanceof PlainTextContent
            && custom.getSiblings().isEmpty()
            && custom.getStyle().isEmpty()
            && CompanionNames.isLegacyGeneratedName(custom.getString())) {
            setCustomName(null);
        }

        movementMode = Math.max(0, Math.min(2, nbt.getInt("BtrMovementMode")));
        hummingMuted = nbt.getBoolean("BtrHummingMuted");
        hummingStep = -1;

        growthTicks = Math.max(0, Math.min(6000, nbt.getInt("BtrGrowth")));
        breedingCooldown = Math.max(0, Math.min(6000, nbt.getInt("BtrBreedCooldown")));
        pairingTicks = Math.max(0, Math.min(600, nbt.getInt("BtrPairing")));
        lastScale = -1.0F;

        bandLeader = nbt.containsUuid("BocchiBandLeader") ? nbt.getUuid("BocchiBandLeader") : null;
        setInvisible(true);
        setSilent(true);
    }
}
