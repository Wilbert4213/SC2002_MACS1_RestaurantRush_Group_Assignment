package restaurantrush;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Plays recorded manager choices through the normal Stage 3 CLI, one turn at a time. */
public final class VideoReplay {
    private VideoReplay() { }

    public static void main(String[] args) throws IOException {
        if (args.length != 1 || (!args[0].equals("victory") && !args[0].equals("defeat"))) {
            System.err.println("Usage: bash video.sh victory|defeat");
            return;
        }
        List<List<String>> turns = readTurns(Path.of("tests/replays/video-" + args[0] + ".txt"));
        System.out.println("Video replay: " + args[0] + ". Press Enter once per turn.");
        InputStream commands = new ReplayInput(turns, System.in, System.out);
        new RestaurantRush(new GameController(), new Scanner(commands, StandardCharsets.UTF_8), System.out).run();
    }

    static List<List<String>> readTurns(Path path) throws IOException {
        List<List<String>> turns = new ArrayList<>();
        List<String> commands = new ArrayList<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                if (!commands.isEmpty()) {
                    turns.add(List.copyOf(commands));
                    commands.clear();
                }
            } else {
                commands.add(line.trim());
            }
        }
        if (!commands.isEmpty()) {
            turns.add(List.copyOf(commands));
        }
        if (turns.size() != GameController.MAX_TURNS) {
            throw new IllegalArgumentException("A video replay must contain 18 turns separated by blank lines.");
        }
        return List.copyOf(turns);
    }

    /** Makes each recorded command available only when the CLI asks for it. */
    static final class ReplayInput extends InputStream {
        private final List<List<String>> turns;
        private final BufferedReader controls;
        private final PrintStream output;
        private int turn = -1;
        private int command;
        private byte[] line = new byte[0];
        private int offset;

        ReplayInput(List<List<String>> turns, InputStream controls, PrintStream output) {
            this.turns = turns;
            this.controls = new BufferedReader(new InputStreamReader(controls, StandardCharsets.UTF_8));
            this.output = output;
        }

        @Override
        public int read() throws IOException {
            byte[] oneByte = new byte[1];
            return read(oneByte, 0, 1) == -1 ? -1 : oneByte[0] & 0xff;
        }

        @Override
        public int read(byte[] buffer, int start, int length) throws IOException {
            if (length == 0) return 0;
            if (offset == line.length && !nextCommand()) return -1;
            int count = Math.min(length, line.length - offset);
            System.arraycopy(line, offset, buffer, start, count);
            offset += count;
            return count;
        }

        private boolean nextCommand() throws IOException {
            if (turn == -1 || command == turns.get(turn).size()) {
                turn++;
                if (turn == turns.size()) return false;
                command = 0;
                output.print("[Replay] Press Enter to play turn " + (turn + 1) + "> ");
                output.flush();
                if (controls.readLine() == null) return false;
            }
            String choice = turns.get(turn).get(command++);
            output.println(choice);
            line = (choice + "\n").getBytes(StandardCharsets.UTF_8);
            offset = 0;
            return true;
        }
    }
}
