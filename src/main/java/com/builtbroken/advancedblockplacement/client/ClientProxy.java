package com.builtbroken.advancedblockplacement.client;

import com.builtbroken.advancedblockplacement.AdvancedBlockPlacement;
import com.builtbroken.advancedblockplacement.ISidedProxy;
import com.builtbroken.advancedblockplacement.config.ConfigClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = AdvancedBlockPlacement.MODID, value = Side.CLIENT)
public class ClientProxy implements ISidedProxy
{
    public static KeyBinding placement_toggle = null;

    @Override
    public void init(FMLInitializationEvent event)
    {
        placement_toggle = new KeyBinding("key.advancedblockplacement.switch", ConfigClient.default_keybind, "key.advancedblockplacement.category");
        ClientRegistry.registerKeyBinding(placement_toggle);
    }
}
