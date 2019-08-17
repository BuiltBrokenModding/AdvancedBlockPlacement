package com.builtbroken.advancedblockplacement.fakeworld;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;

/**
 * Created by Cow Pi on 8/24/2015.
 */
public class FakeChunk extends Chunk
{
    public FakeChunk(World p_i1995_1_, int p_i1995_2_, int p_i1995_3_)
    {
        super(p_i1995_1_, p_i1995_2_, p_i1995_3_);
    }

    public FakeChunk(World p_i45446_1_, ChunkPrimer p_i45446_2_, int p_i45446_3_, int p_i45446_4_)
    {
        super(p_i45446_1_, p_i45446_2_, p_i45446_3_, p_i45446_4_);
    }

    @Override
    public void removeTileEntity(BlockPos pos)
    {

    }
}
