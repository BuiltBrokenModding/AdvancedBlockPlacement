package com.builtbroken.advancedblockplacement.client;

import com.builtbroken.advancedblockplacement.AdvancedBlockPlacement;
import com.builtbroken.advancedblockplacement.config.ConfigClient;
import com.builtbroken.advancedblockplacement.config.ConfigMain;
import com.builtbroken.advancedblockplacement.fakeworld.FakeWorld;
import com.builtbroken.advancedblockplacement.logic.PlacementHandler;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
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

            //Get position
            final BlockPos targetPos = event.getTarget().getBlockPos();

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
                    if (item instanceof ItemBlock)
                    {
                        final ItemBlock itemblock = (ItemBlock) item;
                        if (ConfigMain.isAffected(itemblock.getBlock()))
                        {
                            final IBlockState defState = itemblock.getBlock().getDefaultState();
                            try
                            {
                                //Get raytrace
                                final Vec3d hit = event.getTarget().hitVec;

                                //Get state
                                IBlockState blockState = defState;
                                if (defState.getPropertyKeys().contains(BlockDirectional.FACING) || defState.getPropertyKeys().contains(BlockHorizontal.FACING))
                                {
                                    blockState = PlacementHandler.getNewState(defState, event.getTarget().sideHit, (float) hit.x, (float) hit.y, (float) hit.z);
                                }

                                //Get render offset
                                final double renderX = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
                                final double renderY = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
                                final double renderZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();

                                boolean hasRenderedArrow = false;

                                //Update world info
                                fakeWorld.actualWorld = world;
                                fakeWorld.setBlockState(pos, blockState);

                                //Render arrow
                                if (ConfigClient.always_display_arrow)
                                {
                                    renderArrow(-renderX + pos.getX(), -renderY + pos.getY(), -renderZ + pos.getZ(), getDirection(blockState));
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
                                        renderArrow(-renderX + pos.getX(), -renderY + pos.getY(), -renderZ + pos.getZ(), getDirection(blockState));
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
                                AdvancedBlockPlacement.logger.error("Problem rendering advanced placement for " + itemblock.getBlock().getRegistryName(), e);
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
