package xiaoshi2022.anywaydoor.regsiter;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import xiaoshi2022.anywaydoor.AnywayDoor;

import java.util.EnumMap;
import java.util.List;

import static xiaoshi2022.anywaydoor.AnywayDoor.MOD_ID;

public class ModArmorMaterials {

    public static final Holder<ArmorMaterial> XIAOFU_MASK = Registry.registerForHolder(
            BuiltInRegistries.ARMOR_MATERIAL,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "xiaofu_mask"),
            new ArmorMaterial(
                    new EnumMap<>(ArmorItem.Type.class) {{
                        put(ArmorItem.Type.HELMET, 2);
                    }},
                    15,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(Items.LEATHER),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(MOD_ID, "xiaofu_mask")
                    )),
                    0.0F,
                    0.0F
            )
    );
}