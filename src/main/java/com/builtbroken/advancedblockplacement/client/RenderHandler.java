package com.builtbroken.advancedblockplacement.client;

import com.builtbroken.advancedblockplacement.AdvancedBlockPlacement;
import com.builtbroken.advancedblockplacement.config.ConfigClient;
import com.builtbroken.advancedblockplacement.config.ConfigMain;
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
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
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
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GL11;

/**
 * Created by Dark(DarkGuardsman, Robert) on 8/12/2019.
 */
@Mod.EventBusSubscriber(modid = AdvancedBlockPlacement.MODID, value = Side.CLIENT)
public class RenderHandler
{
    public static final ResourceLocation ARROW_TEXTURE = new ResourceLocation(AdvancedBlockPlacement.MODID, "textures/arrow.png");

    @SubscribeEvent
    public static void renderTick(DrawBlockHighlightEvent event)
    {
        if (ConfigClient.do_special_rendering && InputHandler.mode.isAdvanced())
        {
            EntityPlayer player = Minecraft.getMinecraft().player;
            World world = Minecraft.getMinecraft().player.world;
            BlockPos pos = event.getTarget().getBlockPos();
            pos = world.getBlockState(pos).getBlock().isReplaceable(world, pos) ? pos : pos.offset(event.getTarget().sideHit);
            if (InputHandler.mode.isAdvanced() && !player.world.isAirBlock(event.getTarget().getBlockPos()))
            {
                ItemStack stack = event.getPlayer().getHeldItem(EnumHand.MAIN_HAND);
                Item item = stack.getItem();
                if (item instanceof ItemBlock)
                {
                    ItemBlock itemblock = (ItemBlock) item;
                    if (ConfigMain.isAffected(itemblock.getBlock()))
                    {
                        IBlockState defState = itemblock.getBlock().getDefaultState();
                        try
                        {
                            Vec3d hit = event.getTarget().hitVec;
                            IBlockState state = defState;
                            if (defState.getPropertyKeys().contains(BlockDirectional.FACING) || defState.getPropertyKeys().contains(BlockHorizontal.FACING))
                            {
                                state = PlacementHandler.getNewState(defState, event.getTarget().sideHit, (float) hit.x, (float) hit.y, (float) hit.z);
                            }
                            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
                            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
                            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();
                            if (itemblock.getBlock().hasTileEntity(state) && state.getRenderType() == EnumBlockRenderType.ENTITYBLOCK_ANIMATED || ConfigClient.always_display_arrow)
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
                                if (newFacing != null)
                                {
                                    GlStateManager.pushMatrix();
                                    {
                                        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                                        GlStateManager.depthMask(false);
                                        GlStateManager.translate(-x, -y, -z);
                                        GlStateManager.translate(pos.getX(), pos.getY(), pos.getZ());
                                        GlStateManager.translate(0.5F, 0F, 0.5F);
                                        GlStateManager.rotate(newFacing.getAxis() == EnumFacing.Axis.Z ? newFacing.getOpposite().getHorizontalAngle() : newFacing.getHorizontalAngle(), 0F, 1F, 0F);
                                        if (newFacing.getAxis() == EnumFacing.Axis.Y)
                                        {
                                            float mult = newFacing == EnumFacing.UP ? 1 : -1;
                                            GlStateManager.translate(0, 0.5F, -mult * 0.5F);
                                            GlStateManager.rotate(mult * 90F, 1F, 0F, 0F);
                                        }
                                        GlStateManager.translate(-0.5F, 0.5F, -0.5F);
                                        GlStateManager.rotate(90F, 1F, 0F, 0F);


                                        //GlStateManager.rotate(newFacing.getAxis() == Axis.Y ? newFacing == EnumFacing.UP ? 90F : -90F : 0F, 0F, 1F, 0F);

                                        Tessellator t = Tessellator.getInstance();
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
                            if (ConfigClient.do_block_preview)
                            {
                                GlStateManager.pushMatrix();
                                {
                                    GlStateManager.scale(0.95F, 0.95F, 0.95F);
                                    GlStateManager.translate(-x, -y + 0.08, -z);
                                    BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();

                                    RenderHelper.disableStandardItemLighting();

                                    Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                                    GlStateManager.enableBlend();
                                    GlStateManager.enableAlpha();
                                    GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                                    Tessellator tess = Tessellator.getInstance();
                                    BufferBuilder buffer = tess.getBuffer();
                                    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
                                    dispatcher.renderBlock(state, pos, world, buffer);
                                    tess.draw();
                                    RenderHelper.enableStandardItemLighting();
                                    GlStateManager.disableAlpha();
                                    GlStateManager.disableBlend();
                                }
                                GlStateManager.popMatrix();
                            }
                        }
                        catch (Exception e)
                        {
                            LogManager.getLogger().warn("Problem rendering advanced placement for " + itemblock.getBlock().getRegistryName());
                        }
                    }
                }
            }
        }
    }
}
