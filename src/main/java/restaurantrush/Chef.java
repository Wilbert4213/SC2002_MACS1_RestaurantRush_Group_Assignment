package restaurantrush;

public class Chef extends Staff {
    public void prepare(Customer customer, int turn) {
        requireAvailable();
        if (customer.getOrder() == null) {
            throw new IllegalArgumentException("This customer has not placed an order.");
        }
        customer.getOrder().prepare(turn);
        completeTask();
    }
}
