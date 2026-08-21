package xiaoshi2022.anywaydoor.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import xiaoshi2022.anywaydoor.client.renderer.armor.XiaofuMaskRenderer;

import java.util.function.Consumer;

public class XiaofuMaskItem extends ArmorItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public XiaofuMaskItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private XiaofuMaskRenderer renderer;

            @Override
            public <T extends LivingEntity> net.minecraft.client.model.HumanoidModel<?> getGeoArmorRenderer(
                    @Nullable T livingEntity,
                    ItemStack itemStack,
                    @Nullable net.minecraft.world.entity.EquipmentSlot equipmentSlot,
                    @Nullable net.minecraft.client.model.HumanoidModel<T> original) {
                if (this.renderer == null) {
                    this.renderer = new XiaofuMaskRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 20, state -> {
            // 使用默认空闲动画，或者使用 xiaofu.animation.json 中的 idle
            // state.setAnimation(DefaultAnimations.IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}