package com.craisinlord.anthology.mixins.gravity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PathNavigation.class)
public interface PathNavigationAccessor {
    @Accessor("mob")
    Mob anthology$getMob();

    @Accessor("path")
    Path anthology$getPath();

    @Accessor("nodeEvaluator")
    void anthology$setNodeEvaluator(NodeEvaluator nodeEvaluator);
}
