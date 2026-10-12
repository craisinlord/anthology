package com.craisinlord.anthology.gravity;

import com.craisinlord.anthology.Anthology;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class GravityWalkNodeEvaluator extends WalkNodeEvaluator {
    private static final long LOG_TIME_THRESHOLD_NANOS = 25_000_000L;

    private long anthology$prepareStartNanos;
    private int anthology$nodeProbeCount;

    private static final int[] START_Y_SEARCH = {0, 1, -1, 2, -2};
    private static final int[][] INVERTED_CARDINAL_NEIGHBOR_OFFSETS = {
            {1, 0, 0},
            {-1, 0, 0},
            {0, 0, 1},
            {0, 0, -1}
    };
    private static final int[][] INVERTED_DIAGONAL_NEIGHBOR_OFFSETS = {
            {1, 0, 1},
            {1, 0, -1},
            {-1, 0, 1},
            {-1, 0, -1}
    };

    @Override
    public void prepare(PathNavigationRegion sourceIn, Mob mob) {
        super.prepare(sourceIn, mob);
        this.anthology$prepareStartNanos = System.nanoTime();
        this.anthology$nodeProbeCount = 0;
    }

    @Override
    public void done() {
        if (this.mob != null && GravityApi.isGravityInverted(this.mob)) {
            long elapsedNanos = System.nanoTime() - this.anthology$prepareStartNanos;
            if (elapsedNanos > LOG_TIME_THRESHOLD_NANOS) {
                Anthology.LOGGER.warn(
                        "[anthology-gravity] slow inverted pathfind: type={} uuid={} pos=({}, {}, {}) nodeProbes={} elapsedMs={}",
                        this.mob.getType(), this.mob.getUUID(),
                        this.mob.getX(), this.mob.getY(), this.mob.getZ(),
                        this.anthology$nodeProbeCount, elapsedNanos / 1_000_000.0
                );
            }
        }
        super.done();
    }

    @Override
    public Node getStart() {
        if (!GravityApi.isGravityInverted(this.mob)) {
            return super.getStart();
        }

        int x = Mth.floor(this.mob.getX());
        int z = Mth.floor(this.mob.getZ());
        int y = Mth.floor(this.mob.getY() + this.mob.getBbHeight()) - 1;
        Node node = this.anthology$findStartNode(x, y, z);

        return node;
    }

    private Node anthology$findStartNode(int x, int y, int z) {
        for (int dy : START_Y_SEARCH) {
            int candidateY = y + dy;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Node node = this.anthology$createStartNode(x + dx, candidateY, z + dz);
                    if (node != null) {
                        return node;
                    }
                }
            }
        }
        return null;
    }

    private Node anthology$createStartNode(int x, int y, int z) {
        this.anthology$nodeProbeCount++;
        if (!this.anthology$hasCeilingSupport(this.currentContext, x, y, z)) {
            return null;
        }

        PathType type = this.getPathTypeOfMob(this.currentContext, x, y, z, this.mob);
        float malus = this.mob.getPathfindingMalus(type);
        if (malus < 0.0F || type == PathType.OPEN) {
            return null;
        }

        Node node = this.getNode(x, y, z);
        node.type = type;
        node.costMalus = malus;
        return node;
    }

    private boolean anthology$hasCeilingSupport(PathfindingContext context, int x, int y, int z) {
        for (int dx = 0; dx < this.entityWidth; dx++) {
            for (int dz = 0; dz < this.entityDepth; dz++) {
                BlockPos ceilingPos = new BlockPos(x + dx, y + 1, z + dz);
                if (!context.level().getBlockState(ceilingPos).isFaceSturdy(context.level(), ceilingPos, Direction.DOWN)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public PathType getPathTypeOfMob(PathfindingContext context, int x, int y, int z, Mob mob) {
        if (!GravityApi.isGravityInverted(mob)) {
            return super.getPathTypeOfMob(context, x, y, z, mob);
        }

        if (!this.anthology$hasCeilingSupport(context, x, y, z)) {
            return PathType.OPEN;
        }

        PathType result = PathType.WALKABLE;
        for (int dw = 0; dw < this.entityWidth; dw++) {
            for (int dd = 0; dd < this.entityDepth; dd++) {
                for (int dh = 0; dh < this.entityHeight; dh++) {
                    PathType type = this.getPathType(context, x + dw, y - dh, z + dd);
                    float malus = mob.getPathfindingMalus(type);
                    if (malus < 0.0F) {
                        return type;
                    }
                    if (malus >= mob.getPathfindingMalus(result)) {
                        result = type;
                    }
                }
            }
        }

        return result;
    }

    @Override
    public int getNeighbors(Node[] neighbors, Node current) {
        if (!GravityApi.isGravityInverted(this.mob)) {
            return super.getNeighbors(neighbors, current);
        }

        int count = 0;
        Node[] cardinalNeighbors = new Node[INVERTED_CARDINAL_NEIGHBOR_OFFSETS.length];
        for (int i = 0; i < INVERTED_CARDINAL_NEIGHBOR_OFFSETS.length; i++) {
            int[] offset = INVERTED_CARDINAL_NEIGHBOR_OFFSETS[i];
            Node neighbor = this.anthology$getNeighbor(current.x + offset[0], current.y, current.z + offset[2]);
            cardinalNeighbors[i] = neighbor;
            if (neighbor != null) {
                neighbors[count++] = neighbor;
            }
        }

        for (int i = 0; i < INVERTED_DIAGONAL_NEIGHBOR_OFFSETS.length; i++) {
            int[] offset = INVERTED_DIAGONAL_NEIGHBOR_OFFSETS[i];
            if (!this.anthology$canUseDiagonal(cardinalNeighbors, i)) {
                continue;
            }
            Node neighbor = this.anthology$getNeighbor(current.x + offset[0], current.y, current.z + offset[2]);
            if (neighbor != null) {
                neighbors[count++] = neighbor;
            }
        }

        Node stepTowardInvertedUp = this.anthology$getNeighbor(current.x, current.y - 1, current.z);
        if (stepTowardInvertedUp != null) {
            neighbors[count++] = stepTowardInvertedUp;
        }
        Node stepTowardInvertedDown = this.anthology$getNeighbor(current.x, current.y + 1, current.z);
        if (stepTowardInvertedDown != null) {
            neighbors[count++] = stepTowardInvertedDown;
        }

        return count;
    }

    private boolean anthology$canUseDiagonal(Node[] cardinalNeighbors, int diagonalIndex) {
        return switch (diagonalIndex) {
            case 0 -> cardinalNeighbors[0] != null && cardinalNeighbors[2] != null;
            case 1 -> cardinalNeighbors[0] != null && cardinalNeighbors[3] != null;
            case 2 -> cardinalNeighbors[1] != null && cardinalNeighbors[2] != null;
            case 3 -> cardinalNeighbors[1] != null && cardinalNeighbors[3] != null;
            default -> false;
        };
    }

    private Node anthology$getNeighbor(int x, int y, int z) {
        this.anthology$nodeProbeCount++;
        PathType type = this.getPathTypeOfMob(this.currentContext, x, y, z, this.mob);
        if (type == PathType.OPEN || this.mob.getPathfindingMalus(type) < 0.0F) {
            return null;
        }

        Node node = this.getNode(x, y, z);
        if (node.closed) {
            return null;
        }
        node.type = type;
        node.costMalus = this.mob.getPathfindingMalus(type);
        return node;
    }
}
