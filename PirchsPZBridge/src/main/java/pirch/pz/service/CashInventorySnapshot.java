package pirch.pz.service;

public record CashInventorySnapshot(
    int looseMoney,
    int walletMoney,
    int totalMoney,
    int walletCount,
    String moneyFullType,
    String walletFullType
) {
}
