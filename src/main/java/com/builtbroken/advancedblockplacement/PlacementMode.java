package com.builtbroken.advancedblockplacement;

/**
 * Created by Dark(DarkGuardsman, Robert) on 8/12/2019.
 */
public enum PlacementMode
{
    NORMAL(false),
    ADVANCED(true);

    private boolean isAdvanced;

    private PlacementMode(boolean isAdvanced)
    {
        this.isAdvanced = isAdvanced;
    }

    public boolean isAdvanced()
    {
        return isAdvanced;
    }
}
