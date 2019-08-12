package com.builtbroken.advancedblockplacement.server;

import com.builtbroken.advancedblockplacement.PlacementMode;
import com.builtbroken.advancedblockplacement.logic.PlacementHandler;
import com.builtbroken.advancedblockplacement.network.NetworkHandler;
import com.builtbroken.advancedblockplacement.network.PacketSetMode;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CommandAPToggle extends CommandBase
{

    @Override
    public String getName()
    {
        return "aptoggle";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/aptoggle";
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender)
    {
        return sender instanceof EntityPlayer;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (sender instanceof EntityPlayer)
        {
            EntityPlayer player = (EntityPlayer) sender;
            PlacementMode mode = PlacementHandler.ADVANCED_PLACEMENT_MAP.getOrDefault(player.getGameProfile().getId(), PlacementMode.NORMAL);
            PlacementMode inverse = mode.isAdvanced() ? PlacementMode.NORMAL : PlacementMode.ADVANCED;
            PlacementHandler.ADVANCED_PLACEMENT_MAP.put(player.getGameProfile().getId(), inverse);
            player.sendMessage(new TextComponentString("Placement mode set to " + inverse.toString().toLowerCase()));
            if (player instanceof EntityPlayerMP)
            {
                NetworkHandler.NETWORK_INSTANCE.sendTo(new PacketSetMode(inverse), (EntityPlayerMP) player);
            }
        }
    }

}
