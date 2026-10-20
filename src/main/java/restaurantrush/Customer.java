package restaurantrush;

import java.util.List;

/** Regular customer and the shared lifecycle for all customer types. */
public class Customer {
    private final int id;
    private int satisfaction = 100;
    private CustomerStatus status = CustomerStatus.WAITING;
    private Order order;
    private int readyToPayTurn;

    public Customer(int id) { this.id = id; }

    public MenuItem chooseMeal(List<MenuItem> menu) {
        if (menu == null || menu.isEmpty()) {
            throw new IllegalArgumentException("Menu cannot be empty.");
        }

        if (id % 3 == 0 && menu.size() >= 6) {
            int comboIndex = 3 + ((id / 3 - 1) % 3);
            return menu.get(comboIndex);
        }

        return menu.get(((id - 1) % 3) % menu.size());
    }

    public String getTypeName() { return "Regular"; }
    public int getWaitingSatisfactionLoss() { return 8; }

    protected int calculateBillCents(MenuItem item) {
        return item.getPriceCents();
    }

    public void seat() {
        if (status != CustomerStatus.WAITING) {
            throw new IllegalArgumentException("Only a queued customer can be seated.");
        }
        status = CustomerStatus.SEATED;
    }

    public void placeOrder(List<MenuItem> menu) {
        placeOrder(menu, false);
    }

    public void placeOrder(List<MenuItem> menu, boolean happyHourActive) {
        if (status != CustomerStatus.SEATED || order != null) {
            throw new IllegalArgumentException("The customer must be seated and have no existing order.");
        }

        MenuItem item = chooseMeal(menu);

        // Apply VIP discount first, if applicable.
        int billCents = calculateBillCents(item);

        // Apply Happy Hour discount afterward.
        if (happyHourActive) {
            billCents = (int) ((billCents * 80L + 50) / 100);
        }

        // Lock the final price when the order is placed.
        order = new Order(item, billCents);
    }

    public void serve(int turn) {
        if (status != CustomerStatus.SEATED || order == null) {
            throw new IllegalArgumentException("The customer must be seated with an order before serving.");
        }
        order.serve(turn);
        status = CustomerStatus.EATING;
    }

    public boolean finishEating(int turn) {
        if (status == CustomerStatus.EATING && order.getServedTurn() < turn) {
            status = CustomerStatus.READY_TO_PAY;
            readyToPayTurn = turn;
            return true;
        }
        return false;
    }

    public boolean canPay(int turn) {
        return status == CustomerStatus.READY_TO_PAY && readyToPayTurn < turn;
    }

    public int pay(int turn) {
        if (!canPay(turn)) {
            throw new IllegalArgumentException("Payment must wait until the turn after READY_TO_PAY.");
        }
        order.markPaid();
        status = CustomerStatus.PAID;
        return order.getBillCents();
    }

    /** Returns true only on the turn the customer abandons the restaurant. */
    public boolean loseSatisfaction() {
        if (status != CustomerStatus.WAITING && status != CustomerStatus.SEATED) {
            return false;
        }
        satisfaction = Math.max(0, satisfaction - getWaitingSatisfactionLoss());
        if (satisfaction != 0) {
            return false;
        }
        if (order != null) {
            order.cancel();
        }
        status = CustomerStatus.LEFT;
        return true;
    }

    public void restoreSatisfaction(int amount) {
        if (status == CustomerStatus.WAITING
                || status == CustomerStatus.SEATED) {

            satisfaction = Math.min(100, satisfaction + amount);
        }
    }

    public int getId() { return id; }
    public int getSatisfaction() { return satisfaction; }
    public CustomerStatus getStatus() { return status; }
    public Order getOrder() { return order; }
    public int getReadyToPayTurn() { return readyToPayTurn; }
}
