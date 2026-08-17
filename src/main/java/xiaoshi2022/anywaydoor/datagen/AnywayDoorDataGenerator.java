package xiaoshi2022.anywaydoor.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class AnywayDoorDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(ModModelProvider::new);
		pack.addProvider(ModLanguageProvider.ZhCn::new);
		pack.addProvider(ModLanguageProvider.EnUs::new);
		pack.addProvider(ModBlockTagProvider::new);
	}
}
