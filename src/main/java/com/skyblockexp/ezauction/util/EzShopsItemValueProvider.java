package com.skyblockexp.ezauction.util;

import com.skyblockexp.ezauction.config.AuctionValueConfiguration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.OptionalDouble;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * {@link ItemValueProvider} implementation that bridges EzAuction to EzShops pricing data.
 */
public final class EzShopsItemValueProvider implements ItemValueProvider {

    private static final String SHOP_PRICE_SERVICE_CLASS_NAME = "com.skyblockexp.shop.api.ShopPriceService";

    private final Logger logger;
    private final Object priceService;
    private final Method buyPriceMethod;
    private final Method sellPriceMethod;
    private final AuctionValueConfiguration.Mode mode;

    private EzShopsItemValueProvider(
            Logger logger, Object priceService, Method buyPriceMethod, Method sellPriceMethod,
            AuctionValueConfiguration.Mode mode) {
        this.logger = logger;
        this.priceService = priceService;
        this.buyPriceMethod = buyPriceMethod;
        this.sellPriceMethod = sellPriceMethod;
        this.mode = mode;
    }

    public static ItemValueProvider create(JavaPlugin plugin, AuctionValueConfiguration.Mode mode) {
        if (plugin == null || mode == null) {
            return null;
        }

        if (plugin.getServer() == null) {
            return null;
        }
        ServicesManager servicesManager = plugin.getServer().getServicesManager();
        if (servicesManager == null) {
            return null;
        }

        Class<?> serviceClass = findShopPriceServiceClass(servicesManager);
        if (serviceClass == null) {
            return null;
        }

        RegisteredServiceProvider<?> registration = findRegistration(servicesManager, serviceClass);
        if (registration == null || registration.getProvider() == null) {
            return null;
        }

        Method buyMethod = findMethod(serviceClass, "findBuyPrice");
        Method sellMethod = findMethod(serviceClass, "findSellPrice");
        if (buyMethod == null || sellMethod == null) {
            return null;
        }

        return new EzShopsItemValueProvider(plugin.getLogger(), registration.getProvider(), buyMethod, sellMethod, mode);
    }

    private static Class<?> findShopPriceServiceClass(ServicesManager servicesManager) {
        try {
            Collection<Class<?>> knownServices = servicesManager.getKnownServices();
            if (knownServices == null) {
                return null;
            }
            for (Class<?> serviceClass : knownServices) {
                if (serviceClass != null && SHOP_PRICE_SERVICE_CLASS_NAME.equals(serviceClass.getName())) {
                    return serviceClass;
                }
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private static RegisteredServiceProvider<?> findRegistration(ServicesManager servicesManager, Class<?> serviceClass) {
        try {
            @SuppressWarnings("unchecked")
            RegisteredServiceProvider<?> registration = servicesManager.getRegistration((Class) serviceClass);
            return registration;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> serviceClass, String methodName) {
        try {
            return serviceClass.getMethod(methodName, ItemStack.class);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    @Override
    public OptionalDouble estimate(ItemStack itemStack) {
        if (priceService == null || itemStack == null) {
            return OptionalDouble.empty();
        }

        Method method = mode == AuctionValueConfiguration.Mode.EZSHOPS_BUY ? buyPriceMethod : sellPriceMethod;
        if (method == null) {
            return OptionalDouble.empty();
        }

        try {
            Object result = method.invoke(priceService, itemStack);
            if (result instanceof OptionalDouble optional) {
                return optional;
            }
        } catch (IllegalAccessException | InvocationTargetException ex) {
            logQueryFailure(ex.getCause() != null ? ex.getCause() : ex);
        } catch (RuntimeException ex) {
            logQueryFailure(ex);
        }
        return OptionalDouble.empty();
    }

    private void logQueryFailure(Throwable throwable) {
        if (logger != null && throwable != null) {
            logger.warning("Failed to query EzShops price service: " + throwable.getMessage());
        }
    }
}
