package com.builtbroken.advancedblockplacement.fakeworld;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

import javax.annotation.Nullable;
import java.util.HashMap;

/**
 * Created by Cow Pi on 8/24/2015.
 */
public class AbstractFakeWorld extends World
{
    public World actualWorld;

    public HashMap<BlockPos, IBlockState> fakeBlockSet = new HashMap();
    public HashMap<BlockPos, TileEntity> fakeTileSet = new HashMap();

    public AbstractFakeWorld(ISaveHandler saveHandler, WorldInfo worldInfo, WorldProvider worldProvider, Profiler profiler, boolean client)
    {
        super(saveHandler, worldInfo, worldProvider, profiler, client);
        worldProvider.setWorld(this);
        chunkProvider = new ChunkProviderServer(this, this.saveHandler.getChunkLoader(this.provider), provider.createChunkGenerator());
    }

    @Override
    public int getLight(BlockPos pos, boolean checkNeighbors)
    {
        return actualWorld.getLight(pos, checkNeighbors);
    }

    @Override
    public Biome getBiome(final BlockPos pos)
    {
        return actualWorld.getBiome(pos);
    }

    @Override
    public Biome getBiomeForCoordsBody(final BlockPos pos)
    {
        return actualWorld.getBiomeForCoordsBody(pos);
    }

    @Override
    public boolean setBlockState(BlockPos pos, IBlockState block, int flags)
    {
        if (block == null)
        {
            fakeBlockSet.remove(pos);
        }
        else
        {
            fakeBlockSet.put(pos, block);
        }
        return true;
    }

    @Override
    public void setTileEntity(BlockPos pos, @Nullable TileEntity tileEntityIn)
    {
        if (tileEntityIn == null)
        {
            fakeTileSet.remove(pos);
        }
        else
        {
            fakeTileSet.put(pos, tileEntityIn);
        }
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos)
    {
        if (fakeTileSet.containsKey(pos))
        {
            return fakeTileSet.get(pos);
        }
        return actualWorld.getTileEntity(pos);
    }

    @Override
    public IBlockState getBlockState(BlockPos pos)
    {
        if (fakeBlockSet.containsKey(pos))
        {
            return fakeBlockSet.get(pos);
        }
        return actualWorld.getBlockState(pos);
    }

    @Override
    protected IChunkProvider createChunkProvider()
    {
        IChunkProvider provider = new ChunkProviderEmpty(this);
        this.chunkProvider = provider;
        return provider;
    }

    @Override
    public Entity getEntityByID(int p_73045_1_)
    {
        return actualWorld.getEntityByID(p_73045_1_);
    }

    @Override
    protected boolean isChunkLoaded(int x, int z, boolean allowEmpty)
    {
        return false;
    }
}
