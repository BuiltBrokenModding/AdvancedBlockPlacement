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
        boolean isDown = Keyboard.isKeyDown(placement_toggle.getKeyCode());
        if (ConfigClient.is_keybind_toggle)
        {
            if (!wasDownLastTick && isDown)
            {
                PlacementMode inverse = mode.isAdvanced() ? PlacementMode.NORMAL : PlacementMode.ADVANCED;
                mode = inverse;
                NetworkHandler.NETWORK_INSTANCE.sendToServer(new PacketSetMode(inverse));
                Minecraft.getMinecraft().player.playSound(SoundEvents.UI_BUTTON_CLICK, 1F, 1F);
            }

        }
        else if (isDown ^ wasDownLastTick)
        {
            if (isDown && !wasDownLastTick)
            {
                mode = PlacementMode.ADVANCED;
                NetworkHandler.NETWORK_INSTANCE.sendToServer(new PacketSetMode(PlacementMode.ADVANCED));
                Minecraft.getMinecraft().player.playSound(SoundEvents.UI_BUTTON_CLICK, 1F, 1F);
            }
            else if (!isDown && wasDownLastTick)
            {
                mode = PlacementMode.NORMAL;
                NetworkHandler.NETWORK_INSTANCE.sendToServer(new PacketSetMode(PlacementMode.NORMAL));
                Minecraft.getMinecraft().player.playSound(SoundEvents.UI_BUTTON_CLICK, 1F, 0.8F);
            }
        }
        wasDownLastTick = isDown;
    }
}
