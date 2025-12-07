package pm.meh.emienchants;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

public class Util {
    public static EmiStack getBookStackForLevel(Holder<Enchantment> holder, int level) {
		ItemStack item = new ItemStack(Items.ENCHANTED_BOOK);

		item.enchant(holder, level);

        return EmiStack.of(item);
    }
}
