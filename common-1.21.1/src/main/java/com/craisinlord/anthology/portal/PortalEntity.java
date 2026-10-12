package com.craisinlord.anthology.portal;

import com.craisinlord.anthology.Anthology;
import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.mixins.portal.AbstractArrowAccessor;
import com.craisinlord.anthology.mixins.portal.ChunkMapAccessor;
import com.craisinlord.anthology.mixins.portal.FallingBlockEntityAccessor;
import com.craisinlord.anthology.mixins.portal.ServerEntityAccessor;
import com.craisinlord.anthology.mixins.portal.ServerGamePacketListenerTeleportAccessor;
import com.craisinlord.anthology.mixins.portal.TrackedEntityAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public abstract class PortalEntity extends Entity {
    private static final EntityDataAccessor<Integer> FACING = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> UP_AXIS = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> LINKED_PORTAL = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> PORTAL_WIDTH = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PORTAL_HEIGHT = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OPENING_ANIMATION_ID = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    public static final int OPEN_TICKS = 5;
    public static final int TELEPORT_COOLDOWN_TICKS = 3;
    private static final String DEBUG_TRAVERSAL_PROPERTY = "anthology.portal.debugTraversal";
    private static final double PORTAL_INSIDE_SHIFT = 0.05D;
    private static final double HALF_DEPTH = 0.35D;
    private static final double MAX_CLIENT_TRANSIT_START_ERROR_SQR = 1.0D;
    private static final double MAX_CLIENT_TRANSIT_MOVEMENT_SQR = 100.0D;
    private static final double PROJECTILE_PLANE_TOLERANCE = 0.1D;
    private final Map<UUID, Integer> teleportCooldowns = new HashMap<>();
    private UUID linkedPortalId;
    private int ageTicks;
    private int lastOpeningAnimationId;
    private int pairTime;
    private PortalShape cachedShape;
    private Vec3 cachedShapePosition;
    private int cachedShapeFacing;
    private int cachedShapeUpAxis;
    private int cachedShapeWidth;
    private int cachedShapeHeight;

    protected PortalEntity(EntityType<? extends PortalEntity> entityType, Level level) {
        super(entityType, level);
        this.setInvulnerable(true);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public abstract PortalAppearance appearance();

    protected abstract boolean isStillValid(ServerLevel level);

    protected boolean tickBeforeTransit(ServerLevel level) {
        return false;
    }

    protected void clientTick() {
    }

    public boolean hasCustomView() {
        return false;
    }

    protected PortalSounds sounds() {
        return PortalSounds.NONE;
    }

    protected float ambientPitch() {
        return 1.0F;
    }

    protected double minFloorExitSpeed() {
        return PortalHooks.settings().minFloorExitSpeed();
    }

    protected double maxExitSpeed() {
        return PortalHooks.settings().maxExitSpeed();
    }

    public boolean canTransit(Entity entity) {
        return !PortalHooks.isTransitExcluded(entity);
    }

    protected void saveExtra(CompoundTag tag) {
    }

    protected void loadExtra(CompoundTag tag) {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FACING, Direction.NORTH.get3DDataValue());
        builder.define(UP_AXIS, Direction.UP.get3DDataValue());
        builder.define(LINKED_PORTAL, Optional.empty());
        builder.define(PORTAL_WIDTH, 1);
        builder.define(PORTAL_HEIGHT, 2);
        builder.define(OPENING_ANIMATION_ID, 0);
    }

    protected void setGeometry(Direction facing, Direction upAxis, int width, int height) {
        this.entityData.set(FACING, facing.get3DDataValue());
        this.entityData.set(UP_AXIS, upAxis.get3DDataValue());
        this.entityData.set(PORTAL_WIDTH, width);
        this.entityData.set(PORTAL_HEIGHT, height);
    }

    public void linkTo(PortalEntity other) {
        this.linkedPortalId = other.getUUID();
        this.entityData.set(LINKED_PORTAL, Optional.of(other.getUUID()));
        this.pairTime = (int) this.level().getGameTime();
    }

    public void restorePair(UUID linkedPortalId, int pairTime) {
        this.linkedPortalId = linkedPortalId;
        this.entityData.set(LINKED_PORTAL, Optional.ofNullable(linkedPortalId));
        this.pairTime = pairTime;
    }

    protected void clearLink() {
        this.linkedPortalId = null;
        this.entityData.set(LINKED_PORTAL, Optional.empty());
    }

    public Direction getFacingDirection() {
        return Direction.from3DDataValue(this.entityData.get(FACING));
    }

    public Direction getUpAxis() {
        return Direction.from3DDataValue(this.entityData.get(UP_AXIS));
    }

    public UUID getLinkedPortalId() {
        return this.entityData.get(LINKED_PORTAL).orElse(this.linkedPortalId);
    }

    public int getPortalWidth() {
        return this.entityData.get(PORTAL_WIDTH);
    }

    public int getPortalHeight() {
        return this.entityData.get(PORTAL_HEIGHT);
    }

    public int getAgeTicks() {
        return this.ageTicks;
    }

    public int getPairTime() {
        return this.pairTime;
    }

    public void restartOpeningAnimation() {
        int animationId = this.entityData.get(OPENING_ANIMATION_ID) + 1;
        this.entityData.set(OPENING_ANIMATION_ID, animationId);
        this.lastOpeningAnimationId = animationId;
        this.ageTicks = 0;
    }

    public PortalEntity getLinkedPortal() {
        UUID linkedId = this.getLinkedPortalId();
        if (linkedId == null) {
            return null;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getEntity(linkedId) instanceof PortalEntity portal && !portal.isRemoved() ? portal : null;
        }
        return PortalRegistry.find(this.level(), linkedId);
    }

    public PortalEntity findLinkedPortal(ServerLevel level) {
        if (this.linkedPortalId == null) {
            return null;
        }
        return level.getEntity(this.linkedPortalId) instanceof PortalEntity portal ? portal : null;
    }

    public Vec3 getNormalVec() {
        return Vec3.atLowerCornerOf(this.getFacingDirection().getNormal());
    }

    public Vec3 getUpVec() {
        return Vec3.atLowerCornerOf(this.getUpAxis().getNormal());
    }

    public Vec3 getWidthVec() {
        return this.getNormalVec().cross(this.getUpVec()).normalize();
    }

    public PortalShape getWorldPortalShape() {
        Vec3 position = this.position();
        int facing = this.entityData.get(FACING);
        int upAxis = this.entityData.get(UP_AXIS);
        int width = this.getPortalWidth();
        int height = this.getPortalHeight();
        PortalShape shape = this.cachedShape;
        if (shape == null || !position.equals(this.cachedShapePosition) || facing != this.cachedShapeFacing
                || upAxis != this.cachedShapeUpAxis || width != this.cachedShapeWidth || height != this.cachedShapeHeight) {
            shape = new PortalShape(position, this.getNormalVec().normalize(), this.getUpVec().normalize(), this.getWidthVec().normalize(), width / 2.0D, height / 2.0D, HALF_DEPTH);
            this.cachedShape = shape;
            this.cachedShapePosition = position;
            this.cachedShapeFacing = facing;
            this.cachedShapeUpAxis = upAxis;
            this.cachedShapeWidth = width;
            this.cachedShapeHeight = height;
        }
        return shape;
    }

    public AABB getPlane() {
        return this.getWorldPortalShape().getPlane();
    }

    public AABB getFlatPlane() {
        return this.getWorldPortalShape().getFlatPlane();
    }

    public AABB getScanRange() {
        return this.getWorldPortalShape().getScanRange();
    }

    public AABB getPortalInsides() {
        return this.getWorldPortalShape().getPortalInsides();
    }

    public AABB getPortalInsides(Entity entity) {
        double shiftAmount = Math.min(PORTAL_INSIDE_SHIFT, Math.abs(this.getWorldPortalShape().localCoords(entity.position()).depth()));
        return this.getPortalInsides().move(this.getNormalVec().normalize().scale(shiftAmount));
    }

    public AABB getTeleportPlane(double offset) {
        return this.getWorldPortalShape().getTeleportPlane(offset);
    }

    public AABB getCollisionRemovalAabbForEntity(Entity entity) {
        double speed = entity.getDeltaMovement().length();
        double extent = Math.max(1.0D, Math.max(entity.getBbWidth(), entity.getBbHeight()) + speed);
        return this.getFlatPlane().expandTowards(this.getNormalVec().normalize().scale(extent));
    }

    public Vec3 teleportProbePosition(Entity entity) {
        return entity.position().add(0.0D, this.getTeleportProbeHeight(entity), 0.0D);
    }

    public Vec3 teleportProbePosition(Entity entity, AABB bounds) {
        double x = (bounds.minX + bounds.maxX) * 0.5D;
        double y = bounds.minY + this.getTeleportProbeHeight(entity);
        double z = (bounds.minZ + bounds.maxZ) * 0.5D;
        return new Vec3(x, y, z);
    }

    private double getTeleportProbeHeight(Entity entity) {
        return entity instanceof LivingEntity living ? living.getEyeHeight(living.getPose()) : entity.getEyeHeight();
    }

    public PortalCrossing resolveCrossing(Entity entity, AABB previousBox, AABB currentBox, Vec3 attemptedMovement) {
        PortalShape shape = this.getWorldPortalShape();
        if (!PortalTransitMath.crossesWithBody(shape, previousBox, currentBox, attemptedMovement)) {
            return null;
        }
        return new PortalCrossing(this.teleportProbePosition(entity, currentBox));
    }

    public PortalTransit computeTransit(PortalEntity destination, AABB box, Vec3 velocity, Vec3 look) {
        PortalShape source = this.getWorldPortalShape();
        PortalShape target = destination.getWorldPortalShape();
        AABB exitBox = PortalTransitMath.exitBox(source, target, box);
        Vec3 exitVelocity = PortalTransitMath.exitVelocity(source, target, velocity, this.minFloorExitSpeed(), this.maxExitSpeed());
        Vec3 exitLook = PortalTransitMath.transformVector(source, target, look).normalize();
        return new PortalTransit(exitBox, PortalTransitMath.feetPosition(exitBox), exitVelocity, exitLook);
    }

    public PortalTransit computeTransit(Entity entity, PortalEntity destination, AABB box, Vec3 velocity, Vec3 look) {
        if (!GravityApi.isGravityInverted(entity)) {
            return this.computeTransit(destination, box, velocity, look);
        }
        PortalShape source = this.getWorldPortalShape();
        PortalShape target = destination.getWorldPortalShape();
        AABB exitBox = PortalTransitMath.exitBox(source, target, box);
        Vec3 worldVelocity = PortalTransitMath.entityVelocityToWorld(entity, velocity);
        Vec3 worldExitVelocity = PortalTransitMath.exitVelocity(source, target, worldVelocity, this.minFloorExitSpeed(), this.maxExitSpeed());
        Vec3 exitVelocity = PortalTransitMath.entityVelocityFromWorld(entity, worldExitVelocity);
        Vec3 worldLook = PortalTransitMath.entityVelocityToWorld(entity, look);
        Vec3 worldExitLook = PortalTransitMath.transformVector(source, target, worldLook).normalize();
        Vec3 exitLook = PortalTransitMath.entityVelocityFromWorld(entity, worldExitLook);
        return new PortalTransit(exitBox, PortalTransitMath.entityPosition(exitBox, entity), exitVelocity, exitLook);
    }

    public static void teleportEntityAfterMovement(Entity entity, AABB previousBox, Vec3 velocity) {
        if (entity instanceof Player) {
            return;
        }
        teleportAfterMovement(entity, previousBox, entity.getBoundingBox(), velocity);
    }

    public static void teleportPlayerAfterMovementPacket(ServerPlayer player, AABB previousBox, Vec3 claimedMovement) {
        Vec3 transitVelocity = PortalTransitMath.entityVelocityFromWorld(player, claimedMovement);
        teleportAfterMovement(player, previousBox, previousBox.move(claimedMovement), transitVelocity, claimedMovement);
    }

    private static void teleportAfterMovement(Entity entity, AABB previousBox, AABB currentBox, Vec3 velocity) {
        teleportAfterMovement(entity, previousBox, currentBox, velocity, PortalTransitMath.entityVelocityToWorld(entity, velocity));
    }

    private static void teleportAfterMovement(Entity entity, AABB previousBox, AABB currentBox, Vec3 velocity, Vec3 attemptedMovement) {
        if (entity.level().isClientSide || !entity.isAlive() || entity.isPassenger() || entity instanceof PortalEntity) {
            return;
        }
        if (velocity.lengthSqr() <= 1.0E-12D && previousBox.getCenter().distanceToSqr(currentBox.getCenter()) <= 1.0E-10D) {
            return;
        }
        AABB sweptBounds = previousBox.minmax(currentBox).expandTowards(attemptedMovement).inflate(1.0D);
        for (PortalEntity portal : findPortalsNearBounds(entity.level(), sweptBounds)) {
            if (!(portal.level() instanceof ServerLevel serverLevel) || !portal.canTeleportEntity(entity)) {
                continue;
            }
            PortalEntity linked = portal.findLinkedPortal(serverLevel);
            if (linked == null || !linked.isAlive()) {
                continue;
            }
            PortalCrossing crossing = portal.resolveCrossing(entity, previousBox, currentBox, attemptedMovement);
            if (crossing != null) {
                if (entity instanceof Player && Boolean.getBoolean(DEBUG_TRAVERSAL_PROPERTY)) {
                    Anthology.LOGGER.info("Portal traversal accepted path=server portal={} player={} probe={} linked={}",
                            portal.getUUID(), entity.getUUID(), crossing.probe(), linked.getUUID());
                }
                portal.teleportEntity(entity, linked, currentBox, velocity, false);
                return;
            }
        }
    }

    public static boolean handleClientTransit(ServerPlayer player, int portalId, Vec3 start, Vec3 end, Vec3 velocity) {
        if (!player.isAlive() || player.isPassenger() || player.isSpectator()) {
            return false;
        }
        if (((ServerGamePacketListenerTeleportAccessor) player.connection).anthology$getAwaitingPositionFromClient() != null) {
            return true;
        }
        if (!isFinite(start) || !isFinite(end) || !isFinite(velocity)
                || start.distanceToSqr(player.position()) > MAX_CLIENT_TRANSIT_START_ERROR_SQR
                || end.distanceToSqr(start) > MAX_CLIENT_TRANSIT_MOVEMENT_SQR
                || velocity.lengthSqr() > MAX_CLIENT_TRANSIT_MOVEMENT_SQR) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)
                || !(serverLevel.getEntity(portalId) instanceof PortalEntity portal)
                || !portal.isAlive()) {
            return false;
        }
        PortalEntity linked = portal.findLinkedPortal(serverLevel);
        if (linked == null || !linked.isAlive()) {
            return false;
        }
        AABB startBox = player.getBoundingBox().move(start.subtract(player.position()));
        AABB endBox = startBox.move(end.subtract(start));
        Vec3 crossingVelocity = PortalTransitMath.entityVelocityToWorld(player, velocity);
        if (portal.resolveCrossing(player, startBox, endBox, crossingVelocity) == null) {
            return false;
        }
        if (Boolean.getBoolean(DEBUG_TRAVERSAL_PROPERTY)) {
            Anthology.LOGGER.info("Portal traversal accepted path=client portal={} player={} end={} velocity={} linked={}",
                    portal.getUUID(), player.getUUID(), end, velocity, linked.getUUID());
        }
        portal.teleportEntity(player, linked, endBox, velocity, true);
        return true;
    }

    public static boolean passProjectileThroughPortal(Projectile projectile, BlockHitResult hit) {
        if (PortalHooks.isTransitExcluded(projectile) || projectile.isRemoved() || projectile.isPassenger()) {
            return false;
        }
        Vec3 location = hit.getLocation();
        Vec3 velocity = projectile.getDeltaMovement();
        for (PortalEntity portal : findPortalsNearBounds(projectile.level(), new AABB(location, location).inflate(0.5D))) {
            if (portal.getFacingDirection() != hit.getDirection() || portal.isTeleportCoolingDown(projectile) || !portal.canTransit(projectile)) {
                continue;
            }
            PortalShape shape = portal.getWorldPortalShape();
            PortalShape.PortalLocalCoords coords = shape.localCoords(location);
            if (Math.abs(coords.depth()) > PROJECTILE_PLANE_TOLERANCE
                    || Math.abs(coords.horizontal()) > shape.halfWidth()
                    || Math.abs(coords.vertical()) > shape.halfHeight()
                    || velocity.dot(shape.normal()) >= 0.0D) {
                continue;
            }
            PortalEntity linked = projectile.level() instanceof ServerLevel serverLevel ? portal.findLinkedPortal(serverLevel) : portal.getLinkedPortal();
            if (linked == null || !linked.isAlive()) {
                continue;
            }
            AABB crossingBox = projectile.getBoundingBox().move(location.subtract(projectile.position()));
            if (projectile.level().isClientSide) {
                PortalTransit transit = portal.computeTransit(projectile, linked, crossingBox, velocity, projectile.getLookAngle());
                Vec3 exit = transit.position();
                projectile.setPos(exit.x, exit.y, exit.z);
                projectile.xo = exit.x;
                projectile.yo = exit.y;
                projectile.zo = exit.z;
                projectile.setDeltaMovement(transit.velocity());
                orientAlongVelocity(projectile, transit.velocity());
                projectile.setPortalCooldown(TELEPORT_COOLDOWN_TICKS);
            } else {
                portal.teleportEntity(projectile, linked, crossingBox, velocity, false);
            }
            return true;
        }
        return false;
    }

    private static void orientAlongVelocity(Entity entity, Vec3 velocity) {
        if (velocity.lengthSqr() < 1.0E-12D) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(velocity.x, velocity.z));
        float pitch = (float) Math.toDegrees(Math.atan2(velocity.y, velocity.horizontalDistance()));
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.yRotO = yaw;
        entity.xRotO = pitch;
    }

    private static boolean isFinite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    public static List<PortalEntity> findPortalsNearBounds(Level level, AABB bounds) {
        List<PortalEntity> portals = null;
        for (PortalEntity portal : PortalRegistry.all(level)) {
            if (portal.isAlive() && portal.getWorldPortalShape().getBoundsForCulling().intersects(bounds)) {
                if (portals == null) {
                    portals = new ArrayList<>(2);
                }
                portals.add(portal);
            }
        }
        return portals == null ? List.of() : portals;
    }

    @Override
    public void setLevelCallback(EntityInLevelCallback callback) {
        super.setLevelCallback(callback);
        if (callback == EntityInLevelCallback.NULL) {
            PortalRegistry.untrack(this);
        } else {
            PortalRegistry.track(this);
        }
    }

    public boolean containsPoint(Vec3 position) {
        return this.getWorldPortalShape().contains(position, 0.0D);
    }

    public boolean intersectsEntityBounds(Entity entity) {
        AABB bounds = entity.getBoundingBox();
        for (Vec3 corner : new Vec3[] {
                new Vec3(bounds.minX, bounds.minY, bounds.minZ),
                new Vec3(bounds.minX, bounds.minY, bounds.maxZ),
                new Vec3(bounds.minX, bounds.maxY, bounds.minZ),
                new Vec3(bounds.minX, bounds.maxY, bounds.maxZ),
                new Vec3(bounds.maxX, bounds.minY, bounds.minZ),
                new Vec3(bounds.maxX, bounds.minY, bounds.maxZ),
                new Vec3(bounds.maxX, bounds.maxY, bounds.minZ),
                new Vec3(bounds.maxX, bounds.maxY, bounds.maxZ)
        }) {
            if (this.getWorldPortalShape().contains(corner, 0.08D)) {
                return true;
            }
        }
        return this.getPortalInsides(entity).intersects(bounds);
    }

    public boolean crossesPortal(Vec3 previousProbePosition, Vec3 currentProbePosition) {
        return this.getWorldPortalShape().crosses(previousProbePosition, currentProbePosition);
    }

    public boolean shouldRenderFront(Vec3 cameraPos) {
        return this.getWorldPortalShape().shouldRenderFront(cameraPos);
    }

    public float getPortalVisualScale(float partialTick) {
        return Mth.clamp((this.ageTicks - 1.0F + partialTick) / OPEN_TICKS, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isRemoved()) {
            PortalRegistry.track(this);
        }
        int openingAnimationId = this.entityData.get(OPENING_ANIMATION_ID);
        if (openingAnimationId != this.lastOpeningAnimationId) {
            this.lastOpeningAnimationId = openingAnimationId;
            this.ageTicks = 0;
        } else {
            this.ageTicks++;
        }
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        if (this.level().isClientSide) {
            this.clientTick();
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.tickTeleportCooldowns();
        if (!this.isStillValid(serverLevel)) {
            this.discard();
            return;
        }
        if (this.tickBeforeTransit(serverLevel)) {
            return;
        }
        PortalEntity linked = this.findLinkedPortal(serverLevel);
        if (linked == null || !linked.isAlive()) {
            if (Boolean.getBoolean(DEBUG_TRAVERSAL_PROPERTY) && this.ageTicks % 40 == 0) {
                Anthology.LOGGER.info("Portal traversal unavailable portal={} linked={} loaded={}", this.getUUID(), this.linkedPortalId, linked != null);
            }
            return;
        }
        PortalSounds sounds = this.sounds();
        if (sounds.ambient() != null && sounds.ambientInterval() > 0 && this.ageTicks % sounds.ambientInterval() == 0) {
            this.level().playSound(null, this.blockPosition(), sounds.ambient().get(), SoundSource.BLOCKS, sounds.ambientVolume(), this.ambientPitch());
        }
        this.tickTeleport(linked);
    }

    private void tickTeleport(PortalEntity linked) {
        List<Entity> entities = this.level().getEntities(this, this.getScanRange(), entity -> !(entity instanceof Player) && this.canTeleportEntity(entity));
        for (Entity entity : entities) {
            AABB currentBox = entity.getBoundingBox();
            AABB previousBox = currentBox.move(entity.xo - entity.getX(), entity.yo - entity.getY(), entity.zo - entity.getZ());
            PortalCrossing crossing = this.resolveCrossing(entity, previousBox, currentBox, entity.getDeltaMovement());
            if (crossing == null) {
                continue;
            }
            this.teleportEntity(entity, linked, currentBox, entity.getDeltaMovement(), false);
        }
    }

    private boolean canTeleportEntity(Entity entity) {
        if (!entity.isAlive() || entity instanceof PortalEntity || entity.isPassenger() || !this.canTransit(entity)) {
            return false;
        }
        return !this.isTeleportCoolingDown(entity);
    }

    public boolean isTeleportCoolingDown(Entity entity) {
        return this.teleportCooldowns.containsKey(entity.getUUID()) || entity.getPortalCooldown() > 0;
    }

    private void playTransitSound(Level level, Vec3 position, Supplier<SoundEvent> sound) {
        if (sound != null) {
            level.playSound(null, position.x, position.y, position.z, sound.get(), SoundSource.PLAYERS, 0.7F, 0.96F + this.random.nextFloat() * 0.08F);
        }
    }

    private void teleportEntity(Entity entity, PortalEntity destination, AABB crossingBox, Vec3 velocity, boolean clientAuthoritative) {
        List<EntityTeleportState> group = new ArrayList<>();
        collectTeleportGroup(entity, null, group);
        Vec3 rootOriginalPosition = entity.position();
        PortalTransit transit = this.computeTransit(entity, destination, crossingBox, velocity, entity.getLookAngle());
        Vec3 exitPos = transit.position();
        this.playTransitSound(this.level(), this.position(), this.sounds().enter());
        for (EntityTeleportState state : group) {
            state.entity().stopRiding();
        }
        for (EntityTeleportState state : group) {
            Entity member = state.entity();
            boolean root = member == entity;
            Vec3 memberExitPos = root
                    ? exitPos
                    : exitPos.add(PortalTransforms.transformVector(this, destination, state.position().subtract(rootOriginalPosition)));
            Vec3 memberLook;
            if (root) {
                memberLook = transit.look();
            } else if (GravityApi.isGravityInverted(member)) {
                Vec3 worldLook = PortalTransitMath.entityVelocityToWorld(member, state.look());
                memberLook = PortalTransitMath.entityVelocityFromWorld(member,
                        PortalTransforms.transformVector(this, destination, worldLook).normalize());
            } else {
                memberLook = PortalTransforms.transformVector(this, destination, state.look()).normalize();
            }
            Vec3 memberMotion;
            if (root) {
                memberMotion = transit.velocity();
            } else if (GravityApi.isGravityInverted(member)) {
                Vec3 worldMotion = PortalTransitMath.entityVelocityToWorld(member, state.motion());
                memberMotion = PortalTransitMath.entityVelocityFromWorld(member,
                        PortalTransforms.transformVector(this, destination, worldMotion));
            } else {
                memberMotion = PortalTransforms.transformVector(this, destination, state.motion());
            }
            float yaw = PortalTransforms.yawFromLook(memberLook);
            float pitch = PortalTransforms.pitchFromLook(memberLook);
            float fallDistance = PortalTransitMath.exitFallDistance(member.fallDistance,
                    GravityApi.isGravityInverted(member) ? PortalTransitMath.entityVelocityToWorld(member, memberMotion) : memberMotion,
                    member);
            if (member instanceof ServerPlayer player && root && clientAuthoritative) {
                player.moveTo(memberExitPos.x, memberExitPos.y, memberExitPos.z, yaw, pitch);
                player.setYHeadRot(yaw);
                player.setDeltaMovement(memberMotion);
                player.connection.resetPosition();
                player.serverLevel().getChunkSource().move(player);
            } else if (member instanceof ServerPlayer player && destination.level() instanceof ServerLevel destinationLevel) {
                player.teleportTo(destinationLevel, memberExitPos.x, memberExitPos.y, memberExitPos.z, yaw, pitch);
                player.setDeltaMovement(memberMotion);
                player.hasImpulse = true;
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
            } else {
                member.teleportTo(memberExitPos.x, memberExitPos.y, memberExitPos.z);
                member.setYRot(yaw);
                member.setXRot(pitch);
                member.setYHeadRot(yaw);
                member.setYBodyRot(yaw);
                member.setDeltaMovement(memberMotion);
                member.hasImpulse = true;
                member.hurtMarked = true;
            }
            member.xo = member.getX();
            member.yo = member.getY();
            member.zo = member.getZ();
            member.yRotO = yaw;
            member.xRotO = pitch;
            if (member instanceof Projectile) {
                orientAlongVelocity(member, memberMotion);
            }
            member.setOnGround(false);
            member.horizontalCollision = false;
            member.verticalCollision = false;
            member.verticalCollisionBelow = false;
            member.fallDistance = fallDistance;
            this.applyTeleportCooldown(member, destination);
            handleSpecialEntityPostTeleport(member);
        }
        for (EntityTeleportState state : group) {
            if (state.vehicle() != null && state.entity().isAlive()) {
                state.entity().startRiding(state.vehicle(), true);
            }
        }
        if (destination.level() instanceof ServerLevel destinationLevel) {
            for (EntityTeleportState state : group) {
                Entity member = state.entity();
                if (!(member instanceof ServerPlayer)) {
                    destinationLevel.getChunkSource().broadcastAndSend(member, new ClientboundTeleportEntityPacket(member));
                    destinationLevel.getChunkSource().broadcastAndSend(member, new ClientboundSetEntityMotionPacket(member));
                    syncTrackedPosition(destinationLevel, member);
                }
            }
        }
        destination.playTransitSound(destination.level(), exitPos, this.sounds().exit());
        for (EntityTeleportState state : group) {
            PortalHooks.fireTransit(state.entity(), this, destination);
        }
    }

    private static void syncTrackedPosition(ServerLevel level, Entity entity) {
        Object tracked = ((ChunkMapAccessor) level.getChunkSource().chunkMap).anthology$getEntityMap().get(entity.getId());
        if (tracked instanceof TrackedEntityAccessor trackedEntity) {
            ServerEntityAccessor serverEntity = (ServerEntityAccessor) trackedEntity.anthology$getServerEntity();
            serverEntity.anthology$getPositionCodec().setBase(entity.trackingPosition());
            serverEntity.anthology$setLastSentMovement(entity.getDeltaMovement());
        }
    }

    private static void collectTeleportGroup(Entity entity, Entity vehicle, List<EntityTeleportState> group) {
        group.add(new EntityTeleportState(entity, vehicle, entity.position(), entity.getLookAngle(), entity.getDeltaMovement()));
        for (Entity passenger : entity.getPassengers()) {
            collectTeleportGroup(passenger, entity, group);
        }
    }

    private void tickTeleportCooldowns() {
        Iterator<Map.Entry<UUID, Integer>> iterator = this.teleportCooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int cooldown = entry.getValue() - 1;
            if (cooldown < 0) {
                iterator.remove();
            } else {
                entry.setValue(cooldown);
            }
        }
    }

    private void applyTeleportCooldown(Entity entity, PortalEntity destination) {
        UUID entityId = entity.getUUID();
        this.teleportCooldowns.put(entityId, TELEPORT_COOLDOWN_TICKS);
        destination.teleportCooldowns.put(entityId, TELEPORT_COOLDOWN_TICKS);
        entity.setPortalCooldown(TELEPORT_COOLDOWN_TICKS);
    }

    private static void handleSpecialEntityPostTeleport(Entity entity) {
        if (entity instanceof FallingBlockEntity) {
            ((FallingBlockEntityAccessor) entity).anthology$setTime(2);
        }
        if (entity instanceof AbstractArrow arrow) {
            ((AbstractArrowAccessor) arrow).anthology$setInGround(false);
            ((AbstractArrowAccessor) arrow).anthology$setInGroundTime(0);
            ((AbstractArrowAccessor) arrow).anthology$setShakeTime(0);
            arrow.setNoPhysics(false);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getWorldPortalShape().getBoundsForCulling();
    }

    @Override
    protected AABB makeBoundingBox() {
        return this.getPortalInsides();
    }

    @Override
    public final void addAdditionalSaveData(CompoundTag tag) {
        if (this.linkedPortalId != null) {
            tag.putUUID("LinkedPortalId", this.linkedPortalId);
        }
        tag.putInt("AgeTicks", this.ageTicks);
        tag.putInt("PairTime", this.pairTime);
        tag.putInt("Facing", this.getFacingDirection().get3DDataValue());
        tag.putInt("UpAxis", this.getUpAxis().get3DDataValue());
        tag.putInt("PortalWidth", this.getPortalWidth());
        tag.putInt("PortalHeight", this.getPortalHeight());
        this.saveExtra(tag);
    }

    @Override
    public final void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("LinkedPortalId")) {
            this.linkedPortalId = tag.getUUID("LinkedPortalId");
            this.entityData.set(LINKED_PORTAL, Optional.of(this.linkedPortalId));
        }
        this.ageTicks = tag.getInt("AgeTicks");
        this.pairTime = tag.getInt("PairTime");
        this.entityData.set(FACING, tag.getInt("Facing"));
        this.entityData.set(UP_AXIS, tag.getInt("UpAxis"));
        this.entityData.set(PORTAL_WIDTH, Mth.clamp(tag.getInt("PortalWidth"), 1, 16));
        this.entityData.set(PORTAL_HEIGHT, Mth.clamp(tag.getInt("PortalHeight"), 2, 16));
        this.loadExtra(tag);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void lavaHurt() {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    public record PortalCrossing(Vec3 probe) {
    }

    public record PortalTransit(AABB exitBox, Vec3 position, Vec3 velocity, Vec3 look) {
    }

    private record EntityTeleportState(Entity entity, Entity vehicle, Vec3 position, Vec3 look, Vec3 motion) {
    }
}
