package pirch.pz.service;

public record BankSnapshot(
    PlayerIdentity identity,
    int balance,
    boolean localIdentity,
    String source
) {
}
