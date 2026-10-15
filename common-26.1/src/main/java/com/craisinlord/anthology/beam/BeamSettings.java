package com.craisinlord.anthology.beam;

public record BeamSettings(
        double range,
        double targetTracking,
        double pathStep,
        int durationTicks,
        int travelTicks,
        int cooldownTicks,
        float pathDamageRadius,
        float impactDamageRadius,
        float damage,
        float knockback,
        int terrainIntervalTicks,
        double maxTerrainDistance,
        float pathTerrainRadius,
        float impactTerrainRadius,
        int maxTerrainMutationsPerTick,
        float terrainMutationChance,
        float terrainDropChance,
        float maxTerrainResistance,
        boolean terrainEnabled,
        boolean requireLineOfSightForDamage
) {
    public BeamSettings {
        range = Math.max(1.0D, range);
        targetTracking = Math.clamp(targetTracking, 0.0D, 1.0D);
        pathStep = Math.max(1.0D, pathStep);
        durationTicks = Math.max(1, durationTicks);
        travelTicks = Math.max(1, travelTicks);
        cooldownTicks = Math.max(1, cooldownTicks);
        pathDamageRadius = Math.max(0.0F, pathDamageRadius);
        impactDamageRadius = Math.max(0.0F, impactDamageRadius);
        damage = Math.max(0.0F, damage);
        knockback = Math.max(0.0F, knockback);
        terrainIntervalTicks = Math.max(1, terrainIntervalTicks);
        maxTerrainDistance = Math.max(0.0D, maxTerrainDistance);
        pathTerrainRadius = Math.max(0.0F, pathTerrainRadius);
        impactTerrainRadius = Math.max(0.0F, impactTerrainRadius);
        maxTerrainMutationsPerTick = Math.max(0, maxTerrainMutationsPerTick);
        terrainMutationChance = Math.clamp(terrainMutationChance, 0.0F, 1.0F);
        terrainDropChance = Math.clamp(terrainDropChance, 0.0F, 1.0F);
        maxTerrainResistance = Math.max(0.0F, maxTerrainResistance);
    }
}
