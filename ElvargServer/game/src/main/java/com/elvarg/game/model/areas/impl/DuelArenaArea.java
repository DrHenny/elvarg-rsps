package com.elvarg.game.model.areas.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.elvarg.game.GameConstants;
import com.elvarg.game.content.Dueling.DuelRule;
import com.elvarg.game.content.Dueling.DuelState;
import com.elvarg.game.content.combat.CombatFactory.CanAttackResponse;
import com.elvarg.game.entity.impl.Mobile;
import com.elvarg.game.entity.impl.object.GameObject;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.entity.impl.playerbot.PlayerBot;
import com.elvarg.game.model.Boundary;
import com.elvarg.game.model.Location;
import com.elvarg.game.model.areas.Area;
import com.elvarg.util.timers.TimerKey;

public class DuelArenaArea extends Area {

    private static final List<Boundary> area = Arrays.asList(
            new Boundary(3324, 3399, 3253, 3266),//massive square around the whole duel arena
            new Boundary(3345, 3382, 3265, 3329),//main building + building behind
            new Boundary(3312, 3327, 3224, 3247));//entrance

    public static boolean inBounds(Player player) {
        Location loc = player.getLocation();
        return area.stream().anyMatch(f -> f.inside(loc));
    }

    public static boolean inBounds(Location location) {
        return area.stream().anyMatch(f -> f.inside(location));
    }

    public DuelArenaArea() {
        super(area);
    }

    @Override
    public void postEnter(Mobile character) {
        if (character.isPlayer()) {
            Player player = character.getAsPlayer();
            player.getPacketSender().sendInteractionOption("Challenge", 1, false);
            player.getPacketSender().sendInteractionOption("null", 2, true);
        }
    }

    @Override
    public void postLeave(Mobile character, boolean logout) {
        if (character.isPlayer()) {
            Player player = character.getAsPlayer();
            if (player.getDueling().inDuel()) {
                player.getDueling().duelLost();
            }
            player.getPacketSender().sendInteractionOption("null", 2, true);
            player.getPacketSender().sendInteractionOption("null", 1, false);


        }
    }

    @Override
    public void process(Mobile character) {

    }

    @Override
    public boolean canTeleport(Player player) {
        if (player.getDueling().inDuel()) {
            return false;
        }
        return true;
    }

    @Override
    public CanAttackResponse canAttack(Mobile character, Mobile target) {
        if (character.isPlayer() && target.isPlayer()) {
            Player a = character.getAsPlayer();
            Player t = target.getAsPlayer();
            if (a.getDueling().getState() == DuelState.IN_DUEL && t.getDueling().getState() == DuelState.IN_DUEL) {
                return CanAttackResponse.CAN_ATTACK;
            } else if (a.getDueling().getState() == DuelState.STARTING_DUEL
                    || t.getDueling().getState() == DuelState.STARTING_DUEL) {
                return CanAttackResponse.DUEL_NOT_STARTED_YET;
            }

            return CanAttackResponse.DUEL_WRONG_OPPONENT;
        }

        return CanAttackResponse.CAN_ATTACK;
    }

    @Override
    public boolean canTrade(Player player, Player target) {
        if (player.getDueling().inDuel()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isMulti(Mobile character) {
        return true;
    }

    @Override
    public boolean canEat(Player player, int itemId) {
        if (player.getDueling().inDuel() && player.getDueling().getRules()[DuelRule.NO_FOOD.ordinal()]) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canDrink(Player player, int itemId) {
        if (player.getDueling().inDuel() && player.getDueling().getRules()[DuelRule.NO_POTIONS.ordinal()]) {
            return false;
        }
        return true;
    }

    @Override
    public boolean dropItemsOnDeath(Player player, Optional<Player> killer) {
        if (player.getDueling().inDuel()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean handleDeath(Player player, Optional<Player> killer) {
        if (player.getDueling().inDuel()) {
            player.getDueling().duelLost();
            return true;
        }
        return false;
    }

    @Override
    public void onPlayerRightClick(Player player, Player rightClicked, int option) {
        if (option == 1) {
            if (player.busy()) {
                player.getPacketSender().sendMessage("You cannot do that right now.");
                return;
            }
            if (rightClicked.busy()) {
                player.getPacketSender().sendMessage("That player is currently busy.");
                return;
            }
            player.getDueling().requestDuel(rightClicked);
        }
    }

    @Override
    public void defeated(Player player, Mobile character) {
    }

    @Override
    public boolean handleObjectClick(Player player, GameObject object, int type) {
        return false;
    }
}
