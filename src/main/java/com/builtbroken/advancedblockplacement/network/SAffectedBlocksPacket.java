package com.builtbroken.advancedblockplacement.network;

import com.builtbroken.advancedblockplacement.AdvancedBlockPlacement;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SAffectedBlocksPacket implements IMessage {
    
    public String[] affectedBlocks;
    public boolean isBlacklist;
    
    public SAffectedBlocksPacket() {}
    
    public SAffectedBlocksPacket(String[] affectedBlocks, boolean isBlacklist) {
        this.affectedBlocks = affectedBlocks;
        this.isBlacklist = isBlacklist;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.isBlacklist = buf.readBoolean();
        int arrLen = buf.readInt();
        String[] affected = new String[arrLen];
        for(int i = 0; i < arrLen; i++) {
            affected[i] = ByteBufUtils.readUTF8String(buf);
        }
        affectedBlocks = affected;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(isBlacklist);
        buf.writeInt(affectedBlocks.length);
        for(String block : affectedBlocks) {
            ByteBufUtils.writeUTF8String(buf, block);
        }
    }
    
    public static class ClientHandler implements IMessageHandler<SAffectedBlocksPacket, IMessage> {

        @Override
        public IMessage onMessage(SAffectedBlocksPacket message, MessageContext ctx) {
            AdvancedBlockPlacement.ServerConfiguration.blocks_affected = message.affectedBlocks;
            AdvancedBlockPlacement.ServerConfiguration.is_blacklist = message.isBlacklist;
            return null;
        }
        
    }

}
