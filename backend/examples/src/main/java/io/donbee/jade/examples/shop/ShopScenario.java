package io.donbee.jade.examples.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.donbee.jade.rest.scenario.AgentSpec;
import io.donbee.jade.rest.scenario.Scenario;
import io.donbee.jade.rest.scenario.ScenarioParam;

/**
 * Scenario template for the online-shop example.
 *
 * <p>Registered via
 * {@code META-INF/services/io.donbee.jade.rest.scenario.Scenario} so the
 * platform discovers it through ServiceLoader and offers it on the scenarios
 * API / UI.</p>
 *
 * <p><b>Old GUI implementation:</b> No direct Swing GUI equivalent; this is a
 * Jade-specific demo scenario.</p>
 */
public class ShopScenario implements Scenario {

    static final String SHOP_CLASS = ShopAgent.class.getName();
    static final String INVENTORY_CLASS = InventoryAgent.class.getName();
    static final String SUPPLIER_CLASS = RestockSupplierAgent.class.getName();
    static final String CUSTOMER_CLASS = CustomerAgent.class.getName();

    private static final List<String> SKUS = List.of("sku-1", "sku-2", "sku-3");

    @Override
    public String id() {
        return "online-shop";
    }

    @Override
    public String title() {
        return "Online Shop";
    }

    @Override
    public String description() {
        return "Customers buy products from a storefront agent; the storefront reserves "
            + "stock in the warehouse and the warehouse auto-restocks low items from a "
            + "supplier. Demonstrates FIPA-REQUEST conversations and DF service discovery.";
    }

    @Override
    public List<ScenarioParam> params() {
        return List.of(
            ScenarioParam.intParam("initialStock", 10, 0, 1000,
                "Starting quantity of each product"),
            ScenarioParam.intParam("restockThreshold", 3, 0, 100,
                "Reorder when an item drops to or below this quantity"),
            ScenarioParam.intParam("restockQuantity", 20, 1, 500,
                "Amount ordered from the supplier per restock"),
            ScenarioParam.intParam("inventoryCheckIntervalSec", 10, 2, 3600,
                "Seconds between warehouse low-stock checks"),
            ScenarioParam.intParam("customerCount", 2, 0, 20,
                "Number of simulated customer agents"),
            ScenarioParam.intParam("purchaseIntervalSec", 8, 1, 3600,
                "Seconds between two purchase attempts of a customer"),
            ScenarioParam.intParam("shippingDelaySec", 5, 0, 600,
                "Supplier shipping delay in seconds"));
    }

    @Override
    public List<AgentSpec> agents(Map<String, Object> config) {
        int initialStock = (Integer) config.get("initialStock");
        int restockThreshold = (Integer) config.get("restockThreshold");
        int restockQuantity = (Integer) config.get("restockQuantity");
        int checkInterval = (Integer) config.get("inventoryCheckIntervalSec");
        int customerCount = (Integer) config.get("customerCount");
        int purchaseInterval = (Integer) config.get("purchaseIntervalSec");
        int shippingDelay = (Integer) config.get("shippingDelaySec");

        String stockConfig = String.join("|",
            SKUS.stream().map(sku -> sku + ":" + initialStock).toList());

        List<AgentSpec> specs = new ArrayList<>();
        specs.add(new AgentSpec("shop", SHOP_CLASS, List.of()));
        specs.add(new AgentSpec("inventory", INVENTORY_CLASS, List.of(
            stockConfig,
            String.valueOf(restockThreshold),
            String.valueOf(checkInterval),
            String.valueOf(restockQuantity))));
        specs.add(new AgentSpec("supplier", SUPPLIER_CLASS, List.of(
            String.valueOf(shippingDelay))));
        for (int i = 1; i <= customerCount; i++) {
            List<Object> args = new ArrayList<>();
            args.add(String.valueOf(purchaseInterval));
            args.add("1");
            args.addAll(SKUS);
            specs.add(new AgentSpec("customer" + i, CUSTOMER_CLASS, args));
        }
        return specs;
    }
}
