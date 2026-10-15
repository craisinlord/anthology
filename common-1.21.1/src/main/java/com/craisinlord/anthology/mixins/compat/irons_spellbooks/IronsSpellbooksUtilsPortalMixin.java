package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.compat.irons_spellbooks.IronsSpellbooksCompat;
import com.craisinlord.anthology.portal.PortalRaycast;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "io.redspace.ironsspellbooks.api.util.Utils")
@Pseudo
public abstract class IronsSpellbooksUtilsPortalMixin {
    @Unique
    private static final int anthology$maxPortalHops = 4;

    @Shadow
    public static HitResult checkEntityIntersecting(Entity entity, Vec3 start, Vec3 end, float bbInflation) {
        throw new AssertionError();
    }

    @WrapMethod(method = "internalRaycastForEntity")
    private static HitResult anthology$raycastThroughPortals(Level level, Entity originEntity, Vec3 start, Vec3 end, boolean checkForBlocks,
                                                             float bbInflation, Predicate<? super Entity> filter, Operation<HitResult> original) {
        boolean opaque = IronsSpellbooksCompat.isPortalOpaque();
        List<PortalRaycast.Crossing> crossings = new ArrayList<>();
        Vec3 segmentStart = start;
        Vec3 segmentEnd = end;
        HitResult result;
        for (int hop = 0; ; hop++) {
            PortalRaycast.Crossing crossing = opaque || hop >= anthology$maxPortalHops
                    ? null
                    : PortalRaycast.findCrossing(level, segmentStart, segmentEnd, originEntity);
            if (crossing == null || crossing.destination() == null) {
                result = hop == 0
                        ? original.call(level, originEntity, segmentStart, segmentEnd, checkForBlocks, bbInflation, filter)
                        : anthology$scan(level, originEntity, segmentStart, segmentEnd, checkForBlocks, bbInflation, filter);
                break;
            }
            HitResult near = hop == 0
                    ? original.call(level, originEntity, segmentStart, crossing.entry(), false, bbInflation, filter)
                    : anthology$scan(level, originEntity, segmentStart, crossing.entry(), false, bbInflation, filter);
            if (near.getType() == HitResult.Type.ENTITY) {
                result = near;
                break;
            }
            crossings.add(crossing);
            double remaining = Math.max(0.0D, segmentStart.distanceTo(segmentEnd) - crossing.distance());
            segmentStart = crossing.exit();
            segmentEnd = segmentStart.add(crossing.exitDirection().scale(remaining));
        }
        IronsSpellbooksCompat.recordRay(start, crossings, result.getLocation());
        return result;
    }

    @WrapMethod(method = "handleSpellTeleport")
    private static boolean anthology$applyPortalLook(AbstractSpell spell, Entity entity, Vec3 destination, Operation<Boolean> original) {
        boolean canceled = original.call(spell, entity, destination);
        if (canceled) {
            IronsSpellbooksCompat.clearPortalLook(entity);
        } else {
            IronsSpellbooksCompat.applyPortalLook(entity, destination);
        }
        return canceled;
    }

    @Unique
    private static HitResult anthology$scan(Level level, Entity originEntity, Vec3 start, Vec3 end, boolean checkForBlocks,
                                            float bbInflation, Predicate<? super Entity> filter) {
        BlockHitResult blockHit = null;
        Vec3 scanEnd = end;
        if (checkForBlocks) {
            blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, originEntity));
            scanEnd = blockHit.getLocation();
        }
        List<HitResult> hits = new ArrayList<>();
        for (Entity target : level.getEntities(originEntity, new AABB(start, scanEnd).inflate(1.0D + bbInflation), filter)) {
            HitResult hit = checkEntityIntersecting(target, start, scanEnd, bbInflation);
            if (hit.getType() != HitResult.Type.MISS) {
                hits.add(hit);
            }
        }
        if (!hits.isEmpty()) {
            hits.sort(Comparator.comparingDouble(hit -> hit.getLocation().distanceToSqr(start)));
            return hits.getFirst();
        }
        return blockHit != null ? blockHit : BlockHitResult.miss(scanEnd, Direction.UP, BlockPos.containing(scanEnd));
    }
}
