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
    public static final String MONEY_FULL_TYPE = "PirchsPZDBI.PirchCash";
    public static final String WALLET_FULL_TYPE = "PirchsPZDBI.PirchWallet";

    private CashInventoryService() {
    }

    public static CashInventorySnapshot snapshotLocalPlayerMoney() {
        IsoPlayer player = requireLocalPlayer();
        ItemContainer inventory = player.getInventory();
        int loose = countMoneyRecursive(inventory, true, false);
        int wallet = countMoneyRecursive(inventory, false, true);
        int walletCount = countWalletContainers(inventory);
        return new CashInventorySnapshot(
            loose,
            wallet,
            loose + wallet,
            walletCount,
            MONEY_FULL_TYPE,
            WALLET_FULL_TYPE
        );
    }

    public static int countLocalPlayerMoney() {
        return snapshotLocalPlayerMoney().totalMoney();
    }

    public static int countLooseLocalPlayerMoney() {
        return snapshotLocalPlayerMoney().looseMoney();
    }

    public static int countWalletLocalPlayerMoney() {
        return snapshotLocalPlayerMoney().walletMoney();
    }

    public static int removeLocalPlayerMoney(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("deposit amount must be greater than 0");
        }

        IsoPlayer player = requireLocalPlayer();
        ItemContainer inventory = player.getInventory();
        int carried = countMoneyRecursive(inventory, true, true);
        if (carried < amount) {
            throw new IllegalStateException("You tried to deposit more money than you are carrying");
        }

        List<InventoryItem> moneyItems = new ArrayList<>();
        collectMoneyItemsRecursive(inventory, moneyItems, true, true);
        removeFromCollectedItems(moneyItems, amount);
        return amount;
    }

    public static int removeLooseLocalPlayerMoney(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("wallet transfer amount must be greater than 0");
        }

        IsoPlayer player = requireLocalPlayer();
        ItemContainer inventory = player.getInventory();
        int loose = countMoneyRecursive(inventory, true, false);
        if (loose < amount) {
            throw new IllegalStateException("You tried to move more loose cash than you are carrying");
        }

        List<InventoryItem> moneyItems = new ArrayList<>();
        collectMoneyItemsRecursive(inventory, moneyItems, true, false);
        removeFromCollectedItems(moneyItems, amount);
        return amount;
    }

    public static int removeWalletLocalPlayerMoney(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("wallet transfer amount must be greater than 0");
        }

        IsoPlayer player = requireLocalPlayer();
        ItemContainer inventory = player.getInventory();
        int walletCash = countMoneyRecursive(inventory, false, true);
        if (walletCash < amount) {
            throw new IllegalStateException("You tried to move more wallet cash than the wallet contains");
        }

        List<InventoryItem> moneyItems = new ArrayList<>();
        collectMoneyItemsRecursive(inventory, moneyItems, false, true);
        removeFromCollectedItems(moneyItems, amount);
        return amount;
    }

    public static int moveLooseLocalPlayerMoneyToWallet(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("wallet transfer amount must be greater than 0");
        }

        CashInventorySnapshot snapshot = snapshotLocalPlayerMoney();
        if (snapshot.walletCount() <= 0) {
            throw new IllegalStateException("You are not carrying a wallet");
        }
        if (snapshot.looseMoney() < amount) {
            throw new IllegalStateException("You tried to move more loose cash than you are carrying");
        }

        int removed = removeLooseLocalPlayerMoney(amount);
        int stored = giveLocalPlayerMoneyToWallet(removed);
        if (stored != removed) {
            int returned = removed - stored;
            if (returned > 0) {
                giveLocalPlayerMoney(returned);
            }
            throw new IllegalStateException("Unable to place requested cash into wallet");
        }
        return stored;
    }

    public static int moveWalletLocalPlayerMoneyToInventory(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("wallet transfer amount must be greater than 0");
        }

        int removed = removeWalletLocalPlayerMoney(amount);
        int granted = giveLocalPlayerMoney(removed);
        if (granted != removed) {
            throw new IllegalStateException("Unable to move requested wallet cash back into inventory");
        }
        return granted;
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

    public static int giveLocalPlayerMoneyToWallet(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("wallet transfer amount must be greater than 0");
        }

        IsoPlayer player = requireLocalPlayer();
        List<ItemContainer> wallets = new ArrayList<>();
        collectWalletContainers(player.getInventory(), wallets);
        if (wallets.isEmpty()) {
            throw new IllegalStateException("You are not carrying a wallet");
        }

        int granted = 0;
        int walletIndex = 0;
        for (int i = 0; i < amount; i++) {
            InventoryItem item = InventoryItemFactory.CreateItem(MONEY_FULL_TYPE);
            if (item == null) {
                throw new IllegalStateException("failed to create cash item: " + MONEY_FULL_TYPE);
            }

            InventoryItem added = null;
            for (int attempt = 0; attempt < wallets.size(); attempt++) {
                ItemContainer wallet = wallets.get((walletIndex + attempt) % wallets.size());
                added = wallet.AddItem(item);
                if (added != null) {
                    walletIndex = (walletIndex + attempt + 1) % wallets.size();
                    break;
                }
            }

            if (added == null) {
                break;
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

    private static void removeFromCollectedItems(List<InventoryItem> moneyItems, int amount) {
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
    }

    private static int countWalletContainers(ItemContainer container) {
        if (container == null) {
            return 0;
        }

        int total = 0;
        List<?> items = container.getItems();
        for (Object raw : items) {
            if (!(raw instanceof InventoryItem item)) {
                continue;
            }

            if (isWalletItem(item)) {
                total++;
            }

            if (item instanceof InventoryContainer inventoryContainer && inventoryContainer.getItemContainer() != null) {
                total += countWalletContainers(inventoryContainer.getItemContainer());
            }
        }
        return total;
    }

    private static int countMoneyRecursive(ItemContainer container, boolean includeLoose, boolean includeWallet) {
        return countMoneyRecursive(container, false, includeLoose, includeWallet);
    }

    private static int countMoneyRecursive(ItemContainer container, boolean insideWallet, boolean includeLoose, boolean includeWallet) {
        if (container == null) {
            return 0;
        }

        int total = 0;
        List<?> items = container.getItems();
        for (Object raw : items) {
            if (!(raw instanceof InventoryItem item)) {
                continue;
            }

            boolean itemInsideWallet = insideWallet;
            if (MONEY_FULL_TYPE.equals(item.getFullType())) {
                if ((itemInsideWallet && includeWallet) || (!itemInsideWallet && includeLoose)) {
                    total += Math.max(1, item.getCount());
                }
            }

            if (item instanceof InventoryContainer inventoryContainer && inventoryContainer.getItemContainer() != null) {
                boolean childInsideWallet = itemInsideWallet || isWalletItem(item);
                total += countMoneyRecursive(inventoryContainer.getItemContainer(), childInsideWallet, includeLoose, includeWallet);
            }
        }
        return total;
    }

    private static void collectMoneyItemsRecursive(ItemContainer container, List<InventoryItem> sink, boolean includeLoose, boolean includeWallet) {
        collectMoneyItemsRecursive(container, sink, false, includeLoose, includeWallet);
    }

    private static void collectMoneyItemsRecursive(
        ItemContainer container,
        List<InventoryItem> sink,
        boolean insideWallet,
        boolean includeLoose,
        boolean includeWallet
    ) {
        if (container == null) {
            return;
        }

        List<?> items = container.getItems();
        for (Object raw : items) {
            if (!(raw instanceof InventoryItem item)) {
                continue;
            }

            boolean itemInsideWallet = insideWallet;
            if (MONEY_FULL_TYPE.equals(item.getFullType())) {
                if ((itemInsideWallet && includeWallet) || (!itemInsideWallet && includeLoose)) {
                    sink.add(item);
                }
            }

            if (item instanceof InventoryContainer inventoryContainer && inventoryContainer.getItemContainer() != null) {
                boolean childInsideWallet = itemInsideWallet || isWalletItem(item);
                collectMoneyItemsRecursive(inventoryContainer.getItemContainer(), sink, childInsideWallet, includeLoose, includeWallet);
            }
        }
    }

    private static void collectWalletContainers(ItemContainer container, List<ItemContainer> sink) {
        if (container == null) {
            return;
        }

        List<?> items = container.getItems();
        for (Object raw : items) {
            if (!(raw instanceof InventoryItem item)) {
                continue;
            }

            if (item instanceof InventoryContainer inventoryContainer && inventoryContainer.getItemContainer() != null) {
                if (isWalletItem(item)) {
                    sink.add(inventoryContainer.getItemContainer());
                }
                collectWalletContainers(inventoryContainer.getItemContainer(), sink);
            }
        }
    }

    private static boolean isWalletItem(InventoryItem item) {
        return item != null && WALLET_FULL_TYPE.equals(item.getFullType());
    }
}
