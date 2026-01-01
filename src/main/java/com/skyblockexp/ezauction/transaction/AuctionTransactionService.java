package com.skyblockexp.ezauction.transaction;

import com.skyblockexp.ezauction.AuctionOperationResult;

import com.skyblockexp.ezauction.config.AuctionBackendMessages;
import com.skyblockexp.ezauction.util.EconomyUtils;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles money movement for auction listings.
 */
public class AuctionTransactionService {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    private final JavaPlugin plugin;
    private final Economy economy;
    private final AuctionBackendMessages.EconomyMessages messages;
    private final AuctionBackendMessages.FallbackMessages fallbackMessages;

    public AuctionTransactionService(JavaPlugin plugin, Economy economy,
            AuctionBackendMessages.EconomyMessages messages,
            AuctionBackendMessages.FallbackMessages fallbackMessages) {
        this.plugin = plugin;
        this.economy = economy;
        this.messages = messages != null ? messages : AuctionBackendMessages.EconomyMessages.defaults();
        this.fallbackMessages = fallbackMessages != null
                ? fallbackMessages
                : AuctionBackendMessages.FallbackMessages.defaults();
    }

    public AuctionOperationResult chargeListingDeposit(Player seller, double deposit) {
        double normalized = EconomyUtils.normalizeCurrency(deposit);
        if (normalized <= 0 || economy == null) {
            if (economy == null && normalized > 0) {
                return AuctionOperationResult.failure(formatMessage(messages.noProvider()));
            }
            return AuctionOperationResult.success("");
        }

        if (economy.getBalance(seller) < normalized) {
            return AuctionOperationResult.failure(formatMessage(messages.depositInsufficient(),
                    "amount", formatCurrency(normalized)));
        }

        EconomyResponse response = economy.withdrawPlayer(seller, normalized);
        if (!response.transactionSuccess()) {
            return AuctionOperationResult.failure(formatMessage(messages.transactionFailed(),
                    "error", normalizeError(response.errorMessage)));
        }

        return AuctionOperationResult.success("");
    }

    public AuctionOperationResult withdrawBuyer(Player buyer, double amount) {
        double normalized = EconomyUtils.normalizeCurrency(amount);
        if (normalized <= 0) {
            return AuctionOperationResult.failure(formatMessage(messages.invalidPrice()));
        }

        if (economy == null) {
            return AuctionOperationResult.failure(formatMessage(messages.noProvider()));
        }

        if (economy.getBalance(buyer) < normalized) {
            return AuctionOperationResult.failure(formatMessage(messages.purchaseInsufficient()));
        }

        EconomyResponse response = economy.withdrawPlayer(buyer, normalized);
        if (!response.transactionSuccess()) {
            return AuctionOperationResult.failure(formatMessage(messages.transactionFailed(),
                    "error", normalizeError(response.errorMessage)));
        }

        return AuctionOperationResult.success("");
    }

    public AuctionOperationResult creditSeller(UUID sellerId, double amount) {
        double normalized = EconomyUtils.normalizeCurrency(amount);
        if (normalized <= 0) {
            return AuctionOperationResult.success("");
        }

        if (economy == null) {
            return AuctionOperationResult.failure(formatMessage(messages.noProvider()));
        }

        OfflinePlayer seller = plugin.getServer().getOfflinePlayer(sellerId);
        EconomyResponse response = economy.depositPlayer(seller, normalized);
        if (!response.transactionSuccess()) {
            return AuctionOperationResult.failure(formatMessage(messages.transactionFailed(),
                    "error", normalizeError(response.errorMessage)));
        }

        return AuctionOperationResult.success("");
    }

    public AuctionOperationResult reserveOrderFunds(Player buyer, double amount) {
        double normalized = EconomyUtils.normalizeCurrency(amount);
        if (normalized <= 0.0D) {
            return AuctionOperationResult.failure(formatMessage(messages.reservePositive()));
        }

        if (economy == null) {
            return AuctionOperationResult.failure(formatMessage(messages.noProvider()));
        }

        if (economy.getBalance(buyer) < normalized) {
            return AuctionOperationResult.failure(formatMessage(messages.reserveInsufficient()));
        }

        EconomyResponse response = economy.withdrawPlayer(buyer, normalized);
        if (!response.transactionSuccess()) {
            return AuctionOperationResult.failure(formatMessage(messages.transactionFailed(),
                    "error", normalizeError(response.errorMessage)));
        }

        return AuctionOperationResult.success("");
    }

    public void refundOrderBuyer(UUID buyerId, double amount) {
        double normalized = EconomyUtils.normalizeCurrency(amount);
        if (economy == null || normalized <= 0.0D || buyerId == null) {
            return;
        }

        OfflinePlayer buyer = plugin.getServer().getOfflinePlayer(buyerId);
        EconomyResponse response = economy.depositPlayer(buyer, normalized);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("Failed to refund buy order funds: " + response.errorMessage);
        }
    }

    public AuctionOperationResult payOrderSeller(UUID sellerId, double amount) {
        double normalized = EconomyUtils.normalizeCurrency(amount);
        if (normalized <= 0.0D) {
            return AuctionOperationResult.success("");
        }

        if (economy == null) {
            return AuctionOperationResult.failure(formatMessage(messages.noProvider()));
        }

        OfflinePlayer seller = plugin.getServer().getOfflinePlayer(sellerId);
        EconomyResponse response = economy.depositPlayer(seller, normalized);
        if (!response.transactionSuccess()) {
            return AuctionOperationResult.failure(formatMessage(messages.transactionFailed(),
                    "error", normalizeError(response.errorMessage)));
        }

        return AuctionOperationResult.success("");
    }

    public void refundBuyer(Player buyer, double amount) {
        double normalized = EconomyUtils.normalizeCurrency(amount);
        if (economy == null || normalized <= 0) {
            return;
        }

        EconomyResponse response = economy.depositPlayer(buyer, normalized);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning(
                    "Failed to refund buyer after unsuccessful auction purchase: " + response.errorMessage);
        }
    }

    public void refundListingDeposit(UUID sellerId, double deposit) {
        double normalized = EconomyUtils.normalizeCurrency(deposit);
        if (economy == null || normalized <= 0) {
            return;
        }

        OfflinePlayer seller = plugin.getServer().getOfflinePlayer(sellerId);
        EconomyResponse response = economy.depositPlayer(seller, normalized);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning(
                    "Failed to refund auction listing deposit: " + response.errorMessage);
        }
    }

    public String formatCurrency(double amount) {
        double normalized = EconomyUtils.normalizeCurrency(amount);
        if (economy != null) {
            return economy.format(normalized);
        }
        synchronized (CURRENCY_FORMAT) {
            return CURRENCY_FORMAT.format(normalized);
        }
    }

    private String formatMessage(String template, String... replacements) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        String formatted = template;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String key = replacements[i];
            String value = replacements[i + 1];
            formatted = formatted.replace("{" + key + "}", value != null ? value : "");
        }
        return colorize(formatted);
    }

    private String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String normalizeError(String error) {
        if (error == null || error.isBlank()) {
            return fallbackMessages.unknownError();
        }
        return error;
    }
}
