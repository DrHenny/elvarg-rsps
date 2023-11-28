package com.elvarg.game.content;

import com.elvarg.game.definition.ItemDefinition;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.*;
import com.elvarg.game.model.container.ItemContainer;
import com.elvarg.game.model.container.StackType;
import com.elvarg.game.model.container.impl.Equipment;
import com.elvarg.game.model.container.impl.Inventory;
import com.elvarg.game.task.Task;
import com.elvarg.game.task.TaskManager;
import com.elvarg.util.Misc;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the dueling system.
 *
 * @author Professor Oak
 */
public class Dueling {

    public static final int MAIN_INTERFACE_CONTAINER = 6669;
    private static final int DUELING_WITH_FRAME = 6671;
    private static final int INTERFACE_ID = 6575;
    private static final int CONFIRM_INTERFACE_ID = 6412;
    private static final int SCOREBOARD_INTERFACE_ID = 6733;
    private static final int SCOREBOARD_CONTAINER = 6822;
    private static final int SCOREBOARD_USERNAME_FRAME = 6840;
    private static final int SCOREBOARD_COMBAT_LEVEL_FRAME = 6839;
    private static final int SECOND_INTERFACE_CONTAINER = 6670;
    private static final int STATUS_FRAME_1 = 6684;
    private static final int STATUS_FRAME_2 = 6571;
    private static final int ITEM_LIST_1_FRAME = 6516;
    private static final int ITEM_LIST_2_FRAME = 6517;
    private static final int RULES_FRAME_START = 8242;
    private static final int RULES_CONFIG_ID = 286;
    private static final int TOTAL_WORTH_FRAME = 24234;
    private final Player defeated;
    private final ItemContainer container;
    // Rules
    private final boolean[] rules = new boolean[DuelRule.values().length];
    private Player winner;
    private int configValue;
    private DuelState state = DuelState.NONE;
    // Delays!!
    private SecondsTimer button_delay = new SecondsTimer();
    private SecondsTimer request_delay = new SecondsTimer();

    public Dueling(Player defeated) {
        this.defeated = defeated;
        // The container which will hold all our offered items.
        this.container = new ItemContainer(defeated) {
            @Override
            public StackType stackType() {
                return StackType.DEFAULT;
            }

            @Override
            public ItemContainer refreshItems() {
                defeated.getPacketSender().sendInterfaceSet(INTERFACE_ID, Trading.CONTAINER_INVENTORY_INTERFACE);
                defeated.getPacketSender().sendItemContainer(defeated.getInventory(),
                        Trading.INVENTORY_CONTAINER_INTERFACE);
                defeated.getPacketSender().sendInterfaceItems(MAIN_INTERFACE_CONTAINER,
                        defeated.getDueling().getContainer().getValidItems());
                defeated.getPacketSender().sendInterfaceItems(SECOND_INTERFACE_CONTAINER,
                        winner.getDueling().getContainer().getValidItems());
                winner.getPacketSender().sendInterfaceItems(MAIN_INTERFACE_CONTAINER,
                        winner.getDueling().getContainer().getValidItems());
                winner.getPacketSender().sendInterfaceItems(SECOND_INTERFACE_CONTAINER,
                        defeated.getDueling().getContainer().getValidItems());
                return this;
            }

            @Override
            public ItemContainer full() {
                getPlayer().getPacketSender().sendMessage("You cannot stake more items.");
                return this;
            }

            @Override
            public int capacity() {
                return 28;
            }
        };
    }

    /**
     * Validates a player. Basically checks that all specified params add up.
     *
     * @param player
     * @param interact
     * @param playerStatus
     * @param duelStates
     * @return
     */
    private static boolean validate(Player player, Player interact, PlayerStatus playerStatus,
                                    DuelState... duelStates) {
        // Verify player...
        if (player == null || interact == null) {
            return false;
        }

        // Make sure we have proper status
        if (playerStatus != null) {
            if (player.getStatus() != playerStatus) {
                return false;
            }

            // Make sure we're interacting with eachother
            if (interact.getStatus() != playerStatus) {
                return false;
            }
        }

        if (player.getDueling().getWinner() == null || player.getDueling().getWinner() != interact) {
            return false;
        }
        if (interact.getDueling().getWinner() == null || interact.getDueling().getWinner() != player) {
            return false;
        }

        // Make sure we have proper duel state.
        boolean found = false;
        for (DuelState duelState : duelStates) {
            if (player.getDueling().getState() == duelState) {
                found = true;
                break;
            }
        }
        if (!found) {
            return false;
        }

        // Do the same for our interact
        found = false;
        for (DuelState duelState : duelStates) {
            if (interact.getDueling().getState() == duelState) {
                found = true;
                break;
            }
        }
        if (!found) {
            return false;
        }
        return true;
    }

    public void requestDuel(Player t_) {
        if (state == DuelState.NONE || state == DuelState.REQUESTED_DUEL) {

            // Make sure to not allow flooding!
            if (!request_delay.finished()) {
                int seconds = request_delay.secondsRemaining();
                defeated.getPacketSender()
                        .sendMessage("You must wait another " + (seconds == 1 ? "second" : "" + seconds + " seconds")
                                + " before sending more duel challenges.");
                return;
            }

            // The other players' current duel state.
            final DuelState t_state = t_.getDueling().getState();

            // Should we initiate the duel or simply send a request?
            boolean initiateDuel = false;

            // Update this instance...
            this.setWinner(t_);
            this.setState(DuelState.REQUESTED_DUEL);

            // Check if target requested a duel with us...
            if (t_state == DuelState.REQUESTED_DUEL) {
                if (t_.getDueling().getWinner() != null && t_.getDueling().getWinner() == defeated) {
                    initiateDuel = true;
                }
            }

            // Initiate duel for both players with eachother?
            if (initiateDuel) {
                defeated.getDueling().initiateDuel();
                t_.getDueling().initiateDuel();
            } else {
                defeated.getPacketSender().sendMessage("You've sent a duel challenge to " + t_.getUsername() + "...");
                t_.getPacketSender().sendMessage(defeated.getUsername() + ":duelreq:");

                if (t_.isPlayerBot()) {
                    // Player Bots: Automatically accept any duel request
                    t_.getDueling().requestDuel(defeated);
                }
            }

            // Set the request delay to 2 seconds at least.
            request_delay.start(2);
        } else {
            defeated.getPacketSender().sendMessage("You cannot do that right now.");
        }
    }

    public void initiateDuel() {
        // Set our duel state
        setState(DuelState.DUEL_SCREEN);

        // Set our player status
        defeated.setStatus(PlayerStatus.DUELING);

        // Reset right click options
        defeated.getPacketSender().sendInteractionOption("null", 2, true);
        defeated.getPacketSender().sendInteractionOption("null", 1, false);

        // Reset rule toggle configs
        defeated.getPacketSender().sendConfig(RULES_CONFIG_ID, 0);

        // Update strings on interface
        defeated.getPacketSender()
                .sendString(DUELING_WITH_FRAME,
                        "@or1@Dueling with: @whi@" + winner.getUsername() + "@or1@          Combat level: @whi@"
                                + winner.getSkillManager().getCombatLevel())
                .sendString(STATUS_FRAME_1, "").sendString(669, "Lock Weapon")
                .sendString(8278, "Neither player is allowed to change weapon.");

        // Send equipment on the interface..
        int equipSlot = 0;
        for (Item item : defeated.getEquipment().getItems()) {
            defeated.getPacketSender().sendItemOnInterface(13824, item.getId(), equipSlot, item.getAmount());
            equipSlot++;
        }

        // Reset container
        container.resetItems();

        // Refresh and send container...
        container.refreshItems();
    }

    public void closeDuel() {
        if (state != DuelState.NONE) {

            // Cache the current interact
            final Player interact_ = winner;

            // Return all items...
            for (Item t : container.getValidItems()) {
                container.switchItem(defeated.getInventory(), t.clone(), false, false);
            }

            // Refresh inventory
            defeated.getInventory().refreshItems();

            // Reset all attributes...
            resetAttributes();

            // Send decline message
            defeated.getPacketSender().sendMessage("Duel declined.");
            defeated.getPacketSender().sendInterfaceRemoval();

            // Reset/close duel for other player aswell (the cached interact)
            if (interact_ != null) {
                if (interact_.getStatus() == PlayerStatus.DUELING) {
                    if (interact_.getDueling().getWinner() != null
                            && interact_.getDueling().getWinner() == defeated) {
                        interact_.getPacketSender().sendInterfaceRemoval();
                    }
                }
            }
        }
    }

    public void resetAttributes() {

        // Reset duel attributes
        setWinner(null);
        setState(DuelState.NONE);

        // Reset player status if it's dueling.
        if (defeated.getStatus() == PlayerStatus.DUELING) {
            defeated.setStatus(PlayerStatus.NONE);
        }

        // Reset container..
        container.resetItems();

        // Reset rules
        for (int i = 0; i < rules.length; i++) {
            rules[i] = false;
        }

        // Clear toggles
        configValue = 0;
        defeated.getPacketSender().sendConfig(RULES_CONFIG_ID, 0);

        // Update right click options..
        defeated.getPacketSender().sendInteractionOption("Challenge", 1, false);
        defeated.getPacketSender().sendInteractionOption("null", 2, true);

        // Clear head hint
        defeated.getPacketSender().sendEntityHintRemoval(true);

        // Clear items on interface
        defeated.getPacketSender().clearItemOnInterface(MAIN_INTERFACE_CONTAINER)
                .clearItemOnInterface(SECOND_INTERFACE_CONTAINER);
    }

    // Deposit or withdraw an item....
    public void handleItem(int id, int amount, int slot, ItemContainer from, ItemContainer to) {
        if (defeated.getInterfaceId() == INTERFACE_ID) {

            // Validate this stake action..
            if (!validate(defeated, winner, PlayerStatus.DUELING,
                    new DuelState[]{DuelState.DUEL_SCREEN, DuelState.ACCEPTED_DUEL_SCREEN})) {
                return;
            }

            if (ItemDefinition.forId(id).getValue() == 0) {
                defeated.getPacketSender().sendMessage("There's no point in staking that. It's spawnable!");
                return;
            }

            // Check if the duel was previously accepted (and now modified)...
            if (state == DuelState.ACCEPTED_DUEL_SCREEN) {
                state = DuelState.DUEL_SCREEN;
            }
            if (winner.getDueling().getState() == DuelState.ACCEPTED_DUEL_SCREEN) {
                winner.getDueling().setState(DuelState.DUEL_SCREEN);
            }
            defeated.getPacketSender().sendString(STATUS_FRAME_1, "@red@DUEL MODIFIED!");
            winner.getPacketSender().sendString(STATUS_FRAME_1, "@red@DUEL MODIFIED!");

            // Handle the item switch..
            if (state == DuelState.DUEL_SCREEN && winner.getDueling().getState() == DuelState.DUEL_SCREEN) {

                // Check if the item is in the right place
                if (from.getItems()[slot].getId() == id) {

                    // Make sure we can fit that amount in the duel
                    if (from instanceof Inventory) {
                        if (!ItemDefinition.forId(id).isStackable()) {
                            if (amount > container.getFreeSlots()) {
                                amount = container.getFreeSlots();
                            }
                        }
                    }

                    if (amount <= 0) {
                        return;
                    }

                    final Item item = new Item(id, amount);

                    // Only sort items if we're withdrawing items from the duel.
                    final boolean sort = (from == (defeated.getDueling().getContainer()));

                    // Do the switch!
                    if (item.getAmount() == 1) {
                        from.switchItem(to, item, slot, sort, true);
                    } else {
                        from.switchItem(to, item, sort, true);
                    }
                }
            } else {
                defeated.getPacketSender().sendInterfaceRemoval();
            }
        }
    }

    public void acceptDuel() {

        // Validate this stake action..
        if (!validate(defeated, winner, PlayerStatus.DUELING, new DuelState[]{DuelState.DUEL_SCREEN,
                DuelState.ACCEPTED_DUEL_SCREEN, DuelState.CONFIRM_SCREEN, DuelState.ACCEPTED_CONFIRM_SCREEN})) {
            return;
        }

        // Check button delay...
        if (!button_delay.finished()) {
            return;
        }

        // Check button delay...
        // if(!button_delay.finished()) {
        // return;
        // }

        // Cache the interact...
        final Player interact_ = winner;

        // Interact's current trade state.
        final DuelState t_state = interact_.getDueling().getState();

        // Check which action to take..
        if (state == DuelState.DUEL_SCREEN) {

            // Verify that the interact can receive all items first..
            int slotsRequired = getFreeSlotsRequired(defeated);
            if (defeated.getInventory().getFreeSlots() < slotsRequired) {
                defeated.getPacketSender()
                        .sendMessage("You need at least " + slotsRequired + " free inventory slots for this duel.");
                return;
            }

            if (rules[DuelRule.NO_MELEE.ordinal()] && rules[DuelRule.NO_RANGED.ordinal()]
                    && rules[DuelRule.NO_MAGIC.ordinal()]) {
                defeated.getPacketSender().sendMessage("You must enable at least one of the three combat styles.");
                return;
            }

            // Both are in the same state. Do the first-stage accept.
            setState(DuelState.ACCEPTED_DUEL_SCREEN);

            // Update status...
            defeated.getPacketSender().sendString(STATUS_FRAME_1, "Waiting for other player..");
            interact_.getPacketSender().sendString(STATUS_FRAME_1, "" + defeated.getUsername() + " has accepted.");

            // Check if both have accepted..
            if (state == DuelState.ACCEPTED_DUEL_SCREEN && t_state == DuelState.ACCEPTED_DUEL_SCREEN) {

                // Technically here, both have accepted.
                // Go into confirm screen!
                defeated.getDueling().confirmScreen();
                interact_.getDueling().confirmScreen();
            } else {
                if (interact_.isPlayerBot()) {
                    interact_.getDueling().acceptDuel();
                }
            }
        } else if (state == DuelState.CONFIRM_SCREEN) {
            // Both are in the same state. Do the second-stage accept.
            setState(DuelState.ACCEPTED_CONFIRM_SCREEN);

            // Update status...
            defeated.getPacketSender().sendString(STATUS_FRAME_2,
                    "Waiting for " + interact_.getUsername() + "'s confirmation..");
            interact_.getPacketSender().sendString(STATUS_FRAME_2,
                    "" + defeated.getUsername() + " has accepted. Do you wish to do the same?");

            // Check if both have accepted..
            if (state == DuelState.ACCEPTED_CONFIRM_SCREEN && t_state == DuelState.ACCEPTED_CONFIRM_SCREEN) {

                // Both accepted, start duel

                // Decide where they will spawn in the arena..
                final boolean obstacle = rules[DuelRule.OBSTACLES.ordinal()];
                final boolean movementDisabled = rules[DuelRule.NO_MOVEMENT.ordinal()];

                Location pos1 = getRandomSpawn(obstacle);
                Location pos2 = getRandomSpawn(obstacle);

                // Make them spaw next to eachother
                if (movementDisabled) {
                    pos2 = pos1.clone().add(-1, 0);
                }

                defeated.getDueling().startDuel(pos1);
                interact_.getDueling().startDuel(pos2);
            } else {
                if (interact_.isPlayerBot()) {
                    interact_.getDueling().acceptDuel();
                }
            }
        }

        button_delay.start(1);

    }

    public Location getRandomSpawn(boolean obstacle) {
        if (obstacle) {
            return new Location(3366 + Misc.getRandom(11), 3246 + Misc.getRandom(6));
        }
        return new Location(3335 + Misc.getRandom(11), 3246 + Misc.getRandom(6));
    }

    private void confirmScreen() {
        // Update state
        defeated.getDueling().setState(DuelState.CONFIRM_SCREEN);

        // Send new interface frames
        String this_items = Trading.listItems(container);
        String interact_item = Trading.listItems(winner.getDueling().getContainer());
        defeated.getPacketSender().sendString(ITEM_LIST_1_FRAME, this_items);
        defeated.getPacketSender().sendString(ITEM_LIST_2_FRAME, interact_item);

        // Reset all previous strings related to rules
        for (int i = 8238; i <= 8253; i++) {
            defeated.getPacketSender().sendString(i, "");
        }

        // Send new ones
        defeated.getPacketSender().sendString(8250, "Hitpoints will be restored.");
        defeated.getPacketSender().sendString(8238, "Boosted stats will be restored.");
        if (rules[DuelRule.OBSTACLES.ordinal()]) {
            defeated.getPacketSender().sendString(8239, "@red@There will be obstacles in the arena.");
        }
        defeated.getPacketSender().sendString(8240, "");
        defeated.getPacketSender().sendString(8241, "");

        int ruleFrameIndex = RULES_FRAME_START;
        for (int i = 0; i < DuelRule.values().length; i++) {
            if (i == DuelRule.OBSTACLES.ordinal())
                continue;
            if (rules[i]) {
                defeated.getPacketSender().sendString(ruleFrameIndex, "" + DuelRule.forId(i).toString());
                ruleFrameIndex++;
            }
        }

        defeated.getPacketSender().sendString(STATUS_FRAME_2, "");

        // Send new interface..
        defeated.getPacketSender().sendInterfaceSet(CONFIRM_INTERFACE_ID, Inventory.INTERFACE_ID);
        defeated.getPacketSender().sendItemContainer(defeated.getInventory(), Trading.INVENTORY_CONTAINER_INTERFACE);
    }

    public boolean checkRule(int button) {
        DuelRule rule = DuelRule.forButtonId(button);
        if (rule != null) {
            checkRule(rule);
            return true;
        }
        return false;
    }

    private void checkRule(DuelRule rule) {

        // Check if we're actually dueling..
        if (defeated.getStatus() != PlayerStatus.DUELING) {
            return;
        }

        // Verify stake...
        if (!validate(defeated, winner, PlayerStatus.DUELING,
                new DuelState[]{DuelState.DUEL_SCREEN, DuelState.ACCEPTED_DUEL_SCREEN})) {
            return;
        }

        // Verify our current state..
        if (state == DuelState.DUEL_SCREEN || state == DuelState.ACCEPTED_DUEL_SCREEN) {

            // Toggle the rule..
            if (!rules[rule.ordinal()]) {
                rules[rule.ordinal()] = true;
                configValue += rule.getConfigId();
            } else {
                rules[rule.ordinal()] = false;
                configValue -= rule.getConfigId();
            }

            // Update interact's rules to match ours.
            winner.getDueling().setConfigValue(configValue);
            winner.getDueling().getRules()[rule.ordinal()] = rules[rule.ordinal()];

            // Send toggles for both players.
            defeated.getPacketSender().sendToggle(RULES_CONFIG_ID, configValue);
            winner.getPacketSender().sendToggle(RULES_CONFIG_ID, configValue);

            // Send modify status
            if (state == DuelState.ACCEPTED_DUEL_SCREEN) {
                state = DuelState.DUEL_SCREEN;
            }
            if (winner.getDueling().getState() == DuelState.ACCEPTED_DUEL_SCREEN) {
                winner.getDueling().setState(DuelState.DUEL_SCREEN);
            }
            defeated.getPacketSender().sendString(STATUS_FRAME_1, "@red@DUEL MODIFIED!");
            winner.getPacketSender().sendString(STATUS_FRAME_1, "@red@DUEL MODIFIED!");

            // Inform them about this "custom" rule.
            if (rule == DuelRule.LOCK_WEAPON && rules[rule.ordinal()]) {
                defeated.getPacketSender()
                        .sendMessage(
                                "@red@Warning! The rule 'Lock Weapon' has been enabled. You will not be able to change")
                        .sendMessage("@red@weapon during the duel!");
                winner.getPacketSender()
                        .sendMessage(
                                "@red@Warning! The rule 'Lock Weapon' has been enabled. You will not be able to change")
                        .sendMessage("@red@weapon during the duel!");
            }
        }
    }

    private void startDuel(Location telePos) {
        // Let's start the duel!

        // Set current duel state
        setState(DuelState.STARTING_DUEL);

        // Close open interfaces
        defeated.getPacketSender().sendInterfaceRemoval();

        // Unequip items based on the rules set for this duel
        for (int i = 11; i < rules.length; i++) {
            DuelRule rule = DuelRule.forId(i);
            if (rules[i]) {
                if (rule.getEquipmentSlot() < 0)
                    continue;
                if (defeated.getEquipment().getItems()[rule.getEquipmentSlot()].getId() > 0) {
                    Item item = new Item(defeated.getEquipment().getItems()[rule.getEquipmentSlot()].getId(),
                            defeated.getEquipment().getItems()[rule.getEquipmentSlot()].getAmount());
                    defeated.getEquipment().delete(item);
                    defeated.getInventory().add(item);
                }
            }
        }
        if (rules[DuelRule.NO_WEAPON.ordinal()] || rules[DuelRule.NO_SHIELD.ordinal()]) {
            if (defeated.getEquipment().getItems()[Equipment.WEAPON_SLOT].getId() > 0) {
                if (ItemDefinition.forId(defeated.getEquipment().getItems()[Equipment.WEAPON_SLOT].getId())
                        .isDoubleHanded()) {
                    Item item = new Item(defeated.getEquipment().getItems()[Equipment.WEAPON_SLOT].getId(),
                            defeated.getEquipment().getItems()[Equipment.WEAPON_SLOT].getAmount());
                    defeated.getEquipment().delete(item);
                    defeated.getInventory().add(item);
                }
            }
        }

        // Clear items on interface
        defeated.getPacketSender().clearItemOnInterface(MAIN_INTERFACE_CONTAINER)
                .clearItemOnInterface(SECOND_INTERFACE_CONTAINER);

        // Update right click options..
        defeated.getPacketSender().sendInteractionOption("Attack", 2, true);
        defeated.getPacketSender().sendInteractionOption("null", 1, false);

        // Reset attributes..
        defeated.resetAttributes();

        // Freeze the player
        if (rules[DuelRule.NO_MOVEMENT.ordinal()]) {
            defeated.getMovementQueue().reset().setBlockMovement(true);
        }

        // Send interact hints
        defeated.getPacketSender().sendPositionalHint(winner.getLocation().clone(), 10);
        defeated.getPacketSender().sendEntityHint(winner);

        // Teleport the player
        defeated.moveTo(telePos);

        // Make them interact with eachother
        defeated.setMobileInteraction(winner);

        // Send countdown as a task
        TaskManager.submit(new Task(2, defeated, false) {
            int timer = 3;

            @Override
            public void execute() {
                if (defeated.getDueling().getState() != DuelState.STARTING_DUEL) {
                    stop();
                    return;
                }
                if (timer == 3 || timer == 2 || timer == 1) {
                    defeated.forceChat("" + timer + "..");
                } else {
                    defeated.getDueling().setState(DuelState.IN_DUEL);
                    defeated.forceChat("FIGHT!!");
                    stop();
                }
                timer--;
            }
        });
    }

    public void duelLost() {

        // Make sure both players are in a duel..
        if (validate(defeated, winner, null, new DuelState[]{DuelState.STARTING_DUEL, DuelState.IN_DUEL})) {

            // Add won items to a list..
            int totalValue = 0;
            List<Item> winnings = new ArrayList<Item>();
            for (Item item : winner.getDueling().getContainer().getValidItems()) {
                winner.getInventory().add(item);
                winnings.add(item);
                totalValue += item.getDefinition().getValue();
            }
            for (Item item : defeated.getDueling().getContainer().getValidItems()) {
                winner.getInventory().add(item);
                winnings.add(item);
                totalValue += item.getDefinition().getValue();
            }

            // Send interface data..
            winner.getPacketSender().sendString(SCOREBOARD_USERNAME_FRAME, defeated.getUsername())
                    .sendString(SCOREBOARD_COMBAT_LEVEL_FRAME, "" + defeated.getSkillManager().getCombatLevel())
                    .sendString(TOTAL_WORTH_FRAME,
                            "@yel@Total: @or1@" + Misc.insertCommasToNumber("" + totalValue + "") + " value!");

            // Send winnings onto interface
            winner.getPacketSender().sendInterfaceItems(SCOREBOARD_CONTAINER, winnings);

            // Send the scoreboard interface
            winner.getPacketSender().sendInterface(SCOREBOARD_INTERFACE_ID);

            // Restart the winner's stats
            winner.resetAttributes();

            // Move players home
            Boundary spawn = new Boundary(3356, 3378, 3268, 3278);
            defeated.smartMove(spawn);
            winner.smartMove(spawn);

            // Send messages
            winner.getPacketSender().sendMessage("You won the duel!");
            winner.duelWins++;
            winner.duelWinStreak++;
            winner.duelLossStreak = 0;
            showStatistics(winner);

            defeated.getPacketSender().sendMessage("You lost the duel!");
            defeated.duelLosses++;
            defeated.duelLossStreak++;
            defeated.duelWinStreak = 0;
            showStatistics(defeated);

            if (defeated.duelLossStreak > defeated.duelLossStreakHighest) {
                /** New Record **/
                defeated.duelLossStreakHighest = defeated.duelLossStreak;
                defeated.getPacketSender().sendMessage("Your highest Duel loss streak is now: "+winner.duelLossStreakHighest);
            }

            if (winner.duelWinStreak > winner.duelWinStreakHighest) {
                /** New Record **/
                winner.duelWinStreakHighest = winner.duelWinStreak;
                winner.getPacketSender().sendMessage("Your highest Duel win streak is now: "+winner.duelWinStreakHighest);
            }


            // Reset attributes for both
            winner.getDueling().resetAttributes();
            defeated.getDueling().resetAttributes();
        } else {

            defeated.getDueling().resetAttributes();
            defeated.getPacketSender().sendInterfaceRemoval();

            if (winner != null) {
                winner.getDueling().resetAttributes();
                winner.getPacketSender().sendInterfaceRemoval();
            }
        }
    }

    private void showStatistics(Player player) {
        player.getPacketSender().sendMessage("Duel Stats - Wins: "+player.duelWins+" Losses: "+player.duelLosses+" Win Ratio: "+getWinRatio(player));
    }

    private String getWinRatio(Player player) {
        double radio = (double) (player.duelWins) / (double) (player.duelLosses == 0 ? 1 : player.duelLosses);
        if (radio == 0.00) {
            return "N/A";
        }
        return Misc.format(radio);
    }

    public boolean inDuel() {
        return state == DuelState.STARTING_DUEL || state == DuelState.IN_DUEL;
    }

    private int getFreeSlotsRequired(Player player) {
        int slots = 0;

        // Count equipment that needs to be taken off
        for (int i = 11; i < player.getDueling().getRules().length; i++) {
            DuelRule rule = DuelRule.values()[i];
            if (player.getDueling().getRules()[rule.ordinal()]) {
                Item item = player.getEquipment().getItems()[rule.getEquipmentSlot()];
                if (!item.isValid()) {
                    continue;
                }
                if (!(item.getDefinition().isStackable() && player.getInventory().contains(item.getId()))) {
                    slots += rule.getInventorySpaceReq();
                }
                if (rule == DuelRule.NO_WEAPON || rule == DuelRule.NO_SHIELD) {

                }
            }
        }

        // Count inventory slots from interact's container aswell as ours
        for (Item item : container.getItems()) {
            if (item == null || !item.isValid())
                continue;
            if (!(item.getDefinition().isStackable() && player.getInventory().contains(item.getId()))) {
                slots++;
            }
        }

        for (Item item : winner.getDueling().getContainer().getItems()) {
            if (item == null || !item.isValid())
                continue;
            if (!(item.getDefinition().isStackable() && player.getInventory().contains(item.getId()))) {
                slots++;
            }
        }

        return slots;
    }

    public SecondsTimer getButtonDelay() {
        return button_delay;
    }

    public DuelState getState() {
        return state;
    }

    public void setState(DuelState state) {
        this.state = state;
    }

    public ItemContainer getContainer() {
        return container;
    }

    public Player getWinner() {
        return winner;
    }

    public void setWinner(Player winner) {
        this.winner = winner;
    }

    public boolean[] getRules() {
        return rules;
    }

    public int getConfigValue() {
        return configValue;
    }

    public void setConfigValue(int configValue) {
        this.configValue = configValue;
    }

    public void incrementConfigValue(int configValue) {
        this.configValue += configValue;
    }

    public enum DuelState {
        NONE, REQUESTED_DUEL, DUEL_SCREEN, ACCEPTED_DUEL_SCREEN, CONFIRM_SCREEN, ACCEPTED_CONFIRM_SCREEN, STARTING_DUEL, IN_DUEL;
    }

    public enum DuelRule {
        NO_RANGED(16, 6725, -1, -1), NO_MELEE(32, 6726, -1, -1), NO_MAGIC(64, 6727, -1, -1), NO_SPECIAL_ATTACKS(8192,
                7816, -1, -1), LOCK_WEAPON(4096, 670, -1, -1), NO_FORFEIT(1, 6721, -1, -1), NO_POTIONS(128, 6728, -1,
                -1), NO_FOOD(256, 6729, -1, -1), NO_PRAYER(512, 6730, -1,
                -1), NO_MOVEMENT(2, 6722, -1, -1), OBSTACLES(1024, 6732, -1, -1),

        NO_HELM(16384, 13813, 1, Equipment.HEAD_SLOT), NO_CAPE(32768, 13814, 1, Equipment.CAPE_SLOT), NO_AMULET(65536,
                13815, 1,
                Equipment.AMULET_SLOT), NO_AMMUNITION(134217728, 13816, 1, Equipment.AMMUNITION_SLOT), NO_WEAPON(131072,
                13817, 1, Equipment.WEAPON_SLOT), NO_BODY(262144, 13818, 1, Equipment.BODY_SLOT), NO_SHIELD(
                524288, 13819, 1, Equipment.SHIELD_SLOT), NO_LEGS(2097152, 13820, 1,
                Equipment.LEG_SLOT), NO_RING(67108864, 13821, 1, Equipment.RING_SLOT), NO_BOOTS(
                16777216, 13822, 1, Equipment.FEET_SLOT), NO_GLOVES(8388608, 13823, 1,
                Equipment.HANDS_SLOT);

        private int configId;
        private int buttonId;
        private int inventorySpaceReq;
        private int equipmentSlot;
        DuelRule(int configId, int buttonId, int inventorySpaceReq, int equipmentSlot) {
            this.configId = configId;
            this.buttonId = buttonId;
            this.inventorySpaceReq = inventorySpaceReq;
            this.equipmentSlot = equipmentSlot;
        }

        public static DuelRule forId(int i) {
            for (DuelRule r : DuelRule.values()) {
                if (r.ordinal() == i)
                    return r;
            }
            return null;
        }

        static DuelRule forButtonId(int buttonId) {
            for (DuelRule r : DuelRule.values()) {
                if (r.getButtonId() == buttonId)
                    return r;
            }
            return null;
        }

        public int getConfigId() {
            return configId;
        }

        public int getButtonId() {
            return this.buttonId;
        }

        public int getInventorySpaceReq() {
            return this.inventorySpaceReq;
        }

        public int getEquipmentSlot() {
            return this.equipmentSlot;
        }

        @Override
        public String toString() {
            return Misc.formatText(this.name().toLowerCase());
        }
    }
}
