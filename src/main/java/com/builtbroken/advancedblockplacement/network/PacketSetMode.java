package com.builtbroken.advancedblockplacement.network;

import com.builtbroken.advancedblockplacement.PlacementMode;
import com.builtbroken.advancedblockplacement.client.InputHandler;
import com.builtbroken.advancedblockplacement.logic.PlacementHandler;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSetMode implements IMessage
{

    public PlacementMode mode = PlacementMode.NORMAL;

    public PacketSetMode()
    {
    }

    public PacketSetMode(PlacementMode mode)
    {
        this.mode = mode;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        boolean advanced = buf.readBoolean();
        mode = advanced ? PlacementMode.ADVANCED : PlacementMode.NORMAL;
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeBoolean(mode.isAdvanced());
    }

    public static class ClientHandler implements IMessageHandler<PacketSetMode, IMessage>
    {

        @Override
        public IMessage onMessage(PacketSetMode message, MessageContext ctx)
        {
            InputHandler.mode = message.mode;
            return null;
        }

    }

    public static class ServerHandler implements IMessageHandler<PacketSetMode, IMessage>
    {

        @Override
        public IMessage onMessage(PacketSetMode message, MessageContext ctx)
        {
            PlacementHandler.ADVANCED_PLACEMENT_MAP.put(ctx.getServerHandler().player.getGameProfile().getId(), message.mode);
            return null;
        }

    }

}
