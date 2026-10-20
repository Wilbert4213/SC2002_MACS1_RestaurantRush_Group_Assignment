package restaurantrush;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

/** Dependency-free Stage 3 checks; assertions run without -ea. */
public final class Stage3Tests {
    private static int passed;

    private Stage3Tests() { }

    public static void main(String[] args) {
        test("default game enables Stage 3 and registers six menu items", Stage3Tests::menu);
        test("combo composition, prices, preparation units and rounding", Stage3Tests::combos);
        test("deterministic combo choices and Critic preference", Stage3Tests::choices);
        test("Host must act before Waiter; separate task budgets", Stage3Tests::hostPhase);
        test("invalid Host actions preserve task and queue", Stage3Tests::invalidHost);
        test("Host seating permits same-turn Waiter order and Chef work", Stage3Tests::sameTurn);
        test("Happy Hour activates once, at the beginning of a turn", Stage3Tests::activation);
        test("Happy Hour lasts exactly two turns", Stage3Tests::duration);
        test("satisfaction recovery applies only to eligible states and caps at 100", Stage3Tests::recovery);
        test("Happy Hour discounts and VIP stacking are rounded and frozen", Stage3Tests::discounts);
        test("combo order requires combined preparation units", Stage3Tests::comboTimeline);
        test("complete 18-turn Stage 3 victory and target accounting", Stage3Tests::victoryRun);
        test("18-turn CLI defeat with valid Host/Waiter/Chef commands", Stage3Tests::defeatReplay);
        test("CLI retries invalid Host input without consuming a task", Stage3Tests::cliRetries);
        test("CLI end of input never invents a result", Stage3Tests::cliEof);
        System.out.println("All " + passed + " Stage 3 checks passed.");
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
        throw new AssertionError("Expected IllegalArgumentException.");
    }

    private static void expectStateError(Runnable action) {
        try { action.run(); }
        catch (IllegalStateException expected) { return; }
        throw new AssertionError("Expected IllegalStateException.");
    }

    private static void idleTurn(GameController game) {
        game.beginTurn();
        game.waitHost();
        game.waitWaiter();
        game.waitChef();
        game.finishTurn();
    }

    private static String runCli(GameController game, String choices) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new RestaurantRush(game, new Scanner(choices),
                new PrintStream(bytes, true, StandardCharsets.UTF_8)).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static void menu() {
        GameController game = new GameController();
        List<MenuItem> items = game.getMenu();
        check(game.isStage3Enabled() && items.size() == 6, "Stage 3 has six items.");
        check(items.get(0).getName().equals("Pasta") && items.get(0).getPriceCents() == 1800, "Pasta.");
        check(items.get(1).getName().equals("Burger") && items.get(1).getPriceCents() == 1500, "Burger.");
        check(items.get(2).getName().equals("Salad") && items.get(2).getPriceCents() == 900, "Salad.");
        check(items.get(3).getPriceCents() == 2160 && items.get(3).getPreparationUnits() == 2,
                "Salad+Burger.");
        check(items.get(4).getPriceCents() == 2430 && items.get(4).getPreparationUnits() == 3,
                "Salad+Pasta.");
        check(items.get(5).getPriceCents() == 2970 && items.get(5).getPreparationUnits() == 3,
                "Burger+Pasta.");
    }

    private static void combos() {
        MenuItem first = new MenuItem("A", 1005, 2);
        MenuItem second = new MenuItem("B", 1000, 3);
        ComboMeal combo = new ComboMeal(first, second);
        check(combo.getFirstItem() == first && combo.getSecondItem() == second, "Original items retained.");
        check(combo.getName().equals("A + B Combo"), "Combo name.");
        check(combo.getPriceCents() == 1805, "90% of 2005 cents rounds to 1805.");
        check(combo.getPreparationUnits() == 5, "Preparation units add.");
        try {
            new ComboMeal(null, second);
            throw new AssertionError("Expected null component rejection.");
        } catch (NullPointerException expected) {
            // ComboMeal uses Objects.requireNonNull.
        }
    }

    private static void choices() {
        List<MenuItem> menu = new GameController().getMenu();
        check(new Customer(1).chooseMeal(menu) == menu.get(0), "C1 Pasta.");
        check(new VIPCustomer(2).chooseMeal(menu) == menu.get(1), "C2 Burger.");
        check(new Customer(3).chooseMeal(menu) == menu.get(3), "C3 first combo.");
        check(new Customer(6).chooseMeal(menu) == menu.get(4), "C6 second combo.");
        check(new Customer(9).chooseMeal(menu) == menu.get(5), "C9 third combo.");
        check(new CriticCustomer(4).chooseMeal(menu) == menu.get(4), "Critic chooses first three-unit combo.");
        check(new Customer(2).chooseMeal(List.of(menu.get(2))) == menu.get(2), "Single-item menu supported.");
        expectInvalid(() -> new Customer(1).chooseMeal(List.of()));
    }

    private static void hostPhase() {
        GameController game = new GameController();
        game.beginTurn();
        expectInvalid(game::waitWaiter);
        expectStateError(game::finishTurn);
        game.waitHost();
        check(!game.isHostAvailable() && game.isWaiterAvailable(), "Host and Waiter budgets independent.");
        expectInvalid(game::waitHost);
        game.seatCustomer(1, 1);
        check(!game.isWaiterAvailable(), "Waiter seat consumes Waiter task.");
        game.waitChef();
        game.finishTurn();
        check(game.getCustomer(1).getStatus() == CustomerStatus.SEATED, "Waiter seat works after Host wait.");
    }

    private static void invalidHost() {
        GameController game = new GameController();
        game.beginTurn();
        expectInvalid(() -> game.hostSeatCustomer(99, 1));
        expectInvalid(() -> game.hostSeatCustomer(1, 3));
        check(game.isHostAvailable() && game.getWaitingQueue().size() == 1, "Invalid seating is atomic.");
        game.hostSeatCustomer(1, 1);
        expectInvalid(() -> game.hostSeatCustomer(1, 2));
        check(game.getTable(2).isFree(), "Second Host task rejected.");
        game.waitWaiter();
        game.waitChef();
        game.finishTurn();
    }

    private static void sameTurn() {
        GameController game = new GameController();
        game.beginTurn();
        game.hostSeatCustomer(1, 1);
        game.takeOrder(1);
        game.prepareOrder(1);
        check(game.getCustomer(1).getOrder().getCompletedUnits() == 1, "One Chef unit completed.");
        check(!game.isHostAvailable() && !game.isWaiterAvailable() && !game.isChefAvailable(),
                "All staff used exactly one task.");
        game.finishTurn();
    }

    private static void activation() {
        GameController game = new GameController();
        expectStateError(game::activateHappyHour);
        game.beginTurn();
        check(game.canActivateHappyHour(), "Available before staff tasks.");
        game.waitHost();
        check(!game.canActivateHappyHour(), "Cannot activate after Host action.");
        expectStateError(game::activateHappyHour);
        game.waitWaiter();
        game.waitChef();
        game.finishTurn();
        game.beginTurn();
        game.activateHappyHour();
        check(game.isHappyHourUsed() && game.isHappyHourActive(), "Activation succeeds.");
        expectStateError(game::activateHappyHour);
        game.waitHost();
        game.waitWaiter();
        game.waitChef();
        game.finishTurn();
    }

    private static void duration() {
        GameController game = new GameController();
        game.beginTurn();
        game.activateHappyHour();
        check(game.getHappyHourRemainingTurns() == 2, "First active turn.");
        game.waitHost();
        game.waitWaiter();
        game.waitChef();
        game.finishTurn();
        game.beginTurn();
        check(game.isHappyHourActive() && game.getHappyHourRemainingTurns() == 1, "Second active turn.");
        game.waitHost();
        game.waitWaiter();
        game.waitChef();
        game.finishTurn();
        game.beginTurn();
        check(!game.isHappyHourActive() && game.getHappyHourRemainingTurns() == 0, "Expires on third turn.");
        check(game.isHappyHourUsed() && !game.canActivateHappyHour(), "Cannot reuse.");
        game.waitHost();
        game.waitWaiter();
        game.waitChef();
        game.finishTurn();
    }

    private static void recovery() {
        Customer waiting = new Customer(1);
        waiting.loseSatisfaction();
        waiting.loseSatisfaction();
        waiting.restoreSatisfaction(15);
        check(waiting.getSatisfaction() == 99, "Waiting restores 15.");
        waiting.restoreSatisfaction(15);
        check(waiting.getSatisfaction() == 100, "Recovery capped at 100.");

        Customer seated = new Customer(2);
        seated.seat();
        seated.loseSatisfaction();
        seated.restoreSatisfaction(15);
        check(seated.getSatisfaction() == 100, "Seated eligible.");

        Customer eating = new Customer(3);
        eating.seat();
        eating.placeOrder(List.of(new MenuItem("A", 100, 1)));
        eating.getOrder().prepare(1);
        eating.loseSatisfaction();
        eating.serve(2);
        eating.restoreSatisfaction(15);
        check(eating.getSatisfaction() == 92, "Eating does not restore.");

        GameController game = new GameController();
        game.beginTurn();
        game.waitHost();
        game.waitWaiter();
        game.waitChef();
        game.finishTurn();
        check(game.getCustomer(1).getSatisfaction() == 92, "Turn 1 loss.");
        game.beginTurn();
        game.activateHappyHour();
        check(game.getCustomer(1).getSatisfaction() == 100, "Activation restores immediately.");
        game.waitHost();
        game.waitWaiter();
        game.waitChef();
        game.finishTurn();
        game.beginTurn();
        check(game.getCustomer(1).getSatisfaction() == 100, "Second active turn restores.");
        game.waitHost();
        game.waitWaiter();
        game.waitChef();
        game.finishTurn();
    }

    private static void discounts() {
        MenuItem burger = new MenuItem("Burger", 1500, 1);
        VIPCustomer vip = new VIPCustomer(2);
        vip.seat();
        vip.placeOrder(List.of(burger), true);
        check(vip.getOrder().getBillCents() == 1080, "VIP 1350 then Happy Hour 1080.");
        vip.getOrder().prepare(1);
        vip.serve(2);
        vip.finishEating(3);
        check(vip.pay(4) == 1080, "Frozen bill paid after Happy Hour.");

        Customer regular = new Customer(1);
        regular.seat();
        regular.placeOrder(List.of(burger), true);
        check(regular.getOrder().getBillCents() == 1200, "Regular 20% off.");

        VIPCustomer noHappyHour = new VIPCustomer(2);
        noHappyHour.seat();
        noHappyHour.placeOrder(List.of(burger), false);
        check(noHappyHour.getOrder().getBillCents() == 1350, "VIP-only discount.");
        expectInvalid(() -> vip.placeOrder(List.of(burger), false));

        GameController game = new GameController();
        game.beginTurn();
        game.activateHappyHour();
        game.hostSeatCustomer(1, 1);
        game.takeOrder(1);
        check(game.getCustomer(1).getOrder().getBillCents() == 1440, "Controller passes Happy Hour into order.");
        game.waitChef();
        game.finishTurn();
    }

    private static void comboTimeline() {
        GameController game = new GameController();
        for (int i = 0; i < 6; i++) idleTurn(game);
        game.beginTurn();
        game.hostSeatCustomer(3, 1);
        game.takeOrder(3);
        Order order = game.getCustomer(3).getOrder();
        check(order.getItem() instanceof ComboMeal && order.getBillCents() == 2160,
                "C3 selects first combo at normal price.");
        game.prepareOrder(3);
        game.finishTurn();
        check(order.getStatus() == OrderStatus.PREPARING && order.getCompletedUnits() == 1,
                "Combo unfinished after first unit.");
        game.beginTurn();
        game.waitHost();
        expectInvalid(() -> game.serveCustomer(3));
        game.waitWaiter();
        game.prepareOrder(3);
        game.finishTurn();
        check(order.getStatus() == OrderStatus.READY, "Combo READY after second unit.");
        game.beginTurn();
        game.waitHost();
        game.serveCustomer(3);
        game.waitChef();
        game.finishTurn();
        check(game.getCustomer(3).getStatus() == CustomerStatus.EATING, "Serving starts eating.");
        idleTurn(game);
        check(game.getCustomer(3).getStatus() == CustomerStatus.READY_TO_PAY, "Eating ends next turn.");
        idleTurn(game);
        check(game.getCustomer(3).getStatus() == CustomerStatus.PAID
                && game.getRevenueCents() == 2160, "Payment next turn, combo bill retained.");
    }

    private static void victoryRun() {
        GameController game = new GameController();
        for (int turn = 1; turn <= 18; turn++) {
            game.beginTurn();
            if (turn == 1) game.activateHappyHour();
            Table freeTable = game.getTables().stream().filter(Table::isFree).findFirst().orElse(null);
            if (freeTable != null && !game.getWaitingQueue().isEmpty()) {
                game.hostSeatCustomer(game.getWaitingQueue().get(0).getId(), freeTable.getId());
            } else {
                game.waitHost();
            }

            Customer ready = null;
            Customer needsOrder = null;
            for (Customer customer : game.getCustomers()) {
                if (customer.getStatus() != CustomerStatus.SEATED) continue;
                if (customer.getOrder() != null && customer.getOrder().getStatus() == OrderStatus.READY
                        && ready == null) ready = customer;
                if (customer.getOrder() == null && needsOrder == null) needsOrder = customer;
            }
            if (ready != null) game.serveCustomer(ready.getId());
            else if (needsOrder != null) game.takeOrder(needsOrder.getId());
            else game.waitWaiter();

            Customer toCook = null;
            for (Customer customer : game.getCustomers()) {
                Order order = customer.getOrder();
                if (order != null && (order.getStatus() == OrderStatus.PLACED
                        || order.getStatus() == OrderStatus.PREPARING)) {
                    toCook = customer;
                    break;
                }
            }
            if (toCook != null) game.prepareOrder(toCook.getId());
            else game.waitChef();
            game.finishTurn();
        }
        check(game.isFinished() && game.isVictory(), "A legal 18-turn Stage 3 strategy wins.");
        check(game.getPaidCount() == 5 && game.getRevenueCents() == 8880,
                "Expected five payments and $88.80 revenue.");
        check(Math.abs(game.getAverageSatisfaction().orElseThrow() - 83.8) < 0.001,
                "Paid satisfaction average is 83.8.");
        check(game.getCustomer(3).getOrder().getBillCents() == 2160,
                "Combo payment is included at its frozen price.");
    }

    private static void defeatReplay() {
        StringBuilder choices = new StringBuilder();
        for (int turn = 1; turn <= 18; turn++) {
            choices.append("no\n");
            choices.append("wait\nwait\nwait\n");
        }
        GameController game = new GameController();
        String output = runCli(game, choices.toString());
        check(game.isFinished() && !game.isVictory(), "18-turn idle run is defeat.");
        check(game.getPaidCount() == 0 && game.getRevenueCents() == 0, "No payments.");
        check(output.contains("Result: DEFEAT") && !output.contains("Invalid choice"), "Valid CLI replay.");
    }

    private static void cliRetries() {
        GameController game = new GameController();
        String output = runCli(game, "maybe\nno\nseat x 1\nseat 1 3\nhelp\nstatus\nseat 1 1\norder 1\ncook 1\n");
        check(game.getTurn() == 2 && game.getCustomer(1).getOrder() != null,
                "Invalid input did not consume Host task.");
        check(output.contains("Invalid input") && output.contains("Invalid choice")
                && output.contains("Host>"), "CLI gives retry feedback.");
    }

    private static void cliEof() {
        GameController game = new GameController();
        String output = runCli(game, "no\n");
        check(game.getTurn() == 1 && !game.isFinished(), "EOF leaves unfinished game.");
        check(output.contains("Input ended") && !output.contains("Result:"), "No invented result.");
    }
}
