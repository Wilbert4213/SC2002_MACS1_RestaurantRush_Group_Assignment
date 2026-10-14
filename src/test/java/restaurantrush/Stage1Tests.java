package restaurantrush;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

/** Dependency-free regression checks; assertions always run, without a JVM flag. */
public final class Stage1Tests {
    private static int passed;

    private Stage1Tests() { }

    public static void main(String[] args) throws IOException {
        test("base menu and customer choices", Stage1Tests::menuAndChoices);
        test("arrivals, abandonment and all 18 turns", Stage1Tests::arrivalsAndDuration);
        test("manual Pasta timeline and payment snapshot", Stage1Tests::pastaTimeline);
        test("invalid Waiter choices do not consume a task", Stage1Tests::invalidWaiterChoices);
        test("Waiter and Chef budgets and execution order", Stage1Tests::staffBudgets);
        test("one-unit dish cannot be served in its preparation turn", Stage1Tests::oneUnitTiming);
        test("placed, preparing and ready orders cancelled on abandonment", Stage1Tests::abandonment);
        test("Cashier payment timing and one-task budget", Stage1Tests::cashierTiming);
        test("manager service priority determines payment order", Stage1Tests::servicePriority);
        test("paid count and revenue cannot compensate for low satisfaction", Stage1Tests::satisfactionDefeat);

        String victory = Files.readString(Path.of("tests/replays/victory.txt"));
        String defeat = Files.readString(Path.of("tests/replays/defeat.txt"));
        test("complete reproducible CLI victory", () -> victoryReplay(victory));
        test("complete reproducible CLI defeat", () -> defeatReplay(defeat));
        test("CLI retries malformed and illegal choices", () -> invalidCliInput(victory));
        test("CLI handles end of input without inventing a result", Stage1Tests::endOfInput);
        System.out.println("All " + passed + " checks passed.");
    }

    private static void test(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS " + name);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectInvalid(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected an invalid-choice explanation.");
    }

    private static void expectStateError(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException expected) {
            return;
        }
        throw new AssertionError("Expected a turn-boundary error.");
    }

    private static void idleTurn(GameController game) {
        game.beginTurn();
        game.waitWaiter();
        game.waitChef();
        game.finishTurn();
    }

    private static void menuAndChoices() {
        GameController game = new GameController();
        List<MenuItem> menu = game.getMenu();
        check(menu.size() == 3, "Exactly three base dishes.");
        check(menu.get(0).getName().equals("Pasta") && menu.get(0).getPriceCents() == 1800
                && menu.get(0).getPreparationUnits() == 2, "Pasta definition.");
        check(menu.get(1).getName().equals("Burger") && menu.get(1).getPriceCents() == 1500
                && menu.get(1).getPreparationUnits() == 1, "Burger definition.");
        check(menu.get(2).getName().equals("Salad") && menu.get(2).getPriceCents() == 900
                && menu.get(2).getPreparationUnits() == 1, "Salad definition.");
        for (int id = 1; id <= 6; id++) {
            check(new Customer(id).chooseMeal(menu) == menu.get((id - 1) % 3), "Deterministic food choice.");
        }
        check(Money.format(900).equals("$9.00"), "Money has two decimal places.");
    }

    private static void arrivalsAndDuration() {
        GameController game = new GameController();
        check(game.getAverageSatisfaction().isEmpty(), "No payments means N/A.");
        expectStateError(game::waitWaiter);
        for (int turn = 1; turn <= 18; turn++) {
            game.beginTurn();
            check(game.getCustomers().size() == (turn + 2) / 3, "Scheduled arrivals only.");
            if ((turn - 1) % 3 == 0) {
                check(game.getCustomer((turn + 2) / 3).getSatisfaction() == 100, "Arrival starts at 100.");
            }
            check(!game.isFinished() && !game.isVictory(), "No early ending.");
            expectStateError(game::beginTurn);
            game.waitWaiter();
            game.waitChef();
            game.finishTurn();
        }
        check(game.isFinished() && !game.isVictory(), "Defeat after exactly 18 turns.");
        check(game.getUnhappyDepartures() == 2 && game.getWaitingQueue().size() == 4, "Queue abandonment.");
        check(game.getRevenueCents() == 0 && game.getAverageSatisfaction().isEmpty(), "Unpaid customers excluded.");
        expectStateError(game::beginTurn);
    }

    private static void pastaTimeline() {
        GameController game = new GameController();
        game.beginTurn();
        game.seatCustomer(1, 1);
        game.waitChef();
        game.finishTurn();
        check(game.getCustomer(1).getSatisfaction() == 92, "Turn 1 satisfaction.");
        game.beginTurn();
        game.takeOrder(1);
        game.prepareOrder(1);
        game.finishTurn();
        check(game.getCustomer(1).getSatisfaction() == 84, "Turn 2 satisfaction.");
        check(game.getCustomer(1).getOrder().getStatus() == OrderStatus.PREPARING, "Two-unit dish unfinished.");
        game.beginTurn();
        expectInvalid(() -> game.serveCustomer(1));
        game.waitWaiter();
        game.prepareOrder(1);
        game.finishTurn();
        check(game.getCustomer(1).getSatisfaction() == 76, "Turn 3 satisfaction.");
        check(game.getCustomer(1).getOrder().getStatus() == OrderStatus.READY, "READY after second unit.");
        game.beginTurn();
        game.serveCustomer(1);
        game.waitChef();
        game.finishTurn();
        check(game.getCustomer(1).getStatus() == CustomerStatus.EATING, "No same-turn eating completion.");
        check(game.getCustomer(1).getOrder().getStatus() == OrderStatus.SERVED, "SERVED state.");
        idleTurn(game);
        check(game.getCustomer(1).getStatus() == CustomerStatus.READY_TO_PAY, "Ready on turn 5.");
        check(!game.getTable(1).isFree() && game.getRevenueCents() == 0, "Table and bill retained until payment.");
        idleTurn(game);
        check(game.getCustomer(1).getStatus() == CustomerStatus.PAID, "Paid on turn 6.");
        check(game.getCustomer(1).getOrder().getStatus() == OrderStatus.PAID, "Order paid too.");
        check(game.getTable(1).isFree() && game.getRevenueCents() == 1800, "Payment frees table and earns bill.");
        check(game.getAverageSatisfaction().orElseThrow() == 76, "Paid satisfaction snapshot.");
        idleTurn(game);
        check(game.getCustomer(1).getSatisfaction() == 76 && game.getAverageSatisfaction().orElseThrow() == 76,
                "Served customers lose nothing and unpaid customers do not affect the average.");
    }

    private static void invalidWaiterChoices() {
        GameController game = new GameController();
        game.beginTurn();
        expectInvalid(() -> game.seatCustomer(99, 1));
        expectInvalid(() -> game.seatCustomer(1, 3));
        expectInvalid(() -> game.takeOrder(1));
        expectInvalid(() -> game.serveCustomer(1));
        check(game.isWaiterAvailable() && game.getCustomer(1).getStatus() == CustomerStatus.WAITING,
                "Invalid choices change neither budget nor state.");
        game.seatCustomer(1, 1);
        game.waitChef();
        game.finishTurn();
        game.beginTurn();
        game.takeOrder(1);
        game.waitChef();
        game.finishTurn();
        game.beginTurn();
        expectInvalid(() -> game.takeOrder(1));
        game.waitWaiter();
        game.waitChef();
        game.finishTurn();
        game.beginTurn();
        expectInvalid(() -> game.seatCustomer(2, 1));
        check(game.isWaiterAvailable() && game.getCustomer(2).getStatus() == CustomerStatus.WAITING,
                "Occupied-table failure preserves task and queue.");
        game.seatCustomer(2, 2);
    }

    private static void staffBudgets() {
        GameController game = new GameController();
        game.beginTurn();
        expectInvalid(game::waitChef);
        game.seatCustomer(1, 1);
        expectInvalid(() -> game.takeOrder(1));
        expectInvalid(() -> game.prepareOrder(1));
        check(game.isChefAvailable(), "Chef can retry an invalid order.");
        game.waitChef();
        expectInvalid(game::waitChef);
        expectInvalid(game::waitWaiter);
        game.finishTurn();
        game.beginTurn();
        check(game.isWaiterAvailable() && game.isChefAvailable() && game.isCashierAvailable(), "Budgets reset.");
        expectStateError(game::finishTurn);
        game.takeOrder(1);
        game.prepareOrder(1);
        expectInvalid(() -> game.prepareOrder(1));
        check(game.getCustomer(1).getOrder().getCompletedUnits() == 1, "One Chef unit per turn.");
    }

    private static void oneUnitTiming() {
        Customer customer = new Customer(2);
        customer.seat();
        customer.placeOrder(new GameController().getMenu());
        Chef chef = new Chef();
        chef.prepare(customer, 6);
        expectInvalid(() -> customer.serve(6));
        check(customer.getOrder().getStatus() == OrderStatus.READY, "Same-turn rejection preserves READY.");
        chef.startTurn();
        expectInvalid(() -> chef.prepare(customer, 7));
        check(chef.isAvailable(), "READY order cannot waste Chef budget.");
        customer.serve(7);
        check(!customer.finishEating(7) && customer.finishEating(8), "One full turn of eating.");
        expectInvalid(() -> customer.pay(8));
        check(customer.pay(9) == 1500, "One-unit bill paid at the correct turn.");
    }

    private static void abandonment() {
        for (int units = 0; units <= 2; units++) {
            GameController game = new GameController();
            game.beginTurn();
            game.seatCustomer(1, 1);
            game.waitChef();
            game.finishTurn();
            game.beginTurn();
            game.takeOrder(1);
            if (units > 0) { game.prepareOrder(1); } else { game.waitChef(); }
            game.finishTurn();
            game.beginTurn();
            game.waitWaiter();
            if (units > 1) { game.prepareOrder(1); } else { game.waitChef(); }
            game.finishTurn();
            while (game.getTurn() < 13) { idleTurn(game); }
            check(game.getCustomer(1).getSatisfaction() == 0, "Satisfaction clamps at zero.");
            check(game.getCustomer(1).getStatus() == CustomerStatus.LEFT, "Unserved customer leaves.");
            check(game.getCustomer(1).getOrder().getStatus() == OrderStatus.CANCELLED, "Unpaid order cancelled.");
            check(game.getTable(1).isFree() && game.getRevenueCents() == 0, "Abandonment frees table without income.");
        }
    }

    private static Customer readyCustomer(int id, int servedTurn) {
        Customer customer = new Customer(id);
        customer.seat();
        customer.placeOrder(List.of(new MenuItem("Salad", 900, 1)));
        customer.getOrder().prepare(servedTurn - 1);
        customer.serve(servedTurn);
        customer.finishEating(servedTurn + 1);
        return customer;
    }

    private static void cashierTiming() {
        Cashier cashier = new Cashier();
        Customer first = readyCustomer(1, 3);
        Customer second = readyCustomer(2, 4);
        expectInvalid(() -> cashier.collectPayment(first, 4));
        check(cashier.isAvailable(), "Premature payment leaves budget available.");
        check(cashier.collectPayment(first, 5) == 900, "Eligible payment collected.");
        expectInvalid(() -> cashier.collectPayment(second, 6));
        cashier.startTurn();
        check(cashier.collectPayment(second, 6) == 900, "Next-turn budget reset.");
        expectInvalid(() -> second.pay(7));
    }

    private static String runCli(GameController game, String choices) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new RestaurantRush(game, new Scanner(choices), new PrintStream(bytes, true, StandardCharsets.UTF_8)).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static void servicePriority() {
        GameController game = new GameController();
        String choices = "seat 1 1\nwait\norder 1\ncook 1\nwait\ncook 1\n"
                + "seat 2 2\nwait\norder 2\ncook 2\nserve 2\nwait\nserve 1\nwait\nwait\nwait\n";
        runCli(game, choices);
        check(game.getTurn() == 9 && game.getPaidCount() == 1, "Turn 8 collected one payment.");
        check(game.getCustomer(2).getStatus() == CustomerStatus.PAID
                && game.getCustomer(1).getStatus() == CustomerStatus.READY_TO_PAY,
                "Oldest readiness takes precedence over arrival ID.");
        check(game.getRevenueCents() == 1500 && game.getAverageSatisfaction().orElseThrow() == 84,
                "Only the first payment affects the totals.");
        check(game.getTable(2).isFree() && !game.getTable(1).isFree(), "Only the paid customer's table is freed.");
        game.waitWaiter();
        game.waitChef();
        game.finishTurn();
        check(game.getPaidCount() == 2 && game.getRevenueCents() == 3300, "Next eligible payment follows.");
    }

    private static void satisfactionDefeat() {
        String choices = "seat 1 1\nwait\norder 1\ncook 1\nwait\ncook 1\n"
                + "seat 2 2\nwait\norder 2\ncook 2\nwait\nwait\nserve 1\nwait\n"
                + "serve 2\nwait\nwait\nwait\nseat 3 1\nwait\norder 3\ncook 3\n"
                + "serve 3\nwait\nseat 4 2\nwait\norder 4\ncook 4\nwait\ncook 4\n"
                + "serve 4\nwait\nwait\nwait\nwait\nwait\n";
        GameController game = new GameController();
        String output = runCli(game, choices);
        check(!output.contains("Invalid choice") && game.isFinished(), "Delayed-service run is legal and complete.");
        check(game.getPaidCount() == 4 && game.getRevenueCents() == 6000, "Count and revenue targets met.");
        check(game.getAverageSatisfaction().orElseThrow() == 58 && !game.isVictory(), "Low satisfaction alone causes defeat.");
    }

    private static void victoryReplay(String choices) {
        GameController game = new GameController();
        String output = runCli(game, choices);
        check(game.isFinished() && game.isVictory() && output.contains("Result: VICTORY"), "Victory replay finishes.");
        check(game.getTurn() == 18 && game.getPaidCount() == 5 && game.getServedCount() == 5, "Five paid in 18 turns.");
        check(game.getRevenueCents() == 7500 && Math.abs(game.getAverageSatisfaction().orElseThrow() - 74.4) < 0.001,
                "Expected victory revenue and paid average.");
        check(game.getCustomer(6).getStatus() == CustomerStatus.SEATED
                && game.getCustomer(6).getOrder().getStatus() == OrderStatus.READY, "Turn-16 arrival cannot pay by 18.");
        check(!output.contains("Invalid choice"), "Victory fixture contains only legal commands.");
    }

    private static void defeatReplay(String choices) {
        GameController game = new GameController();
        String output = runCli(game, choices);
        check(game.isFinished() && !game.isVictory() && output.contains("Result: DEFEAT"), "Defeat replay finishes.");
        check(game.getPaidCount() == 0 && game.getServedCount() == 0 && game.getRevenueCents() == 0,
                "Waiting earns no revenue.");
        check(game.getUnhappyDepartures() == 2 && output.contains("paid average N/A"), "Defeat metrics and N/A.");
    }

    private static void invalidCliInput(String victory) {
        String invalid = "\nhelp\nstatus\nunknown\nseat x 1\nseat 2147483648 1\nseat 1 3\norder 1\nserve 1\n";
        String choices = victory.replaceFirst("seat 1 1\\nwait\\n", invalid
                + "seat 1 1\nhelp\nstatus\ncook 99\ncook 1\nwait\n");
        GameController game = new GameController();
        String output = runCli(game, choices);
        check(game.isVictory() && game.getRevenueCents() == 7500, "Invalid CLI choices preserve the replay.");
        check(output.contains("Invalid choice") && output.contains("Choose again"), "Useful retry messages.");
    }

    private static void endOfInput() {
        GameController game = new GameController();
        String output = runCli(game, "seat 1 1\n");
        check(!game.isFinished() && game.getTurn() == 1, "EOF does not auto-play remaining turns.");
        check(output.contains("Input ended") && !output.contains("Result:"), "EOF gives a clear incomplete-game message.");
    }
}
