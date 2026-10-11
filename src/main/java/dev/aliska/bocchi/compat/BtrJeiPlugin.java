package dev.aliska.bocchi.compat;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.aliska.bocchi.BocchiMod;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.util.ArrayList;

/**
 * Интеграция с Just Enough Items (JEI) для корректного отображения
 * предметов и 77 рецептов BTR World в интерфейсе JEI на клиенте.
 */
@JeiPlugin
@Environment(EnvType.CLIENT)
public class BtrJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return BocchiMod.id("jei");
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        registration.addExtraIngredients(
            VanillaTypes.ITEM_STACK,
            BocchiMod.ITEMS.stream().map(ItemStack::new).toList()
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var world = MinecraftClient.getInstance().world;
        if (world == null) {
            return;
        }

        var recipes = new ArrayList<RecipeEntry<CraftingRecipe>>();
        var root = FabricLoader.getInstance()
            .getModContainer("bocchi")
            .orElseThrow()
            .findPath("data/bocchi/recipe")
            .orElseThrow();

        try (var files = Files.list(root)) {
            for (var path : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                var json = JsonParser.parseString(Files.readString(path));
                var result = Recipe.CODEC.parse(
                    world.getRegistryManager().getOps(JsonOps.INSTANCE),
                    json
                ).getOrThrow();

                if (result instanceof CraftingRecipe crafting) {
                    recipes.add(new RecipeEntry<>(
                        BocchiMod.id(path.getFileName().toString().replace(".json", "")),
                        crafting
                    ));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot register BTR World JEI recipes", e);
        }

        // Локальные определения рецептов сохраняют реальные сущности предметов вместо бумаги Polymer
        registration.addRecipes(RecipeTypes.CRAFTING, recipes);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        // Скрываем служебные предметы моделей выражений лиц и коробку сиденья из списка JEI
        var hidden = new ArrayList<ItemStack>();
        BocchiMod.MODELS.values().forEach(item -> hidden.add(new ItemStack(item)));
        hidden.add(new ItemStack(BocchiMod.BOX_MODEL));
        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
    }
}
