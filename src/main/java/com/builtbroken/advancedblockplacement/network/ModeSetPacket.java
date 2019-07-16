package com.builtbroken.advancedblockplacement.network;

import com.builtbroken.advancedblockplacement.AdvancedBlockPlacement;
import com.builtbroken.advancedblockplacement.AdvancedBlockPlacement.PlacementMode;
import com.builtbroken.advancedblockplacement.AdvancedBlockPlacementClient;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ModeSetPacket implements IMessage {
    
    public PlacementMode mode = PlacementMode.NORMAL;
    
    public ModeSetPacket() {}
    
    public ModeSetPacket(PlacementMode mode) {
        this.mode = mode;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        boolean advanced = buf.readBoolean();
        mode = advanced ? PlacementMode.ADVANCED : PlacementMode.NORMAL;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(mode.isAdvanced());
    }
    
    public static class ClientHandler implements IMessageHandler<ModeSetPacket, IMessage> {

        @Override
        public IMessage onMessage(ModeSetPacket message, MessageContext ctx) {
            AdvancedBlockPlacementClient.mode = message.mode;
            return null;
        }
        
    }
    
    public static class ServerHandler implements IMessageHandler<ModeSetPacket, IMessage> {

        @Override
        public IMessage onMessage(ModeSetPacket message, MessageContext ctx) {
            AdvancedBlockPlacement.ADVANCED_PLACEMENT_MAP.put(ctx.getServerHandler().player.getGameProfile().getId(), message.mode);
            return null;
        }
        
    }

}
