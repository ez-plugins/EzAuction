package com.skyblockexp.shop.api;

import java.util.OptionalDouble;
import org.bukkit.inventory.ItemStack;

public interface ShopPriceService {

    OptionalDouble findBuyPrice(ItemStack itemStack);

    OptionalDouble findSellPrice(ItemStack itemStack);
}
