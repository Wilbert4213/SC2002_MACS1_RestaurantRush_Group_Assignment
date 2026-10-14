package restaurantrush;

import java.util.List;

public class Waiter extends Staff {
    public void seat(Customer customer, Table table) {
        requireAvailable();
        table.seat(customer);
        completeTask();
    }

    public void takeOrder(Customer customer, List<MenuItem> menu) {
        requireAvailable();
        customer.placeOrder(menu);
        completeTask();
    }

    public void serve(Customer customer, int turn) {
        requireAvailable();
        customer.serve(turn);
        completeTask();
    }
}
