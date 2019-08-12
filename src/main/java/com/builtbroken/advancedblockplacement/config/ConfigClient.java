package com.builtbroken.advancedblockplacement.config;

import com.builtbroken.advancedblockplacement.AdvancedBlockPlacement;
import net.minecraftforge.common.config.Config;

/**
 * Created by Dark(DarkGuardsman, Robert) on 8/12/2019.
 */
@Config(modid = AdvancedBlockPlacement.MODID, name = AdvancedBlockPlacement.MODID + "-client")
public class ConfigClient
{

    @Config.Comment("A keycode corresponding to the default keybinding for the advanced placement mode (for pack makers to ship keybinds with) 0 = none")
    public static int default_keybind = 0;

    @Config.Comment("Setting to false will remove the block preview AND arrow effect when in advanced placement mode")
    public static boolean do_special_rendering = true;

    @Config.Comment("Setting to false will require the user to hold down the advanced placement key to stay in advanced placement mode rather than toggling it")
    public static boolean is_keybind_toggle = true;

    @Config.Comment("Setting to true will always display the arrow- not just for tile entities")
    public static boolean always_display_arrow = false;

    @Config.Comment("Setting to false stops block previews but allows custom rendering (the arrow)")
    public static boolean do_block_preview = true;

}
