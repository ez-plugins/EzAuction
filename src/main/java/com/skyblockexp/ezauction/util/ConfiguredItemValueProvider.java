package com.skyblockexp.ezauction.util;

import com.skyblockexp.ezauction.config.AuctionValueConfiguration;
import java.util.Objects;
import java.util.OptionalDouble;
import org.bukkit.inventory.ItemStack;

/**
 * Default {@link ItemValueProvider} that sources estimates from {@link AuctionValueConfiguration}.
 */
public final class ConfiguredItemValueProvider implements ItemValueProvider {

    private final AuctionValueConfiguration configuration;

    public ConfiguredItemValueProvider(AuctionValueConfiguration configuration) {
        this.configuration = configuration != null ? configuration : AuctionValueConfiguration.defaults();
    }

    @Override
    public OptionalDouble estimate(ItemStack itemStack) {
        if (configuration == null) {
            return OptionalDouble.empty();
        }
        return configuration.estimate(itemStack);
    }

    public AuctionValueConfiguration configuration() {
        return configuration;
    }

    @Override
    public String toString() {
        return "ConfiguredItemValueProvider{" + "configuration=" + configuration + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConfiguredItemValueProvider that)) {
            return false;
        }
        return Objects.equals(configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configuration);
    }
}
