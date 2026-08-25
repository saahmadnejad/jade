package io.donbee.jade.examples.shop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class InventoryTest {

    @Test
    public void Given_ConfiguredStock_When_ReserveWithinAvailability_Then_StockDecrementedAndTrueReturned() {
        // --- Arrange ---
        Inventory inventory = Inventory.fromConfig("sku-1:10,sku-2:5", 3);

        // --- Act ---
        boolean reserved = inventory.reserve("sku-1", 4);

        // --- Assert ---
        assertThat(reserved).isTrue();
        assertThat(inventory.getStock("sku-1")).isEqualTo(6);
    }

    @Test
    public void Given_InsufficientOrUnknownStock_When_Reserve_Then_FalseAndNoChange() {
        // --- Arrange ---
        Inventory inventory = Inventory.fromConfig("sku-1:2", 3);

        // --- Act / Assert ---
        assertThat(inventory.reserve("sku-1", 3)).isFalse();
        assertThat(inventory.reserve("sku-unknown", 1)).isFalse();
        assertThat(inventory.reserve("sku-1", 0)).isFalse();
        assertThat(inventory.reserve("sku-1", -5)).isFalse();
        assertThat(inventory.getStock("sku-1")).isEqualTo(2);
    }

    @Test
    public void Given_MalformedConfigEntries_When_FromConfig_Then_EntriesSkippedWithoutFailure() {
        // --- Arrange / Act ---
        Inventory inventory = Inventory.fromConfig("sku-1:10,broken,sku-2:notanumber,,sku-3:2", 3);

        // --- Assert ---
        assertThat(inventory.snapshot()).containsOnlyKeys("sku-1", "sku-3");
        assertThat(inventory.getStock("sku-1")).isEqualTo(10);
    }

    @Test
    public void Given_SkuAtOrBelowThreshold_When_LowStockChecked_Then_ReturnedAscendingByRemaining() {
        // --- Arrange ---
        Inventory inventory = Inventory.fromConfig("a:10,b:3,c:0,d:7", 3);

        // --- Act ---
        var low = inventory.lowStockSkus();

        // --- Assert ---
        assertThat(low).containsExactly("c", "b");
    }

    @Test
    public void Given_RestockDelivery_When_AddStock_Then_QuantityMerged() {
        // --- Arrange ---
        Inventory inventory = Inventory.fromConfig("sku-1:4", 3);

        // --- Act ---
        inventory.addStock("sku-1", 20);

        // --- Assert ---
        assertThat(inventory.getStock("sku-1")).isEqualTo(24);
    }

    @Test
    public void Given_RestockBelowThresholdExceeded_When_AddStock_Then_SkuLeavesLowStockList() {
        // --- Arrange ---
        Inventory inventory = Inventory.fromConfig("sku-1:2", 3);
        assertThat(inventory.lowStockSkus()).containsExactly("sku-1");

        // --- Act ---
        inventory.addStock("sku-1", 20);

        // --- Assert ---
        assertThat(inventory.lowStockSkus()).isEmpty();
    }
}
