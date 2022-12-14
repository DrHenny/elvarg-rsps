package com.elvarg.game.model.commands.impl;

import com.elvarg.game.collision.RegionManager;
import com.elvarg.game.definition.NPCDef;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.commands.Command;
import com.elvarg.game.model.dialogues.builders.impl.NieveDialogue;
import com.elvarg.game.model.rights.PlayerRights;

public class DebugCommand implements Command {

    @Override
    public void execute(Player player, String command, String[] parts) {
        //System.out.println(RegionManager.wallsExist(player.getLocation().clone(), player.getPrivateArea()));
        try {
            NPCDef def = NPCDef.lookup(Integer.valueOf(parts[1]));
            System.err.println("defs for "+def.name+" are..");
            System.err.println("Walk="+def.walkAnim+" Stand="+def.standAnim+" Id="+def.id+" Size="+def.size+" "+def.turn180AnimIndex+" "+def.turn90CCWAnimIndex+" "+def.turn90CWAnimIndex+" ");
        } catch (Exception e) {
            System.err.println("parts="+parts.length+" first="+parts[0]);
            e.printStackTrace();
        }
    }

    @Override
    public boolean canUse(Player player) {
        return true;
    }

}
