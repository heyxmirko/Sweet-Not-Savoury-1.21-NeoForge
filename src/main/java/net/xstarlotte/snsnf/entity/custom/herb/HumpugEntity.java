package net.xstarlotte.snsnf.entity.custom.herb;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.event.EventHooks;
import net.xstarlotte.snsnf.entity.SNSEntity;
import net.xstarlotte.snsnf.item.SNSItem;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HumpugEntity extends TamableAnimal implements GeoEntity {

    private static final EntityDataAccessor<Boolean> SITTING =
            SynchedEntityData.defineId(HumpugEntity.class, EntityDataSerializers.BOOLEAN);

    public HumpugEntity(EntityType<? extends HumpugEntity> type, Level level) {
        super(type, level);

    //animations

    }
    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        if(tAnimationState.isMoving()) {
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("animation.humpug.running", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        if(this.isSitting()) {
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("animation.humpug.sitting", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        tAnimationState.getController().setAnimation(RawAnimation.begin().then("animation.humpug.idle", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

//AI

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new SitWhenOrderedToGoal(this));

        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 3.0, true));
        this.goalSelector.addGoal(1, new OwnerHurtByTargetGoal(this));

        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new OwnerHurtTargetGoal(this));

        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 2d, 10f, 2f));

        this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1d));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.2D, Ingredient.of(SNSItem.CANDY_CANE_SUGAR.get()), true));

        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));

        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 4f));

        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes().add(Attributes.MAX_HEALTH, 35D)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 5f)
                .add(Attributes.FOLLOW_RANGE, 24D);
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(SNSItem.CANDY_CANE_SUGAR.get());
    }
    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return SNSEntity.HUMPUG.get().create(level);
    }

    //tameable

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        Item item = itemstack.getItem();
        Item itemForTaming = SNSItem.CANDY_CANE.get();
        if(item == itemForTaming && !isTame()) {
            if(this.level().isClientSide()) {
                return InteractionResult.CONSUME;
            } else {
                if (!pPlayer.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                if (!EventHooks.onAnimalTame(this, pPlayer)) {
                    super.tame(pPlayer);
                    this.navigation.recomputePath();
                    this.setTarget(null);
                    this.level().broadcastEntityEvent(this, (byte)7);
                    setSitting(true);
                }
                return InteractionResult.SUCCESS;
            }
        }
        if(isTame() && pHand == InteractionHand.MAIN_HAND && !isFood(itemstack)) {
            setSitting(!isSitting());
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(pPlayer, pHand);
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING);
    }

    public void setSitting(boolean sitting) {
        this.entityData.set(SITTING, sitting);
        this.setOrderedToSit(sitting);
    }

    //data

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        setSitting(pCompound.getBoolean("isSitting"));
        super.readAdditionalSaveData(pCompound);
    }



    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.putBoolean("isSitting", this.isSitting());
        super.addAdditionalSaveData(pCompound);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        pBuilder.define(SITTING, false);
        super.defineSynchedData(pBuilder);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason,
                                        @Nullable SpawnGroupData pSpawnData) {
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
    }


}
