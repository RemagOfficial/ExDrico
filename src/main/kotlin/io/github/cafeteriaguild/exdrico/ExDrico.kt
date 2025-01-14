package io.github.cafeteriaguild.exdrico

import io.github.cafeteriaguild.exdrico.common.blocks.BlockCompendium
import io.github.cafeteriaguild.exdrico.common.blocks.BlockCompendium.OAK_SIEVE
import io.github.cafeteriaguild.exdrico.common.fluids.FluidCompendium
import io.github.cafeteriaguild.exdrico.common.items.ItemCompendium
import io.github.cafeteriaguild.exdrico.common.items.ItemCompendium.MESH
import io.github.cafeteriaguild.exdrico.common.material.MaterialCompendium
import io.github.cafeteriaguild.exdrico.common.meshes.MeshResource
import io.github.cafeteriaguild.exdrico.common.meshes.MeshType
import io.github.cafeteriaguild.exdrico.common.recipes.VatRecipe
import io.github.cafeteriaguild.exdrico.utils.ModIdentifier
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.resource.ResourceType
import net.minecraft.util.registry.Registry

class ExDrico: ModInitializer {

    companion object {
        const val MOD_ID = "exdrico"
        var CREATIVE_TAB: ItemGroup = FabricItemGroupBuilder.create(ModIdentifier("creative_tab")).icon { ItemStack(OAK_SIEVE) }.appendItems{ stacks ->
            stacks.addAll(ItemCompendium.itemsStack)
            MeshType.TYPES.forEach {
                val stack = ItemStack(MESH)
                stack.orCreateTag.putString("mesh", it.key.toString())
                stacks.add(stack)
            }
            stacks.addAll(BlockCompendium.blocksStack)
        }.build()
    }

    override fun onInitialize() {
        MaterialCompendium.initMaterials()
        BlockCompendium.initBlocks()
        ItemCompendium.initItems()
        FluidCompendium.initFluids()

        Registry.register(Registry.RECIPE_TYPE, VatRecipe.ID, VatRecipe.TYPE)
        Registry.register(Registry.RECIPE_SERIALIZER, VatRecipe.ID, VatRecipe.SERIALIZER)

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(MeshResource())
    }

}