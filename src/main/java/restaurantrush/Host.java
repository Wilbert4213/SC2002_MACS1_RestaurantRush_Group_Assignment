package restaurantrush;

public class Host extends Staff {

    public void seat(Customer customer, Table table) {
        requireAvailable();
        table.seat(customer);
        completeTask();
    }
}