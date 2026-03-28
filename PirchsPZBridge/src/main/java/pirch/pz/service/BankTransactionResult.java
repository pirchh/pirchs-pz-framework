package pirch.pz.service;

public final class BankTransactionResult {
    private final String action;
    private final int requestedAmount;
    private final int processedAmount;
    private final int balanceBefore;
    private final int balanceAfter;
    private final int carriedBefore;
    private final int carriedAfter;
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
            + ", success=" + success
            + (error == null ? "" : ", error=" + error)
            + "]";
    }
}
