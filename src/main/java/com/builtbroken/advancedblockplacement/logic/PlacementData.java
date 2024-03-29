package com.builtbroken.advancedblockplacement.logic;

import com.builtbroken.advancedblockplacement.client.FunctionRenderer;
import net.minecraft.item.Item;

/**
 * Created by Dark(DarkGuardsman, Robert) on 8/18/2019.
 */
public class PlacementData
{
    /** Block */
    public final Item item;

    /** Should an outline be rendered to should the bounds of the placement */
    public boolean renderOutline = false;

    /** Renderer to use for showing the block before placement */
    public FunctionRenderer renderer = null;

    public FunctionPlacement placement = null;

    public PlacementData(Item item)
    {
        this.item = item;
    }

    public PlacementData setRenderer(FunctionRenderer func)
    {
        this.renderer = func;
        return this;
    }

    public PlacementData setPlacement(FunctionPlacement func)
    {
        this.placement = func;
        return this;
    }
}
