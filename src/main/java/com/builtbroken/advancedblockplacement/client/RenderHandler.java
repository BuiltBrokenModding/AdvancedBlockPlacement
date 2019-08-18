package com.builtbroken.advancedblockplacement.client;

import com.builtbroken.advancedblockplacement.AdvancedBlockPlacement;
import com.builtbroken.advancedblockplacement.config.ConfigClient;
import com.builtbroken.advancedblockplacement.config.ConfigMain;
import com.builtbroken.advancedblockplacement.fakeworld.FakeWorld;
import com.builtbroken.advancedblockplacement.logic.PlacementHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

/**
 * Created by Dark(DarkGuardsman, Robert) on 8/12/2019.
 */
@Mod.EventBusSubscriber(modid = AdvancedBlockPlacement.MODID, value = Side.CLIENT)
public class RenderHandler
{
    private static final FakeWorld fakeWorld = FakeWorld.newWorld("render");

    public static final ResourceLocation ARROW_TEXTURE = new ResourceLocation(AdvancedBlockPlacement.MODID, "textures/arrow.png");

    @SubscribeEvent
    public static void renderTick(DrawBlockHighlightEvent event)
    {
        if (ConfigClient.do_special_rendering && InputHandler.mode.isAdvanced())
        {
            final EntityPlayer player = Minecraft.getMinecraft().player;
            final World world = Minecraft.getMinecraft().player.world;
            final Vec3d rayTraceHit = event.getTarget().hitVec;
            final EnumFacing sideHit = event.getTarget().sideHit;

            //Get hit vector for side, get decimal and convert to positive
            final double hitX = Math.abs(rayTraceHit.x - Math.floor(rayTraceHit.x));
            final double hitY = Math.abs(rayTraceHit.y - Math.floor(rayTraceHit.y));
            final double hitZ = Math.abs(rayTraceHit.z - Math.floor(rayTraceHit.z));

            //Update world info
            fakeWorld.actualWorld = world;

            //Get position
            final BlockPos targetPos = event.getTarget().getBlockPos();

            if (targetPos == null)
            {
                return;
            }

            if (!world.isOutsideBuildHeight(targetPos) && world.isBlockLoaded(targetPos))
            {
                final BlockPos pos = world.getBlockState(targetPos).getBlock().isReplaceable(world, targetPos)
                        ? targetPos
                        : targetPos.offset(event.getTarget().sideHit);

                //Only render on solid blocks
                if (!player.world.isAirBlock(event.getTarget().getBlockPos()))
                {
                    final ItemStack stack = event.getPlayer().getHeldItem(EnumHand.MAIN_HAND);
                    final Item item = stack.getItem();

                    //Get block state for placement
                    IBlockState blockState = null;
                    if (PlacementHandler.placementData.containsKey(item))
                    {
                        blockState = PlacementHandler.placementData.get(item).placement.getExpectedPlacement(fakeWorld, pos, sideHit, sideHit);
                    }
                    else if (item instanceof ItemBlock)
                    {
                        final ItemBlock itemBlock = ((ItemBlock) item);
                        final Block block = itemBlock.getBlock();
                        final int meta = itemBlock.getMetadata(stack);

                        if (!itemBlock.canPlaceBlockOnSide(fakeWorld, pos, sideHit, player, stack)
                                || !block.canPlaceBlockAt(world, pos))
                        {
                            return;
                        }

                        EntityZombie zombie = new EntityZombie(fakeWorld); //TODO switch to fake player
                        zombie.setPosition(pos.getX(), pos.getY(), pos.getZ()); //TODO offset to be in front of the expected side to fake rotation

                        //Get actual placement
                        blockState = block.getStateForPlacement(fakeWorld, pos, sideHit, (float) hitX, (float) hitY, (float) hitZ, meta, zombie, EnumHand.MAIN_HAND);

                        //Get rotation
                        if (blockState.getPropertyKeys().contains(BlockDirectional.FACING)
                                || blockState.getPropertyKeys().contains(BlockHorizontal.FACING))
                        {
                            blockState = PlacementHandler.getNewState(blockState, event.getTarget().sideHit, (float) hitX, (float) hitY, (float) hitZ);
                        }
                    }


                    //Reject null and config check
                    if (blockState == null || !ConfigMain.isAffected(blockState.getBlock()))
                    {
                        return;
                    }

                    //Make sure the block can be placed
                    if (!blockState.getBlock().canPlaceBlockOnSide(world, pos, sideHit))
                    {
                        return;
                    }

                    //Set block into fake world
                    fakeWorld.setBlockState(pos, blockState);

                    //Get render offset
                    final double renderX = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
                    final double renderY = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
                    final double renderZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();

                    //BlockPos related to render space
                    final double xx = -renderX + pos.getX();
                    final double yy = -renderY + pos.getY();
                    final double zz = -renderZ + pos.getZ();

                    try
                    {
                        boolean hasRenderedArrow = false;

                        //Render selection cubes
                        final EnumFacing selectedSide = PlacementHandler.getPlacement(sideHit, (float) hitX, (float) hitY, (float) hitZ);
                        renderSelectedSide(sideHit, selectedSide, xx, yy, zz);

                        //Render arrow
                        if (ConfigClient.always_display_arrow)
                        {
                            renderArrow(xx, yy, zz, getDirection(blockState));
                        }
                        //Render TileEntity
                        else if (renderTileEntity(blockState))
                        {
                            boolean rendered = false;
                            if (blockState.getRenderType() == EnumBlockRenderType.ENTITYBLOCK_ANIMATED)
                            {
                                rendered = renderTile(world, pos, blockState, renderX, renderY, renderZ);
                            }
                            if (!rendered)
                            {
                                renderArrow(xx, yy, zz, getDirection(blockState));
                                hasRenderedArrow = true;
                            }
                            else
                            {
                                drawSelectionBox(fakeWorld, pos, renderX, renderY, renderZ);
                            }
                        }
                        //Render block preview
                        else if (ConfigClient.do_block_preview)
                        {
                            renderBlock(world, pos, blockState, renderX, renderY, renderZ);
                        }

                        //Render facing arrow to help with visualization on flat looking blocks
                        if (!hasRenderedArrow)
                        {
                            final EnumFacing facing = getDirection(blockState);
                            if (facing != null)
                            {
                                double x = -renderX + pos.getX() + facing.getXOffset();
                                double y = -renderY + pos.getY() + facing.getYOffset();
                                double z = -renderZ + pos.getZ() + facing.getZOffset();
                                renderArrow(x, y, z, facing);
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        AdvancedBlockPlacement.logger.error("Problem rendering advanced placement for " + item, e);
                    }
                    finally
                    {
                        //Clear fake world settings
                        fakeWorld.setBlockState(pos, null);
                    }
                }
            }
        }
    }

    public static void drawSelectionBox(World world, BlockPos pos, double renderX, double renderY, double renderZ)
    {
        final Minecraft mc = Minecraft.getMinecraft();
        final IBlockState iblockstate = world.getBlockState(pos);
        if (iblockstate.getMaterial() != Material.AIR && world.getWorldBorder().contains(pos))
        {
            //Setup
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.glLineWidth(2.0F);
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);

            //Get selection bounds
            AxisAlignedBB bb = iblockstate.getSelectedBoundingBox(world, pos)
                    .grow(0.0020000000949949026D)
                    .offset(-renderX, -renderY, -renderZ);

            //Render
            mc.renderGlobal.drawSelectionBoundingBox(bb, 0.0F, 0.0F, 0.0F, 0.4F);


            //Cleanup
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }
    }

    public static void renderSelectedSide(EnumFacing blockSide, EnumFacing selection, double xx, double yy, double zz)
    {
        final Minecraft mc = Minecraft.getMinecraft();
        //Setup
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        final float size = PlacementHandler.DETECTION_EDGE_SIZE;


        if (blockSide == EnumFacing.UP)
        {
            final double yh = yy + 0.05;
            final double northMin = size;
            final double northMax = 0;

            final double southMin = (1 - size);
            final double southMax = 1;

            final double westMin = size;
            final double westMax = 0;

            final double eastMin = (1 - size);
            final double eastMax = 1;

            final int grey_r = 145;
            final int grey_g = 145;
            final int grey_b = 145;

            final int selected_r = 255;
            final int selected_g = 57;
            final int selected_b = 11;


            //CORNERS
            //Render corner NW
            RenderGlobal.renderFilledBox(
                    xx + westMax, yy, zz + northMax,
                    xx + westMin, yh, zz + northMin,
                    selection == EnumFacing.DOWN ? selected_r : grey_r,
                    selection == EnumFacing.DOWN ? selected_g : grey_g,
                    selection == EnumFacing.DOWN ? selected_b : grey_b, 1);

            //Render corner NE
            RenderGlobal.renderFilledBox(
                    xx + eastMin, yy, zz + northMax,
                    xx + eastMax, yh, zz + northMin,
                    selection == EnumFacing.DOWN ? selected_r : grey_r,
                    selection == EnumFacing.DOWN ? selected_g : grey_g,
                    selection == EnumFacing.DOWN ? selected_b : grey_b, 1);

            //Render corner SW
            RenderGlobal.renderFilledBox(
                    xx + westMax, yy, zz + southMin,
                    xx + westMin, yh, zz + southMax,
                    selection == EnumFacing.DOWN ? selected_r : grey_r,
                    selection == EnumFacing.DOWN ? selected_g : grey_g,
                    selection == EnumFacing.DOWN ? selected_b : grey_b, 1);

            //Render corner SE
            RenderGlobal.renderFilledBox(
                    xx + eastMin, yy, zz + southMin,
                    xx + eastMax, yh, zz + southMax,
                    selection == EnumFacing.DOWN ? selected_r : grey_r,
                    selection == EnumFacing.DOWN ? selected_g : grey_g,
                    selection == EnumFacing.DOWN ? selected_b : grey_b, 1);

            //SIDES
            //Render side N
            RenderGlobal.renderFilledBox(
                    xx + westMin, yy, zz + northMax,
                    xx + eastMin, yh, zz + northMin,
                    selection == EnumFacing.NORTH ? selected_r : grey_r,
                    selection == EnumFacing.NORTH ? selected_g : grey_g,
                    selection == EnumFacing.NORTH ? selected_b : grey_b, 1);

            //Render side S
            RenderGlobal.renderFilledBox(
                    xx + westMin, yy, zz + southMin,
                    xx + eastMin, yh, zz + southMax,
                    selection == EnumFacing.SOUTH ? selected_r : grey_r,
                    selection == EnumFacing.SOUTH ? selected_g : grey_g,
                    selection == EnumFacing.SOUTH ? selected_b : grey_b, 1);

            //Render side W
            RenderGlobal.renderFilledBox(
                    xx + westMax, yy, zz + northMin,
                    xx + westMin, yh, zz + southMin,
                    selection == EnumFacing.WEST ? selected_r : grey_r,
                    selection == EnumFacing.WEST ? selected_g : grey_g,
                    selection == EnumFacing.WEST ? selected_b : grey_b, 1);

            //Render side E
            RenderGlobal.renderFilledBox(
                    xx + eastMin, yy, zz + northMin,
                    xx + eastMax, yh, zz + southMin,
                    selection == EnumFacing.EAST ? selected_r : grey_r,
                    selection == EnumFacing.EAST ? selected_g : grey_g,
                    selection == EnumFacing.EAST ? selected_b : grey_b, 1);

            //CENTER
            RenderGlobal.renderFilledBox(
                    xx + westMin, yy, zz + northMin,
                    xx + eastMin, yh, zz + southMin,
                    selection == EnumFacing.UP ? selected_r : grey_r,
                    selection == EnumFacing.UP ? selected_g : grey_g,
                    selection == EnumFacing.UP ? selected_b : grey_b, 1);
        }


        //Cleanup
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static boolean renderTileEntity(IBlockState blockState)
    {
        return blockState.getBlock().hasTileEntity(blockState)
                && blockState.getRenderType() == EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    public static EnumFacing getDirection(IBlockState state)
    {
        if (state.getPropertyKeys().contains(BlockDirectional.FACING))
        {
            return state.getValue(BlockDirectional.FACING);
        }
        else if (state.getPropertyKeys().contains(BlockHorizontal.FACING))
        {
            return state.getValue(BlockHorizontal.FACING);
        }
        return null;
    }

    public static boolean renderTile(World world, BlockPos pos, IBlockState blockState, double renderX, double renderY, double renderZ)
    {
        GlStateManager.pushMatrix();
        try
        {
            final TileEntityRendererDispatcher rendererDispatcher = TileEntityRendererDispatcher.instance;


            //Fake tile entity
            final TileEntity tileEntity = blockState.getBlock().createTileEntity(world, blockState);
            tileEntity.setWorld(fakeWorld);
            tileEntity.setPos(pos);

            //Setup world
            fakeWorld.setTileEntity(pos, tileEntity);

            //Setup
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            //RenderHelper.disableStandardItemLighting();
            //GlStateManager.enableBlend();
            //GlStateManager.enableAlpha();
            //GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            //GlStateManager.depthMask(false);

            //do render
            rendererDispatcher.render(tileEntity, -renderX + pos.getX(), -renderY + pos.getY(), -renderZ + pos.getZ(), 0);

            //Clean up
            //GlStateManager.depthMask(true);
            //RenderHelper.enableStandardItemLighting();
            //GlStateManager.disableAlpha();
            //GlStateManager.disableBlend();
        }
        catch (Exception e)
        {
            AdvancedBlockPlacement.logger.warn("Problem rendering advanced placement for " + blockState);
        }
        finally
        {
            //Clear world data
            fakeWorld.setTileEntity(pos, null);
        }
        GlStateManager.popMatrix();
        return true;
    }

    public static void renderBlock(World world, BlockPos pos, IBlockState state, double renderX, double renderY, double renderZ)
    {
        GlStateManager.pushMatrix();
        {
            final BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();

            //Bind texture
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

            //Position and scale
            GlStateManager.scale(0.95F, 0.95F, 0.95F);
            GlStateManager.translate(-renderX, -renderY + 0.08, -renderZ);

            //Alpha
            RenderHelper.disableStandardItemLighting();
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(false);

            //Render block
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buffer = tess.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            dispatcher.renderBlock(state, pos, world, buffer);
            tess.draw();

            //Reset
            GlStateManager.depthMask(true);
            RenderHelper.enableStandardItemLighting();
            GlStateManager.disableAlpha();
            GlStateManager.disableBlend();
        }
        GlStateManager.popMatrix();
    }

    public static void renderArrow(double x, double y, double z, EnumFacing newFacing)
    {
        if (newFacing != null)
        {
            GlStateManager.pushMatrix();
            {
                //Setup
                GlStateManager.enableBlend();
                GlStateManager.enableAlpha();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                //GlStateManager.depthMask(false);

                //Offset
                GlStateManager.translate(x, y, z);

                //Offset for rotation
                GlStateManager.translate(0.5F, 0F, 0.5F);

                //Rotate
                GlStateManager.rotate(newFacing.getAxis() == EnumFacing.Axis.Z ? newFacing.getOpposite().getHorizontalAngle() : newFacing.getHorizontalAngle(), 0F, 1F, 0F);
                if (newFacing.getAxis() == EnumFacing.Axis.Y)
                {
                    float mult = newFacing == EnumFacing.UP ? 1 : -1;
                    GlStateManager.translate(0, 0.5F, -mult * 0.5F);
                    GlStateManager.rotate(mult * 90F, 1F, 0F, 0F);
                }

                //Return to position
                GlStateManager.translate(-0.5F, 0.5F, -0.5F);

                //Rotate again
                GlStateManager.rotate(90F, 1F, 0F, 0F);


                //GlStateManager.rotate(newFacing.getAxis() == Axis.Y ? newFacing == EnumFacing.UP ? 90F : -90F : 0F, 0F, 1F, 0F);

                //Render
                final Tessellator t = Tessellator.getInstance();
                BufferBuilder r = t.getBuffer();

                r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
                Minecraft.getMinecraft().getTextureManager().bindTexture(ARROW_TEXTURE);

                r.pos(0, 1, 0).tex(0, 1).endVertex();
                r.pos(1, 1, 0).tex(1, 1).endVertex();
                r.pos(1, 0, 0).tex(1, 0).endVertex();
                r.pos(0, 0, 0).tex(0, 0).endVertex();

                t.draw();

                //Cleanup
                GlStateManager.disableAlpha();
                GlStateManager.disableBlend();
                //GlStateManager.depthMask(true);
            }
            GlStateManager.popMatrix();
        }
    }
}
