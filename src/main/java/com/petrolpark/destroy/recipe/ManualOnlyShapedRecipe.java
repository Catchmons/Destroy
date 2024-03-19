package com.petrolpark.destroy.recipe;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import com.google.gson.JsonObject;
import com.petrolpark.destroy.util.DestroyReloadListener;

import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

public class ManualOnlyShapedRecipe extends ShapedRecipe {

    public static final AllowedMenuListener ALLOWED_MENU_LISTENER = new AllowedMenuListener();

    public static final Set<MenuType<?>> ALLOWED_MENUS = new HashSet<>();

    public ManualOnlyShapedRecipe(ResourceLocation id, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> recipeItems, ItemStack result) {
        super(id, group, category, width, height, recipeItems, result);
    };

    public static boolean isAllowed(CraftingContainer inv) {
        return inv instanceof TransientCraftingContainer container && (container.menu instanceof InventoryMenu || ALLOWED_MENUS.contains(container.menu.getType()));
    };

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        return super.matches(inv, level) && isAllowed(inv);
    };

    @Override
    public boolean isSpecial() {
        return true;
    };

    @Override
    public RecipeSerializer<?> getSerializer() {
        return DestroyRecipeTypes.MANUAL_ONLY_CRAFTING_SHAPED.getSerializer();
    };

    public ItemStack getExampleResult() {
        return getResultItem(null);
    };

    public static class AllowedMenuListener extends DestroyReloadListener {

        @Override
        public String getPath() {
            return "destroy_compat/manual_crafting_allowed_menus";
        };

        @Override
        public void beforeReload() {
            ALLOWED_MENUS.clear();
        };

        @Override
        @SuppressWarnings("deprecation")
        public void forEachNameSpaceJsonFile(JsonObject jsonObject) {
            jsonObject.get("values").getAsJsonArray().forEach(element -> {
                ResourceLocation menuTypeRl = new ResourceLocation(element.getAsString());
                Optional<Holder.Reference<MenuType<?>>> menuTypeOptional = BuiltInRegistries.MENU.asLookup().get(ResourceKey.create(Registries.MENU, menuTypeRl));
                if (menuTypeOptional.isEmpty()) throw new IllegalArgumentException("Invalid menu type ID: "+menuTypeRl);
                ALLOWED_MENUS.add(menuTypeOptional.get().value());
            });
        };

    };

    public static class Serializer implements RecipeSerializer<ManualOnlyShapedRecipe> {

        private final ShapedRecipe.Serializer parent;

        public Serializer() {
            parent = new ShapedRecipe.Serializer();
        };

        @Override
        public ManualOnlyShapedRecipe fromJson(ResourceLocation recipeId, JsonObject serializedRecipe) {
            ShapedRecipe recipe = parent.fromJson(recipeId, serializedRecipe);
            return new ManualOnlyShapedRecipe(recipeId, recipe.getGroup(), recipe.category(), recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), recipe.getResultItem(null));
        };

        @Override
        public @Nullable ManualOnlyShapedRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            ShapedRecipe recipe = parent.fromNetwork(recipeId, buffer);
            return new ManualOnlyShapedRecipe(recipeId, recipe.getGroup(), recipe.category(), recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), recipe.getResultItem(null));
        };

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ManualOnlyShapedRecipe recipe) {
            parent.toNetwork(buffer, recipe);
        };


    };
    
};
