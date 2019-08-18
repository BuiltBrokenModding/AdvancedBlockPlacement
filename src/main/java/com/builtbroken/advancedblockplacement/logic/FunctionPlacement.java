package com.builtbroken.advancedblockplacement.logic;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Created by Dark(DarkGuardsman, Robert) on 8/18/2019.
 */
@FunctionalInterface
public interface FunctionPlacement
{
    IBlockState getExpectedPlacement(IBlockAccess world, BlockPos pos, EnumFacing placeHeading, EnumFacing placeSide);
}
