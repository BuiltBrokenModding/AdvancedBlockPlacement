package com.builtbroken.advancedblockplacement.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Created by Dark(DarkGuardsman, Robert) on 8/18/2019.
 */
@FunctionalInterface
public interface FunctionRenderer
{
    /**
     * Called to render a block, tileEntity, or placeholder
     *
     * @param state      - block being placed
     * @param tileEntity - tile that would be placed, faked for rendering
     * @param world      - world to use for data, faked for rendering but wrappers to the real world in some placed
     * @param renderX    - render position
     * @param renderY    - render position
     * @param renderZ    - render position
     * @return true that something rendered, false to render the defaults
     */
    boolean render(@Nonnull IBlockState state, @Nullable TileEntity tileEntity, @Nonnull World world, double renderX, double renderY, double renderZ);
}
