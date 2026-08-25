package io.donbee.jade.examples.shop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import io.donbee.jade.rest.scenario.AgentSpec;
import io.donbee.jade.rest.scenario.ScenarioParam;

public class ShopScenarioTest {

    private final ShopScenario scenario = new ShopScenario();

    /** Build a complete config: declared defaults plus key/value overrides. */
    private Map<String, Object> config(Object... overrides) {
        Map<String, Object> result = new HashMap<>();
        for (ScenarioParam p : scenario.params()) {
            result.put(p.getName(), p.getDefaultValue());
        }
        for (int i = 0; i < overrides.length; i += 2) {
            result.put((String) overrides[i], overrides[i + 1]);
        }
        return result;
    }

    @Test
    public void Given_Defaults_When_AgentsComputed_Then_CoreAgentsAndConfiguredCustomerCount() {
        // --- Act ---
        var specs = scenario.agents(config());

        // --- Assert ---
        assertThat(specs).extracting(AgentSpec::getNameSuffix)
            .containsExactly("shop", "inventory", "supplier", "customer1", "customer2");
    }

    @Test
    public void Given_CustomStockAndThreshold_When_AgentsComputed_Then_InventoryArgsReflectConfig() {
        // --- Act ---
        var specs = scenario.agents(config(
            "initialStock", 25,
            "restockThreshold", 7,
            "restockQuantity", 40,
            "inventoryCheckIntervalSec", 30));

        // --- Assert ---
        AgentSpec inventory = specs.get(1);
        assertThat(inventory.getClassName()).isEqualTo(ShopScenario.INVENTORY_CLASS);
        assertThat(inventory.getArgs()).containsExactly(
            "sku-1:25|sku-2:25|sku-3:25", "7", "30", "40");
    }

    @Test
    public void Given_CustomerCountZero_When_AgentsComputed_Then_NoCustomerAgents() {
        // --- Act ---
        var specs = scenario.agents(config("customerCount", 0));

        // --- Assert ---
        assertThat(specs).hasSize(3); // shop, inventory, supplier only
    }

    @Test
    public void Given_SupplierDelay_When_AgentsComputed_Then_SupplierArgReflectsConfig() {
        // --- Act ---
        var specs = scenario.agents(config("shippingDelaySec", 12));

        // --- Assert ---
        assertThat(specs.get(2).getArgs()).containsExactly("12");
    }
}
