package com.skyblockexp.ezauction.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOrder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class AuctionMenuSortTest {

    @Test
    void listingQuantitySortPlacesLargerStacksFirst() throws ReflectiveOperationException {
        Enum<?> sort = getEnumConstant("com.skyblockexp.ezauction.gui.AuctionMenu$ListingSort", "QUANTITY_HIGH_LOW");
        Method method = sort.getClass().getDeclaredMethod("sort", List.class);
        method.setAccessible(true);

        List<AuctionListing> listings = new ArrayList<>();
        listings.add(createListing("single", Material.DIAMOND, 1, 50.0D, 30));
        listings.add(createListing("stack", Material.DIAMOND, 64, 40.0D, 20));
        listings.add(createListing("handful", Material.DIAMOND, 16, 45.0D, 10));

        invokeSort(method, sort, listings);

        assertEquals("stack", listings.get(0).id());
        assertEquals("handful", listings.get(1).id());
        assertEquals("single", listings.get(2).id());
    }

    @Test
    void orderNameSortArrangesAlphabetically() throws ReflectiveOperationException {
        Enum<?> sort = getEnumConstant("com.skyblockexp.ezauction.gui.AuctionMenu$OrderSort", "ITEM_A_Z");
        Method method = sort.getClass().getDeclaredMethod("sort", List.class);
        method.setAccessible(true);

        List<AuctionOrder> orders = new ArrayList<>();
        orders.add(createOrder("emerald", Material.EMERALD, 16, 150.0D, 40));
        orders.add(createOrder("apple", Material.APPLE, 8, 25.0D, 30));
        orders.add(createOrder("diamond", Material.DIAMOND, 4, 300.0D, 20));

        invokeSort(method, sort, orders);

        assertEquals("apple", orders.get(0).id());
        assertEquals("diamond", orders.get(1).id());
        assertEquals("emerald", orders.get(2).id());
    }

    private AuctionListing createListing(String id, Material material, int amount, double price, long minutesUntilExpiry) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getAmount()).thenReturn(amount);
        when(stack.getType()).thenReturn(material);
        try {
            when(stack.clone()).thenReturn(stack);
        } catch (Exception ignored) {
        }
        long expiry = System.currentTimeMillis() + Duration.ofMinutes(minutesUntilExpiry).toMillis();
        return new AuctionListing(id, UUID.randomUUID(), price, expiry, stack, 0.0D);
    }

    private AuctionOrder createOrder(String id, Material material, int amount, double offeredPrice, long minutesUntilExpiry) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getAmount()).thenReturn(amount);
        when(stack.getType()).thenReturn(material);
        try {
            when(stack.clone()).thenReturn(stack);
        } catch (Exception ignored) {
        }
        long expiry = System.currentTimeMillis() + Duration.ofMinutes(minutesUntilExpiry).toMillis();
        return new AuctionOrder(id, UUID.randomUUID(), offeredPrice, expiry, stack, offeredPrice * amount);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Enum<?> getEnumConstant(String className, String constant) throws ClassNotFoundException {
        Class enumClass = Class.forName(className);
        return Enum.valueOf(enumClass, constant);
    }

    private void invokeSort(Method method, Enum<?> sort, List<?> entries)
            throws IllegalAccessException, InvocationTargetException {
        method.invoke(sort, entries);
    }
}
