package restaurantrush;

/** A more patient customer who receives a 10% discount. */
public class VIPCustomer extends Customer {
    public VIPCustomer(int id) { super(id); }

    @Override
    public String getTypeName() { return "VIP"; }

    @Override
    public int getWaitingSatisfactionLoss() { return 5; }

    @Override
    protected int calculateBillCents(MenuItem item) {
        // Round 90% of the price to the nearest cent, with halves rounded up.
        return (item.getPriceCents() * 90 + 50) / 100;
    }
}
