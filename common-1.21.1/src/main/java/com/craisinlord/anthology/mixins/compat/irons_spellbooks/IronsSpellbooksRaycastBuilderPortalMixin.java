package com.craisinlord.anthology.mixins.compat.irons_spellbooks;

import com.craisinlord.anthology.compat.irons_spellbooks.IronsSpellbooksCompat;
import com.craisinlord.anthology.portal.PortalRaycast;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.redspace.ironsspellbooks.api.util.Utils;
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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "io.redspace.ironsspellbooks.api.util.RaycastBuilder")
@Pseudo
public abstract class IronsSpellbooksRaycastBuilderPortalMixin {
    @Unique
    private static final int anthology$maxPortalHops = 4;

    @Shadow
    @Final
    private Level level;
    @Shadow
    @Final
    private Entity originEntity;
    @Shadow
    private Vec3 start;
    @Shadow
    private Vec3 end;
    @Shadow
    private boolean checkForBlocks;
    @Shadow
    private float bbInflation;
    @Shadow
    private Predicate<? super Entity> filter;

    @WrapMethod(require = 0, method = "performRaycast")
    private HitResult anthology$raycastThroughPortals(Operation<HitResult> original) {
        boolean opaque = IronsSpellbooksCompat.isPortalOpaque();
        Vec3 start = this.start;
        Vec3 end = this.end;
        boolean checkForBlocks = this.checkForBlocks;
        List<PortalRaycast.Crossing> crossings = new ArrayList<>();
        Vec3 segmentStart = start;
        Vec3 segmentEnd = end;
        HitResult result;
        try {
            for (int hop = 0; ; hop++) {
                PortalRaycast.Crossing crossing = opaque || hop >= anthology$maxPortalHops
                        ? null
                        : PortalRaycast.findCrossing(this.level, segmentStart, segmentEnd, this.originEntity);
                if (crossing == null || crossing.destination() == null) {
                    result = hop == 0
                            ? this.anthology$original(original, segmentStart, segmentEnd, checkForBlocks)
                            : this.anthology$scan(segmentStart, segmentEnd, checkForBlocks);
                    break;
                }
                HitResult near = hop == 0
                        ? this.anthology$original(original, segmentStart, crossing.entry(), false)
                        : this.anthology$scan(segmentStart, crossing.entry(), false);
                if (near.getType() == HitResult.Type.ENTITY) {
                    result = near;
                    break;
                }
                crossings.add(crossing);
                double remaining = Math.max(0.0D, segmentStart.distanceTo(segmentEnd) - crossing.distance());
                segmentStart = crossing.exit();
                segmentEnd = segmentStart.add(crossing.exitDirection().scale(remaining));
            }
        } finally {
            this.start = start;
            this.end = end;
            this.checkForBlocks = checkForBlocks;
        }
        IronsSpellbooksCompat.recordRay(start, crossings, result.getLocation());
        return result;
    }

    @Unique
    private HitResult anthology$original(Operation<HitResult> original, Vec3 start, Vec3 end, boolean checkForBlocks) {
        this.start = start;
        this.end = end;
        this.checkForBlocks = checkForBlocks;
        return original.call();
    }

    @Unique
    private HitResult anthology$scan(Vec3 start, Vec3 end, boolean checkForBlocks) {
        BlockHitResult blockHit = null;
        Vec3 scanEnd = end;
        if (checkForBlocks) {
            blockHit = this.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.originEntity));
            scanEnd = blockHit.getLocation();
        }
        List<HitResult> hits = new ArrayList<>();
        for (Entity target : this.level.getEntities(this.originEntity, new AABB(start, scanEnd).inflate(1.0D + this.bbInflation), this.filter)) {
            HitResult hit = Utils.checkEntityIntersecting(target, start, scanEnd, this.bbInflation);
            if (hit.getType() != HitResult.Type.MISS) {
                hits.add(hit);
            }
        }
        if (!hits.isEmpty()) {
            hits.sort(Comparator.comparingDouble(hit -> hit.getLocation().distanceToSqr(start)));
            return hits.get(0);
        }
        return blockHit != null ? blockHit : BlockHitResult.miss(scanEnd, Direction.UP, BlockPos.containing(scanEnd));
    }
}
