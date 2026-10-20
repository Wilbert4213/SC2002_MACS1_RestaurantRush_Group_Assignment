package restaurantrush;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

/** Dependency-free regression checks for Stage 2 customer polymorphism. */
public final class Stage2Tests {
    private static int passed;

    private Stage2Tests() { }

    public static void main(String[] args) throws IOException {
        test("mixed deterministic arrival types", Stage2Tests::arrivalTypes);
        test("customer-specific satisfaction in waiting and seated states", Stage2Tests::satisfactionLoss);
        test("served lifecycle states do not lose satisfaction", Stage2Tests::servedStates);
        test("polymorphic prices and frozen order bills", Stage2Tests::billing);
        test("Critic preference ignores ID and keeps first preparation tie", Stage2Tests::criticChoice);
        test("Critic abandonment cancels seated order and frees its table", Stage2Tests::criticAbandonment);
        test("regular-only constructor regression", Stage2Tests::regularOnlyGame);
        String victory = Files.readString(Path.of("tests/replays/stage2-victory.txt"));
        String defeat = Files.readString(Path.of("tests/replays/stage2-defeat.txt"));
        test("mixed-type CLI victory replay", () -> victoryReplay(victory));
        test("mixed-type CLI retries preserve the victory replay", () -> invalidCliChoices(victory));
        test("mixed-type CLI defeat replay", () -> defeatReplay(defeat));
        System.out.println("All " + passed + " Stage 2 checks passed.");
    }

    private static void test(String name, Runnable action) {
        action.run();
        passed++;
        System.out.println("PASS " + name);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectInvalid(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected an invalid-choice explanation.");
    }

    private static String runCli(GameController game, String choices) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new RestaurantRush(game, new Scanner(choices),
                new PrintStream(bytes, true, StandardCharsets.UTF_8)).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static void idleTurn(GameController game) {
        game.beginTurn();
        game.waitWaiter();
        game.waitChef();
        game.finishTurn();
    }

    private static void arrivalTypes() {
        GameController game = stage2Game();
        String[] names = {"Regular", "VIP", "Regular", "Critic", "Regular", "Regular"};
        Class<?>[] types = {Customer.class, VIPCustomer.class, Customer.class,
                CriticCustomer.class, Customer.class, Customer.class};
        for (int turn = 1; turn <= 16; turn++) {
            game.beginTurn();
            int count = (turn + 2) / 3;
            check(game.getCustomers().size() == count, "Only scheduled arrivals are created.");
            if ((turn - 1) % 3 == 0) {
                Customer arriving = game.getCustomer(count);
                check(arriving.getClass() == types[count - 1], "Unexpected concrete type for C" + count);
                check(arriving.getTypeName().equals(names[count - 1]), "Unexpected displayed type name.");
            }
            game.waitWaiter();
            game.waitChef();
            game.finishTurn();
        }
    }

    private static void satisfactionLoss() {
        Customer[] customers = {new Customer(1), new VIPCustomer(2), new CriticCustomer(3)};
        int[] losses = {8, 5, 12};
        for (int index = 0; index < customers.length; index++) {
            Customer customer = customers[index];
            customer.loseSatisfaction();
            check(customer.getSatisfaction() == 100 - losses[index], "Correct WAITING loss.");
            customer.seat();
            customer.loseSatisfaction();
            check(customer.getSatisfaction() == 100 - losses[index] * 2, "Correct SEATED loss.");
            while (customer.getSatisfaction() > 0) customer.loseSatisfaction();
            check(customer.getSatisfaction() == 0 && customer.getStatus() == CustomerStatus.LEFT,
                    "Each type leaves at zero satisfaction.");
        }
    }

    private static void servedStates() {
        List<Customer> types = List.of(new Customer(1), new VIPCustomer(2), new CriticCustomer(3));
        int[] bills = {1500, 1350, 1500};
        for (int index = 0; index < types.size(); index++) {
            Customer customer = readyToServe(types.get(index));
            customer.serve(3);
            assertNoLoss(customer, CustomerStatus.EATING);
            customer.finishEating(4);
            assertNoLoss(customer, CustomerStatus.READY_TO_PAY);
            check(customer.pay(5) == bills[index], "Correct full or discounted Burger bill.");
            assertNoLoss(customer, CustomerStatus.PAID);
        }
        Customer left = new CriticCustomer(4);
        while (left.getStatus() != CustomerStatus.LEFT) left.loseSatisfaction();
        assertNoLoss(left, CustomerStatus.LEFT);
    }

    private static Customer readyToServe(Customer customer) {
        customer.seat();
        customer.placeOrder(List.of(new MenuItem("Burger", 1500, 1)));
        customer.getOrder().prepare(2);
        return customer;
    }

    private static void assertNoLoss(Customer customer, CustomerStatus status) {
        int satisfaction = customer.getSatisfaction();
        check(customer.getStatus() == status, "Customer reached " + status);
        for (int turn = 0; turn < 2; turn++) {
            check(!customer.loseSatisfaction(), "Non-waiting status is ineligible for satisfaction loss.");
            check(customer.getSatisfaction() == satisfaction, "Satisfaction remains unchanged in " + status);
        }
    }

    private static void billing() {
        List<MenuItem> menu = List.of(new MenuItem("Pasta", 1800, 2),
                new MenuItem("Burger", 1500, 1), new MenuItem("Salad", 900, 1));
        int[] fullPrices = {1800, 1500, 900};
        int[] vipPrices = {1620, 1350, 810};
        String[] dishNames = {"Pasta", "Burger", "Salad"};
        for (int index = 0; index < menu.size(); index++) {
            List<MenuItem> oneDish = List.of(menu.get(index));
            Customer regular = new Customer(index + 1);
            VIPCustomer vip = new VIPCustomer(index + 1);
            regular.seat();
            regular.placeOrder(oneDish);
            vip.seat();
            vip.placeOrder(oneDish);
            check(menu.get(index).getName().equals(dishNames[index]), "Menu item order is stable.");
            check(regular.getOrder().getBillCents() == fullPrices[index], "Regular pays full " + dishNames[index] + " price.");
            check(vip.getOrder().getBillCents() == vipPrices[index], "VIP discount applies to " + dishNames[index] + ".");
        }
        VIPCustomer fractional = new VIPCustomer(20);
        fractional.seat();
        fractional.placeOrder(List.of(new MenuItem("Fractional", 1005, 1)));
        check(fractional.getOrder().getBillCents() == 905, "Half-cent discount rounds to nearest cent.");
        Order fractionalOrder = fractional.getOrder();
        expectInvalid(() -> fractional.placeOrder(List.of(new MenuItem("Replacement", 1, 1))));
        fractionalOrder.prepare(1);
        fractional.serve(2);
        check(fractional.finishEating(3), "Fractional-price VIP finishes eating.");
        expectInvalid(() -> fractional.pay(3));
        check(fractional.pay(4) == 905 && fractionalOrder.getBillCents() == 905,
                "Correctly timed payment retains the rounded order-time bill.");
    }

    private static void criticChoice() {
        MenuItem shortDish = new MenuItem("Short", 100, 1);
        MenuItem longDish = new MenuItem("Long", 200, 3);
        MenuItem longerDish = new MenuItem("Longer", 300, 4);
        CriticCustomer lowId = new CriticCustomer(1);
        CriticCustomer highId = new CriticCustomer(97);
        check(lowId.chooseMeal(List.of(shortDish, longDish, longerDish)) == longerDish,
                "Critic selects greatest preparation time.");
        check(highId.chooseMeal(List.of(longerDish, shortDish, longDish)) == longerDish,
                "Critic choice is independent of ID and menu position.");
        MenuItem firstTie = new MenuItem("First tie", 400, 5);
        MenuItem secondTie = new MenuItem("Second tie", 500, 5);
        check(new CriticCustomer(33).chooseMeal(List.of(shortDish, firstTie, secondTie)) == firstTie,
                "First item wins a preparation-time tie.");
    }

    private static void criticAbandonment() {
        GameController game = stage2Game();
        for (int turn = 1; turn <= 9; turn++) idleTurn(game);
        game.beginTurn();
        game.seatCustomer(4, 1);
        game.waitChef();
        game.finishTurn();
        game.beginTurn();
        game.takeOrder(4);
        game.waitChef();
        game.finishTurn();
        for (int turn = 12; turn <= 18; turn++) idleTurn(game);
        Customer critic = game.getCustomer(4);
        check(game.isFinished() && !game.isVictory() && game.getCustomers().size() == 6,
                "All six arrivals complete in a defeated 18-turn run.");
        check(critic.getTypeName().equals("Critic") && critic.getStatus() == CustomerStatus.LEFT
                && critic.getSatisfaction() == 0, "Seated Critic reaches zero and leaves.");
        check(critic.getOrder().getStatus() == OrderStatus.CANCELLED && game.getTable(1).isFree(),
                "Critic abandonment cancels its order and releases the table.");
        check(game.getRevenueCents() == 0 && game.getPaidCount() == 0,
                "Abandonment cannot create revenue or a payment.");
    }

    private static void regularOnlyGame() {
        GameController game = new GameController(List.of(new Customer(1), new Customer(2),
                new Customer(3), new Customer(4), new Customer(5), new Customer(6)));
        check(game.getCustomers().isEmpty(), "Scheduled roster starts outside the restaurant.");
        game.beginTurn();
        check(game.getCustomers().size() == 1 && game.getCustomer(1).getTypeName().equals("Regular"),
                "Regular roster member enters on its scheduled arrival turn.");
        game.waitWaiter();
        game.waitChef();
        game.finishTurn();
    }

    private static void invalidCliChoices(String victory) {
        String invalid = "order 1\nseat 99 1\nseat 1 3\nseat 1 1\ncook 1\nwait\n";
        String choices = victory.replaceFirst("seat 1 1\\nwait\\n", invalid);
        GameController game = stage2Game();
        String output = runCli(game, choices);
        check(game.isVictory() && game.getTurn() == 18 && game.getPaidCount() == 5
                && game.getRevenueCents() == 7350
                && Math.abs(game.getAverageSatisfaction().orElseThrow() - 73.0) < 0.001,
                "Invalid choices leave the mixed-type victory run unchanged.");
        check(output.contains("Invalid choice") && output.contains("Choose again")
                && !output.contains("Input ended"), "CLI retries invalid Waiter and Chef choices.");
    }

    private static void victoryReplay(String choices) {
        GameController game = stage2Game();
        String output = runCli(game, choices);
        check(game.isFinished() && game.isVictory() && output.contains("Result: VICTORY"), "Victory result.");
        check(game.getTurn() == 18 && game.getPaidCount() == 5 && game.getServedCount() == 5,
                "Five mixed-type customers pay in 18 turns.");
        check(game.getRevenueCents() == 7350
                && Math.abs(game.getAverageSatisfaction().orElseThrow() - 73.0) < 0.001,
                "Discounted victory revenue and paid average.");
        check(game.getCustomer(2).getTypeName().equals("VIP")
                && game.getCustomer(2).getOrder().getBillCents() == 1350
                && game.getCustomer(2).getSatisfaction() == 85, "VIP discount and satisfaction in replay.");
        check(game.getCustomer(4).getTypeName().equals("Critic")
                && game.getCustomer(4).getOrder().getItem().getName().equals("Pasta")
                && game.getCustomer(4).getOrder().getBillCents() == 1800
                && game.getCustomer(4).getSatisfaction() == 52, "Critic preference and full price in replay.");
        check(output.contains("Regular") && output.contains("VIP") && output.contains("Critic"),
                "CLI identifies each customer type.");
        check(!output.contains("Invalid choice"), "Victory fixture contains legal commands.");
    }

    private static void defeatReplay(String choices) {
        GameController game = stage2Game();
        String output = runCli(game, choices);
        check(game.isFinished() && !game.isVictory() && output.contains("Result: DEFEAT"), "Defeat result.");
        check(game.getTurn() == 18 && game.getPaidCount() == 0 && game.getServedCount() == 0
                && game.getRevenueCents() == 0 && game.getAverageSatisfaction().isEmpty(),
                "Unserved customers produce no payments or revenue.");
        check(game.getUnhappyDepartures() == 2 && game.getCustomer(1).getStatus() == CustomerStatus.LEFT
                && game.getCustomer(4).getStatus() == CustomerStatus.LEFT, "Regular and Critic abandon.");
        check(game.getCustomer(1).getSatisfaction() == 0 && game.getCustomer(2).getSatisfaction() == 25
                && game.getCustomer(3).getSatisfaction() == 4 && game.getCustomer(4).getSatisfaction() == 0
                && game.getCustomer(5).getSatisfaction() == 52 && game.getCustomer(6).getSatisfaction() == 76,
                "Waiting losses follow each concrete customer type.");
        check(game.getCustomer(2).getStatus() == CustomerStatus.WAITING,
                "VIP remains in queue after regular and Critic have left.");
        check(output.contains("paid average N/A") && output.contains("Regular")
                && output.contains("VIP") && output.contains("Critic"), "Defeat report includes metrics and types.");
    }

    private static GameController stage2Game() {
        return new GameController(List.of(
                new Customer(1),
                new VIPCustomer(2),
                new Customer(3),
                new CriticCustomer(4),
                new Customer(5),
                new Customer(6)
        ), false);
        }
}
