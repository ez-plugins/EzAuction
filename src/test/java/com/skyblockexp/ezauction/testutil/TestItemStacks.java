package com.skyblockexp.ezauction.testutil;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

public final class TestItemStacks {

    private TestItemStacks() {
    }

    public static ItemStack mock(Material material, int amount) {
        ItemStack item = Mockito.mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenReturn(amount);
        when(item.getMaxStackSize()).thenReturn(64);
        when(item.clone()).thenAnswer(invocation -> item);
        return item;
    }
}
