package com.elvarg.game.definition.loader.impl;

import com.elvarg.game.model.Direction;
import com.elvarg.game.model.Location;

/**
 * @author Ynneh | 13/03/2023 - 14:43
 * <https://github.com/drhenny>
 */
public class JsonNPC {

    public String facing;
    public int id;
    public Location[] position;

    public String description;

    public JsonNPC(String facing, int id, Location[] position, String direction) {
        this.facing = facing;
        this.id = id;
        this.position = position;
        this.description = direction;
    }

    public Direction dir() {
        if (facing != null) {
            switch (facing) {
                case "NORTH":
                    return Direction.NORTH;
                case "NORTH_EAST":
                    return Direction.NORTH_EAST;
                case "NORTH_WEST":
                    return Direction.NORTH_WEST;
                case "SOUTH":
                    return Direction.SOUTH;
                case "SOUTH_EAST":
                    return Direction.SOUTH_EAST;
                case "SOUTH_WEST":
                    return Direction.SOUTH_WEST;
                case "EAST":
                    return Direction.EAST;
                case "WEST":
                    return Direction.WEST;
            }
        }
        return Direction.SOUTH;
    }
}
