package com.steelstorm.arsenal.world.structure.build;

import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModLootTables;
import com.steelstorm.arsenal.weapon.Rune;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import com.steelstorm.arsenal.world.structure.SteelstormStructurePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A working smithy with an open front: anvil, furnaces, a lava forge pit, whetstone and weapon
 * racks, and a weaponsmith who sells Steelstorm gear. Sometimes it has a Rune Forge too.
 */
public class BlacksmithBuilder extends Builder {
    public BlacksmithBuilder(SteelstormStructurePiece piece, WorldGenLevel level, BoundingBox box, RandomSource random) {
        super(piece, level, box, random);
    }

    @Override
    public void build() {
        groundwork(Blocks.COBBLESTONE.defaultBlockState(), 11);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                float n = noise(x, g, z);
                set(x, g, z, z < 3 ? (n < 0.4F ? Blocks.DIRT_PATH.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState())
                        : (n < 0.3F ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState()));
            }
        }
        // Building: x 1..13, z 3..11. Front (z = 3) is an open porch under the roof.
        int x0 = 1;
        int x1 = w - 2;
        int z0 = 3;
        int z1 = d - 2;
        fill(x0, g, z0, x1, g, z1, Blocks.STONE_BRICKS.defaultBlockState());
        fill(x0 + 1, g, z0 + 1, x1 - 1, g, z1 - 1, Blocks.SPRUCE_PLANKS.defaultBlockState());
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
        for (int y = g + 1; y <= g + 4; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    boolean edge = x == x0 || x == x1 || z == z1;
                    boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                    boolean post = z == z0 && (x == x0 || x == x1 || x == (x0 + x1) / 2);
                    if (corner || post) {
                        set(x, y, z, log);
                    } else if (edge) {
                        boolean window = y == g + 2 && ((x == x0 || x == x1) && (z == z0 + 3 || z == z1 - 2));
                        set(x, y, z, window ? Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.NORTH, true)
                                .setValue(IronBarsBlock.SOUTH, true)
                                : y == g + 1 ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState());
                    }
                }
            }
        }
        // Plank ceiling, then a gable roof of spruce stairs with the ridge running along x.
        fill(x0, g + 5, z0, x1, g + 5, z1, Blocks.SPRUCE_PLANKS.defaultBlockState());
        int mid = (z0 + z1) / 2;
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int i = 0; z0 - 1 + i < mid; i++) {
                set(x, g + 5 + i, z0 - 1 + i, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
                set(x, g + 5 + i, z1 + 1 - i, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
            }
            set(x, g + 5 + (mid - z0), mid, Blocks.SPRUCE_PLANKS.defaultBlockState());
            set(x, g + 6 + (mid - z0), mid, Blocks.SPRUCE_SLAB.defaultBlockState());
        }
        // Gable ends.
        for (int i = 1; z0 - 1 + i < mid; i++) {
            for (int z = z0 - 1 + i + 1; z <= z1 + 1 - i - 1; z++) {
                set(x0, g + 5 + i, z, Blocks.SPRUCE_PLANKS);
                set(x1, g + 5 + i, z, Blocks.SPRUCE_PLANKS);
            }
        }
        // Brick chimney over the forge with a smoking campfire on top.
        int cx = x1 - 2;
        int cz = z1 - 1;
        for (int y = g + 1; y <= g + 10; y++) {
            set(cx, y, cz, Blocks.BRICKS);
        }
        set(cx, g + 11, cz, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.SIGNAL_FIRE, true));
        // The forge: a crucible of molten metal on a magma hearth (no loose lava to set the
        // smithy alight), furnaces either side, anvil in front, all on a stone floor.
        fill(cx - 3, g, cz - 3, cx + 1, g, cz, Blocks.POLISHED_ANDESITE.defaultBlockState());
        set(cx - 1, g, cz, Blocks.MAGMA_BLOCK);
        set(cx - 1, g + 1, cz, Blocks.LAVA_CAULDRON);
        set(cx - 2, g + 1, cz, Blocks.BLAST_FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.SOUTH)
                .setValue(AbstractFurnaceBlock.LIT, true));
        set(cx + 1, g + 1, cz, Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.SOUTH)
                .setValue(AbstractFurnaceBlock.LIT, true));
        set(cx - 1, g + 1, cz - 2, Blocks.ANVIL);
        set(cx + 1, g + 1, cz - 2, Blocks.CAULDRON);
        set(x0 + 1, g + 1, z1 - 1, Blocks.SMITHING_TABLE);
        set(x0 + 2, g + 1, z1 - 1, Blocks.GRINDSTONE.defaultBlockState().setValue(GrindstoneBlock.FACE, AttachFace.FLOOR)
                .setValue(GrindstoneBlock.FACING, Direction.SOUTH));
        set(x0 + 3, g + 1, z1 - 1, facing(ModBlocks.WHETSTONE.get(), Direction.SOUTH));
        if (noise(3, 3, 3) < 0.35F) {
            set(x0 + 1, g + 1, z1 - 3, ModBlocks.RUNE_FORGE.get());
        } else {
            set(x0 + 1, g + 1, z1 - 3, Blocks.BARREL);
        }
        chest(x0 + 1, g + 1, z0 + 2, ModLootTables.BLACKSMITH);
        set(x0 + 1, g + 1, z0 + 1, Blocks.BARREL);
        // Weapons for sale on the porch.
        rack(x0 + 2, g + 1, z0, Direction.SOUTH, weapon(WeaponType.LONGSWORD, WeaponTier.IRON), weapon(WeaponType.KATANA, WeaponTier.IRON),
                weapon(WeaponType.DUAL_DAGGERS, WeaponTier.IRON));
        rack(x1 - 2, g + 1, z0, Direction.SOUTH, weapon(WeaponType.SPEAR, WeaponTier.IRON), ItemStack.EMPTY,
                weapon(WeaponType.BATTLEAXE, WeaponTier.IRON));
        set((x0 + x1) / 2, g + 4, z0 + 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set((x0 + x1) / 2, g + 4, z1 - 3, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // Woodpile outside.
        BlockState logX = Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        fill(x1 + 1, g + 1, z1 - 3, x1 + 1, g + 2, z1 - 1, logX);
        statue(x1 - 1, g + 1, z0 + 2, Direction.SOUTH, new ItemStack(Items.IRON_HELMET), new ItemStack(Items.CHAINMAIL_CHESTPLATE),
                new ItemStack(Items.CHAINMAIL_LEGGINGS), new ItemStack(Items.IRON_BOOTS), weapon(WeaponType.GREATSWORD, WeaponTier.IRON));
        weaponsmith((x0 + x1) / 2, g + 1, (z0 + z1) / 2);
    }

    private WeaponType randomType() {
        return WeaponType.values()[random.nextInt(WeaponType.values().length)];
    }

    private void weaponsmith(int x, int y, int z) {
        BlockPos pos = world(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        Villager villager = EntityType.VILLAGER.create(level.getLevel());
        if (villager == null) {
            return;
        }
        villager.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        villager.setVillagerData(villager.getVillagerData().setType(VillagerType.byBiome(level.getBiome(pos)))
                .setProfession(VillagerProfession.WEAPONSMITH).setLevel(2));
        villager.setVillagerXp(10);
        // A travelling smith stocks Steelstorm gear from the start.
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 6), weapon(WeaponType.LONGSWORD, WeaponTier.IRON), 4, 5, 0.05F));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 7), weapon(randomType(), WeaponTier.IRON), 4, 5, 0.05F));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 3), new ItemStack(ModItems.THROWING_KNIFE.get(), 4), 8, 3, 0.05F));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 4), new ItemStack(ModItems.WHETSTONE.get()), 4, 5, 0.05F));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 12),
                new ItemStack(ModItems.rune(Rune.values()[random.nextInt(Rune.values().length)]).get()), 2, 10, 0.05F));
        offers.add(new MerchantOffer(new ItemCost(Items.IRON_INGOT, 6), new ItemStack(Items.EMERALD), 12, 2, 0.05F));
        villager.setOffers(offers);
        villager.setPersistenceRequired();
        level.addFreshEntityWithPassengers(villager);
    }
}
