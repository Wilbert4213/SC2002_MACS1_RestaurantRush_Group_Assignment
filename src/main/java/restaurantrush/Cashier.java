package restaurantrush;

public class Cashier extends Staff {
    public int collectPayment(Customer customer, int turn) {
        requireAvailable();
        int billCents = customer.pay(turn);
        completeTask();
        return billCents;
    }
}
