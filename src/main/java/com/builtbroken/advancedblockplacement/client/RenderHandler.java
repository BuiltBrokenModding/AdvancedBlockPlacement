package com.builtbroken.advancedblockplacement.client;

import com.builtbroken.advancedblockplacement.AdvancedBlockPlacement;
import com.builtbroken.advancedblockplacement.config.ConfigClient;
import com.builtbroken.advancedblockplacement.config.ConfigMain;
import com.builtbroken.advancedblockplacement.fakeworld.FakeWorld;
import com.builtbroken.advancedblockplacement.logic.PlacementHandler;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockHorizontal;
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
            BlockPos pos = event.getTarget().getBlockPos();

            if (!world.isOutsideBuildHeight(pos) && world.isBlockLoaded(pos))
            {
                pos = world.getBlockState(pos).getBlock().isReplaceable(world, pos) ? pos : pos.offset(event.getTarget().sideHit);

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

                                //Render arrow
                                if (itemblock.getBlock().hasTileEntity(blockState) && blockState.getRenderType() == EnumBlockRenderType.ENTITYBLOCK_ANIMATED || ConfigClient.always_display_arrow)
                                {
                                    if (blockState.getRenderType() == EnumBlockRenderType.ENTITYBLOCK_ANIMATED)
                                    {
                                        try
                                        {
                                            final TileEntityRendererDispatcher rendererDispatcher = TileEntityRendererDispatcher.instance;

                                            //Update world info
                                            fakeWorld.actualWorld = world;

                                            //Fake tile entity
                                            final TileEntity tileEntity = itemblock.getBlock().createTileEntity(world, blockState);
                                            tileEntity.setWorld(fakeWorld);
                                            tileEntity.setPos(pos);

                                            //Setup world
                                            fakeWorld.setBlockState(pos, blockState);
                                            fakeWorld.setTileEntity(pos, tileEntity);

                                            //do render
                                            GlStateManager.pushMatrix();
                                            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                                            RenderHelper.disableStandardItemLighting();
                                            rendererDispatcher.render(tileEntity, -renderX + pos.getX(), -renderY + pos.getY(), -renderZ + pos.getZ(), 0);
                                            RenderHelper.enableStandardItemLighting();
                                            GlStateManager.popMatrix();


                                        }
                                        catch (Exception e)
                                        {
                                            AdvancedBlockPlacement.logger.warn("Problem rendering advanced placement for " + itemblock.getBlock().getRegistryName());
                                        }
                                        finally
                                        {
                                            //Clear world data
                                            fakeWorld.setBlockState(pos, null);
                                            fakeWorld.setTileEntity(pos, null);
                                        }
                                    }
                                    //renderArrow(-renderX + pos.getX(), -renderY + pos.getY(), -renderZ + pos.getZ(), getDirection(state));
                                }

                                //Render block preview
                                if (ConfigClient.do_block_preview)
                                {
                                    //renderBlock(world, pos, state, renderX, renderY, renderZ);
                                }
                            }
                            catch (Exception e)
                            {
                                AdvancedBlockPlacement.logger.warn("Problem rendering advanced placement for " + itemblock.getBlock().getRegistryName());
                            }
                        }
                    }
                }
            }
        }
    }

    public static EnumFacing getDirection(IBlockState state)
    {
        EnumFacing newFacing = null;
        if (state.getPropertyKeys().contains(BlockDirectional.FACING))
        {
            newFacing = state.getValue(BlockDirectional.FACING);
        }
        else if (state.getPropertyKeys().contains(BlockHorizontal.FACING))
        {
            newFacing = state.getValue(BlockHorizontal.FACING);
        }
        return newFacing;
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

            //Render block
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buffer = tess.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            dispatcher.renderBlock(state, pos, world, buffer);
            tess.draw();

            //Reset
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
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GlStateManager.depthMask(false);

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

                GlStateManager.depthMask(true);
            }
            GlStateManager.popMatrix();
        }
    }
}
