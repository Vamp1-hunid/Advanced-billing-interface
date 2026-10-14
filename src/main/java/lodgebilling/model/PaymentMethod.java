package lodgebilling.model;

/** How a payment was made. Card payments are taken on the lodge's own card machine. */
public enum PaymentMethod {
    CASH("Cash"),
    CARD("Card (POS machine)"),
    MOBILE_MONEY("Mobile money"),
    BANK_TRANSFER("Bank transfer");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
