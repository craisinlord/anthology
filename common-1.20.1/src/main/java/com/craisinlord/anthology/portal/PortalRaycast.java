package com.craisinlord.anthology.portal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PortalRaycast {
    private static final double EXIT_OFFSET = 0.05D;
    private static final double BLOCK_TOLERANCE = 0.1D;

    private PortalRaycast() {
    }

    public record Crossing(PortalEntity source, PortalEntity destination, Vec3 entry, Vec3 exit, Vec3 exitDirection, double distance) {
        public Vec3 transformDirection(Vec3 direction) {
            return PortalTransforms.transformVector(this.source.getWorldPortalShape(), this.destination.getWorldPortalShape(), direction);
        }
    }

    public static Crossing findCrossing(Level level, Vec3 start, Vec3 end, Entity context) {
        return findCrossing(level, start, end, context, true);
    }

    public static Crossing findCrossing(Level level, Vec3 start, Vec3 end, Entity context, boolean requireLinked) {
        if (!PortalRegistry.hasAny(level)) {
            return null;
        }
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-6D) {
            return null;
        }
        PortalEntity bestPortal = null;
        double bestT = Double.MAX_VALUE;
        for (PortalEntity portal : PortalEntity.findPortalsNearBounds(level, new AABB(start, end).inflate(0.5D))) {
            double t = intersect(portal.getWorldPortalShape(), start, end);
            if (t >= 0.0D && t < bestT && (!requireLinked || linkedPortal(level, portal) != null)) {
                bestT = t;
                bestPortal = portal;
            }
        }
        if (bestPortal == null) {
            return null;
        }
        Vec3 direction = delta.scale(1.0D / length);
        Vec3 entry = start.add(delta.scale(bestT));
        double distance = length * bestT;
        if (distance > BLOCK_TOLERANCE) {
            Vec3 blockEnd = entry.subtract(direction.scale(BLOCK_TOLERANCE));
            ClipContext clip = new ClipContext(start, blockEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context);
            if (level.clip(clip).getType() != HitResult.Type.MISS) {
                return null;
            }
        }
        PortalEntity destination = linkedPortal(level, bestPortal);
        if (destination == null) {
            return new Crossing(bestPortal, null, entry, null, null, distance);
        }
        PortalShape source = bestPortal.getWorldPortalShape();
        PortalShape target = destination.getWorldPortalShape();
        Vec3 exitDirection = PortalTransforms.transformVector(source, target, direction).normalize();
        Vec3 exit = target.center()
                .add(PortalTransforms.transformPosition(source, target, entry.subtract(source.center())))
                .add(exitDirection.scale(EXIT_OFFSET));
        return new Crossing(bestPortal, destination, entry, exit, exitDirection, distance);
    }

    public static double intersect(PortalShape shape, Vec3 start, Vec3 end) {
        PortalShape.PortalLocalCoords from = shape.localCoords(start);
        PortalShape.PortalLocalCoords to = shape.localCoords(end);
        if (from.depth() <= 0.0D || to.depth() > 0.0D) {
            return -1.0D;
        }
        double t = from.depth() / (from.depth() - to.depth());
        double horizontal = from.horizontal() + (to.horizontal() - from.horizontal()) * t;
        double vertical = from.vertical() + (to.vertical() - from.vertical()) * t;
        if (Math.abs(horizontal) > shape.halfWidth() || Math.abs(vertical) > shape.halfHeight()) {
            return -1.0D;
        }
        return t;
    }

    private static PortalEntity linkedPortal(Level level, PortalEntity portal) {
        PortalEntity linked = portal.getLinkedPortal();
        return linked != null && linked.isAlive() && linked.level() == level ? linked : null;
    }
}
