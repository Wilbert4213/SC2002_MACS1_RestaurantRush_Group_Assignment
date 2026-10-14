package restaurantrush;

/** Shared one-task-per-turn budget. Invalid tasks leave the budget available. */
public abstract class Staff {
    private boolean available = true;

    public void startTurn() { available = true; }
    public boolean isAvailable() { return available; }

    protected void requireAvailable() {
        if (!available) {
            throw new IllegalArgumentException("This staff member has already used their task this turn.");
        }
    }

    protected void completeTask() { available = false; }

    public void waitForTurn() {
        requireAvailable();
        completeTask();
    }
}
