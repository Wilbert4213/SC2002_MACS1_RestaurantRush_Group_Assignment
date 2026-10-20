package restaurantrush;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.OptionalDouble;

/** Owns the restaurant state and coordinates the six turn steps. */
public class GameController {
    public static final int MAX_TURNS = 18;
    public static final int TARGET_PAID_CUSTOMERS = 4;
    public static final int TARGET_REVENUE_CENTS = 4500;
    public static final int TARGET_SATISFACTION = 60;

    // Regular and VIP customers cycle through this order; Critics choose their preference.
    private final List<MenuItem> menu = createMenu();

    private static List<MenuItem> createMenu() {
        MenuItem pasta = new MenuItem("Pasta", 1800, 2);
        MenuItem burger = new MenuItem("Burger", 1500, 1);
        MenuItem salad = new MenuItem("Salad", 900, 1);
        ComboMeal combo = new ComboMeal(salad, pasta);
        return List.of(pasta, burger, salad, combo);
    }

    private final List<Customer> customers = new ArrayList<>();
    private final List<Customer> scheduledCustomers;
    private final Deque<Customer> waitingQueue = new ArrayDeque<>();
    private final List<Table> tables = List.of(new Table(1), new Table(2));
    private final Waiter waiter = new Waiter();
    private final Host host = new Host();
    private final Chef chef = new Chef();
    private final Cashier cashier = new Cashier();
    private int turn;
    private boolean turnActive;
    private int revenueCents;
    private int paidCount;
    private int paidSatisfactionTotal;
    private int servedCount;
    private int unhappyDepartures;
    private boolean happyHourUsed;
    private int happyHourStartTurn = -1;
    
    public GameController() {
        this(List.of(new Customer(1), new VIPCustomer(2), new Customer(3),
                new CriticCustomer(4), new Customer(5), new Customer(6)));
    }

    // Tests can replay the regular-only Stage 1 setup using the same turn rules.
    GameController(List<Customer> scheduledCustomers) {
        this.scheduledCustomers = List.copyOf(scheduledCustomers);
    }

    public void beginTurn() {
        if (turnActive || isFinished()) {
            throw new IllegalStateException("Finish the current turn before starting another; games last 18 turns.");
        }
        turn++;
        turnActive = true;
        waiter.startTurn();
        host.startTurn();
        chef.startTurn();
        cashier.startTurn();
        if ((turn - 1) % 3 == 0) {
            Customer customer = scheduledCustomers.get(customers.size());
            customers.add(customer);
            waitingQueue.addLast(customer);
        }
        
        if (isHappyHourActive()) {
            restoreHappyHourSatisfaction();
        }
    }

    public String seatCustomer(int customerId, int tableId) {
        requireWaiterPhase();
        Customer customer = getCustomer(customerId);
        if (!waitingQueue.contains(customer)) {
            throw new IllegalArgumentException("Customer " + customerId + " is not in the waiting queue.");
        }
        waiter.seat(customer, getTable(tableId));
        waitingQueue.remove(customer);
        return "Customer " + customerId + " seated at table " + tableId + ".";
    }

    public String hostSeatCustomer(int customerId, int tableId) {
        requireActiveTurn();

        Customer customer = getCustomer(customerId);

        if (!waitingQueue.contains(customer)) {
            throw new IllegalArgumentException(
                    "Customer " + customerId + " is not in the waiting queue.");
        }

        host.seat(customer, getTable(tableId));
        waitingQueue.remove(customer);

        return "Host seated customer " + customerId
                + " at table " + tableId + ".";
    }

    public void waitHost() {
        requireActiveTurn();
        host.waitForTurn();
    }

    public String takeOrder(int customerId) {
        requireWaiterPhase();
        Customer customer = getCustomer(customerId);
        waiter.takeOrder(customer, menu, isHappyHourActive());
        Order order = customer.getOrder();
        return "Customer " + customerId + " chose " + order.getItem().getName()
                + " (" + Money.format(order.getBillCents()) + ", "
                + order.getItem().getPreparationUnits() + " preparation units).";
    }

    public String serveCustomer(int customerId) {
        requireWaiterPhase();
        waiter.serve(getCustomer(customerId), turn);
        servedCount++;
        return "Customer " + customerId + " served; eating begins.";
    }

    public void waitWaiter() {
        requireWaiterPhase();
        waiter.waitForTurn();
    }

    public String prepareOrder(int customerId) {
        requireChefPhase();
        Customer customer = getCustomer(customerId);
        chef.prepare(customer, turn);
        Order order = customer.getOrder();
        return "Customer " + customerId + ": " + order.getCompletedUnits() + "/"
                + order.getItem().getPreparationUnits() + " units, " + order.getStatus() + ".";
    }

    public void waitChef() {
        requireChefPhase();
        chef.waitForTurn();
    }

    public List<String> finishTurn() {
        requireActiveTurn();
        if (host.isAvailable() || waiter.isAvailable() || chef.isAvailable()) {
            throw new IllegalStateException("Choose a task or Wait for the Host, Waiter and Chef first.");
        }
        List<String> events = new ArrayList<>();

        // Only customers already ready on an earlier turn are eligible for payment.
        customers.stream().filter(customer -> customer.canPay(turn))
                .min(Comparator.comparingInt(Customer::getReadyToPayTurn)
                        .thenComparingInt(Customer::getId))
                .ifPresent(customer -> {
                    int billCents = cashier.collectPayment(customer, turn);
                    revenueCents += billCents;
                    paidCount++;
                    paidSatisfactionTotal += customer.getSatisfaction();
                    releaseTable(customer);
                    events.add("Customer " + customer.getId() + " paid " + Money.format(billCents) + ".");
                });

        for (Customer customer : customers) {
            if (customer.finishEating(turn)) {
                events.add("Customer " + customer.getId() + " is READY_TO_PAY; payment next turn at earliest.");
            }
            if (customer.loseSatisfaction()) {
                unhappyDepartures++;
                waitingQueue.remove(customer);
                releaseTable(customer);
                events.add("Customer " + customer.getId() + " left unhappy without paying.");
            }
        }
        turnActive = false;
        return List.copyOf(events);
    }

    private void releaseTable(Customer customer) {
        tables.stream().filter(table -> table.getCustomer() == customer)
                .findFirst().ifPresent(Table::release);
    }

    private void requireActiveTurn() {
        if (!turnActive) {
            throw new IllegalStateException("Start a turn before assigning tasks.");
        }
    }

    private void requireWaiterPhase() {
        requireActiveTurn();

        if (host.isAvailable()) {
            throw new IllegalArgumentException(
                    "The Host must act or wait before the Waiter.");
        }

        if (!chef.isAvailable()) {
            throw new IllegalArgumentException(
                    "The Waiter must act before the Chef.");
        }
    }

    private void requireChefPhase() {
        requireActiveTurn();
        if (waiter.isAvailable()) {
            throw new IllegalArgumentException("Choose the Waiter's task or Wait before the Chef acts.");
        }
    }

    public Customer getCustomer(int id) {
        return customers.stream().filter(customer -> customer.getId() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No customer with ID " + id + " has arrived."));
    }

    public Table getTable(int id) {
        return tables.stream().filter(table -> table.getId() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Choose table 1 or 2."));
    }

    public boolean canActivateHappyHour() {
        return turnActive && !happyHourUsed
                && host.isAvailable()
                && waiter.isAvailable()
                && chef.isAvailable();
    }

    public void activateHappyHour() {
        if (!canActivateHappyHour()) {
            throw new IllegalStateException(
                    "Happy Hour can only be activated once, at the start of a turn.");
        }

        happyHourUsed = true;
        happyHourStartTurn = turn;

        restoreHappyHourSatisfaction();
    }

    public boolean isHappyHourActive() {
        return happyHourUsed
                && turn >= happyHourStartTurn
                && turn < happyHourStartTurn + 2;
    }

    public int getHappyHourRemainingTurns() {
        if (!isHappyHourActive()) {
            return 0;
        }
        return happyHourStartTurn + 2 - turn;
    }

    public boolean isHappyHourUsed() {
        return happyHourUsed;
    }

    private void restoreHappyHourSatisfaction() {
        for (Customer customer : customers) {
            customer.restoreSatisfaction(15);
        }
    }

    public boolean isFinished() { return turn == MAX_TURNS && !turnActive; }

    public boolean isVictory() {
        return isFinished() && paidCount >= TARGET_PAID_CUSTOMERS
                && revenueCents >= TARGET_REVENUE_CENTS
                && getAverageSatisfaction().orElse(0) >= TARGET_SATISFACTION;
    }

    public OptionalDouble getAverageSatisfaction() {
        return paidCount == 0 ? OptionalDouble.empty()
                : OptionalDouble.of((double) paidSatisfactionTotal / paidCount);
    }

    public int getTurn() { return turn; }
    public int getRevenueCents() { return revenueCents; }
    public int getPaidCount() { return paidCount; }
    public int getServedCount() { return servedCount; }
    public int getUnhappyDepartures() { return unhappyDepartures; }
    public List<Customer> getCustomers() { return List.copyOf(customers); }
    public List<Customer> getWaitingQueue() { return List.copyOf(waitingQueue); }
    public List<Table> getTables() { return tables; }
    public List<MenuItem> getMenu() { return menu; }
    public boolean isWaiterAvailable() { return waiter.isAvailable(); }
    public boolean isChefAvailable() { return chef.isAvailable(); }
    public boolean isCashierAvailable() { return cashier.isAvailable(); }
    public boolean isHostAvailable() { return host.isAvailable(); }
}
