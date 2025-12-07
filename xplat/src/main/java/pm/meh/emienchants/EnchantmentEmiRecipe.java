package pm.meh.emienchants;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static pm.meh.emienchants.Util.getBookStackForLevel;

public class EnchantmentEmiRecipe implements EmiRecipe {

    private static final ResourceLocation ICON_INFO = ResourceLocation.fromNamespaceAndPath(Common.MOD_ID, "textures/gui/icon_info.png");
    private static final ResourceLocation ICON_ENCH_TABLE = ResourceLocation.fromNamespaceAndPath(Common.MOD_ID, "textures/gui/icon_ench_table.png");
    private static final ResourceLocation ICON_VILLAGER = ResourceLocation.fromNamespaceAndPath(Common.MOD_ID, "textures/gui/icon_villager.png");
    private static final ResourceLocation ICON_TREASURE = ResourceLocation.fromNamespaceAndPath(Common.MOD_ID, "textures/gui/icon_treasure.png");
    private static final ResourceLocation ICON_CURSE = ResourceLocation.fromNamespaceAndPath(Common.MOD_ID, "textures/gui/icon_curse.png");

    private static final int LAYOUT_X_OFFSET = 22;
    private static final int LAYOUT_X_OFFSET_SMALL = 2;
    private static final int LAYOUT_Y_OFFSET = 2;
    private static final int LAYOUT_ROW_HEIGHT = 10;
    private static final int LAYOUT_TEXT_COLOR = 0x333333;
    private static final boolean LAYOUT_TEXT_SHADOW = false;
    private final int LAYOUT_DESCRIPTION_OFFSET;

    private final ResourceLocation id;
	private final Holder<Enchantment> holder;
    private final ResourceLocation enchantmentResourceLocation;
    private final Enchantment enchantment;
    private final List<EmiStack> inputs;
    private final EmiIngredient canApplyTo;
    private final EmiIngredient incompatibleSlot;
    private final List<IconBoolStatEntry> iconStats;
    private final List<FormattedCharSequence> description;

    public EnchantmentEmiRecipe(ResourceLocation location, Holder.Reference<Enchantment> holder) {
		this.holder = holder;
        enchantmentResourceLocation = location;
        enchantment = holder.value();

        id = ResourceLocation.fromNamespaceAndPath(Common.MOD_ID, String.format("/%s/%s", location.getNamespace(), location.getPath()));

        inputs = IntStream.range(1, enchantment.getMaxLevel() + 1).mapToObj(this::getBookForLevel).toList();
        canApplyTo = EmiIngredient.of(BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(enchantment::canEnchant).map(EmiStack::of).toList());
        incompatibleSlot = EmiIngredient.of(Objects.requireNonNull(Minecraft.getInstance().level).registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
				.filter(h -> !(h.is(holder) || Enchantment.areCompatible(h, holder)))
				.map(h -> Util.getBookStackForLevel(h, h.value().getMaxLevel()))
				.toList());

		boolean enchTable = holder.is(EnchantmentTags.IN_ENCHANTING_TABLE);
		boolean treasure = holder.is(EnchantmentTags.TREASURE);
        iconStats = List.of(
                new IconBoolStatEntry(ICON_ENCH_TABLE, "ench_table", enchTable && !treasure, true),
                new IconBoolStatEntry(ICON_VILLAGER, "tradeable", holder.is(EnchantmentTags.TRADEABLE), true),
                new IconBoolStatEntry(ICON_TREASURE, "treasure", treasure, false),
                new IconBoolStatEntry(ICON_CURSE, "curse", holder.is(EnchantmentTags.CURSE), false)
        );

        LAYOUT_DESCRIPTION_OFFSET = LAYOUT_Y_OFFSET + LAYOUT_ROW_HEIGHT * 4;

        String descriptionId = location.toLanguageKey("enchantment", "desc");
        if (I18n.exists(descriptionId)) {
			Component descriptionTranslatable = Component.translatable(descriptionId).withStyle(ChatFormatting.ITALIC);
			description = Minecraft.getInstance().font.split(descriptionTranslatable, getDisplayWidth() - LAYOUT_X_OFFSET_SMALL * 2 - (incompatibleSlot.isEmpty() ? 0 : LAYOUT_X_OFFSET));
        } else {
			description = List.of();
        }
    }

    private EmiStack getBookForLevel(int level) {
        return getBookStackForLevel(holder, level);
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return EnchantmentEmiPlugin.ENCHANTS_CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of(canApplyTo, EmiIngredient.of(inputs));
    }

    @Override
    public List<EmiStack> getOutputs() {
        return inputs;
    }

    @Override
    public int getDisplayWidth() {
        return 160;
    }

    @Override
    public int getDisplayHeight() {
        return LAYOUT_DESCRIPTION_OFFSET + LAYOUT_ROW_HEIGHT * description.size();
    }

    @Override
    public void addWidgets(WidgetHolder widgetHolder) {
        int row = 0;

        // enchantment slot
        widgetHolder.addSlot(getBookForLevel(enchantment.getMaxLevel()), LAYOUT_X_OFFSET_SMALL, LAYOUT_Y_OFFSET);
        // applicable items slot
        widgetHolder.add(new CustomEmiSlotWidget(canApplyTo, LAYOUT_X_OFFSET_SMALL, LAYOUT_Y_OFFSET + 20, false, Component.translatable("emienchants.property.applicable_to")));
        // incompatible enchants slot
        if (!incompatibleSlot.isEmpty()) {
            widgetHolder.add(new CustomEmiSlotWidget(incompatibleSlot, LAYOUT_X_OFFSET_SMALL, LAYOUT_Y_OFFSET + 40, true, Component.translatable("emienchants.property.conflicts")));
        }

        // enchantment name and level range
        MutableComponent title = enchantment.description().copy();
        if (enchantment.getMaxLevel() > 1) {
            title = title.append(String.format(" §5%d-%d", enchantment.getMinLevel(), enchantment.getMaxLevel()));
        }
        TextWidget titleWidget = widgetHolder.addText(title, LAYOUT_X_OFFSET, LAYOUT_Y_OFFSET + LAYOUT_ROW_HEIGHT * row, LAYOUT_TEXT_COLOR, LAYOUT_TEXT_SHADOW);

		// weight
		widgetHolder.addTexture(ICON_INFO, LAYOUT_X_OFFSET + titleWidget.getBounds().width() + 1, LAYOUT_Y_OFFSET + LAYOUT_ROW_HEIGHT * row++, 7, 8, 7, 8, 7, 8, 7, 8)
				.tooltipText(Stream.concat(
						Stream.<Component>of(Component.translatable("emienchants.property.weight", enchantment.getWeight())),
						IntStream.range(1, enchantment.getMaxLevel() + 1).mapToObj(lvl -> (Component) Component.translatable("emienchants.property.cost", lvl, enchantment.getMinCost(lvl), enchantment.getMaxCost(lvl)))
				).toList());

        // mod id
        widgetHolder.addText(Component.literal(enchantmentResourceLocation.getNamespace()).withStyle(ChatFormatting.DARK_BLUE), LAYOUT_X_OFFSET, LAYOUT_Y_OFFSET + LAYOUT_ROW_HEIGHT * row++, LAYOUT_TEXT_COLOR, LAYOUT_TEXT_SHADOW);

        // icon stats
        int iconSectionWidth = (getDisplayWidth() - LAYOUT_X_OFFSET - LAYOUT_X_OFFSET_SMALL) / 3;
        int iconXOffset = LAYOUT_X_OFFSET;
        int iconYOffset = LAYOUT_Y_OFFSET + LAYOUT_ROW_HEIGHT * row;
        int iconCounter = 0;

        for (IconBoolStatEntry stat : iconStats) {
            widgetHolder.addTexture(stat.icon, iconXOffset, iconYOffset, 8, 8, 8, 8, 8, 8, 8, 8);
            TextWidget statWidget = widgetHolder.addText(stat.getValueLabel(), iconXOffset + 10, iconYOffset, LAYOUT_TEXT_COLOR, LAYOUT_TEXT_SHADOW);
            widgetHolder.addTooltipText(List.of(Component.translatable(String.format("emienchants.property.%s.%s", stat.label, stat.value))), iconXOffset, iconYOffset, statWidget.getBounds().width() + 10, 8);
            iconCounter += 1;
            if (iconCounter == 3) {
                iconCounter = 0;
                iconXOffset = LAYOUT_X_OFFSET;
                iconYOffset += LAYOUT_ROW_HEIGHT;
            } else {
                iconXOffset += iconSectionWidth;
            }
        }

        // description
        row = 0;
        for (FormattedCharSequence line : description) {
			widgetHolder.addText(line, incompatibleSlot.isEmpty() ? LAYOUT_X_OFFSET_SMALL : LAYOUT_X_OFFSET, LAYOUT_DESCRIPTION_OFFSET + LAYOUT_ROW_HEIGHT * row++, LAYOUT_TEXT_COLOR, LAYOUT_TEXT_SHADOW);
        }
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    private record IconBoolStatEntry(ResourceLocation icon, String label, boolean value, boolean isPositive) {
        public Component getValueLabel() {
            return Component.translatable("emienchants.property.value." + value).withStyle(
                    Style.EMPTY.withColor(value ^ isPositive ? 0xAA0000 : 0x008800));
        }
    }
}
