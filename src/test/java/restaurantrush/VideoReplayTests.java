package restaurantrush;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

/** Checks the recorded choices through the actual Stage 3 CLI. */
public final class VideoReplayTests {
    private VideoReplayTests() { }

    public static void main(String[] args) throws IOException {
        checkVictory();
        System.out.println("PASS guided video victory replay");
        checkDefeat();
        System.out.println("PASS guided video defeat replay");
        checkStoppedReplay();
        System.out.println("PASS guided video replay stops when input ends");
        System.out.println("All 3 video replay checks passed.");
    }

    private static String play(GameController game, String replay, int enterCount) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        ByteArrayInputStream controls = new ByteArrayInputStream("\n".repeat(enterCount)
                .getBytes(StandardCharsets.UTF_8));
        VideoReplay.ReplayInput choices = new VideoReplay.ReplayInput(
                VideoReplay.readTurns(Path.of("tests/replays/video-" + replay + ".txt")), controls, output);
        new RestaurantRush(game, new Scanner(choices, StandardCharsets.UTF_8), output).run();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static void checkVictory() throws IOException {
        GameController game = new GameController();
        String output = play(game, "victory", 18);
        check(game.isFinished() && game.isVictory() && output.contains("Result: VICTORY"), "Victory result.");
        check(game.getPaidCount() == 5 && game.getRevenueCents() == 8880
                && Math.abs(game.getAverageSatisfaction().orElseThrow() - 83.8) < 0.001,
                "Victory totals match the video notes.");
        check(output.contains("Happy Hour activated") && output.contains("Host seated customer 1")
                && output.contains("Salad + Burger Combo") && output.contains("Customer 2 paid $13.50"),
                "The replay shows the behaviours described in the video notes.");
        check(!output.contains("Invalid choice") && !output.contains("Invalid input"), "Every choice is valid.");
        check(count(output, "[Replay] Press Enter") == 18, "Pause exactly once per turn.");
    }

    private static void checkDefeat() throws IOException {
        GameController game = new GameController();
        String output = play(game, "defeat", 18);
        check(game.isFinished() && !game.isVictory() && output.contains("Result: DEFEAT"), "Defeat result.");
        check(game.getPaidCount() == 0 && game.getRevenueCents() == 0
                && game.getAverageSatisfaction().isEmpty() && game.getUnhappyDepartures() == 2,
                "Defeat totals match the video notes.");
        check(!output.contains("Invalid choice") && !output.contains("Invalid input"), "Every choice is valid.");
        check(count(output, "[Replay] Press Enter") == 18, "Pause exactly once per turn.");
    }

    private static void checkStoppedReplay() throws IOException {
        GameController game = new GameController();
        String output = play(game, "victory", 1);
        check(!game.isFinished() && game.getTurn() == 2, "EOF stops before the second turn's choices.");
        check(output.contains("Input ended") && !output.contains("Result:"), "No result is invented.");
    }

    private static int count(String text, String phrase) {
        int count = 0;
        int position = 0;
        while ((position = text.indexOf(phrase, position)) != -1) {
            count++;
            position += phrase.length();
        }
        return count;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
