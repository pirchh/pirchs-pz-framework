package pirch.pz.bridge;

import pirch.pz.service.BankService;
import pirch.pz.service.BankSnapshot;
import pirch.pz.service.BankTransactionResult;
import pirch.pz.service.CashInventorySnapshot;
import pirch.pz.service.LocalPlayerIdentityResolver;
import pirch.pz.service.PlayerIdentity;
import pirch.pz.service.PlayerIdentityService;
import pirch.pzloader.runtime.BridgeMethodDefinition;
import pirch.pzloader.runtime.BridgeResult;
import pirch.pzloader.runtime.ModuleRegistry;

public final class BankBridge {
    private BankBridge() {
    }

    public static void register() {
        registerExplicitMethods();
        registerSelfMethods();
        registerInventoryMoneyMethods();
        registerDebugAliases();
    }

    private static void registerExplicitMethods() {
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

    private static void registerSelfMethods() {
        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.bank.getBalanceSelf")
                .description("Returns the resolved local player's wallet balance")
                .minArgCount(0)
                .build(),
            args -> {
                try {
                    return BridgeResult.ok(
                        BankService.getBalance(LocalPlayerIdentityResolver.requireLocalIdentity())
                    );
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.bank.depositSelf")
                .description("Deposits currency into the resolved local player's wallet")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    int amount = Integer.parseInt(String.valueOf(args[0]));
                    return BridgeResult.ok(
                        BankService.deposit(LocalPlayerIdentityResolver.requireLocalIdentity(), amount)
                    );
                } catch (NumberFormatException e) {
                    return BridgeResult.fail("amount must be a valid integer");
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.bank.withdrawSelf")
                .description("Withdraws currency from the resolved local player's wallet")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    int amount = Integer.parseInt(String.valueOf(args[0]));
                    return BridgeResult.ok(
                        BankService.withdraw(LocalPlayerIdentityResolver.requireLocalIdentity(), amount)
                    );
                } catch (NumberFormatException e) {
                    return BridgeResult.fail("amount must be a valid integer");
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.bank.snapshotSelf")
                .description("Returns wallet snapshot for the resolved local player")
                .minArgCount(0)
                .build(),
            args -> {
                try {
                    BankSnapshot snapshot = BankService.localSnapshot();
                    return BridgeResult.ok(snapshot);
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );
    }

    private static void registerInventoryMoneyMethods() {
        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.bank.getCarriedMoneySelf")
                .description("Returns the amount of carried PirchCash across inventory and wallet containers")
                .minArgCount(0)
                .build(),
            args -> {
                try {
                    return BridgeResult.ok(BankService.countCarriedMoney());
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.bank.getCashInventorySnapshotSelf")
                .description("Returns loose cash, wallet cash, total cash, and wallet counts for the resolved local player")
                .minArgCount(0)
                .build(),
            args -> {
                try {
                    CashInventorySnapshot snapshot = BankService.getCashInventorySnapshot();
                    return BridgeResult.ok(snapshot);
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.bank.depositCarriedMoneySelf")
                .description("Deposits a typed amount of carried PirchCash into the resolved local player's wallet")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    int amount = Integer.parseInt(String.valueOf(args[0]));
                    BankTransactionResult result = BankService.depositCarriedMoney(
                        LocalPlayerIdentityResolver.requireLocalIdentity(),
                        amount
                    );
                    return BridgeResult.ok(result);
                } catch (NumberFormatException e) {
                    return BridgeResult.fail("amount must be a valid integer");
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.bank.depositAllCarriedMoneySelf")
                .description("Deposits all carried PirchCash into the resolved local player's wallet")
                .minArgCount(0)
                .build(),
            args -> {
                try {
                    BankTransactionResult result = BankService.depositAllCarriedMoney(
                        LocalPlayerIdentityResolver.requireLocalIdentity()
                    );
                    return BridgeResult.ok(result);
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.bank.withdrawCashToInventorySelf")
                .description("Withdraws wallet funds into PirchCash items in the local player's inventory")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    int amount = Integer.parseInt(String.valueOf(args[0]));
                    BankTransactionResult result = BankService.withdrawCashToInventory(
                        LocalPlayerIdentityResolver.requireLocalIdentity(),
                        amount
                    );
                    return BridgeResult.ok(result);
                } catch (NumberFormatException e) {
                    return BridgeResult.fail("amount must be a valid integer");
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.bank.moveCashToWalletSelf")
                .description("Moves loose PirchCash from the local inventory into carried wallet containers")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    int amount = Integer.parseInt(String.valueOf(args[0]));
                    BankTransactionResult result = BankService.moveCashToWallet(
                        LocalPlayerIdentityResolver.requireLocalIdentity(),
                        amount
                    );
                    return BridgeResult.ok(result);
                } catch (NumberFormatException e) {
                    return BridgeResult.fail("amount must be a valid integer");
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.bank.moveAllCashToWalletSelf")
                .description("Moves all loose PirchCash from the local inventory into carried wallet containers")
                .minArgCount(0)
                .build(),
            args -> {
                try {
                    BankTransactionResult result = BankService.moveAllLooseCashToWallet(
                        LocalPlayerIdentityResolver.requireLocalIdentity()
                    );
                    return BridgeResult.ok(result);
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.bank.moveCashFromWalletSelf")
                .description("Moves PirchCash from carried wallet containers back into the local inventory")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    int amount = Integer.parseInt(String.valueOf(args[0]));
                    BankTransactionResult result = BankService.moveCashFromWallet(
                        LocalPlayerIdentityResolver.requireLocalIdentity(),
                        amount
                    );
                    return BridgeResult.ok(result);
                } catch (NumberFormatException e) {
                    return BridgeResult.fail("amount must be a valid integer");
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.bank.moveAllCashFromWalletSelf")
                .description("Moves all PirchCash from carried wallet containers back into the local inventory")
                .minArgCount(0)
                .build(),
            args -> {
                try {
                    BankTransactionResult result = BankService.moveAllWalletCashToInventory(
                        LocalPlayerIdentityResolver.requireLocalIdentity()
                    );
                    return BridgeResult.ok(result);
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );
    }

    private static void registerDebugAliases() {
        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.getBalance")
                .description("Debug alias for local wallet balance")
                .minArgCount(0)
                .build(),
            args -> invokeZeroArg("pz.bridge.bank.getBalanceSelf")
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.depositSelf")
                .description("Debug alias for depositing into local wallet")
                .minArgCount(1)
                .build(),
            args -> invokeOneArg("pz.bridge.bank.depositSelf", args[0])
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.withdrawSelf")
                .description("Debug alias for withdrawing from local wallet")
                .minArgCount(1)
                .build(),
            args -> invokeOneArg("pz.bridge.bank.withdrawSelf", args[0])
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.bankSnapshot")
                .description("Debug alias for local bank snapshot")
                .minArgCount(0)
                .build(),
            args -> invokeZeroArg("pz.bridge.bank.snapshotSelf")
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.getCarriedMoney")
                .description("Debug alias for carried PirchCash")
                .minArgCount(0)
                .build(),
            args -> invokeZeroArg("pz.bridge.bank.getCarriedMoneySelf")
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.getCashInventorySnapshot")
                .description("Debug alias for loose-vs-wallet PirchCash snapshot")
                .minArgCount(0)
                .build(),
            args -> invokeZeroArg("pz.bridge.bank.getCashInventorySnapshotSelf")
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.depositCarriedMoneySelf")
                .description("Debug alias for depositing carried PirchCash")
                .minArgCount(1)
                .build(),
            args -> invokeOneArg("pz.bridge.bank.depositCarriedMoneySelf", args[0])
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.depositAllCarriedMoneySelf")
                .description("Debug alias for depositing all carried PirchCash")
                .minArgCount(0)
                .build(),
            args -> invokeZeroArg("pz.bridge.bank.depositAllCarriedMoneySelf")
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.withdrawCashToInventorySelf")
                .description("Debug alias for withdrawing wallet funds into PirchCash")
                .minArgCount(1)
                .build(),
            args -> invokeOneArg("pz.bridge.bank.withdrawCashToInventorySelf", args[0])
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.moveCashToWalletSelf")
                .description("Debug alias for moving loose PirchCash into wallet containers")
                .minArgCount(1)
                .build(),
            args -> invokeOneArg("pz.bridge.bank.moveCashToWalletSelf", args[0])
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.moveAllCashToWalletSelf")
                .description("Debug alias for moving all loose PirchCash into wallet containers")
                .minArgCount(0)
                .build(),
            args -> invokeZeroArg("pz.bridge.bank.moveAllCashToWalletSelf")
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.moveCashFromWalletSelf")
                .description("Debug alias for moving PirchCash out of wallet containers")
                .minArgCount(1)
                .build(),
            args -> invokeOneArg("pz.bridge.bank.moveCashFromWalletSelf", args[0])
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.moveAllCashFromWalletSelf")
                .description("Debug alias for moving all PirchCash out of wallet containers")
                .minArgCount(0)
                .build(),
            args -> invokeZeroArg("pz.bridge.bank.moveAllCashFromWalletSelf")
        );
    }

    private static BridgeResult invokeZeroArg(String methodName) {
        if (!ModuleRegistry.has(methodName)) {
            return BridgeResult.fail("Bridge method not registered: " + methodName);
        }

        Object result = ModuleRegistry.get(methodName).invoke(new Object[0]);
        if (result instanceof BridgeResult bridgeResult) {
            return bridgeResult;
        }
        return BridgeResult.fail("Bridge method did not return BridgeResult: " + methodName);
    }

    private static BridgeResult invokeOneArg(String methodName, Object arg) {
        if (!ModuleRegistry.has(methodName)) {
            return BridgeResult.fail("Bridge method not registered: " + methodName);
        }

        Object result = ModuleRegistry.get(methodName).invoke(new Object[] { arg });
        if (result instanceof BridgeResult bridgeResult) {
            return bridgeResult;
        }
        return BridgeResult.fail("Bridge method did not return BridgeResult: " + methodName);
    }
}
