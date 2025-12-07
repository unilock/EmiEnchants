package pm.meh.emienchants;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

@EmiEntrypoint
public class EnchantmentEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory ENCHANTS_CATEGORY = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath(Common.MOD_ID, "enchants"), EmiStack.of(Items.ENCHANTED_BOOK));

    @Override
    public void register(EmiRegistry emiRegistry) {
        emiRegistry.addCategory(ENCHANTS_CATEGORY);

		var level = Minecraft.getInstance().level;
		if (level != null) {
			level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).holders().forEach(holder -> {
				ResourceLocation location = holder.unwrapKey().orElseThrow().location();

				emiRegistry.addRecipe(new EnchantmentEmiRecipe(location, holder));
			});
		}
    }
}
