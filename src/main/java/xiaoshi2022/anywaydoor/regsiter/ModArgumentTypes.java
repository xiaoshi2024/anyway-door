package xiaoshi2022.anywaydoor.regsiter;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.resources.ResourceLocation;
import xiaoshi2022.anywaydoor.command.StructureNameArgumentType;

import static xiaoshi2022.anywaydoor.AnywayDoor.MOD_ID;

public class ModArgumentTypes {

    public static void init() {
        ArgumentTypeRegistry.registerArgumentType(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "structure_name"),
                StructureNameArgumentType.class,
                SingletonArgumentInfo.contextFree(StructureNameArgumentType::structure)
        );
    }
}