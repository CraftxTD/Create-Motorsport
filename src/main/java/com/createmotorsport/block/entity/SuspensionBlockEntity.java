package com.createmotorsport.block.entity;

import com.createmotorsport.Config;
import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.block.SuspensionBlock;
import com.createmotorsport.physics.TireModel;
import com.createmotorsport.physics.TireSpec;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import net.createmod.catnip.data.Couple;
import net.minecraft.world.SimpleContainer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Collection;
import java.util.List;

public class SuspensionBlockEntity extends SmartBlockEntity implements BlockEntitySubLevelActor, GeoBlockEntity {
    public static final double REST_LENGTH = 7.0 / 16.0;
    public static final double MAX_TRAVEL = 0.25;
    public static final double MAX_DROOP_RENDER = 0.15;
    private static final double MAX_STEER_RAD = Math.toRadians(32.0);
    private static final double GROUND_MARGIN = 0.15;
    private static final int SYNC_INTERVAL_TICKS = 2;

    // Server-side regitries for engine lookup and batched force flushing
    private static final Collection<SuspensionBlockEntity> LOADED = new ObjectOpenHashSet<>();
    private static final Collection<SuspensionBlockEntity> QUEUED = new ObjectOpenHashSet<>();

    public enum WheelSide {
        LEFT, RIGHT
    }

    public enum SuspensionSetting {
        SOFT("Soft", 1.6, 0.35),
        MEDIUM("Medium", 2.2, 0.55),
        FIRM("Firm", 3.0, 0.70),
        RACE("Race", 3.8, 0.90);

        private final String displayName;
        private final double naturalFreqHz;
        private final double dampingRatio;

        SuspensionSetting(String displayName, double naturalFreqHz, double dampingRatio) {
            this.displayName = displayName;
            this.naturalFreqHz = naturalFreqHz;
            this.dampingRatio = dampingRatio;
        }

        public String getDisplayName() {
            return displayName;
        }

        public SuspensionSetting next() {
            SuspensionSetting[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public static class WheelState {
        // simulation (server)
        double omega;
        double springLength = REST_LENGTH;
        boolean grounded;
        double surfaceMu = 1.0;

        // interpolation (client)
        double clientSpringLength = REST_LENGTH;
        double lastClientSpringLength = REST_LENGTH;
        double angle;
        double lastAngle;

        // tire placing animation (client): 0 = none, 1 = deployed
        double deploy;
        double lastDeploy;

        // sync bookkeeping (server)
        double syncedOmega;
        double syncedSpringLength = REST_LENGTH;

        // last used physics values, for csv logging
        double telemLoad;
        double telemSlipRatio;
        double telemSlipAngleRad;
        double telemVLon;
        double telemVLat;
        double telemLongForce;
        double telemLatForce;
        double telemWheelSpeed;
        double telemCompression;
        double telemBrakeTorque;
        double telemGripMult = 1.0;
        double prevLongForce; // last longitudinal tire force, for the relaxation low-pass
    }

    // snapshot of a wheel's last physics step, for csv logging
    public record WheelTelemetry(boolean grounded, double loadN, double slipRatio, double slipAngleDeg,
                                 double vLonMs, double vLatMs, double longForceN, double latForceN,
                                 double omega, double wheelSpeedMs, double springLenM, double compressionM,
                                 double surfaceMu, double brakeTorqueNm, double gripMult, double driveTorqueNm) {
    }

    public WheelTelemetry getTelemetry(WheelSide side) {
        WheelState w = getWheel(side);
        return new WheelTelemetry(w.grounded, w.telemLoad, w.telemSlipRatio,
                Math.toDegrees(w.telemSlipAngleRad), w.telemVLon, w.telemVLat, w.telemLongForce,
                w.telemLatForce, w.omega, w.telemWheelSpeed, w.springLength, w.telemCompression,
                w.surfaceMu * w.telemGripMult, w.telemBrakeTorque, w.telemGripMult, driveTorquePerWheel);
    }


    // redstone link control channels, used by steering menu
    public enum SteerChannel {
        STEER_LEFT("Steer Left"),
        STEER_RIGHT("Steer Right"),
        BRAKE("Brake");

        private final String displayName;

        SteerChannel(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static final SteerChannel[] CHANNELS = SteerChannel.values();
    public static final int CONTROL_SLOT_COUNT = CHANNELS.length * 2;

    public static int channelSlotA(SteerChannel channel) {
        return channel.ordinal() * 2;
    }

    public static int channelSlotB(SteerChannel channel) {
        return channel.ordinal() * 2 + 1;
    }

    private final WheelState leftWheel = new WheelState();
    private final WheelState rightWheel = new WheelState();
    private final NonNullList<ItemStack> tires = NonNullList.withSize(2, ItemStack.EMPTY);
    private final ForceTotal forceTotal = new ForceTotal();

    // two create redstone link frequency slots per named channel
    private final NonNullList<ItemStack> controlItems = NonNullList.withSize(CONTROL_SLOT_COUNT, ItemStack.EMPTY);
    private final SimpleContainer controls = new SimpleContainer(CONTROL_SLOT_COUNT) {
        @Override
        public void setChanged() {
            super.setChanged();
            for (int slot = 0; slot < CONTROL_SLOT_COUNT; slot++) {
                controlItems.set(slot, getItem(slot));
            }
            refreshLinkNetwork();
            SuspensionBlockEntity.this.setChanged();
        }
    };
    private final int[] receivedSignals = new int[CHANNELS.length];
    private final boolean[] registeredLinks = new boolean[CHANNELS.length];
    private final IRedstoneLinkable[] channelLinks = new IRedstoneLinkable[CHANNELS.length];


    private double driverSteer; // -15 to 15, + is left
    private double driverBrake; // 0 to 15
    private long driverControlTime = Long.MIN_VALUE;

    private SuspensionSetting setting = SuspensionSetting.MEDIUM;
    private double driveTorquePerWheel;
    private long driveTorqueGameTime = Long.MIN_VALUE;
    private double brake01;
    private double steerSignal;
    private double chasingSteer;
    private double lastChasingSteer;
    private int syncCooldown;
    private boolean syncDirty;

    // defaults to rear axle, which means the user does need to change the other to 'front' for now, will fix later
    private boolean frontAxle;

    // geckolib clips from blockbench
    private static final RawAnimation AXLE_ROTATION = RawAnimation.begin().thenLoop("axle_rotation");
    private static final RawAnimation AXLE_ROTATION_REVERSE = RawAnimation.begin().thenLoop("axle_rotation_reverse");
    private static final RawAnimation LEFT_SUSPENSION = RawAnimation.begin().thenLoop("left_suspension");
    private static final RawAnimation RIGHT_SUSPENSION = RawAnimation.begin().thenLoop("right_suspension");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public SuspensionBlockEntity(BlockPos pos, BlockState state) {
        super(CreateMotorsport.SUSPENSION_BLOCK_ENTITY.get(), pos, state);
        for (int slot = 0; slot < CONTROL_SLOT_COUNT; slot++) {
            controls.setItem(slot, controlItems.get(slot));
        }
        for (SteerChannel channel : CHANNELS) {
            channelLinks[channel.ordinal()] = new ChannelLink(channel);
        }
    }

    public SimpleContainer getControls() {
        return controls;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    // ===================================================
    // GeckoLib (client)
    // ========================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // wheel spin
        controllers.add(new AnimationController<>(this, "axle", 0, state -> {
            float speed = getWheelAnimationSpeed();
            if (Math.abs(speed) <= 0.001F) {
                state.setControllerSpeed(0.0F);
                return state.setAndContinue(AXLE_ROTATION);
            }
            state.setControllerSpeed(Math.abs(speed));
            return state.setAndContinue(speed < 0.0F ? AXLE_ROTATION_REVERSE : AXLE_ROTATION);
        }));
        // wishbone bob on each side
        controllers.add(new AnimationController<>(this, "left_suspension", 0, state ->
                isSuspensionMoving(WheelSide.LEFT) ? state.setAndContinue(LEFT_SUSPENSION) : PlayState.STOP));
        controllers.add(new AnimationController<>(this, "right_suspension", 0, state ->
                isSuspensionMoving(WheelSide.RIGHT) ? state.setAndContinue(RIGHT_SUSPENSION) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    // animation speed derived from average synced wheel angular velocity
    private float getWheelAnimationSpeed() {
        double avgOmega = 0.5 * (leftWheel.syncedOmega + rightWheel.syncedOmega);
        return (float) (avgOmega / (2.0 * Math.PI));
    }

    private static final double SUSPENSION_BUMP_RATE = 0.03;

    private boolean isSuspensionMoving(WheelSide side) {
        WheelState w = getWheel(side);
        return hasTire(side) && Math.abs(w.clientSpringLength - w.lastClientSpringLength) > SUSPENSION_BUMP_RATE;
    }

    // =======================================
    // Registry and batching force application
    // ====================================

    // =
    @Override
    public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide) {
            LOADED.add(this);
            refreshLinkNetwork();
        }
    }

    @Override
    public void remove() {
        LOADED.remove(this);
        QUEUED.remove(this);
        removeLinks();
        super.remove();
    }

    @Override
    public void onChunkUnloaded() {
        LOADED.remove(this);
        QUEUED.remove(this);
        removeLinks();
        super.onChunkUnloaded();
    }

    // =======================
    // Steering chanels & redstone links
    // ==========================


    public boolean isFrontAxle() {
        return frontAxle;
    }

    // Toggle front/rear from the menu; sendData() pushes the new state to clients for the button's label
    public void toggleAxleEnd() {
        if (level == null || level.isClientSide) {
            return;
        }
        frontAxle = !frontAxle;
        setChanged();
        sendData();
    }

    public void setDriverSteering(double steer, double brake) {
        if (level == null || level.isClientSide) {
            return;
        }
        driverSteer = Mth.clamp(steer, -15.0, 15.0);
        driverBrake = Mth.clamp(brake, 0.0, 15.0);
        driverControlTime = level.getGameTime();
    }

    private boolean driverActive() {
        return level != null && driverControlTime >= level.getGameTime() - 2;
    }

    private int channelSignal(SteerChannel channel) {
        return receivedSignals[channel.ordinal()];
    }

    private boolean hasLink(SteerChannel channel) {
        return !controls.getItem(channelSlotA(channel)).isEmpty()
                || !controls.getItem(channelSlotB(channel)).isEmpty();
    }

    private void refreshLinkNetwork() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (SteerChannel channel : CHANNELS) {
            int i = channel.ordinal();
            if (registeredLinks[i]) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, channelLinks[i]);
                registeredLinks[i] = false;
            }
            if (hasLink(channel)) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, channelLinks[i]);
                registeredLinks[i] = true;
            } else {
                receivedSignals[i] = 0;
            }
        }
    }

    private void removeLinks() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (SteerChannel channel : CHANNELS) {
            int i = channel.ordinal();
            if (registeredLinks[i]) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, channelLinks[i]);
                registeredLinks[i] = false;
            }
        }
    }

    private class ChannelLink implements IRedstoneLinkable {
        private final SteerChannel channel;

        private ChannelLink(SteerChannel channel) {
            this.channel = channel;
        }

        @Override
        public int getTransmittedStrength() {
            return 0;
        }

        @Override
        public void setReceivedStrength(int power) {
            receivedSignals[channel.ordinal()] = power;
        }

        @Override
        public boolean isListening() {
            return hasLink(channel);
        }

        @Override
        public boolean isAlive() {
            return level != null && !level.isClientSide && !isRemoved() && level.isLoaded(worldPosition);
        }

        @Override
        public Couple<RedstoneLinkNetworkHandler.Frequency> getNetworkKey() {
            return Couple.create(
                    RedstoneLinkNetworkHandler.Frequency.of(controls.getItem(channelSlotA(channel))),
                    RedstoneLinkNetworkHandler.Frequency.of(controls.getItem(channelSlotB(channel)))
            );
        }

        @Override
        public BlockPos getLocation() {
            return worldPosition;
        }
    }


    // server thread only; collection of all suspensions loaded, engines filters by sub-level.
    public static Collection<SuspensionBlockEntity> allLoaded() {
        return LOADED;
    }


    // applies queued force once per physics substep from the Sable pre-tick event
    public static void flushBatchedForces(ServerLevel level, double timeStep) {
        ObjectOpenHashSet<ServerSubLevel> dragged = new ObjectOpenHashSet<>();
        for (SuspensionBlockEntity be : QUEUED) {
            if (be.isRemoved()) {
                continue;
            }
            SubLevel subLevel = Sable.HELPER.getContaining(be);
            if (!(subLevel instanceof ServerSubLevel serverSubLevel)) {
                continue;
            }
            RigidBodyHandle handle = RigidBodyHandle.of(serverSubLevel);
            if (handle != null) {
                if (dragged.add(serverSubLevel)) {
                    applyDragCompensation(level, serverSubLevel, be.forceTotal, timeStep);
                }
                handle.applyForcesAndReset(be.forceTotal);
            }
        }
        QUEUED.clear();
    }


    // fake force to counteract sables velocity-proportional linear damping (universal_drag)
    // Due to scaling of forces in our units, this standard force is about 7x too strong,
    // so this is still a cheap fix just to speed up the cars for now
    private static void applyDragCompensation(ServerLevel level, ServerSubLevel subLevel,
                                              ForceTotal forceTotal, double dt) {
        double keep = Config.SABLE_DRAG_SCALE.getAsDouble();
        if (keep >= 1.0) {
            return; // full Sable drag, nothing to cancel
        }
        MassData massData = subLevel.getMassTracker();
        if (massData == null || massData.isInvalid()) {
            return;
        }
        double drag = DimensionPhysicsData.getUniversalDrag(level);
        if (drag <= 0.0) {
            return;
        }
        double mass = massData.getMass();
        Vector3d localVel = subLevel.logicalPose().orientation()
                .transformInverse(new Vector3d(subLevel.latestLinearVelocity));
        Vector3d impulse = localVel.mul(mass * drag * (1.0 - keep) * dt);
        forceTotal.applyLinearImpulse(impulse);
    }

    // ==============================================
    // Game tick
    // ===============================

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            clientTick();
            return;
        }


        // trying to auto recognize steering signal on the same sub-level
        Direction facing = getFacing();
        double newSteer;
        double newBrake;
        if (driverActive()) {
            newSteer = driverSteer;
            newBrake = driverBrake / 15.0;
        } else {
            int adjacentRight = level.getSignal(worldPosition.relative(facing), facing);
            int adjacentLeft = level.getSignal(worldPosition.relative(facing.getOpposite()), facing.getOpposite());
            int left = Math.max(adjacentLeft, channelSignal(SteerChannel.STEER_LEFT));
            int right = Math.max(adjacentRight, channelSignal(SteerChannel.STEER_RIGHT));
            newSteer = Mth.clamp(left - right, -15, 15);
            int adjacentBrake = level.getSignal(worldPosition.above(), Direction.UP);
            newBrake = Math.max(adjacentBrake, channelSignal(SteerChannel.BRAKE)) / 15.0;
        }
        if (Math.abs(newSteer - steerSignal) > 0.05) {
            steerSignal = newSteer;
            syncDirty = true;
        }
        brake01 = newBrake;

        lastChasingSteer = chasingSteer;
        chasingSteer = Mth.lerp(0.4, chasingSteer, steerSignal / 15.0 * MAX_STEER_RAD * speedSteerLock());


        // torque stops when engine stops ticking
        if (level.getGameTime() - driveTorqueGameTime > 3) {
            driveTorquePerWheel = 0.0;
        }

        if (syncCooldown > 0) {
            syncCooldown--;
        }
        if ((syncDirty || wheelStateChangedEnough()) && syncCooldown <= 0) {
            syncDirty = false;
            syncCooldown = SYNC_INTERVAL_TICKS;
            for (WheelState w : new WheelState[]{leftWheel, rightWheel}) {
                w.syncedOmega = w.omega;
                w.syncedSpringLength = w.springLength;
            }
            sendData();
        }
    }

    private boolean wheelStateChangedEnough() {
        for (WheelState w : new WheelState[]{leftWheel, rightWheel}) {
            if (Math.abs(w.omega - w.syncedOmega) > 0.4 || Math.abs(w.springLength - w.syncedSpringLength) > 0.015) {
                return true;
            }
        }
        return false;
    }

    // speed sensitive steering lock; one of the controller assists; 1/(1 + k*v^2)
    private double speedSteerLock() {
        double k = Config.STEER_SPEED_SENSITIVITY.getAsDouble();
        if (k <= 0.0 || level == null) {
            return 1.0;
        }
        double v = Sable.HELPER.getVelocity(level, Vec3.atCenterOf(worldPosition)).length();
        return 1.0 / (1.0 + k * v * v);
    }

    private void clientTick() {
        lastChasingSteer = chasingSteer;
        chasingSteer = Mth.lerp(0.4, chasingSteer, steerSignal / 15.0 * MAX_STEER_RAD * speedSteerLock());
        for (WheelSide side : WheelSide.values()) {
            WheelState w = getWheel(side);
            w.lastAngle = w.angle;
            w.angle += w.omega / 20.0;
            w.lastClientSpringLength = w.clientSpringLength;
            w.clientSpringLength = Mth.lerp(0.5, w.clientSpringLength, w.springLength);
            // Ease the tire down when it is first installed (mirrors the wheel mount's drop-in).
            w.lastDeploy = w.deploy;
            w.deploy = Mth.lerp(0.25, w.deploy, hasTire(side) ? 1.0 : 0.0);
        }
    }

    // =======================
    // Physics substep
    // ================

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (!hasAnyTire()) {
            return;
        }
        Pose3d pose = subLevel.logicalPose();
        MassData massData = subLevel.getMassTracker();
        if (massData.isInvalid()) {
            return;
        }

        boolean queued = false;
        queued |= stepWheel(WheelSide.LEFT, pose, massData, timeStep);
        queued |= stepWheel(WheelSide.RIGHT, pose, massData, timeStep);
        applyDifferential(timeStep);
        if (queued) {
            QUEUED.add(this);
        }
    }


    // Limited-slip differential model from vdrift (driveline.h solveDiffClutch2)
    // the velocity constraint pulls the two wheels torward a shared speed, but the corrective torque is capped by
    // an anti-slip limit so it slips instead of locking. antiSlip=0 is open differentials, big number is a spool
    private void applyDifferential(double dt) {
        if (!hasTire(WheelSide.LEFT) || !hasTire(WheelSide.RIGHT)) {
            return;
        }
        double antiSlip = Config.DIFFERENTIAL_ANTISLIP_TORQUE.getAsDouble();
        if (antiSlip <= 0.0) {
            return;
        }
        double inertiaL = wheelInertia(WheelSide.LEFT);
        double inertiaR = wheelInertia(WheelSide.RIGHT);

        // equal and opposite impulse driving two wheels to the same speed, clamped by anti-slip torque budget for the substep
        double velErr = leftWheel.omega - rightWheel.omega;
        double jointInertia = 1.0 / (1.0 / inertiaL + 1.0 / inertiaR);
        double limit = antiSlip * dt;
        double lambda = Mth.clamp(-velErr * jointInertia, -limit, limit);

        leftWheel.omega += lambda / inertiaL;
        rightWheel.omega -= lambda / inertiaR;
    }

    private double wheelInertia(WheelSide side) {
        TireLike tire = getTire(side).get(OffroadDataComponents.TIRE);
        double radius = tire != null ? tire.radius() : REST_LENGTH;
        return Math.max(0.5, 5.0 * radius * radius * radius * radius);
    }

    private boolean stepWheel(WheelSide side, Pose3d pose, MassData massData, double dt) {
        WheelState wheel = getWheel(side);
        TireLike tire = getTire(side).get(OffroadDataComponents.TIRE);
        if (tire == null) {
            wheel.grounded = false;
            wheel.springLength = Mth.lerp(0.5, wheel.springLength, REST_LENGTH);
            return false;
        }

        double radius = tire.radius();
        Direction facing = getFacing();
        Direction sideDir = getSideDirection(side);
        Vec3 hardpoint = worldPosition.relative(sideDir).getCenter();
        Vector3d hardpointJoml = JOMLConversion.toJOML(hardpoint);

        // steering
        Vector3d forward = new Vector3d(facing.getStepX(), 0.0, facing.getStepZ()).rotateY(chasingSteer);
        Direction rightDir = facing.getClockWise();
        Vector3d axle = new Vector3d(rightDir.getStepX(), 0.0, rightDir.getStepZ()).rotateY(chasingSteer);

        TerrainCastResult cast = castToTerrain(hardpoint, forward, pose);
        double distance = cast.distance();
        boolean grounded = distance <= REST_LENGTH + radius + GROUND_MARGIN;
        wheel.grounded = grounded;
        wheel.surfaceMu = cast.hitBlock() != null
                ? fudgeFriction(PhysicsBlockPropertyHelper.getFriction(level.getBlockState(cast.hitBlock())))
                : 1.0;

        double wheelInertia = Math.max(0.5, 5.0 * radius * radius * radius * radius);
        double brakeTorque = brake01 * 2000.0 * radius;

        if (!grounded) {
            wheel.springLength = Mth.clamp(Mth.lerp(0.4, wheel.springLength, REST_LENGTH + MAX_DROOP_RENDER),
                    REST_LENGTH - MAX_TRAVEL, REST_LENGTH + MAX_DROOP_RENDER);
            wheel.omega = TireModel.integrateSpin(wheel.omega, radius, wheelInertia, driveTorquePerWheel,
                    brakeTorque, 0.0, wheel.omega * radius, dt) * 0.995;
            wheel.telemLoad = 0.0;
            wheel.telemLongForce = 0.0;
            wheel.telemLatForce = 0.0;
            wheel.telemSlipRatio = 0.0;
            wheel.telemSlipAngleRad = 0.0;
            wheel.telemVLon = 0.0;
            wheel.telemVLat = 0.0;
            wheel.telemCompression = 0.0;
            wheel.telemBrakeTorque = brakeTorque;
            wheel.telemWheelSpeed = wheel.omega * radius;
            return false;
        }

        wheel.springLength = Mth.clamp(distance - radius, REST_LENGTH - MAX_TRAVEL, REST_LENGTH + MAX_DROOP_RENDER);
        double compression = Math.max(0.0, REST_LENGTH - wheel.springLength);

        // mass per corner, Sable gives the exact jacobian denominator
        double invNormalMass = massData.getInverseNormalMass(hardpointJoml, OrientedBoundingBox3d.UP);
        if (invNormalMass <= 1.0e-9) {
            return false;
        }
        double effectiveMass = 1.0 / invNormalMass;

        Vector3d velocity = Sable.HELPER.getVelocity(level, hardpointJoml, new Vector3d());
        Vector3d localVelocity = pose.transformNormalInverse(velocity);

        double springForce = TireModel.suspensionForce(effectiveMass, setting.naturalFreqHz, setting.dampingRatio,
                compression, localVelocity.y);

        // slope correction (got it from Bullet's clipped_inv_contact_dot_suspension)
        Vector3d hitNormal = new Vector3d(cast.normal().getStepX(), cast.normal().getStepY(), cast.normal().getStepZ());
        if (cast.hitSubLevel() != null) {
            cast.hitSubLevel().logicalPose().transformNormal(hitNormal);
        }
        pose.transformNormalInverse(hitNormal);
        if (hitNormal.lengthSquared() < 1.0e-6 || hitNormal.y < 0.05) {
            hitNormal.set(0.0, 1.0, 0.0);
        } else {
            hitNormal.normalize();
        }
        springForce *= Mth.clamp(1.0 / Math.max(0.5, hitNormal.y), 1.0, 2.0);
        springForce = Math.min(springForce, Config.MAX_CORNERING_G.getAsDouble() * effectiveMass * 9.81);

        Vector3d springImpulse = new Vector3d(hitNormal).mul(springForce * dt);
        forceTotal.applyImpulseAtPoint(massData, hardpointJoml, springImpulse);

        // ================================
        // Load sensitive tire friction
        // ===================================
        double normalForce = springForce;
        double vLon = localVelocity.dot(forward);
        double vLat = localVelocity.dot(axle);


        // per-axle tire tier so front grips better than rear, should help the turning be more natural
        TireSpec tireSpec = isFrontAxle() ? TireSpec.RACING_SLICK_FRONT : TireSpec.RACING_SLICK_REAR;

        float designLoad = getTire(side).getOrDefault(CreateMotorsport.TIRE_DESIGN_LOAD, 0.0f);
        double gripMult = TireModel.loadSensitivity(normalForce, designLoad, tireSpec);
        double effectiveMu = wheel.surfaceMu * gripMult;
        wheel.telemGripMult = gripMult;

        // non driving or braked wheels take from offroad wheel mount, just track ground so it cant start creating slip force and maybe wont roll as much
        boolean powered = Math.abs(driveTorquePerWheel) > 1.0e-3 || brakeTorque > 1.0e-3;
        double forwardImpulse;
        double sideImpulse;

        if (Config.SIM_TIRE_MODEL.get()) {

            // ------ TOGGLEABLE "SIM" mode for tires --------------------------------
            // If the driving mode is set to "Racing Sim" instead of "Arcade", then this is the section that takes
            // one combined slip force split between longitudinal and lateral, so any grip spent accelerating or braking is taken away from cornering
            // this lets the car break away and spin

            if (!powered) {
                wheel.omega = vLon / radius;
            }
            double wheelSpeed = wheel.omega * radius;
            double denom = Math.max(Math.abs(vLon), 2.0);
            double slipLon = (wheelSpeed - vLon) / denom;
            double slipLat = vLat / Math.max(Math.abs(vLon), 0.05);
            double s = Math.sqrt(slipLon * slipLon + slipLat * slipLat);

            double forwardForce = 0.0;
            double lateralForce = 0.0;
            if (s > 1.0e-5) {
                double fMag = normalForce * effectiveMu * tireSpec.grip()
                        * TireModel.slipCurve(Math.min(s, Config.SIM_SLIP_LIMIT.getAsDouble()),
                                tireSpec.pacejkaB(), tireSpec.pacejkaC(), tireSpec.pacejkaE());
                forwardForce = fMag * slipLon / s;
                lateralForce = -fMag * slipLat / s;
            }
            if (!powered) {
                forwardForce = -TireModel.rollingResistance(normalForce, vLon);
            }
            forwardForce = wheel.prevLongForce + (forwardForce - wheel.prevLongForce) * Config.TIRE_FORCE_RELAXATION.getAsDouble();
            wheel.prevLongForce = forwardForce;


            // low speed blending to make it stabilize and not jitter
            double speed = Math.sqrt(vLon * vLon + vLat * vLat);
            double slipBlend = Mth.clamp(speed / Config.SIM_LOWSPEED_BLEND_MS.getAsDouble(), 0.0, 1.0);
            double invSideMass = massData.getInverseNormalMass(hardpointJoml, axle);
            double cancelForce = invSideMass > 1.0e-9 ? (-vLat / invSideMass * Config.LATERAL_GRIP_FRACTION.getAsDouble()) / dt : 0.0;
            lateralForce = Mth.lerp(slipBlend, cancelForce, lateralForce);

            forwardImpulse = forwardForce * dt;
            sideImpulse = lateralForce * dt;

            if (powered) {
                wheel.omega = TireModel.integrateSpin(wheel.omega, radius, wheelInertia, driveTorquePerWheel,
                        brakeTorque, forwardForce, vLon, dt);
            }
        } else {

            // ------ TOGGLEABLE "ARCADE" mode for tires --------------------------------
            double tireForce;
            if (powered) {
                tireForce = TireModel.longitudinalForce(normalForce, effectiveMu, wheel.omega * radius, vLon, tireSpec);
            } else {
                wheel.omega = vLon / radius; // roll with the ground, no slip
                tireForce = -TireModel.rollingResistance(normalForce, vLon);
            }
            // Speed-dreams runs FLOAT_RELAXATION2 on the tire force to relax longitudinal force towards its target, to damp oscillation
            tireForce = wheel.prevLongForce + (tireForce - wheel.prevLongForce) * Config.TIRE_FORCE_RELAXATION.getAsDouble();
            wheel.prevLongForce = tireForce;
            forwardImpulse = tireForce * dt;

            // cancel a fraction of the lateral velocity per substep same way Bullet does
            double invSideMass = massData.getInverseNormalMass(hardpointJoml, axle);
            sideImpulse = invSideMass > 1.0e-9 ? -vLat * (1.0 / invSideMass) * Config.LATERAL_GRIP_FRACTION.getAsDouble() : 0.0;

            double maxImpulse = normalForce * effectiveMu * tireSpec.grip() * dt;
            double ellipseScale = TireModel.frictionEllipseScale(forwardImpulse, sideImpulse, maxImpulse);
            forwardImpulse *= ellipseScale;
            sideImpulse *= ellipseScale;

            if (powered) {
                wheel.omega = TireModel.integrateSpin(wheel.omega, radius, wheelInertia, driveTorquePerWheel,
                        brakeTorque, forwardImpulse / dt, vLon, dt);
            }
        }


        // recording the resolved physics for csv logging
        wheel.telemLoad = normalForce;
        wheel.telemVLon = vLon;
        wheel.telemVLat = vLat;
        wheel.telemSlipRatio = (wheel.omega * radius - vLon) / Math.max(Math.abs(vLon), 2.0);
        wheel.telemSlipAngleRad = Math.atan2(vLat, Math.max(Math.abs(vLon), 0.05));
        wheel.telemLongForce = forwardImpulse / dt;
        wheel.telemLatForce = sideImpulse / dt;
        wheel.telemWheelSpeed = wheel.omega * radius;
        wheel.telemCompression = compression;
        wheel.telemBrakeTorque = brakeTorque;


        // Forward forward at the wheel center, prevents barrel rolls. Thanks again bullet
        Vector3d contactPoint = new Vector3d(hardpointJoml).sub(0.0, wheel.springLength, 0.0);
        forceTotal.applyImpulseAtPoint(massData, contactPoint, new Vector3d(forward).mul(forwardImpulse));

        Vector3d sidePoint = new Vector3d(contactPoint);
        if (massData.getCenterOfMass() != null) {
            double heightAboveCom = contactPoint.y - massData.getCenterOfMass().y();
            double rollInfluence = Config.ROLL_INFLUENCE.getAsDouble();
            sidePoint.y -= heightAboveCom * (1.0 - rollInfluence);
        }
        forceTotal.applyImpulseAtPoint(massData, sidePoint, new Vector3d(axle).mul(sideImpulse));

        return true;
    }

    // wheel mount gives slight friction even at 0
    private static double fudgeFriction(double realValue) {
        return realValue < 1.0 ? 0.1 + 0.9 * realValue : realValue;
    }

    // =========================================================================
    // Terrain raycast adapted from wheel mount
    // =====================================================================

    private record TerrainCastResult(double distance, @NotNull Direction normal,
                                     @Nullable SubLevel hitSubLevel, @Nullable BlockPos hitBlock) {
    }

    private TerrainCastResult castToTerrain(Vec3 wheelCenter, Vector3d forward, Pose3dc pose) {
        double minDistance = 5.0;
        Direction minNormal = Direction.UP;
        SubLevel minHitSubLevel = null;
        BlockPos minHitBlock = null;

        for (int i = -1; i <= 1; i++) {
            Vec3 origin = wheelCenter.add(JOMLConversion.toMojang(forward).scale(i));

            ClipContext clipContext = new ClipContext(origin, origin.subtract(0.0, 5.0, 0.0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
            ((ClipContextExtension) clipContext).sable$setIgnoredSubLevel(Sable.HELPER.getContaining(this));
            BlockHitResult clipResult = level.clip(clipContext);

            if (clipResult.getType() == HitResult.Type.MISS) {
                continue;
            }

            SubLevel hitSubLevel = Sable.HELPER.getContaining(level, clipResult.getLocation());
            Vec3 localHitPos = pose.transformPositionInverse(hitSubLevel == null
                    ? clipResult.getLocation()
                    : hitSubLevel.logicalPose().transformPosition(clipResult.getLocation()));

            if (localHitPos.y > wheelCenter.y || origin.distanceTo(localHitPos) < 0.05) {
                continue;
            }

            double dist = wheelCenter.y - localHitPos.y;
            if (dist <= 1.0e-5) {
                continue;
            }

            Direction dir = clipResult.getDirection();
            Vector3d hitNormal = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());
            if (hitSubLevel != null) {
                hitSubLevel.logicalPose().transformNormal(hitNormal);
            }
            pose.transformNormalInverse(hitNormal);
            if (hitNormal.dot(0.0, 1.0, 0.0) < 0.5) {
                continue;
            }

            if (dist < minDistance) {
                minDistance = dist;
                minNormal = clipResult.getDirection();
                minHitSubLevel = hitSubLevel;
                minHitBlock = clipResult.getBlockPos();
            }
        }

        return new TerrainCastResult(minDistance, minNormal, minHitSubLevel, minHitBlock);
    }

    // ===============================================
    // Engine interface
    // ===================

    // called by an engine block on the same sublevel as suspension. Once per game tick. Torque is per wheel
    public void applyDriveTorque(double torquePerWheel, long gameTime) {
        if (gameTime == driveTorqueGameTime) {
            driveTorquePerWheel += torquePerWheel;
        } else {
            driveTorquePerWheel = torquePerWheel;
            driveTorqueGameTime = gameTime;
        }
    }

    // average signed wheel speed (rad/s)
    public double averageDrivenOmega(int sign) {
        double sum = 0.0;
        int count = 0;
        for (WheelSide side : WheelSide.values()) {
            if (hasTire(side)) {
                sum += getWheel(side).omega;
                count++;
            }
        }
        return count == 0 ? 0.0 : sum / count * sign;
    }

    public boolean hasAnyTire() {
        return hasTire(WheelSide.LEFT) || hasTire(WheelSide.RIGHT);
    }

    public boolean hasTire(WheelSide side) {
        return getTire(side).get(OffroadDataComponents.TIRE) != null;
    }

    // ===============================================================
    // Tires
    // =============

    public ItemStack getTire(WheelSide side) {
        return tires.get(side.ordinal());
    }

    public boolean installTire(WheelSide side, ItemStack stack) {
        if (!getTire(side).isEmpty() || stack.get(OffroadDataComponents.TIRE) == null) {
            return false;
        }
        tires.set(side.ordinal(), stack.copyWithCount(1));
        notifyUpdate();
        invalidateRenderBoundingBox();
        return true;
    }

    public ItemStack removeTire(WheelSide side) {
        ItemStack removed = tires.get(side.ordinal());
        if (!removed.isEmpty()) {
            tires.set(side.ordinal(), ItemStack.EMPTY);
            getWheel(side).omega = 0.0;
            notifyUpdate();
            invalidateRenderBoundingBox();
        }
        return removed;
    }

    public NonNullList<ItemStack> getTires() {
        return tires;
    }

    // =======================================================
    // accessors
    // =====================================


    public SuspensionSetting getSetting() {
        return setting;
    }

    // server-side, current steer angle of this axle (rad)
    public double getSteerAngleRad() {
        return chasingSteer;
    }

    public double getTireRadius() {
        for (WheelSide side : WheelSide.values()) {
            TireLike tire = getTire(side).get(OffroadDataComponents.TIRE);
            if (tire != null) {
                return tire.radius();
            }
        }
        return 0.0;
    }

    public SuspensionSetting cycleSetting() {
        setting = setting.next();
        notifyUpdate();
        return setting;
    }

    public Direction getFacing() {
        return getBlockState().getValue(SuspensionBlock.FACING);
    }

    public Direction getSideDirection(WheelSide side) {
        Direction facing = getFacing();
        return side == WheelSide.LEFT ? facing.getCounterClockWise() : facing.getClockWise();
    }

    public WheelState getWheel(WheelSide side) {
        return side == WheelSide.LEFT ? leftWheel : rightWheel;
    }


    // Renderer accessors
    public float getLerpedSpringLength(WheelSide side, float partialTicks) {
        WheelState w = getWheel(side);
        return (float) Mth.lerp(partialTicks, w.lastClientSpringLength, w.clientSpringLength);
    }

    public float getLerpedAngle(WheelSide side, float partialTicks) {
        WheelState w = getWheel(side);
        return (float) Mth.lerp(partialTicks, w.lastAngle, w.angle);
    }

    public float getLerpedSteer(float partialTicks) {
        return (float) Mth.lerp(partialTicks, lastChasingSteer, chasingSteer);
    }

    // Tire placement animation
    public float getLerpedDeploy(WheelSide side, float partialTicks) {
        WheelState w = getWheel(side);
        return (float) Mth.lerp(partialTicks, w.lastDeploy, w.deploy);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2.5, 2.0, 2.5);
    }

    // ==============================================================
    // NBT
    // ===============================

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ContainerHelper.saveAllItems(tag, tires, registries);
        CompoundTag controlsTag = new CompoundTag();
        ContainerHelper.saveAllItems(controlsTag, controlItems, registries);
        tag.put("Controls", controlsTag);
        tag.putString("Setting", setting.name());
        tag.putBoolean("FrontAxle", frontAxle);
        if (clientPacket) {
            tag.putDouble("SteerSignal", steerSignal);
            tag.putFloat("LeftOmega", (float) leftWheel.omega);
            tag.putFloat("RightOmega", (float) rightWheel.omega);
            tag.putFloat("LeftSpring", (float) leftWheel.springLength);
            tag.putFloat("RightSpring", (float) rightWheel.springLength);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        for (int slot = 0; slot < tires.size(); slot++) {
            tires.set(slot, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag, tires, registries);

        for (int slot = 0; slot < CONTROL_SLOT_COUNT; slot++) {
            controlItems.set(slot, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag.getCompound("Controls"), controlItems, registries);
        for (int slot = 0; slot < CONTROL_SLOT_COUNT; slot++) {
            controls.setItem(slot, controlItems.get(slot));
        }
        refreshLinkNetwork();

        try {
            setting = SuspensionSetting.valueOf(tag.getString("Setting"));
        } catch (IllegalArgumentException ignored) {
            setting = SuspensionSetting.MEDIUM;
        }
        frontAxle = tag.getBoolean("FrontAxle");
        if (clientPacket) {
            steerSignal = tag.getDouble("SteerSignal");
            leftWheel.omega = tag.getFloat("LeftOmega");
            rightWheel.omega = tag.getFloat("RightOmega");
            leftWheel.springLength = tag.getFloat("LeftSpring");
            rightWheel.springLength = tag.getFloat("RightSpring");
        }
    }
}
