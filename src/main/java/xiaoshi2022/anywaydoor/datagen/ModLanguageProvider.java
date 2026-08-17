package xiaoshi2022.anywaydoor.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import xiaoshi2022.anywaydoor.regsiter.ModBlocks;

import java.util.concurrent.CompletableFuture;

public abstract class ModLanguageProvider extends FabricLanguageProvider {

	public ModLanguageProvider(FabricDataOutput dataOutput, String languageCode, CompletableFuture<HolderLookup.Provider> registryLookup) {
		super(dataOutput, languageCode, registryLookup);
	}

	public static class ZhCn extends ModLanguageProvider {
		public ZhCn(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
			super(dataOutput, "zh_cn", registryLookup);
		}

		@Override
		public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder translationBuilder) {
			translationBuilder.add(ModBlocks.DIMENSIONAL_DOOR, "任意门");
		}
	}

	public static class EnUs extends ModLanguageProvider {
		public EnUs(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
			super(dataOutput, "en_us", registryLookup);
		}

		@Override
		public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder translationBuilder) {
			translationBuilder.add(ModBlocks.DIMENSIONAL_DOOR, "Dimensional Door");
		}
	}
}
