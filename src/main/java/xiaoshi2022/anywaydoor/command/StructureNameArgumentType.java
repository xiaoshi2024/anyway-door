package xiaoshi2022.anywaydoor.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.concurrent.CompletableFuture;

public class StructureNameArgumentType implements ArgumentType<String> {

    public static StructureNameArgumentType structure() {
        return new StructureNameArgumentType();
    }

    public static String getStructure(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (context.getSource() instanceof CommandSourceStack source) {
            try {
                Registry<Structure> structureRegistry = source.getServer().registryAccess()
                        .registryOrThrow(Registries.STRUCTURE);

                String remaining = builder.getRemaining().toLowerCase();
                for (ResourceKey<Structure> key : structureRegistry.registryKeySet()) {
                    ResourceLocation location = key.location();
                    String id = location.toString();
                    if (id.toLowerCase().startsWith(remaining)) {
                        builder.suggest(id);
                    }
                    if (location.getNamespace().equals("minecraft") &&
                            location.getPath().toLowerCase().startsWith(remaining)) {
                        builder.suggest(location.getPath());
                    }
                }
            } catch (Exception ignored) {}
        }
        return builder.buildFuture();
    }
}