package com.builtbroken.advancedblockplacement.network;

import com.builtbroken.advancedblockplacement.AdvancedBlockPlacement;
import com.builtbroken.advancedblockplacement.PlacementMode;
import com.builtbroken.advancedblockplacement.config.ConfigMain;
import com.builtbroken.advancedblockplacement.logic.PlacementHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Created by Dark(DarkGuardsman, Robert) on 8/12/2019.
 */
@Mod.EventBusSubscriber(modid = AdvancedBlockPlacement.MODID)
public class NetworkHandler
{
    public static final SimpleNetworkWrapper NETWORK_INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(AdvancedBlockPlacement.MODID);

    public static void registerPackets()
    {
        int nextPacketID = 0;
        NetworkHandler.NETWORK_INSTANCE.registerMessage(PacketConfigSync.ClientHandler.class, PacketConfigSync.class, nextPacketID++, Side.CLIENT);
        NetworkHandler.NETWORK_INSTANCE.registerMessage(PacketSetMode.ClientHandler.class, PacketSetMode.class, nextPacketID++, Side.CLIENT);
        NetworkHandler.NETWORK_INSTANCE.registerMessage(PacketSetMode.ServerHandler.class, PacketSetMode.class, nextPacketID++, Side.SERVER);
    }

    @SubscribeEvent
    public static void onConnect(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.player instanceof EntityPlayerMP)
        {
            NETWORK_INSTANCE.sendTo(new PacketConfigSync(ConfigMain.blocks_affected, ConfigMain.is_blacklist), (EntityPlayerMP) event.player);
            NETWORK_INSTANCE.sendTo(new PacketSetMode(PlacementHandler.ADVANCED_PLACEMENT_MAP.getOrDefault(event.player.getGameProfile().getId(), PlacementMode.NORMAL)), (EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public static void onDisconnect(PlayerEvent.PlayerLoggedOutEvent event)
    {
        PlacementHandler.ADVANCED_PLACEMENT_MAP.remove(event.player.getGameProfile().getId());
        PlacementHandler.LAST_RIGHTCLICK_EVENT.remove(event.player.getGameProfile().getId());
    }
}
