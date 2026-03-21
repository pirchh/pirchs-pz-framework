package pirch.pz.bridge;

import pirch.pz.service.BankService;
import pirch.pzloader.runtime.BridgeResult;
import pirch.pzloader.runtime.ModuleRegistry;

public final class BankBridge {
    private BankBridge() {
    }

    public static void register() {
        ModuleRegistry.register("pz.bridge.bank.getBalance", (args) -> {
            if (args == null || args.length < 1 || args[0] == null) {
                return BridgeResult.fail("Missing playerId");
            }

            String playerId = String.valueOf(args[0]).trim();
            if (playerId.isEmpty()) {
                return BridgeResult.fail("playerId cannot be empty");
            }

            try {
                return BridgeResult.ok(BankService.getBalance(playerId));
            } catch (Exception e) {
                return BridgeResult.fail(e.getMessage());
            }
        });

        ModuleRegistry.register("pz.bridge.bank.deposit", (args) -> {
            if (args == null || args.length < 2 || args[0] == null || args[1] == null) {
                return BridgeResult.fail("Missing playerId or amount");
            }

            String playerId = String.valueOf(args[0]).trim();
            if (playerId.isEmpty()) {
                return BridgeResult.fail("playerId cannot be empty");
            }

            try {
                int amount = Integer.parseInt(String.valueOf(args[1]));
                return BridgeResult.ok(BankService.deposit(playerId, amount));
            } catch (NumberFormatException e) {
                return BridgeResult.fail("amount must be a valid integer");
            } catch (Exception e) {
                return BridgeResult.fail(e.getMessage());
            }
        });

        ModuleRegistry.register("pz.bridge.bank.withdraw", (args) -> {
            if (args == null || args.length < 2 || args[0] == null || args[1] == null) {
                return BridgeResult.fail("Missing playerId or amount");
            }

            String playerId = String.valueOf(args[0]).trim();
            if (playerId.isEmpty()) {
                return BridgeResult.fail("playerId cannot be empty");
            }

            try {
                int amount = Integer.parseInt(String.valueOf(args[1]));
                return BridgeResult.ok(BankService.withdraw(playerId, amount));
            } catch (NumberFormatException e) {
                return BridgeResult.fail("amount must be a valid integer");
            } catch (Exception e) {
                return BridgeResult.fail(e.getMessage());
            }
        });
    }
}