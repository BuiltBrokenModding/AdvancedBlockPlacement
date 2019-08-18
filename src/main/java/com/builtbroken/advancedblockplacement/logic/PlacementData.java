package com.builtbroken.advancedblockplacement.logic;

import com.builtbroken.advancedblockplacement.client.FunctionRenderer;
import net.minecraft.block.Block;

/**
 * Created by Dark(DarkGuardsman, Robert) on 8/18/2019.
 */
public class PlacementData
{
    /** Block */
    public final Block block;

    /** Should an outline be rendered to should the bounds of the placement */
    public boolean renderOutline = false;

    /** Renderer to use for showing the block before placement */
    public FunctionRenderer renderer = null;

    public PlacementData(Block block)
    {
        this.block = block;
    }
}
