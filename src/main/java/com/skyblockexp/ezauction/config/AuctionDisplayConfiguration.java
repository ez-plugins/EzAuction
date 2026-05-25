package com.skyblockexp.ezauction.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration for display formatting — date/time patterns and currency templates.
 *
 * <p>Loaded from the {@code display:} section of {@code auction.yml}.</p>
 */
public final class AuctionDisplayConfiguration {

    private static final String DEFAULT_DATE_FORMAT = "dd-MM-yy HH:mm:ss";

    private final String dateFormat;
    /** Template for currency amounts. Use {@code {amount}} as placeholder. Empty = use Vault/default. */
    private final String currencyTemplate;
    /** {@link java.text.DecimalFormat} number pattern for {@code {amount}} inside the template. */
    private final String currencyNumberFormat;

    public AuctionDisplayConfiguration(String dateFormat,
            String currencyTemplate,
            String currencyNumberFormat) {
        this.dateFormat = (dateFormat != null && !dateFormat.isBlank())
                ? dateFormat : DEFAULT_DATE_FORMAT;
        this.currencyTemplate = (currencyTemplate != null && !currencyTemplate.isBlank())
                ? currencyTemplate : "";
        this.currencyNumberFormat = (currencyNumberFormat != null && !currencyNumberFormat.isBlank())
                ? currencyNumberFormat : "#,##0.00";
    }

    public String dateFormat() { return dateFormat; }

    /**
     * Currency display template; empty string means "use Vault {@code economy.format()} or locale default".
     */
    public String currencyTemplate() { return currencyTemplate; }

    public String currencyNumberFormat() { return currencyNumberFormat; }

    public static AuctionDisplayConfiguration defaults() {
        return new AuctionDisplayConfiguration(DEFAULT_DATE_FORMAT, "", "#,##0.00");
    }

    public static AuctionDisplayConfiguration from(ConfigurationSection section) {
        if (section == null) return defaults();
        String dateFormat = section.getString("date-format", DEFAULT_DATE_FORMAT);
        String currencyTemplate = section.getString("currency-format", "");
        String currencyNumberFormat = section.getString("currency-number-format", "#,##0.00");
        return new AuctionDisplayConfiguration(dateFormat, currencyTemplate, currencyNumberFormat);
    }
}
