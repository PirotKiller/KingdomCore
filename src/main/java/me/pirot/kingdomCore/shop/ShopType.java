package me.pirot.kingdomCore.shop;

/**
 * All shop types and their configuration keys.
 */
public enum ShopType {

    // Shard-only shops
    WOOD("wood", CurrencyMode.SHARDS),
    STONE("stone", CurrencyMode.SHARDS),
    FISHERMAN("fisherman", CurrencyMode.SHARDS),
    FLETCHER("fletcher", CurrencyMode.SHARDS),
    REDSTONE("redstone", CurrencyMode.SHARDS),
    FARMING("farming", CurrencyMode.SHARDS),

    // Gem-only shops (Removed Classes)

    // Dual-currency shops
    ENCHANT("enchant", CurrencyMode.DUAL),
    POTION("potion", CurrencyMode.DUAL),
    NETHER("nether", CurrencyMode.DUAL),
    END("end", CurrencyMode.DUAL),
    ARMOR("armor", CurrencyMode.DUAL),

    // Special
    CONVERTER("converter", CurrencyMode.SHARDS);

    private final String configKey;
    private final CurrencyMode currencyMode;

    ShopType(String configKey, CurrencyMode currencyMode) {
        this.configKey = configKey;
        this.currencyMode = currencyMode;
    }

    public String getConfigKey() {
        return configKey;
    }

    public CurrencyMode getCurrencyMode() {
        return currencyMode;
    }

    public static ShopType fromString(String name) {
        try {
            return ShopType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public enum CurrencyMode {
        SHARDS, GEMS, DUAL
    }
}
