package com.skyblockexp.ezauction.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class AuctionValueConfigurationTest {

    @Test
    void defaultsAutoDetectShopPriceDisplay() {
        AuctionValueConfiguration configuration = AuctionValueConfiguration.defaults();

        AuctionValueConfiguration.ShopPriceConfiguration shopPriceConfiguration =
                configuration.shopPriceConfiguration();

        assertNotNull(shopPriceConfiguration);
        assertTrue(shopPriceConfiguration.autoDetect());
        assertFalse(shopPriceConfiguration.enabled());
        assertEquals("&7Shop Price: &6{value}", shopPriceConfiguration.format());
        assertEquals(AuctionValueConfiguration.Mode.EZSHOPS_BUY, shopPriceConfiguration.mode());
    }

    @Test
    void readsShopPriceConfigurationFromSection() {
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        yamlConfiguration.set("enabled", true);
        yamlConfiguration.set("format", "&7Value: &6{value}");
        yamlConfiguration.set("mode", "configured");

        YamlConfiguration shopPriceSection = new YamlConfiguration();
        shopPriceSection.set("enabled", true);
        shopPriceSection.set("format", "&7Shop: &6{value}");
        shopPriceSection.set("mode", "ezshops-sell");
        yamlConfiguration.createSection("shop-price", shopPriceSection.getValues(false));

        AuctionValueConfiguration configuration = AuctionValueConfiguration.from(yamlConfiguration);
        AuctionValueConfiguration.ShopPriceConfiguration shopPriceConfiguration =
                configuration.shopPriceConfiguration();

        assertNotNull(shopPriceConfiguration);
        assertTrue(shopPriceConfiguration.enabled());
        assertFalse(shopPriceConfiguration.autoDetect());
        assertEquals("&7Shop: &6{value}", shopPriceConfiguration.format());
        assertEquals(AuctionValueConfiguration.Mode.EZSHOPS_SELL, shopPriceConfiguration.mode());
    }

    @Test
    void invalidShopPriceModeFallsBackToDefault() {
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        YamlConfiguration shopPriceSection = new YamlConfiguration();
        shopPriceSection.set("enabled", true);
        shopPriceSection.set("mode", "configured");
        yamlConfiguration.createSection("shop-price", shopPriceSection.getValues(false));

        AuctionValueConfiguration configuration = AuctionValueConfiguration.from(yamlConfiguration);
        AuctionValueConfiguration.ShopPriceConfiguration shopPriceConfiguration =
                configuration.shopPriceConfiguration();

        assertEquals(AuctionValueConfiguration.Mode.EZSHOPS_BUY, shopPriceConfiguration.mode());
    }
}
