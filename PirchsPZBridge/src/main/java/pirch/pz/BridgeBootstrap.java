package pirch.pz;

import pirch.pz.bridge.BankBridge;
import pirch.pz.bridge.SystemBridge;
import pirch.pz.db.SchemaManager;
import pirch.pzloader.bootstrap.LoaderBootstrap;
import pirch.pzloader.runtime.BridgeResult;
import pirch.pzloader.runtime.InvocationDispatcher;
import pirch.pzloader.util.LoaderLog;

public final class BridgeBootstrap {
    private static boolean initialized = false;

    private BridgeBootstrap() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            LoaderLog.info("PirchsPZBridge already initialized.");
            return;
        }

        LoaderBootstrap.initialize();
        SchemaManager.initialize();
        SystemBridge.register();
        BankBridge.register();

        initialized = true;
        LoaderLog.info("PirchsPZBridge initialization complete.");

        BridgeResult pingResult = InvocationDispatcher.invoke("pz.bridge.system.ping");
        LoaderLog.info("Ping result: " + pingResult);

        BridgeResult dbPingResult = InvocationDispatcher.invoke("pz.bridge.system.dbPing");
        LoaderLog.info("DB ping result: " + dbPingResult);

        BridgeResult accountResult = InvocationDispatcher.invoke(
            "pz.bridge.system.resolveAccount",
            "testPlayer",
            "Test Player"
        );
        LoaderLog.info("Resolve account result: " + accountResult);

        BridgeResult depositResult = InvocationDispatcher.invoke("pz.bridge.bank.deposit", "testPlayer", 50);
        LoaderLog.info("Deposit result: " + depositResult);

        BridgeResult balanceResult = InvocationDispatcher.invoke("pz.bridge.bank.getBalance", "testPlayer");
        LoaderLog.info("Balance result: " + balanceResult);
    }
}