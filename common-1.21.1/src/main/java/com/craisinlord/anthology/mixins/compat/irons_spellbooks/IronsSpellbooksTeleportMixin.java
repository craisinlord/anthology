package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.compat.irons_spellbooks.IronsSpellbooksCompat;
import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.anthology.portal.PortalRaycast;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "io.redspace.ironsspellbooks.spells.ender.TeleportSpell")
@Pseudo
public abstract class IronsSpellbooksTeleportMixin {
    @Shadow
    public static Vec3 solveTeleportDestination(Level level, LivingEntity entity, BlockPos blockPos, Vec3 location) {
        throw new AssertionError();
    }

    @WrapMethod(method = "findTeleportLocation")
    private static Vec3 anthology$teleportThroughPortals(Level level, LivingEntity entity, float maxDistance, Operation<Vec3> original) {
        Vec3 eye = entity.getEyePosition();
        Vec3 look = entity.getLookAngle().normalize();
        PortalRaycast.Crossing crossing = PortalRaycast.findCrossing(level, eye, eye.add(look.scale(maxDistance)), entity);
        if (crossing == null || crossing.destination() == null) {
            IronsSpellbooksCompat.clearPortalLook(entity);
            return original.call(level, entity, maxDistance);
        }
        double remaining = Math.max(0.0D, maxDistance - crossing.distance());
        Vec3 end = crossing.exit().add(crossing.exitDirection().scale(remaining));
        BlockHitResult hit = level.clip(new ClipContext(crossing.exit(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        Vec3 destination = solveTeleportDestination(level, entity, hit.getBlockPos(), hit.getLocation());
        IronsSpellbooksCompat.setPortalLook(entity, destination, crossing.transformDirection(look).normalize());
        return destination;
    }

    @WrapMethod(method = "solveTeleportDestination")
    private static Vec3 anthology$solveInvertedDestination(Level level, LivingEntity entity, BlockPos blockPos, Vec3 location, Operation<Vec3> original) {
        if (!GravityApi.isGravityInverted(entity)) {
            return original.call(level, entity, blockPos, location);
        }
        Vec3 bbOffset = entity.getForward().normalize().multiply(entity.getBbWidth() / 3.0F, 0.0D, entity.getBbHeight() / 3.0F);
        Vec3 bbImpact = location.subtract(bbOffset);
        Vec3 underside = Vec3.atBottomCenterOf(blockPos);
        double ledgeY = level.clip(new ClipContext(underside.add(0.0D, -2.0D, 0.0D), underside.add(0.0D, 1.0D, 0.0D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty())).getLocation().y;
        double surfaceY = blockPos.getY() + 1.0D;
        boolean isAir = level.getBlockState(BlockPos.containing(blockPos.getX() + 0.5D, ledgeY - 0.5D, blockPos.getZ() + 0.5D)).isAir();
        boolean lineOfSight = level.clip(new ClipContext(bbImpact, bbImpact.add(0.0D, ledgeY - surfaceY, 0.0D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() == HitResult.Type.MISS;
        if (isAir && lineOfSight && Math.abs(ledgeY - surfaceY) <= 3.0D) {
            return new Vec3(blockPos.getX() + 0.5D, ledgeY - 0.001D, blockPos.getZ() + 0.5D);
        }
        return level.clip(new ClipContext(bbImpact, bbImpact.add(0.0D, entity.getBbHeight(), 0.0D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getLocation().add(0.0D, -0.001D, 0.0D);
    }
}
