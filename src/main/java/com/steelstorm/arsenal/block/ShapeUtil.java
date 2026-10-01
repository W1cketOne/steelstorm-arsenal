package com.steelstorm.arsenal.block;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Rotates block shapes drawn facing north to the other horizontal directions. */
final class ShapeUtil {
    static Map<Direction, VoxelShape> horizontal(VoxelShape north) {
        Map<Direction, VoxelShape> out = new EnumMap<>(Direction.class);
        out.put(Direction.NORTH, north);
        out.put(Direction.EAST, rotate(north, 1));
        out.put(Direction.SOUTH, rotate(north, 2));
        out.put(Direction.WEST, rotate(north, 3));
        return out;
    }

    /** Rotates a shape clockwise (seen from above) by quarter turns. */
    static VoxelShape rotate(VoxelShape shape, int quarterTurns) {
        VoxelShape result = shape;
        for (int i = 0; i < quarterTurns; i++) {
            VoxelShape[] acc = {Shapes.empty()};
            result.forAllBoxes((x0, y0, z0, x1, y1, z1) -> acc[0] = Shapes.or(acc[0], Shapes.box(1 - z1, y0, x0, 1 - z0, y1, x1)));
            result = acc[0];
        }
        return result;
    }

    private ShapeUtil() {
    }
}
