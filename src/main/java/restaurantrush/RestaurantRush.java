package restaurantrush;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Scanner;

/**
 * CLI boundary: parses choices and displays state; GameController owns the
 * rules.
 */
public class RestaurantRush {
    private final GameController game;
    private final Scanner input;
    private final PrintStream output;

    public RestaurantRush(GameController game, Scanner input, PrintStream output) {
        this.game = game;
        this.input = input;
        this.output = output;
    }

    public static void main(String[] args) {
        new RestaurantRush(new GameController(), new Scanner(System.in), System.out).run();
    }

    public void run() {
        output.println(game.isStage3Enabled() ? "Restaurant Rush - Stage 3" : "Restaurant Rush - Legacy Mode");
        output.println("18 turns, 2 tables. Target: 4 paid customers, $45.00 revenue, 60 average satisfaction.");
        output.println("Customers choose their own food. Enter help at either prompt for commands.");
        output.println("Regulars lose 8 satisfaction while waiting. VIPs lose 5 and get 10% off.");
        output.println("Critics lose 12 and prefer the dish requiring the most preparation.");
        output.println("Menu:");
        for (MenuItem item : game.getMenu()) {
            output.printf("  %s: %s, %d preparation units%n", item.getName(),
                    Money.format(item.getPriceCents()), item.getPreparationUnits());
        }
        while (!game.isFinished()) {
            game.beginTurn();
            displayState();
            if (game.isStage3Enabled()) {
                if (!chooseHappyHour()) {
                    output.println("Input ended; game stopped.");
                    return;
                }

                if (!chooseHostTask()) {
                    output.println("Input ended; game stopped before completion. No victory or defeat recorded.");
                    return;
                }
            }

            if (!chooseTask(true) || !chooseTask(false)) {
                output.println("Input ended; game stopped before completion. No victory or defeat recorded.");
                return;
            }
            for (String event : game.finishTurn()) {
                output.println(event);
            }
            output.print("End of turn " + game.getTurn() + ": ");
            displaySummary();
        }
        output.println("\nResult: " + (game.isVictory() ? "VICTORY" : "DEFEAT"));
        displaySummary();
        output.printf("Targets: paid %d/%d, revenue %s/%s, satisfaction %s/%d%n",
                game.getPaidCount(), GameController.TARGET_PAID_CUSTOMERS,
                Money.format(game.getRevenueCents()), Money.format(GameController.TARGET_REVENUE_CENTS),
                averageText(), GameController.TARGET_SATISFACTION);
    }

    private boolean chooseHostTask() {
        while (true) {
            output.print("Host> ");
            output.flush();

            if (!input.hasNextLine()) {
                return false;
            }

            String line = input.nextLine().trim();

            if (line.equalsIgnoreCase("help")) {
                output.println("seat <customerId> <tableId> | wait | status | help");
                continue;
            }

            if (line.equalsIgnoreCase("status")) {
                displayState();
                continue;
            }

            try {
                String[] parts = line.toLowerCase(Locale.ROOT).split("\\s+");
                String message;

                if (parts.length == 1 && parts[0].equals("wait")) {
                    game.waitHost();
                    message = "Host waits.";
                } else if (parts.length == 3 && parts[0].equals("seat")) {
                    message = game.hostSeatCustomer(
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]));
                } else {
                    throw new IllegalArgumentException(
                            "Unknown command. Enter help.");
                }

                output.println(message);
                return true;

            } catch (NumberFormatException exception) {
                output.println("Invalid choice: IDs must be whole numbers.");
            } catch (IllegalArgumentException exception) {
                output.println("Invalid choice: " + exception.getMessage());
            }
        }
    }

    private boolean chooseTask(boolean waiterPhase) {
        String role = waiterPhase ? "Waiter" : "Chef";
        while (true) {
            output.print(role + "> ");
            output.flush();
            if (!input.hasNextLine()) {
                return false;
            }
            String line = input.nextLine().trim();
            if (line.equalsIgnoreCase("help")) {
                output.println(waiterPhase
                        ? "seat <customerId> <tableId> | order <customerId> | serve <customerId> | wait | status | help"
                        : "cook <customerId> | wait | status | help");
                continue;
            }
            if (line.equalsIgnoreCase("status")) {
                displayState();
                continue;
            }
            try {
                String[] parts = line.toLowerCase(Locale.ROOT).split("\\s+");
                String message;
                if (parts[0].equals("wait") && parts.length == 1) {
                    if (waiterPhase) {
                        game.waitWaiter();
                    } else {
                        game.waitChef();
                    }
                    message = role + " waits.";
                } else if (waiterPhase && parts[0].equals("seat") && parts.length == 3) {
                    message = game.seatCustomer(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                } else if (waiterPhase && parts[0].equals("order") && parts.length == 2) {
                    message = game.takeOrder(Integer.parseInt(parts[1]));
                } else if (waiterPhase && parts[0].equals("serve") && parts.length == 2) {
                    message = game.serveCustomer(Integer.parseInt(parts[1]));
                } else if (!waiterPhase && parts[0].equals("cook") && parts.length == 2) {
                    message = game.prepareOrder(Integer.parseInt(parts[1]));
                } else {
                    throw new IllegalArgumentException("Unknown command or wrong number of arguments. Enter help.");
                }
                output.println(message);
                return true;
            } catch (NumberFormatException exception) {
                output.println("Invalid choice: customer and table IDs must be whole numbers. Choose again.");
            } catch (IllegalArgumentException exception) {
                output.println("Invalid choice: " + exception.getMessage() + " Choose again.");
            }
        }
    }

    private void displayState() {
        output.printf("%n--- Turn %d/%d ---%n", game.getTurn(), GameController.MAX_TURNS);
        output.println("Revenue: " + Money.format(game.getRevenueCents()) + " | Paid average: " + averageText());
        output.println("Waiting queue: " + (game.getWaitingQueue().isEmpty() ? "empty"
                : game.getWaitingQueue().stream().map(customer -> "C" + customer.getId())
                        .collect(java.util.stream.Collectors.joining(", "))));
        output.println("Happy Hour: "
        + (game.isHappyHourActive()
            ? "ACTIVE (" + game.getHappyHourRemainingTurns()
                    + " turns remaining)"
            : game.isHappyHourUsed()
                ? "Already used"
                : "Available"));

        for (Table table : game.getTables()) {
            output.println("Table " + table.getId() + ": " + (table.isFree() ? "free"
                    : "C" + table.getCustomer().getId()));
        }
        for (Customer customer : game.getCustomers()) {
            Order order = customer.getOrder();
            output.printf("C%d (%s): %s | satisfaction %d | waiting loss %d/turn | order %s%n",
                    customer.getId(), customer.getTypeName(), customer.getStatus(),
                    customer.getSatisfaction(), customer.getWaitingSatisfactionLoss(), order == null ? "none"
                            : order.getItem().getName() + " " + order.getStatus() + " "
                                    + order.getCompletedUnits() + "/" + order.getItem().getPreparationUnits()
                                    + " | bill " + Money.format(order.getBillCents()));
        }
        output.println("Waiter: " + (game.isWaiterAvailable() ? "1 task: seat / order / serve / wait" : "task used"));
        output.println("Host: " + (game.isHostAvailable()? "1 task: seat / wait" : "task used"));
        output.println("Chef: " + (game.isChefAvailable() ? "1 preparation unit: cook / wait" : "task used"));
        output.println(
                "Cashier: " + (game.isCashierAvailable() ? "1 automatic payment, oldest eligible first" : "task used"));
    }

    private String averageText() {
        return game.getAverageSatisfaction().isPresent()
                ? String.format(Locale.ROOT, "%.2f", game.getAverageSatisfaction().getAsDouble())
                : "N/A";
    }

    private void displaySummary() {
        output.printf("served %d | paid %d | unhappy departures %d | revenue %s | paid average %s | turns %d/%d%n",
                game.getServedCount(), game.getPaidCount(), game.getUnhappyDepartures(),
                Money.format(game.getRevenueCents()), averageText(), game.getTurn(), GameController.MAX_TURNS);
    }

    private boolean chooseHappyHour() {

        // Happy Hour can only be activated once per game.
        if (game.isHappyHourUsed()) {
            return true;
        }

        while (true) {
            output.print("Activate Happy Hour? (yes/no)> ");
            output.flush();

            if (!input.hasNextLine()) {
                return false;
            }

            String answer = input.nextLine().trim();

            if (answer.equalsIgnoreCase("yes")) {
                game.activateHappyHour();
                output.println("Happy Hour activated for 2 turns!");
                return true;
            }

            if (answer.equalsIgnoreCase("no")) {
                return true;
            }

            output.println("Invalid input. Enter yes or no.");
        }
    }
}
