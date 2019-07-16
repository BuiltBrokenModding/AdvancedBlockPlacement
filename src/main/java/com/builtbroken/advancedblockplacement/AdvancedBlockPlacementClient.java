package com.builtbroken.advancedblockplacement;

import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GL11;

import com.builtbroken.advancedblockplacement.AdvancedBlockPlacement.PlacementMode;

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
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = AdvancedBlockPlacement.MODID, value = Side.CLIENT)
public class AdvancedBlockPlacementClient {

    public static PlacementMode mode = PlacementMode.ADVANCED;
    public static final ResourceLocation ARROW_TEXTURE = new ResourceLocation(AdvancedBlockPlacement.MODID, "textures/arrow.png");

    @SubscribeEvent
    public static void renderTick(DrawBlockHighlightEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if(mode.isAdvanced()) {
            ItemStack stack = event.getPlayer().getHeldItem(EnumHand.MAIN_HAND);
            Item item = stack.getItem();
            if(item instanceof ItemBlock) {
                ItemBlock itemblock = (ItemBlock) item;
                if(AdvancedBlockPlacement.getAffectedIDs().contains(itemblock.getBlock().getRegistryName().toString())) {
                    try {
                        World world = Minecraft.getMinecraft().player.world;
                        BlockPos pos = event.getTarget().getBlockPos().offset(event.getTarget().sideHit);
                        GlStateManager.pushMatrix();
                        {
                            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
                            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
                            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();
                            GlStateManager.scale(0.95F, 0.95F, 0.95F);
                            GlStateManager.translate(-x, -y + 0.08, -z);
                            BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
                            Vec3d hit = event.getTarget().hitVec;
                            IBlockState state = AdvancedBlockPlacement.getNewState(itemblock.getBlock().getDefaultState(), event.getTarget().sideHit, (float) hit.x, (float) hit.y, (float) hit.z);

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
                            if(itemblock.getBlock().hasTileEntity(state) && state.getRenderType() == EnumBlockRenderType.ENTITYBLOCK_ANIMATED) {
                                GlStateManager.pushMatrix();
                                {
                                    GlStateManager.depthMask(false);
                                    //GlStateManager.translate();
                                    Tessellator t = Tessellator.getInstance();
                                    BufferBuilder r = t.getBuffer();

                                    r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
                                    Minecraft.getMinecraft().getTextureManager().bindTexture(ARROW_TEXTURE );

                                    r.pos(0, 1, 0).tex(0, 1).endVertex();
                                    r.pos(1, 1, 0).tex(1, 1).endVertex();
                                    r.pos(1, 0, 0).tex(1, 0).endVertex();
                                    r.pos(0, 0, 0).tex(0, 0).endVertex();

                                    t.draw();

                                    GlStateManager.depthMask(true);
                                }
                                GlStateManager.popMatrix();
                            }
                            RenderHelper.enableStandardItemLighting();
                            GlStateManager.disableAlpha();
                            GlStateManager.disableBlend();
                        }
                        GlStateManager.popMatrix();
                    } catch(Exception e) {
                        LogManager.getLogger().warn("Problem rendering advanced placement for " + itemblock.getBlock().getRegistryName());
                    }
                }
            }
        }
    }

    @Config(modid = AdvancedBlockPlacement.MODID, name = AdvancedBlockPlacement.MODID + "-client")
    public static class ClientConfiguration {

        @Config.Comment("A keycode corresponding to the default keybinding for the advanced placement mode (for pack makers to ship keybinds with) 0 = none")
        public static int default_keybind = 0;

        @Config.Comment("Setting to false will remove the \"preview\" effect when in advanced placement mode")
        public static boolean do_special_rendering = true;

        @Config.Comment("Setting to false will require the user to hold down the advanced placement key to stay in advanced placement mode rather than toggling it.")
        public static boolean is_keybind_toggle = true;

    }

}
