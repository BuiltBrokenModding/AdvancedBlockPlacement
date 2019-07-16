package com.builtbroken.advancedblockplacement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.builtbroken.advancedblockplacement.network.ModeSetPacket;
import com.builtbroken.advancedblockplacement.network.SAffectedBlocksPacket;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import scala.actors.threadpool.Arrays;

@Mod.EventBusSubscriber(modid = AdvancedBlockPlacement.MODID)
@Mod(modid = AdvancedBlockPlacement.MODID, name = AdvancedBlockPlacement.NAME, version = AdvancedBlockPlacement.VERSION, acceptableRemoteVersions = "*")
public class AdvancedBlockPlacement {

    // Reference Fields

    public static final String MODID = "advancedblockplacement";
    public static final String NAME = "Advanced Block Placement";
    public static final String MAJOR = "@MAJOR@";
    public static final String MINOR = "@MINOR@";
    public static final String REVIS = "@REVIS@";
    public static final String VERSION = MAJOR + "." + MINOR + "." + REVIS;

    // Functional Fields

    public static final Map<UUID, PlacementMode> ADVANCED_PLACEMENT_MAP = new HashMap<UUID, PlacementMode>();
    public static final Map<UUID, PlayerInteractEvent.RightClickBlock> LAST_RIGHTCLICK_EVENT = new HashMap<UUID, PlayerInteractEvent.RightClickBlock>();
    public static final Map<Integer, Map<BlockPos, IBlockState>> FIX_NEXT_TICK = new HashMap<Integer, Map<BlockPos, IBlockState>>();

    @SidedProxy(clientSide = "com.builtbroken.advancedblockplacement.AdvancedBlockPlacementClient", serverSide = "com.builtbroken.advancedblockplacement.AdvancedBlockPlacement$ISidedProxy$DummyProxy")
    public static ISidedProxy proxy;

    public static final SimpleNetworkWrapper NETWORK_INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
    public static int packets = 0;

    // Objects

    public static enum PlacementMode {
        NORMAL(false),
        ADVANCED(true);

        private boolean isAdvanced;

        private PlacementMode(boolean isAdvanced) {
            this.isAdvanced = isAdvanced;
        }

        public boolean isAdvanced() {
            return isAdvanced;
        }
    }

    // FML Events

    @EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        ADVANCED_PLACEMENT_MAP.clear();
        LAST_RIGHTCLICK_EVENT.clear();
        event.registerServerCommand(new CommandAPToggle());
    }

    @EventHandler
    public static void serverStop(FMLServerStoppingEvent event) {
        ADVANCED_PLACEMENT_MAP.clear();
        LAST_RIGHTCLICK_EVENT.clear();
    }

    @EventHandler
    public static void preinit(FMLPreInitializationEvent event) {
        NETWORK_INSTANCE.registerMessage(SAffectedBlocksPacket.ClientHandler.class, SAffectedBlocksPacket.class, packets++, Side.CLIENT);
        NETWORK_INSTANCE.registerMessage(ModeSetPacket.ClientHandler.class, ModeSetPacket.class, packets++, Side.CLIENT);
        NETWORK_INSTANCE.registerMessage(ModeSetPacket.ServerHandler.class, ModeSetPacket.class, packets++, Side.SERVER);
    }

    @EventHandler
    public static void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    // Forge Events

    @SubscribeEvent
    public static void onConnect(PlayerLoggedInEvent event) {
        if(event.player instanceof EntityPlayerMP) {
            NETWORK_INSTANCE.sendTo(new SAffectedBlocksPacket(ServerConfiguration.blocks_affected, ServerConfiguration.is_blacklist), (EntityPlayerMP) event.player);
            NETWORK_INSTANCE.sendTo(new ModeSetPacket(ADVANCED_PLACEMENT_MAP.getOrDefault(event.player.getGameProfile().getId(), PlacementMode.NORMAL)), (EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public static void onDisconnect(PlayerLoggedOutEvent event) {
        ADVANCED_PLACEMENT_MAP.remove(event.player.getGameProfile().getId());
        LAST_RIGHTCLICK_EVENT.remove(event.player.getGameProfile().getId());
    }

    @SubscribeEvent
    public static void onWorldTick(WorldTickEvent event) {
        int dim = event.world.provider.getDimension();
        if(FIX_NEXT_TICK.containsKey(dim)) {
            if(FIX_NEXT_TICK.get(dim).size() > 0) {
                for(BlockPos pos : FIX_NEXT_TICK.get(dim).keySet()) {
                    event.world.setBlockState(pos, FIX_NEXT_TICK.get(dim).get(pos));
                }
                FIX_NEXT_TICK.get(dim).clear();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if(event.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntity();
            if(ADVANCED_PLACEMENT_MAP.getOrDefault(player.getGameProfile().getId(), PlacementMode.NORMAL).isAdvanced() || true) {
                PlayerInteractEvent.RightClickBlock rightClick = LAST_RIGHTCLICK_EVENT.get(player.getGameProfile().getId());
                if(rightClick != null && rightClick.getFace() != null && rightClick.getHitVec() != null) {
                    if(isAffected(event.getPlacedBlock().getBlock())) {
                        if(event.getPlacedBlock().getBlock() == Blocks.CHEST && isAdjacentBlock(Blocks.CHEST, event.getWorld(), event.getPos())) return;
                        if(event.getPlacedBlock().getBlock() == Blocks.TRAPPED_CHEST && isAdjacentBlock(Blocks.TRAPPED_CHEST, event.getWorld(), event.getPos())) return;
                        IBlockState newState = getNewState(event.getPlacedBlock(), rightClick.getFace(), (float) rightClick.getHitVec().x, (float) rightClick.getHitVec().y, (float) rightClick.getHitVec().z);
                        if(newState != null) {
                            int dim = event.getWorld().provider.getDimension();
                            FIX_NEXT_TICK.putIfAbsent(dim, new HashMap<BlockPos, IBlockState>());
                            FIX_NEXT_TICK.get(dim).put(event.getPos(), newState); // fire next tick
                        }
                    }
                }
            }
        }
    }

    public static boolean isAffected(Block block) {
        boolean contained = getAffectedIDs().contains(block.getRegistryName().toString());
        return (blacklist() && !contained) || (!blacklist() && contained);
    }

    @SubscribeEvent
    public static void onPlaceBlock(PlayerInteractEvent.RightClickBlock event) {
        if(event.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntity();
            if(ADVANCED_PLACEMENT_MAP.getOrDefault(player.getGameProfile().getId(), PlacementMode.NORMAL).isAdvanced() && event.getUseItem() != Result.DENY && event.getItemStack() != null && !event.getItemStack().isEmpty()) {
                LAST_RIGHTCLICK_EVENT.put(player.getGameProfile().getId(), event);
            } else {
                LAST_RIGHTCLICK_EVENT.put(player.getGameProfile().getId(), null);
            }
        }
    }

    @SubscribeEvent
    public static void onConfigUpdate(ConfigChangedEvent event) {
        if(event.getModID().equals(MODID)) {
            ConfigManager.sync(MODID, Config.Type.INSTANCE);
        }
    }

    // Configuration

    @SuppressWarnings("unchecked")
    public static List<String> getAffectedIDs() {
        return Arrays.asList(ServerConfiguration.blocks_affected);
    }

    public static boolean blacklist() {
        return ServerConfiguration.is_blacklist;
    }

    @Config(modid = AdvancedBlockPlacement.MODID, name = AdvancedBlockPlacement.MODID + "-server")
    public static class ServerConfiguration {

        @Config.Comment("A list of registry names of blocks to be affected by advanced placement")
        public static String[] blocks_affected = new String[] {};

        @Config.Comment("Setting to true will make all blocks affected by default but disallow those in the affected list")
        public static boolean is_blacklist = false;

    }

    // Utility

    public static IBlockState getNewState(IBlockState state, EnumFacing hitFace, float hitX, float hitY, float hitZ) {
        EnumFacing newDirection = getPlacement(hitFace, hitX, hitY, hitZ);
        IBlockState newState = null;
        if(state.getPropertyKeys().contains(BlockDirectional.FACING)) {
            newState = state.withProperty(BlockDirectional.FACING, newDirection);
        } else if(state.getPropertyKeys().contains(BlockHorizontal.FACING)) {
            newDirection = newDirection.getAxis() == Axis.Y ? EnumFacing.NORTH : newDirection;
            newState = state.withProperty(BlockHorizontal.FACING, newDirection);
        }
        return newState;
    }

    public static EnumFacing getPlacement(EnumFacing blockSide, float hitX, float hitY, float hitZ) {
        hitX -= (int) hitX;
        hitY -= (int) hitY;
        hitZ -= (int) hitZ;
        hitX = Math.abs(hitX);
        hitY = Math.abs(hitY);
        hitZ = Math.abs(hitZ);
        hitX = 1 - hitX;
        //hitX += 0.5F;
        //hitY += 0.5F;
        //hitZ += 0.5F;
        final float spacing = 0.3f;
        EnumFacing placement;

        if(blockSide == EnumFacing.UP || blockSide == EnumFacing.DOWN) {
            // WEST
            boolean left = hitX <= spacing;
            // EAST
            boolean right = hitX >= (1 - spacing);
            // NORTH
            boolean up = hitZ <= spacing;
            // SOUTH
            boolean down = hitZ >= (1 - spacing);

            if(!up && !down && (left || right)) {
                placement = left ? EnumFacing.WEST : EnumFacing.EAST;
            } else if(!left && !right && (up || down)) {
                placement = up ? EnumFacing.NORTH : EnumFacing.SOUTH;
            } else if(!left && !right && !up && !down) {
                placement = blockSide;
            } else {
                placement = blockSide.getOpposite();
            }
        } else {
            boolean z = blockSide.getAxis() == EnumFacing.Axis.Z;
            boolean left = (z ? hitX : hitZ) <= spacing;
            boolean right = (z ? hitX : hitZ) >= (1 - spacing);

            boolean down = hitY <= spacing;
            boolean up = hitY >= (1 - spacing);

            if(!up && !down && (left || right)) {
                if(z) {
                    placement = left ? EnumFacing.WEST : EnumFacing.EAST;
                } else {
                    placement = left ? EnumFacing.NORTH : EnumFacing.SOUTH;
                }
            } else if(!left && !right && (up || down)) {
                placement = up ? EnumFacing.UP : EnumFacing.DOWN;
            } else if(!left && !right && !up && !down) {
                placement = blockSide;
            } else {
                placement = blockSide.getOpposite();
            }
        }
        return placement;
    }

    private static boolean isAdjacentBlock(Block blockType, World worldIn, BlockPos pos) {
        for(EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL) {
            if(worldIn.getBlockState(pos.offset(enumfacing)).getBlock() == blockType) {
                return true;
            }
        }
        return false;
    }

    public static interface ISidedProxy {
        public void init(FMLInitializationEvent event);

        public static class DummyProxy implements ISidedProxy {
            @Override
            public void init(FMLInitializationEvent event) {}
        }
    }

}