package io.github.cafeteriaguild.exdrico.client.render.color

import com.mojang.datafixers.util.Pair
import io.github.cafeteriaguild.exdrico.common.blocks.ColorBlock
import io.github.cafeteriaguild.exdrico.common.items.ColorItem
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
import net.fabricmc.fabric.impl.client.indigo.renderer.helper.GeometryHelper
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.model.*
import net.minecraft.client.render.model.json.ModelOverrideList
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.ModelIdentifier
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.screen.PlayerScreenHandler
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.BlockRenderView
import java.awt.Color
import java.util.*
import java.util.function.Function
import java.util.function.Supplier

class ColorModel: UnbakedModel, BakedModel, FabricBakedModel {

    private val modelIdentifierCollection: MutableList<ModelIdentifier> = mutableListOf()
    private val spriteIdentifierCollection: MutableList<SpriteIdentifier> = mutableListOf()

    init {
        ColoredBlock.values().forEach {
            modelIdentifierCollection.add(ModelIdentifier(it.identifier, ""))
            spriteIdentifierCollection.add(SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier(it.identifier.namespace, "block/${it.identifier.path}")))
        }
        ColoredItem.values().forEach {
            modelIdentifierCollection.add(ModelIdentifier(it.identifier, "inventory"))
            modelIdentifierCollection.add(ModelIdentifier(Identifier(it.identifier.namespace, it.identifier.path+"_overlay"), "inventory"))
        }
    }

    private val modelArray = arrayOfNulls<BakedModel>(modelIdentifierCollection.size)
    private val spriteArray = arrayOfNulls<Sprite>(spriteIdentifierCollection.size)

    //Get baked model dependencies
    override fun getModelDependencies() = modelIdentifierCollection
    override fun getTextureDependencies(unbakedModelGetter: Function<Identifier, UnbakedModel>?, unresolvedTextureReferences: MutableSet<Pair<String, String>>?) = mutableListOf<SpriteIdentifier>()

    //Get actual baked model
    override fun bake(loader: ModelLoader, textureGetter: Function<SpriteIdentifier, Sprite>, rotationContainer: ModelBakeSettings, modelId: Identifier?): BakedModel {
        spriteIdentifierCollection.forEachIndexed { idx, spriteIdentifier ->
            spriteArray[idx] = textureGetter.apply(spriteIdentifier)
        }
        return this
    }

    //Dummy baked model returns
    override fun getOverrides(): ModelOverrideList = ModelOverrideList.EMPTY
    override fun getQuads(state: BlockState?, face: Direction?, random: Random?): MutableList<BakedQuad> = mutableListOf()
    override fun getSprite() = spriteArray[0]

    //Dummy baked model configs
    override fun useAmbientOcclusion() = true
    override fun isBuiltin() = false
    override fun isSideLit() = false
    override fun hasDepth() = false

    //Get stone block transformation
    override fun getTransformation() = MinecraftClient.getInstance().bakedModelManager.getModel(ModelIdentifier(Identifier("arrow"), "inventory")).transformation

    //Set false to use fabric renderer
    override fun isVanillaAdapter() = false

    //I hate minecraft
    private fun updateModelArray() {
        if(modelArray[0] == null) {
            modelIdentifierCollection.forEachIndexed { idx, modelIdentifier ->
                modelArray[idx] = MinecraftClient.getInstance().bakedModelManager.getModel(modelIdentifier)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun emitBlockQuads(worldView: BlockRenderView?, state: BlockState, pos: BlockPos?, randSupplier: Supplier<Random>, context: RenderContext) {
        updateModelArray() // I hate minecraft

        val block = state.block as? ColorBlock ?: return
        val color = block.color.rgb
        val modelId = ColoredBlock.values().toList().indexOf(block.colorModel)

        modelArray[modelId]?.let { blockModel ->
            context.pushTransform { quad ->
                quad.nominalFace(GeometryHelper.lightFace(quad))
                quad.spriteColor(0, color, color, color, color)
                quad.spriteBake(0, blockModel.sprite, MutableQuadView.BAKE_LOCK_UV)
                true
            }

            context.fallbackConsumer().accept(blockModel)

            context.popTransform()
        }
    }

    @Suppress("DEPRECATION")
    override fun emitItemQuads(stack: ItemStack, randSupplier: Supplier<Random>, context: RenderContext) {
        updateModelArray() // I hate minecraft

        (stack.item as? BlockItem)?.block?.let { block ->
            emitBlockQuads(null, block.defaultState, null, randSupplier, context)
        }
        (stack.item as? ColorItem)?.let { item ->
            val color = Color(item.color.red, item.color.green, item.color.blue, 165).rgb
            val modelId = (ColoredBlock.values().size)+(ColoredItem.values().toList().indexOf(item.colorModel)*2)

            context.fallbackConsumer().accept(modelArray[modelId])

            modelArray[modelId+1]?.let { overlayModel ->
                context.pushTransform { quad ->
                    quad.nominalFace(GeometryHelper.lightFace(quad))
                    quad.spriteColor(0, color, color, color, color)
                    quad.spriteBake(0, overlayModel.sprite, MutableQuadView.BAKE_LOCK_UV)
                    true
                }

                context.fallbackConsumer().accept(overlayModel)

                context.popTransform()
            }
        }
    }


}