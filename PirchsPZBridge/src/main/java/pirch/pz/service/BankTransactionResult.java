package pirch.pz.service;

public final class BankTransactionResult {
    private final String action;
    private final int requestedAmount;
    private final int processedAmount;
    private final int balanceBefore;
    private final int balanceAfter;
    private final int carriedBefore;
    private final int carriedAfter;
    private final int looseBefore;
    private final int looseAfter;
    private final int walletBefore;
    private final int walletAfter;
    private final boolean success;
    private final String error;

    public BankTransactionResult(
        String action,
        int requestedAmount,
        int processedAmount,
        int balanceBefore,
        int balanceAfter,
        int carriedBefore,
        int carriedAfter,
        int looseBefore,
        int looseAfter,
        int walletBefore,
        int walletAfter,
        boolean success,
        String error
    ) {
        this.action = action;
        this.requestedAmount = requestedAmount;
        this.processedAmount = processedAmount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.carriedBefore = carriedBefore;
        this.carriedAfter = carriedAfter;
        this.looseBefore = looseBefore;
        this.looseAfter = looseAfter;
        this.walletBefore = walletBefore;
        this.walletAfter = walletAfter;
        this.success = success;
        this.error = error;
    }

    public String getAction() {
        return action;
    }

    public int getRequestedAmount() {
        return requestedAmount;
    }

    public int getProcessedAmount() {
        return processedAmount;
    }

    public int getBalanceBefore() {
        return balanceBefore;
    }

    public int getBalanceAfter() {
        return balanceAfter;
    }

    public int getCarriedBefore() {
        return carriedBefore;
    }

    public int getCarriedAfter() {
        return carriedAfter;
    }

    public int getLooseBefore() {
        return looseBefore;
    }

    public int getLooseAfter() {
        return looseAfter;
    }

    public int getWalletBefore() {
        return walletBefore;
    }

    public int getWalletAfter() {
        return walletAfter;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        return "BankTransactionResult[action=" + action
            + ", requestedAmount=" + requestedAmount
            + ", processedAmount=" + processedAmount
            + ", balanceBefore=" + balanceBefore
            + ", balanceAfter=" + balanceAfter
            + ", carriedBefore=" + carriedBefore
            + ", carriedAfter=" + carriedAfter
            + ", looseBefore=" + looseBefore
            + ", looseAfter=" + looseAfter
            + ", walletBefore=" + walletBefore
            + ", walletAfter=" + walletAfter
            + ", success=" + success
            + (error == null ? "" : ", error=" + error)
            + "]";
    }
}
