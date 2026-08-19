package xiaoshi2022.anywaydoor.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import xiaoshi2022.anywaydoor.entity.DuolabEntity;

public class DuolabRenderer extends GeoEntityRenderer<DuolabEntity> {

    public DuolabRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DuolabModel());
        this.shadowRadius = 0.5f;

        // 添加手持物品渲染层
        addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Nullable
            @Override
            protected ItemStack getStackForBone(GeoBone bone, DuolabEntity entity) {
                // 绑定右手骨骼（对应模型中的 rightItem）
                if ("rightItem".equals(bone.getName())) {
                    return entity.getMainHandItem();
                }
                // 绑定左手骨骼（对应模型中的 leftItem）
                if ("leftItem".equals(bone.getName())) {
                    return entity.getOffhandItem();
                }
                return null;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, DuolabEntity entity) {
                // 设置第三人称手持渲染模式
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, DuolabEntity entity,
                                              MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
                // 微调物品位置和旋转（根据哆啦B梦模型调整）
                // 右手物品位置调整
                if ("rightItem".equals(bone.getName())) {
                    poseStack.translate(0.1, 0.2, 0.1);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(0));
                }
                // 左手物品位置调整
                if ("leftItem".equals(bone.getName())) {
                    poseStack.translate(-0.1, 0.2, 0.1);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(0));
                }
                super.renderStackForBone(poseStack, bone, stack, entity, bufferSource, partialTick, packedLight, packedOverlay);
            }
        });
    }
}