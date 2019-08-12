package com.builtbroken.advancedblockplacement.client;

import com.builtbroken.advancedblockplacement.AdvancedBlockPlacement;
import com.builtbroken.advancedblockplacement.PlacementMode;
import com.builtbroken.advancedblockplacement.config.ConfigClient;
import com.builtbroken.advancedblockplacement.network.NetworkHandler;
import com.builtbroken.advancedblockplacement.network.PacketSetMode;
import net.minecraft.client.Minecraft;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

import static com.builtbroken.advancedblockplacement.client.ClientProxy.placement_toggle;

/**
 * Created by Dark(DarkGuardsman, Robert) on 8/12/2019.
 */
@Mod.EventBusSubscriber(modid = AdvancedBlockPlacement.MODID, value = Side.CLIENT)
public class InputHandler
{
    public static boolean wasDownLastTick = false;
    public static PlacementMode mode = PlacementMode.NORMAL;

    @SubscribeEvent
    public static void input(InputEvent event)
    {
        final PlacementMode prevMode = mode;
        final boolean isDown = Keyboard.isKeyDown(placement_toggle.getKeyCode());

        //Are in toggle mode
        if (ConfigClient.is_keybind_toggle)
        {
            //Only change on key down
            if (!wasDownLastTick && isDown)
            {
                mode = mode.isAdvanced() ? PlacementMode.NORMAL : PlacementMode.ADVANCED;
            }
        }
        else if (isDown ^ wasDownLastTick)
        {
            if (isDown && !wasDownLastTick)
            {
                mode = PlacementMode.ADVANCED;

            }
            else if (!isDown && wasDownLastTick)
            {
                mode = PlacementMode.NORMAL;
            }
        }

        //Sync
        if (mode != prevMode)
        {
            //Audio
            if (mode.isAdvanced())
            {
                Minecraft.getMinecraft().player.playSound(SoundEvents.UI_BUTTON_CLICK, 1F, 1F);
            }
            else
            {
                Minecraft.getMinecraft().player.playSound(SoundEvents.UI_BUTTON_CLICK, 1F, 0.8F);
            }

            //Network
            NetworkHandler.NETWORK_INSTANCE.sendToServer(new PacketSetMode(mode));
        }

        //Remember last position
        wasDownLastTick = isDown;
    }

}
