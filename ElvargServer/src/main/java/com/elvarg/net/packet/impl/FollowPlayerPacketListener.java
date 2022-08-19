package com.elvarg.net.packet.impl;

import com.elvarg.game.World;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.movement.path.PathFinder;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;
import com.elvarg.game.task.TaskType;
import com.elvarg.net.packet.Packet;
import com.elvarg.net.packet.PacketExecutor;
import com.elvarg.util.TileUtils;

import java.util.Objects;

/**
 * Handles the follow player packet listener Sets the player to follow when the
 * packet is executed
 *
 * @author Gabriel Hannason
 */
public class FollowPlayerPacketListener implements PacketExecutor {

    @Override
    public void execute(Player player, Packet packet) {

        if (player.busy()) {
            return;
        }

        int otherPlayersIndex = packet.readLEShort();

        if (otherPlayersIndex < 0 || otherPlayersIndex > World.getPlayers().capacity())
            return;

        Player leader = World.getPlayers().get(otherPlayersIndex);

        if (leader == null)
            return;

        player.setFollowing(leader);

        TaskManager.submit(new Task(1, player, false) {

            @Override
            protected void execute() {
                if (player.getFollowing() != null) {
                    player.setMobileInteraction(leader);
                    if (leader.isTeleporting() || !leader.getLocation().isWithinDistance(player.getLocation(), 15)) {
                        player.setPositionToFace(null);
                        stop();
                        return;
                    }
                    int destX = leader.getMovementQueue().followX;
                    int destY = leader.getMovementQueue().followY;
                    if (Objects.equals(new Location(destX, destY), player.getLocation())) {
                        return;
                    }
                    player.getMovementQueue().reset();
                    /** Path finder does all clipping check for me :))))) **/
                    PathFinder.calculateWalkRoute(player, destX, destY);
                } else
                    stop();
            }
        });

    }

}
