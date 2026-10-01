package com.steelstorm.arsenal.world.outpost;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

/** The written book placed in the Warrior's Outpost that teaches the controls. */
public final class CombatManual {
    private static final int PAGES = 9;

    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = new ArrayList<>();
        for (int i = 1; i <= PAGES; i++) {
            pages.add(Filterable.passThrough(page(i)));
        }
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough("Steelstorm Combat Manual"), "Outpost Quartermaster", 0, pages, true));
        return book;
    }

    private static Component page(int index) {
        String base = "book.steelstorm.manual.page" + index;
        MutableComponent title = Component.translatable(base + ".title").withStyle(s -> s.withBold(true).withColor(0x7A1F1F));
        // Key names resolve on the reader's client, so rebinding a key updates the book too.
        Object[] keys = {
                key("key.steelstorm.dodge"), key("key.steelstorm.special"), key("key.attack"), key("key.use"), key("key.sneak")
        };
        return Component.empty().append(title).append("\n\n").append(Component.translatable(base + ".body", keys));
    }

    private static Component key(String mapping) {
        return Component.keybind(mapping).withStyle(s -> s.withBold(true).withColor(0x1F4E7A));
    }

    private CombatManual() {
    }
}
