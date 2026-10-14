package restaurantrush;

/** Owns preparation progress and the dish's service lifecycle. */
public class Order {
    private final MenuItem item;
    private final int billCents;
    private int completedUnits;
    private int readyTurn;
    private int servedTurn;
    private OrderStatus status = OrderStatus.PLACED;

    public Order(MenuItem item) {
        this(item, item.getPriceCents());
    }

    public Order(MenuItem item, int billCents) {
        this.item = item;
        this.billCents = billCents;
    }

    public void prepare(int turn) {
        if (status != OrderStatus.PLACED && status != OrderStatus.PREPARING) {
            throw new IllegalArgumentException("Only an unfinished order can be cooked.");
        }
        completedUnits++;
        status = OrderStatus.PREPARING;
        if (completedUnits == item.getPreparationUnits()) {
            status = OrderStatus.READY;
            readyTurn = turn;
        }
    }

    public void serve(int turn) {
        if (status != OrderStatus.READY || readyTurn >= turn) {
            throw new IllegalArgumentException("The dish must be READY from an earlier turn before serving.");
        }
        status = OrderStatus.SERVED;
        servedTurn = turn;
    }

    public void markPaid() {
        if (status != OrderStatus.SERVED) {
            throw new IllegalArgumentException("An unserved order cannot be paid.");
        }
        status = OrderStatus.PAID;
    }

    public void cancel() {
        if (status == OrderStatus.SERVED || status == OrderStatus.PAID) {
            throw new IllegalArgumentException("A served or paid order cannot be cancelled.");
        }
        status = OrderStatus.CANCELLED;
    }

    public MenuItem getItem() { return item; }
    public int getBillCents() { return billCents; }
    public int getCompletedUnits() { return completedUnits; }
    public int getServedTurn() { return servedTurn; }
    public OrderStatus getStatus() { return status; }
}
