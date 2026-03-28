package pirch.pz.service;

import java.util.ArrayList;
import java.util.List;

import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemUser;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;

public final class CashInventoryService {
    public static final String MONEY_FULL_TYPE = "Base.Money";

    private CashInventoryService() {
    }

    public static int countLocalPlayerMoney() {
        IsoPlayer player = requireLocalPlayer();
        return countMoneyRecursive(player.getInventory());
    }

    public static int removeLocalPlayerMoney(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("deposit amount must be greater than 0");
        }

        IsoPlayer player = requireLocalPlayer();
        ItemContainer inventory = player.getInventory();
        int carried = countMoneyRecursive(inventory);
        if (carried < amount) {
            throw new IllegalStateException("You tried to deposit more money than you are carrying");
        }

        List<InventoryItem> moneyItems = new ArrayList<>();
        collectMoneyItemsRecursive(inventory, moneyItems);

        int remaining = amount;
        for (InventoryItem item : moneyItems) {
            if (remaining <= 0) {
                break;
            }

            int count = Math.max(1, item.getCount());
            if (count <= remaining) {
                remaining -= count;
                ItemUser.RemoveItem(item);
            } else {
                item.setCount(count - remaining);
                remaining = 0;
            }
        }

        if (remaining != 0) {
            throw new IllegalStateException("failed to remove requested amount of carried money");
        }

        return amount;
    }

    public static int giveLocalPlayerMoney(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("withdraw amount must be greater than 0");
        }

        IsoPlayer player = requireLocalPlayer();
        ItemContainer inventory = player.getInventory();
        IsoGridSquare square = player.getCurrentSquare();

        int granted = 0;
        for (int i = 0; i < amount; i++) {
            InventoryItem item = InventoryItemFactory.CreateItem(MONEY_FULL_TYPE);
            if (item == null) {
                throw new IllegalStateException("failed to create cash item: " + MONEY_FULL_TYPE);
            }

            InventoryItem added = inventory.AddItem(item);
            if (added == null) {
                if (square == null) {
                    throw new IllegalStateException("Unable to place withdrawn cash in inventory");
                }
                square.AddWorldInventoryItem(item, 0.5f, 0.5f, 0.0f);
            }
            granted++;
        }

        return granted;
    }

    private static IsoPlayer requireLocalPlayer() {
        IsoPlayer player = IsoPlayer.getInstance();
        if (player == null) {
            throw new IllegalStateException("local player is not available");
        }
        if (player.getInventory() == null) {
            throw new IllegalStateException("local player inventory is not available");
        }
        return player;
    }

    private static int countMoneyRecursive(ItemContainer container) {
        if (container == null) {
            return 0;
        }

        int total = 0;
        List<?> items = container.getItems();
        for (Object raw : items) {
            if (!(raw instanceof InventoryItem item)) {
                continue;
            }

            if (MONEY_FULL_TYPE.equals(item.getFullType())) {
                total += Math.max(1, item.getCount());
            }

            if (item instanceof InventoryContainer inventoryContainer && inventoryContainer.getItemContainer() != null) {
                total += countMoneyRecursive(inventoryContainer.getItemContainer());
            }
        }
        return total;
    }

    private static void collectMoneyItemsRecursive(ItemContainer container, List<InventoryItem> sink) {
        if (container == null) {
            return;
        }

        List<?> items = container.getItems();
        for (Object raw : items) {
            if (!(raw instanceof InventoryItem item)) {
                continue;
            }

            if (MONEY_FULL_TYPE.equals(item.getFullType())) {
                sink.add(item);
            }

            if (item instanceof InventoryContainer inventoryContainer && inventoryContainer.getItemContainer() != null) {
                collectMoneyItemsRecursive(inventoryContainer.getItemContainer(), sink);
            }
        }
    }
}
