package com.builtbroken.advancedblockplacement;

import com.builtbroken.advancedblockplacement.AdvancedBlockPlacement.PlacementMode;
import com.builtbroken.advancedblockplacement.network.ModeSetPacket;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CommandAPToggle extends CommandBase {

    @Override
    public String getName() {
        return "aptoggle";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/aptoggle";
    }
    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender instanceof EntityPlayer;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            PlacementMode mode = AdvancedBlockPlacement.ADVANCED_PLACEMENT_MAP.getOrDefault(player.getGameProfile().getId(), PlacementMode.NORMAL);
            PlacementMode inverse = mode.isAdvanced() ? PlacementMode.NORMAL : PlacementMode.ADVANCED;
            AdvancedBlockPlacement.ADVANCED_PLACEMENT_MAP.put(player.getGameProfile().getId(), inverse);
            player.sendMessage(new TextComponentString("Placement mode set to " + inverse.toString().toLowerCase()));
            if(player instanceof EntityPlayerMP) {
                AdvancedBlockPlacement.NETWORK_INSTANCE.sendTo(new ModeSetPacket(inverse), (EntityPlayerMP) player);
            }
        }
    }

}
