package io.donbee.jade.examples.shop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plain stock-keeping logic of the inventory, decoupled from agent
 * behaviour so it can be unit-tested without a running platform.
 *
 * Thread-safe: behaviours run on the same agent thread, but the class
 * guards its state anyway to be safe for reuse.
 */
public class Inventory {

    private static final io.donbee.jade.util.Logger LOG =
        io.donbee.jade.util.Logger.getJADELogger(Inventory.class.getName());

    private final Map<String, Integer> stock = new HashMap<>();
    private final int restockThreshold;

    public Inventory(int restockThreshold) {
        this.restockThreshold = Math.max(0, restockThreshold);
    }

    /**
     * Parse an initial stock configuration like {@code "sku-1:10,sku-2:5"}
     * or {@code "sku-1:10|sku-2:5"} (the latter survives the Boot agent
     * specifier's comma splitting). Malformed entries are skipped.
     */
    public static Inventory fromConfig(String csvConfig, int restockThreshold) {
        Inventory inventory = new Inventory(restockThreshold);
        if (csvConfig != null && !csvConfig.isBlank()) {
            for (String entry : csvConfig.split("[,|]")) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    try {
                        // Initial stock may legitimately be 0 (item currently unavailable)
                        int quantity = Integer.parseInt(parts[1].trim());
                        if (quantity >= 0 && !parts[0].isBlank()) {
                            inventory.stock.put(parts[0].trim(), quantity);
                        }
                    } catch (NumberFormatException ignored) {
                        // Skip malformed quantity
                    }
                }
            }
        }
        return inventory;
    }

    /**
     * Try to reserve (sell) the given quantity of a SKU.
     *
     * @return true when the stock was sufficient and has been decremented
     */
    public synchronized boolean reserve(String sku, int quantity) {
        int available = stock.getOrDefault(sku, 0);
        if (quantity <= 0 || available < quantity) {
            return false;
        }
        stock.put(sku, available - quantity);
        return true;
    }

    public synchronized void addStock(String sku, int quantity) {
        if (sku != null && !sku.isBlank() && quantity > 0) {
            stock.merge(sku, quantity, Integer::sum);
        }
    }

    public synchronized int getStock(String sku) {
        return stock.getOrDefault(sku, 0);
    }

    /** SKUs at or below the restock threshold, ascending by remaining stock. */
    public synchronized List<String> lowStockSkus() {
        List<String> low = new ArrayList<>();
        for (Map.Entry<String, Integer> e : stock.entrySet()) {
            if (e.getValue() <= restockThreshold) {
                low.add(e.getKey());
            }
        }
        low.sort((a, b) -> Integer.compare(stock.get(a), stock.get(b)));
        return low;
    }

    /** Copy of the current SKU -> quantity map. */
    public synchronized Map<String, Integer> snapshot() {
        return new HashMap<>(stock);
    }

    public synchronized int getRestockThreshold() {
        return restockThreshold;
    }
}
