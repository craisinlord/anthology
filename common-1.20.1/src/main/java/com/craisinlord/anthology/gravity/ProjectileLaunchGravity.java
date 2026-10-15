package com.craisinlord.anthology.gravity;

import net.minecraft.world.phys.Vec3;

public final class ProjectileLaunchGravity {
    private ProjectileLaunchGravity() {
    }

    public static Vec3 toWorld(Vec3 launchDirection, GravityDirection gravityDirection) {
        return GravityRotationUtil.vecPlayerToWorld(launchDirection, gravityDirection);
    }
}
