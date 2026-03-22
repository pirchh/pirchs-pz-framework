package pirch.pz.bridge;

import pirch.pz.service.BankService;
import pirch.pz.service.PlayerIdentity;
import pirch.pz.service.PlayerIdentityService;
import pirch.pzloader.runtime.BridgeMethodDefinition;
import pirch.pzloader.runtime.BridgeResult;
import pirch.pzloader.runtime.ModuleRegistry;

public final class BankBridge {
    private BankBridge() {
    }

    public static void register() {
        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.bank.getBalance")
                .description("Returns player wallet balance")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = PlayerIdentityService.fromBridgeArg(args[0]);
                    return BridgeResult.ok(BankService.getBalance(identity));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.bank.deposit")
                .description("Deposits currency into a player wallet")
                .minArgCount(2)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = PlayerIdentityService.fromBridgeArg(args[0]);
                    int amount = Integer.parseInt(String.valueOf(args[1]));
                    return BridgeResult.ok(BankService.deposit(identity, amount));
                } catch (NumberFormatException e) {
                    return BridgeResult.fail("amount must be a valid integer");
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.bank.withdraw")
                .description("Withdraws currency from a player wallet")
                .minArgCount(2)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = PlayerIdentityService.fromBridgeArg(args[0]);
                    int amount = Integer.parseInt(String.valueOf(args[1]));
                    return BridgeResult.ok(BankService.withdraw(identity, amount));
                } catch (NumberFormatException e) {
                    return BridgeResult.fail("amount must be a valid integer");
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );
    }
}
