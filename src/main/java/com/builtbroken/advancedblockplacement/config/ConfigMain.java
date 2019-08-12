package com.builtbroken.advancedblockplacement.config;

import com.builtbroken.advancedblockplacement.AdvancedBlockPlacement;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dark(DarkGuardsman, Robert) on 8/12/2019.
 */
@Mod.EventBusSubscriber(modid = AdvancedBlockPlacement.MODID)
@Config(modid = AdvancedBlockPlacement.MODID, name = AdvancedBlockPlacement.MODID)
public class ConfigMain
{
    @Config.Comment("A list of registry names of blocks to be affected by advanced placement")
    public static String[] blocks_affected = new String[]{
            "minecraft:chest",
            "minecraft:furnace",
            "minecraft:dispenser",
            "minecraft:hopper",
            "minecraft:dropper",
            "minecraft:piston",
            "minecraft:sticky_piston"
    };

    @Config.Comment("Setting to true will make all blocks affected by default but disallow those in the affected list")
    public static boolean is_blacklist = false;

    @Config.Ignore
    public static boolean needsMapped = true;

    //Cache
    @Config.Ignore
    private static List<Block> blockList;

    public static void mapCache()
    {
        needsMapped = false;
        blockList = new ArrayList(blocks_affected.length);
        for (String name : blocks_affected)
        {
            final ResourceLocation regName = new ResourceLocation(name);
            final Block block = ForgeRegistries.BLOCKS.getValue(regName);
            if (block != null && block != Blocks.AIR)
            {
                blockList.add(block);
            }
            else
            {
                AdvancedBlockPlacement.logger.error("ConfigMain: Failed to locate block by name '" + name + "'");
            }
        }
    }

    @SubscribeEvent
    public static void onConfigUpdate(ConfigChangedEvent event)
    {
        if (event.getModID().equals(AdvancedBlockPlacement.MODID))
        {
            ConfigManager.sync(AdvancedBlockPlacement.MODID, Config.Type.INSTANCE);
            mapCache();
        }
    }

    public static boolean isAffected(final Block block)
    {
        if (needsMapped)
        {
            mapCache();
        }
        final boolean contained = blockList.contains(block);
        return (is_blacklist && !contained) || (!is_blacklist && contained);
    }
}
