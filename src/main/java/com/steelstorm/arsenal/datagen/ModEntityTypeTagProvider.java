package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModEntities;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModEntityTypeTagProvider extends EntityTypeTagsProvider {
    public ModEntityTypeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        super(output, lookup, SteelstormArsenal.MODID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Undead: Smite works on them and healing potions hurt them.
        tag(EntityTypeTags.UNDEAD).add(ModEntities.IRON_REVENANT.get(), ModEntities.FALLEN_WARLORD.get(), ModEntities.CRYPT_KNIGHT.get());
        tag(EntityTypeTags.SENSITIVE_TO_SMITE).add(ModEntities.IRON_REVENANT.get(), ModEntities.FALLEN_WARLORD.get(),
                ModEntities.CRYPT_KNIGHT.get());
        tag(EntityTypeTags.INVERTED_HEALING_AND_HARM).add(ModEntities.IRON_REVENANT.get(), ModEntities.FALLEN_WARLORD.get(),
                ModEntities.CRYPT_KNIGHT.get());
        tag(EntityTypeTags.ILLAGER_FRIENDS).add(ModEntities.BANDIT_DUELIST.get(), ModEntities.BANDIT_ARCHER.get(),
                ModEntities.BANDIT_CAPTAIN.get());
    }
}
