package com.nomiceu.nomilabs.remap.datafixer;

import com.google.common.collect.ImmutableMap;
import com.nomiceu.nomilabs.LabsValues;
import com.nomiceu.nomilabs.config.LabsConfig;
import com.nomiceu.nomilabs.remap.LabsRemapHelper;
import com.nomiceu.nomilabs.remap.datafixer.types.LabsFixTypes;
import com.nomiceu.nomilabs.util.LabsModeHelper;
import com.nomiceu.nomilabs.util.LabsNames;
import gregtech.api.GregTechAPI;
import gregtech.api.unification.material.Material;
import gregtech.common.pipelike.cable.Insulation;
import gregtech.common.pipelike.fluidpipe.FluidPipeType;
import gregtech.common.pipelike.itempipe.ItemPipeType;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.Short2ShortLinkedOpenHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.IFixType;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.Loader;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.nomiceu.nomilabs.LabsValues.*;
import static com.nomiceu.nomilabs.util.LabsNames.makeLabsName;

/**
 * Definitions for all values, and all data fixes.
 * <p>
 * No Data Fixes are loaded if the 'FIX_VERSION' is below the version saved in the world, or the Nomi Labs Version in world
 * is the same as the current Nomi Labs Version.
 */
public class LabsFixes {
    /**
     * The name used to store fix data. Do not change this, although changing it will FORCE all fixes to be applied.
     * Do this via another way.
     */
    public static final String DATA_NAME = LABS_MODID + ".fix_data";

    /**
     * In which nbt key the data is stored. Do not change this, although changing it will FORCE all fixes to be applied.
     * Do this via another way. (This is better than changing the data name)
     */
    public static final String DATA_KEY = "LabsFixer";

    /**
     * The Data Fixer Version that occurs when a world has not been loaded with Nomi Labs before, and Nomi-CEu Specific Fixes are not enabled.
     * <p>
     * This is not for new worlds; new worlds must never have Data Fixes.
     * <p>
     * When comparing against this version, check for equality rather than less than!
     */
    public static final int NEW = Integer.MAX_VALUE;

    /**
     * The current data format version. Increment this when breaking changes are made and the
     * data mixer must be applied. If this is not incremented, nothing will be applied.
     */
    public static final int CURRENT = 3;

    /**
     * Version before Capacitor Remapping.
     * <p>
     * Versions before this need nbt of custom capacitors removed.
     */
    public static final int PRE_CAPACITOR_REMAPPING = 2;

    /**
     * Version before Material Registry Rework.
     * <p>
     * Versions before this need to have their material items be remapped. Meta Blocks are remapped based on missing registry event, as they have 'baseID's
     * (thus, any new GT Addons must be checked to make sure they do not have IDs in GregTech's Registry above 32000)
     * <p>
     * Meta Items are mapped below.
     */
    public static final int PRE_MATERIAL_REWORK = 1;

    /**
     * Default version, used if save doesn't have a fix version and nomi-labs was loaded in the world before, or if something goes wrong.
     */
    public static final int DEFAULT = 0;

    /**
     * Default Nomi-CEu version, used if save doesn't have a fix version, or if something goes wrong, and Nomi-CEu Specific Data Fixes are enabled.
     */
    public static final int DEFAULT_NOMI_CEU = -1;

    public static List<DataFix.ItemFix> itemFixes;

    public static List<DataFix.BlockFix> blockFixes;

    public static List<DataFix.TileEntityFix> tileEntityFixes;

    public static Map<IFixType, List<? extends DataFix<?>>> fixes;

    public static Map<ResourceLocation, ResourceLocation> multiblockMetaIdRemap;
    public static Map<Short, Short> multiblockMetaRemap;
    public static Set<ResourceLocation> specialMetaItemsRemap;
    public static Set<String> materialNames;
    public static Map<String, OldCapacitorSpecification> capacitorSpecificationRemap;

    public static void init() {
        helperMapsInit();

        /*
         * Item Fixes.
         * Note that this is not applied on any new items.
         * This fix is applied:
         * - On Player Inventories
         * - On Entity Inventories
         * - On Tile Entity Inventories
         * - Inside Ender Storage Ender Chests
         *
         * Example of an input:
         * ItemStackLike:
         *  rl: "minecraft:skull" // Type: Resource Location. Not Null.
         *  meta: 3 // Type: Short
         *  tag: {owner: "adsf", i: {k: 1, l: 4}, ...} // Type: NBT Tag Compound. Nullable.
         *
         * Example:
         * itemFixes.add(
         *      new DataFix.ItemFix("Example", // Name
         *
         *              "Example Item Fix. Turns Apples into Creeper Heads.", // Description
         *
         *              false, // Whether the correct mode is required for the change to work right
         *
         *              (version) -> version <= DEFAULT_VERSION, // Whether the previous version in the save means that this fix must be applied.
         *                                                       // Note that this is not applied if the previous version is equal to the current overall fix version.
         *
         *              (modList) -> true, // Inputs the previous modlist the world was loaded with (map of modid to modversion), return whether it is valid.
         *                                 // Note that the fix is only applied if the version AND the modlist is valid.
         *
         *              (stack) -> stack.rl.equals(new ResourceLocation("minecraft:apple")), // Input ItemStackLike, return a boolean (true to fix, false to skip)
         *
         *              (stack) -> stack.setRl(new ResourceLocation("minecraft:skull")).setMeta((short) 4))); // Change the given ItemStackLike (Changes to Creeper Head in this case)
         */
        itemFixes = new ObjectArrayList<>();

        itemFixes.add(
                new DataFix.ItemFix("Dark Red Coal Remap",
                        "Correctly remaps Content Tweaker Dark Red Coal to XU2 Red Coal.",
                        false,
                        (version) -> version <= DEFAULT,
                        (modList) -> true,
                        (stack) -> stack.rl.equals(new ResourceLocation(CONTENTTWEAKER_MODID, "dark_red_coal")),
                        (stack) -> stack.setRl(new ResourceLocation(XU2_MODID, "ingredients"))
                                .setMeta((short) 4)) // Red Coal
        );

        if (LabsConfig.modIntegration.enableExtraUtils2Integration)
            itemFixes.add(
                    new DataFix.ItemFix("XU2 Frequency Removal",
                            "Removes Frequency from XU2 Ingredients.",
                            false,
                            (version) -> version <= DEFAULT || version == NEW,
                            (modList) -> modList.containsKey(XU2_MODID),
                            (stack) -> stack.rl.equals(new ResourceLocation(XU2_MODID, "ingredients"))
                                    && stack.tag != null && stack.tag.hasKey("Freq"),
                            (stack) -> {
                                var tag = Objects.requireNonNull(stack.tag);
                                tag.removeTag("Freq");
                                stack.setTag(tag.isEmpty() ? null : tag);
                            })
            );

        itemFixes.add(
                new DataFix.ItemFix("Old Multiblock Metadata Remap",
                        "Remaps old Multiblock Metadata to the new format.",
                        true,
                        (version) -> version <= DEFAULT_NOMI_CEU,
                        (modList) -> true,
                        (stack) -> stack.rl.equals(new ResourceLocation(GREGTECH_MODID,"machine")) && multiblockMetaRemap.containsKey(stack.meta),
                        (stack) -> stack.setMeta(multiblockMetaRemap.get(stack.meta)))
        );

        itemFixes.add(
                new DataFix.ItemFix("Material Meta Item Remap",
                        "Remaps old Meta Items, from Custom Materials, to the new format and registry.",
                        false,
                        (version) -> version <= PRE_MATERIAL_REWORK,
                        (modList) -> true,
                        (stack) -> stack.rl.getNamespace().equals(GREGTECH_MODID) &&
                                LabsRemapHelper.META_ITEM_MATCHER.matcher(stack.rl.getPath()).matches() &&
                                !LabsRemapHelper.META_BLOCK_MATCHER.matcher(stack.rl.getPath()).matches() &&
                                stack.meta >= LabsRemapHelper.MIN_META_ITEM_BASE_ID,
                        (stack) -> stack.setMeta((short) (stack.meta - LabsRemapHelper.MIN_META_ITEM_BASE_ID))
                                .setRl(LabsNames.makeLabsName(stack.rl.getPath())))
        );

        itemFixes.add(
                new DataFix.ItemFix("Material Special Meta Item Remap",
                        "Remaps old special placeable Meta Items, from Custom Materials, to the new format and registry.",
                        false,
                        (version) -> version <= PRE_MATERIAL_REWORK,
                        (modList) -> true,
                        (stack) -> specialMetaItemsRemap.contains(stack.rl) &&
                                stack.meta >= LabsRemapHelper.MIN_META_ITEM_BASE_ID,
                        (stack) -> stack.setMeta((short) (stack.meta - LabsRemapHelper.MIN_META_ITEM_BASE_ID))
                                .setRl(LabsNames.makeLabsName(stack.rl.getPath())))
        );

        if (Loader.isModLoaded(LabsValues.ENDER_IO_MODID))
            itemFixes.add(
                    new DataFix.ItemFix("Custom Capacitor NBT Removal",
                            "Removes NBT from Custom Capacitors.",
                            false,
                            (version) -> version <= PRE_CAPACITOR_REMAPPING,
                            (modList) -> true,
                            (stack) -> (stack.rl.getNamespace().equals(LABS_MODID) ||
                                    stack.rl.getNamespace().equals(CONTENTTWEAKER_MODID)) &&
                                    capacitorSpecificationRemap.containsKey(stack.rl.getPath()) &&
                                    capacitorSpecificationRemap.get(stack.rl.getPath()).needChange(stack.tag),
                            (stack) -> stack.setTag(capacitorSpecificationRemap.get(stack.rl.getPath()).remove(stack.tag)))
            );

        /*
         * Block Fixes.
         * Note that this is not applied on any new blocks, placed or generated.
         * This fix is applied:
         * - On Placed Blocks
         *
         * Example of an input:
         * BlockStateLike:
         *  rl: "minecraft:concrete" // Type: Resource Location. Not Null.
         *  meta: 1 // Type: Short
         *
         * Example:
         * blockFixes.add(
         *      new DataFix.BlockFix("Example", // Name
         *
         *              "Example Block Fix. Turns Oak Logs into Brown Concrete.", // Description
         *
         *              false, // Whether the correct mode is required for the change to work right
         *
         *              (version) -> version <= DEFAULT_VERSION, // Whether the previous version in the save means that this fix must be applied.
         *                                                       // Note that this is not applied if the previous version is equal to the current overall fix version.
         *
         *              (modList) -> true, // Inputs the previous modlist the world was loaded with (map of modid to modversion), return whether it is valid.
         *                                 // Note that the fix is only applied if the version AND the modlist is valid.
         *
         *              false, // Whether the tile entity tag is needed. If yes, it is accessible via state.setTileEntityTag() and state.tileEntityTag. If this is false, that field will be null.
         *
         *              (state) -> state.rl.equals(new ResourceLocation("minecraft:log")) && state.meta == 0, // Input BlockStateLike, return a boolean (true to fix, false to skip). You CAN NOT check the tile entity here!
         *
         *              null, // Secondary check, Input BlockStateLike, return a boolean (true to fix, false to skip). You CAN check the tile entity here! Input Null if checking tile entity is not needed.
         *
         *              (state) -> state.setRl(new ResourceLocation("minecraft:concrete")).setMeta((short) 12), // Change the given BlockStateLike (Changes to Brown Concrete in this case). You can also change the tile entity here if needed.
         * ));
         */
        blockFixes = new ObjectArrayList<>();

        blockFixes.add(
                new DataFix.BlockFix("Material Special Meta Block Remap",
                        "Remaps old special placeable Meta Blocks, from Custom Materials, to the new format and registry.",
                        false,
                        (version) -> version <= PRE_MATERIAL_REWORK,
                        (modList) -> true,
                        true,
                        (state) -> specialMetaItemsRemap.contains(state.rl),
                        (state) -> state.tileEntityTag != null &&
                                specialMetaItemsRemap.contains(new ResourceLocation(state.tileEntityTag.getString("PipeBlock"))) &&
                                materialNames.contains(state.tileEntityTag.getString("PipeMaterial")),
                        (state) -> {
                            state.setRl(LabsNames.makeLabsName(state.rl.getPath()));

                            //noinspection DataFlowIssue
                            state.tileEntityTag.setString("PipeBlock",
                                    LabsNames.makeLabsName(new ResourceLocation(state.tileEntityTag.getString("PipeBlock")).getPath()).toString());
                        })
        );

        /*
         * Tile Entity Fixes.
         * Note that this is not applied on any new tile entities.
         * This fix is applied:
         * - On Existing Tile Entities' Tag Compounds.
         *
         * Example of an input:
         * {x:279,y:73,z:199,Items:[{Slot:0b,id:"minecraft:apple",Count:1,Damage:0s}],id:"minecraft:chest",Lock:"", ...} // Type: NBT Tag Compound. Not Null.
         *
         * Notes:
         * - Item Fixes are called each item in this tile before this fix is applied.
         * - Do `/blockdata ~ ~-1 ~ {}` to get the Tile Entity Compound of the block you are standing on (in game)
         *
         * Example:
         * tileEntityFixes.add(
         *      new DataFix.TileEntityFix("Example", // Name
         *
         *                    "Example Tile Entity Fix. Changes the amount of the all items in chests to be 64.", // Description
         *
         *                    false, // Whether the correct mode is required for the change to work right
         *
         *                    (version) -> version <= DEFAULT_VERSION, // Whether the previous version in the save means that this fix must be applied.
         *                                                             // Note that this is not applied if the previous version is equal to the current overall fix version.
         *
         *                    (modList) -> true, // Inputs the previous modlist the world was loaded with (map of modid to modversion), return whether it is valid.
         *                                       // Note that the fix is only applied if the version AND the modlist is valid.
         *
         *                    // Input NBT Tag Compound, return a boolean (true to fix, false to skip)
         *                    (compound) -> compound.hasKey("id", Constants.NBT.TAG_STRING) &&
         *                     compound.getString("id").equals(new ResourceLocation("minecraft:chest").toString()) &&
         *                     compound.hasKey("Items", Constants.NBT.TAG_LIST) && !compound.getTagList("Items", Constants.NBT.TAG_COMPOUND).isEmpty(),
         *
         *                    // Input NBT Tag Compound, transform it
         *                    (compound -> {
         *                        // Get Item List
         *                        NBTTagList itemList = compound.getTagList("Items", Constants.NBT.TAG_COMPOUND);
         *                        // Iterate through, changing count to 64
         *                       for (int i = 0; i < itemList.tagList.size(); i++) {
         *                            if (!(itemList.get(i) instanceof NBTTagCompound item)) continue;
         *                            item.setInteger("Count", 64);
         *                            itemList.set(i, item);
         *                        }
         *                        // Item List is modified directly, no need to set it back
         *                    })));
         */
        tileEntityFixes = new ObjectArrayList<>();

        tileEntityFixes.add(
                new DataFix.TileEntityFix("Old Multiblock Tile Entity Meta ID Remap",
                        "Remaps old Multiblock Tile Entity Names to the new format.",
                        false,
                        (version) -> version <= DEFAULT_NOMI_CEU,
                        (modList) -> true,
                        (compound) -> compound.hasKey("MetaId", Constants.NBT.TAG_STRING) && compound.hasKey("id", Constants.NBT.TAG_STRING) &&
                                compound.getString("id").equals(new ResourceLocation(LabsValues.GREGTECH_MODID, "machine").toString()) &&
                                multiblockMetaIdRemap.containsKey(new ResourceLocation(compound.getString("MetaId"))),
                        (compound -> compound.setString("MetaId", multiblockMetaIdRemap.get(new ResourceLocation(compound.getString("MetaId"))).toString())))
        );

        fixes = new Object2ObjectOpenHashMap<>();
        fixes.put(LabsFixTypes.FixerTypes.ITEM, itemFixes);
        fixes.put(LabsFixTypes.FixerTypes.CHUNK, blockFixes);
        fixes.put(LabsFixTypes.FixerTypes.TILE_ENTITY, tileEntityFixes);
    }

    private static void helperMapsInit() {
        // Special Meta Items Remap is only needed for some meta items.
        // They are all placeable, and do not have the special syntax that includes a base id.
        // Therefore, they must be remapped.
        // Placeable forms are... stored in Tile Entity. Handled by TileEntityMaterialPipeBaseMixin, and Tile Entity Fix.
        specialMetaItemsRemap = new HashSet<>();
        // Wires and Cables
        // Fine Wire is not placeable, and thus follows normal meta item rules.
        for (var cableType : Insulation.VALUES) { // All Cable Types
            specialMetaItemsRemap.add(new ResourceLocation(GREGTECH_MODID, cableType.getName()));
        }
        // Fluid Pipes
        for (var fluidPipeType : FluidPipeType.VALUES) {
            specialMetaItemsRemap.add(new ResourceLocation(GREGTECH_MODID, String.format("fluid_pipe_%s", fluidPipeType.name)));
        }
        // Item Pipes
        for (var itemPipeType : ItemPipeType.VALUES) {
            specialMetaItemsRemap.add(new ResourceLocation(GREGTECH_MODID, String.format("item_pipe_%s", itemPipeType.name)));
        }
        // Compressed Blocks and Frames follow the special syntax.

        // Get all material names to know which tile entities to fix.
        materialNames = GregTechAPI.materialManager.getRegistry(LABS_MODID).getAllMaterials().stream().map(Material::getName).collect(Collectors.toSet());

        multiblockMetaRemap = new Short2ShortLinkedOpenHashMap();
        multiblockMetaRemap.put((short) 32000, (short) 32100); // Microverse 1
        multiblockMetaRemap.put((short) 32001, (short) 32101); // Microverse 2
        multiblockMetaRemap.put((short) 32002, (short) 32102); // Microverse 3
        if (LabsModeHelper.isNormal()) {
            multiblockMetaRemap.put((short) 32003, (short) 32103); // Creative Tank Provider
            multiblockMetaRemap.put((short) 32004, (short) 32104); // Naq Reactor 1
            multiblockMetaRemap.put((short) 32005, (short) 32105); // Naq Reactor 2
            multiblockMetaRemap.put((short) 3100, (short) 32108); // DME Sim Chamber
        }
        // In case it is some other mode, check if it is expert. This is only done here, as this specifically modifies data.
        if (LabsModeHelper.isExpert()) {
            multiblockMetaRemap.put((short) 32003, (short) 32104); // Naq Reactor 1
            multiblockMetaRemap.put((short) 32004, (short) 32105); // Naq Reactor 2
            multiblockMetaRemap.put((short) 32005, (short) 32106); // Actualization Chamber
            multiblockMetaRemap.put((short) 32006, (short) 32107); // Universal Crystallizer
        }

        multiblockMetaIdRemap = new Object2ObjectLinkedOpenHashMap<>();
        multiblockMetaIdRemap.put(
                new ResourceLocation(MBT_MODID, "microverse_projector_basic"), makeLabsName("microverse_projector_1")
        );
        multiblockMetaIdRemap.put(
                new ResourceLocation(MBT_MODID, "microverse_projector_advanced"), makeLabsName("microverse_projector_2")
        );
        multiblockMetaIdRemap.put(
                new ResourceLocation(MBT_MODID, "microverse_projector_advanced_ii"), makeLabsName("microverse_projector_3")
        );
        multiblockMetaIdRemap.put(
                new ResourceLocation(MBT_MODID, "creative_tank_provider"), makeLabsName("creative_tank_provider")
        );
        multiblockMetaIdRemap.put(
                new ResourceLocation(MULTIBLOCK_TWEAKER_MODID, "naquadah_reactor_1"), makeLabsName("naquadah_reactor_1")
        );
        multiblockMetaIdRemap.put(
                new ResourceLocation(MULTIBLOCK_TWEAKER_MODID, "naquadah_reactor_2"), makeLabsName("naquadah_reactor_2")
        );
        multiblockMetaIdRemap.put(
                new ResourceLocation(MULTIBLOCK_TWEAKER_MODID, "actualization_chamber"), makeLabsName("actualization_chamber")
        );
        multiblockMetaIdRemap.put(
                new ResourceLocation(MULTIBLOCK_TWEAKER_MODID, "universal_crystallizer"), makeLabsName("universal_crystallizer")
        );
        multiblockMetaIdRemap.put(
                new ResourceLocation(MULTIBLOCK_TWEAKER_MODID, "dml_sim_chamber"), makeLabsName("dme_sim_chamber")
        );

        capacitorSpecificationRemap = ImmutableMap.of(
                "compressedoctadiccapacitor",
                new OldCapacitorSpecification(4f, "Compressed Octadic RF Capacitor",
                        "This is what is known as a Compressed Octadic Capacitor.",
                        "Or, you could just call this an Octadic Capacitor Two.",
                        "Can be inserted into EnderIO machines.",
                        "Level: 4"),

                "doublecompressedoctadiccapacitor",
                new OldCapacitorSpecification(5f, "Double Compressed Octadic RF Capacitor",
                        "AND THIS IS TO GO EVEN FURTHER BEYOND!",
                        "Can be inserted into EnderIO machines.",
                        "Level: 9.001",
                        "Just kidding, it's only 5.")
        );
    }

    public static class OldCapacitorSpecification {
        private static final String EIO_KEY = "eiocap";
        private static final String EIO_LEVEL_KEY = "level";
        private static final String DISPLAY_KEY = "display";
        private static final String NAME_KEY = "Name";
        private static final String LORE_KEY = "Lore";
        private final float level;
        private final String name;
        private final String[] lore;

        private OldCapacitorSpecification(float level, String name, String... lore) {
            this.level = level;
            this.name = name;
            this.lore = lore;
        }

        public boolean needChange(@Nullable NBTTagCompound compound) {
            if (compound == null || compound.isEmpty()) return false;
            return testEIO(compound) || testName(compound) || testLore(compound);
        }

        @Nullable
        public NBTTagCompound remove(@Nullable NBTTagCompound compound) {
            if (compound == null || compound.isEmpty()) return null;
            removeEIO(compound);
            removeDisplay(compound);
            return compound.isEmpty() ? null : compound;
        }

        private void removeEIO(NBTTagCompound compound) {
            if (!testEIO(compound)) return;
            var eio = compound.getCompoundTag(EIO_KEY);
            eio.removeTag(EIO_LEVEL_KEY);
            if (eio.isEmpty()) compound.removeTag(EIO_KEY);
            else compound.setTag(EIO_KEY, eio);
        }

        private boolean testEIO(NBTTagCompound compound) {
            if (!compound.hasKey(EIO_KEY, Constants.NBT.TAG_COMPOUND)) return false;
            var eio = compound.getCompoundTag(EIO_KEY);
            if (!eio.hasKey(EIO_LEVEL_KEY, Constants.NBT.TAG_FLOAT)) return false;
            return eio.getFloat(EIO_LEVEL_KEY) == level;
        }

        private void removeDisplay(NBTTagCompound compound) {
            if (!compound.hasKey(DISPLAY_KEY, Constants.NBT.TAG_COMPOUND)) return;
            var display = compound.getCompoundTag(DISPLAY_KEY);
            if (testName(compound)) display.removeTag(NAME_KEY);
            if (testLore(compound)) display.removeTag(LORE_KEY);

            if (display.isEmpty()) compound.removeTag(DISPLAY_KEY);
            else compound.setTag(DISPLAY_KEY, display);
        }

        private boolean testName(NBTTagCompound compound) {
            if (!compound.hasKey(DISPLAY_KEY, Constants.NBT.TAG_COMPOUND)) return false;
            var display = compound.getCompoundTag(DISPLAY_KEY);
            if (!display.hasKey(NAME_KEY, Constants.NBT.TAG_STRING)) return false;
            return display.getString(NAME_KEY).equals(name);
        }

        private boolean testLore(NBTTagCompound compound) {
            if (!compound.hasKey(DISPLAY_KEY, Constants.NBT.TAG_COMPOUND)) return false;
            var display = compound.getCompoundTag(DISPLAY_KEY);
            if (!display.hasKey(LORE_KEY, Constants.NBT.TAG_LIST)) return false;
            var lore = display.getTagList(LORE_KEY, Constants.NBT.TAG_STRING);
            for (int i = 0; i < lore.tagList.size(); i++) {
                var tagStr = (NBTTagString) lore.tagList.get(i);
                var str = tagStr.getString();
                if (!str.equals(this.lore[i])) return false;
            }
            return true;
        }
    }
}
