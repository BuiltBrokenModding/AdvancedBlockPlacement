package com.builtbroken.advancedblockplacement.logic;

import com.builtbroken.advancedblockplacement.AdvancedBlockPlacement;
import com.builtbroken.advancedblockplacement.PlacementMode;
import com.builtbroken.advancedblockplacement.config.ConfigMain;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Dark(DarkGuardsman, Robert) on 8/12/2019.
 */
@Mod.EventBusSubscriber(modid = AdvancedBlockPlacement.MODID)
public class PlacementHandler
{

    public static final float DETECTION_EDGE_SIZE = 0.3f; //TODO make client-config

    public static final Map<UUID, PlacementMode> ADVANCED_PLACEMENT_MAP = new HashMap<UUID, PlacementMode>();
    public static final Map<UUID, PlayerInteractEvent.RightClickBlock> LAST_RIGHTCLICK_EVENT = new HashMap<UUID, PlayerInteractEvent.RightClickBlock>();
    public static final Map<Integer, Map<BlockPos, IBlockState>> FIX_NEXT_TICK = new HashMap<Integer, Map<BlockPos, IBlockState>>();

    public static final Map<Item, PlacementData> placementData = new HashMap();

    static
    {
        addCustomPlacementData(new PlacementData(Items.REDSTONE)
                .setPlacement((world, pos, heading, side) -> Blocks.REDSTONE_WIRE.getActualState(Blocks.REDSTONE_WIRE.getDefaultState(), world, pos))
        );
    }

    public static void addCustomPlacementData(PlacementData data)
    {
        placementData.put(data.item, data);
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event)
    {
        int dim = event.world.provider.getDimension();
        if (FIX_NEXT_TICK.containsKey(dim))
        {
            if (FIX_NEXT_TICK.get(dim).size() > 0)
            {
                for (BlockPos pos : FIX_NEXT_TICK.get(dim).keySet())
                {
                    event.world.setBlockState(pos, FIX_NEXT_TICK.get(dim).get(pos));
                }
                FIX_NEXT_TICK.get(dim).clear();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event)
    {
        if (event.getEntity() instanceof EntityPlayer)
        {
            final EntityPlayer player = (EntityPlayer) event.getEntity();
            if (ADVANCED_PLACEMENT_MAP.getOrDefault(player.getGameProfile().getId(), PlacementMode.NORMAL).isAdvanced())
            {
                final PlayerInteractEvent.RightClickBlock rightClick = LAST_RIGHTCLICK_EVENT.get(player.getGameProfile().getId());
                if (rightClick != null && rightClick.getFace() != null && rightClick.getHitVec() != null)
                {
                    //Can we handle the block placement
                    if (ConfigMain.isAffected(event.getPlacedBlock().getBlock()))
                    {
                        //Ignore chest placement
                        if (event.getPlacedBlock().getBlock() == Blocks.CHEST
                                && isAdjacentBlock(Blocks.CHEST, event.getWorld(), event.getPos()))
                        {
                            return;
                        }
                        if (event.getPlacedBlock().getBlock() == Blocks.TRAPPED_CHEST
                                && isAdjacentBlock(Blocks.TRAPPED_CHEST, event.getWorld(), event.getPos()))
                        {
                            return;
                        }

                        //Get placement
                        final IBlockState newState = getNewState(event.getPlacedBlock(), rightClick.getFace(),
                                (float) rightClick.getHitVec().x, (float) rightClick.getHitVec().y, (float) rightClick.getHitVec().z);
                        if (newState != null)
                        {
                            int dim = event.getWorld().provider.getDimension();
                            FIX_NEXT_TICK.putIfAbsent(dim, new HashMap<BlockPos, IBlockState>());
                            FIX_NEXT_TICK.get(dim).put(event.getPos(), newState); // fire next tick
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlaceBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getEntity() instanceof EntityPlayer)
        {
            EntityPlayer player = (EntityPlayer) event.getEntity();
            if (ADVANCED_PLACEMENT_MAP.getOrDefault(player.getGameProfile().getId(), PlacementMode.NORMAL).isAdvanced() && event.getUseItem() != Event.Result.DENY && event.getItemStack() != null && !event.getItemStack().isEmpty())
            {
                LAST_RIGHTCLICK_EVENT.put(player.getGameProfile().getId(), event);
            }
            else
            {
                LAST_RIGHTCLICK_EVENT.put(player.getGameProfile().getId(), null);
            }
        }
    }

    public static IBlockState getNewState(IBlockState state, EnumFacing hitFace, float hitX, float hitY, float hitZ)
    {
        EnumFacing newDirection = getPlacement(hitFace, hitX, hitY, hitZ);
        IBlockState newState = null;
        if (state.getPropertyKeys().contains(BlockDirectional.FACING))
        {
            newState = state.withProperty(BlockDirectional.FACING, newDirection);
        }
        else if (state.getPropertyKeys().contains(BlockHorizontal.FACING))
        {
            newDirection = newDirection.getAxis() == EnumFacing.Axis.Y ? EnumFacing.NORTH : newDirection;
            newState = state.withProperty(BlockHorizontal.FACING, newDirection);
        }
        return newState;
    }

    public static EnumFacing getPlacement(EnumFacing blockSide, float hitX, float hitY, float hitZ)
    {
        //Changing to be a decimal 5.5 -> 0.5, data passed in is the raytrace
        hitX -= (int) hitX;
        hitY -= (int) hitY;
        hitZ -= (int) hitZ;

        //From negative to positive
        hitX = Math.abs(hitX);
        hitY = Math.abs(hitY);
        hitZ = Math.abs(hitZ);

        EnumFacing placement;

        if (blockSide == EnumFacing.UP || blockSide == EnumFacing.DOWN)
        {
            // WEST
            boolean left = hitX <= DETECTION_EDGE_SIZE;
            // EAST
            boolean right = hitX >= (1 - DETECTION_EDGE_SIZE);
            // NORTH
            boolean up = hitZ <= DETECTION_EDGE_SIZE;
            // SOUTH
            boolean down = hitZ >= (1 - DETECTION_EDGE_SIZE);

            if (!up && !down && (left || right))
            {
                placement = left ? EnumFacing.WEST : EnumFacing.EAST;
            }
            else if (!left && !right && (up || down))
            {
                placement = up ? EnumFacing.NORTH : EnumFacing.SOUTH;
            }
            else if (!left && !right && !up && !down)
            {
                placement = blockSide;
            }
            else
            {
                placement = blockSide.getOpposite();
            }
        }
        else
        {
            boolean z = blockSide.getAxis() == EnumFacing.Axis.Z;
            boolean left = (z ? hitX : hitZ) <= DETECTION_EDGE_SIZE;
            boolean right = (z ? hitX : hitZ) >= (1 - DETECTION_EDGE_SIZE);

            boolean down = hitY <= DETECTION_EDGE_SIZE;
            boolean up = hitY >= (1 - DETECTION_EDGE_SIZE);

            if (!up && !down && (left || right))
            {
                if (z)
                {
                    placement = left ? EnumFacing.WEST : EnumFacing.EAST;
                }
                else
                {
                    placement = left ? EnumFacing.NORTH : EnumFacing.SOUTH;
                }
            }
            else if (!left && !right && (up || down))
            {
                placement = up ? EnumFacing.UP : EnumFacing.DOWN;
            }
            else if (!left && !right && !up && !down)
            {
                placement = blockSide;
            }
            else
            {
                placement = blockSide.getOpposite();
            }
        }
        return placement;
    }

    private static boolean isAdjacentBlock(Block blockType, World worldIn, BlockPos pos)
    {
        for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL)
        {
            if (worldIn.getBlockState(pos.offset(enumfacing)).getBlock() == blockType)
            {
                return true;
            }
        }
        return false;
    }
}
