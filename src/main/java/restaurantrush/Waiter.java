package restaurantrush;

import java.util.List;

public class Waiter extends Staff {
    public void seat(Customer customer, Table table) {
        requireAvailable();
        table.seat(customer);
        completeTask();
    }

    public void takeOrder(Customer customer, List<MenuItem> menu) {
        takeOrder(customer, menu, false);
    }

    public void takeOrder(Customer customer, List<MenuItem> menu, boolean happyHourActive) {
        requireAvailable();
        customer.placeOrder(menu, happyHourActive);
        completeTask();
    }

    public void serve(Customer customer, int turn) {
        requireAvailable();
        customer.serve(turn);
        completeTask();
    }
}
