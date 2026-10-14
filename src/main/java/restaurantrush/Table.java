package restaurantrush;

/** A table keeps its occupant until payment or abandonment. */
public class Table {
    private final int id;
    private Customer customer;

    public Table(int id) { this.id = id; }

    public void seat(Customer arrivingCustomer) {
        if (!isFree()) {
            throw new IllegalArgumentException("Table " + id + " is occupied.");
        }
        arrivingCustomer.seat();
        customer = arrivingCustomer;
    }

    public void release() {
        if (customer == null || (customer.getStatus() != CustomerStatus.PAID
                && customer.getStatus() != CustomerStatus.LEFT)) {
            throw new IllegalArgumentException("The table stays occupied until its customer pays or leaves.");
        }
        customer = null;
    }

    public int getId() { return id; }
    public Customer getCustomer() { return customer; }
    public boolean isFree() { return customer == null; }
}
