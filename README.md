# Video runs

Open a terminal in the project folder. Start either run with the command below.
Press **Enter once per turn**. Pause between turns to explain what happens.

## Victory run

```sh
bash video.sh victory
```

The final result is **VICTORY**: 5 customers paid, revenue is $88.80, and average satisfaction is 83.80.

### What to say

- **Turn 1:** “We activate Happy Hour. The Host seats customer 1, the Waiter takes the order, and the Chef starts cooking. The Pasta costs $14.40 after the discount.”
- **Turns 2–5:** “The Pasta is ready on turn 2 and served on turn 3. The customer finishes eating on turn 4 and pays on turn 5. The table stays occupied until payment.”
- **Turns 4–7:** “Customer 2 is a VIP. The VIP gets 10% off the Burger and pays $13.50 on turn 7. Happy Hour has already ended.”
- **Turns 7–11:** “Customer 3 chooses the Salad and Burger Combo. It costs $21.60 and needs two preparation units. It is served on turn 9 and paid on turn 11.”
- **Turns 10–15:** “Customer 4 is a Critic and chooses the Salad and Pasta Combo. It needs three preparation units. On turn 13, the Host seats customer 5 while the Waiter serves the Critic.”
- **Turn 18:** “We have five paid customers, $88.80 in revenue and 83.80 average satisfaction. All three targets are met, so we win.”

## Defeat run

```sh
bash video.sh defeat
```

The final result is **DEFEAT**: nobody paid, revenue is $0.00, and two customers left unhappy.

### What to say

- **Start:** “In this run, we do not start Happy Hour. We choose Wait for the Host, Waiter and Chef on every turn.”
- **As customers arrive:** “Nobody is seated or served. Customers lose satisfaction while they wait.”
- **Turn 13:** “Customer 1 reaches zero satisfaction and leaves without paying.”
- **Turn 18:** “The Critic also leaves. The VIP is still waiting because VIPs lose satisfaction more slowly. The game still runs all 18 turns.”
- **Final result:** “Nobody paid and revenue is zero, so we lose. The average satisfaction of paid customers is N/A because there were no payments.”
