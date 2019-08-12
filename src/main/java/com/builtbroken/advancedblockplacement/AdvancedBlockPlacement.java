package com.builtbroken.advancedblockplacement;

import com.builtbroken.advancedblockplacement.logic.PlacementHandler;
import com.builtbroken.advancedblockplacement.network.NetworkHandler;
import com.builtbroken.advancedblockplacement.server.CommandAPToggle;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = AdvancedBlockPlacement.MODID, name = AdvancedBlockPlacement.NAME, version = AdvancedBlockPlacement.VERSION, acceptableRemoteVersions = "*")
public class AdvancedBlockPlacement
{

    // Reference Fields
    public static final String MODID = "advancedblockplacement";
    public static final String NAME = "Advanced Block Placement";

    //Version data
    public static final String MAJOR = "@MAJOR@";
    public static final String MINOR = "@MINOR@";
    public static final String REVIS = "@REVIS@";
    public static final String VERSION = MAJOR + "." + MINOR + "." + REVIS;

    public static Logger logger;

    // Functional Fields

    @SidedProxy(clientSide = "com.builtbroken.advancedblockplacement.AdvancedBlockPlacementClient", serverSide = "com.builtbroken.advancedblockplacement.AdvancedBlockPlacement$ISidedProxy$DummyProxy")
    public static ISidedProxy proxy;

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        PlacementHandler.ADVANCED_PLACEMENT_MAP.clear();
        PlacementHandler.LAST_RIGHTCLICK_EVENT.clear();
        event.registerServerCommand(new CommandAPToggle());
    }

    @EventHandler
    public static void serverStopping(FMLServerStoppingEvent event)
    {
        PlacementHandler.ADVANCED_PLACEMENT_MAP.clear();
        PlacementHandler.LAST_RIGHTCLICK_EVENT.clear();
    }

    @EventHandler
    public static void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        NetworkHandler.registerPackets();
    }

    @EventHandler
    public static void init(FMLInitializationEvent event)
    {
        proxy.init(event);
    }
}