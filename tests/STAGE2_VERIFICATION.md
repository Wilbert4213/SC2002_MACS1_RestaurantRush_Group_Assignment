# Stage 2 terminal verification plan

Verify on `codex/stage-2` before approving the Stage 2 PR. The game uses the same
commands and turn flow as Stage 1, with VIP and Critic customer behaviour added.
Record any failure with the turn number, commands entered, and terminal output.

## 1. Build and automated checks

From the repository folder:

```sh
bash build.sh
bash test.sh
```

Expected: compilation succeeds without warnings, all 14 Stage 1 regression checks
and all 10 Stage 2 checks pass (24 total). The Stage 1 checks explicitly select regular
customers so their original results remain unchanged. The Stage 2 checks exercise
the default mixed arrivals and polymorphic customer behaviour.

## 2. Reproduce a Stage 2 victory

```sh
bash run.sh < tests/replays/stage2-victory.txt
```

| Final metric | Expected |
| --- | --- |
| Result | VICTORY |
| Turns | 18/18 |
| Served / paid | 5 / 5 |
| Revenue | $73.50 |
| Average satisfaction of paid customers | 73.00 |
| Unhappy departures | 0 |

Check that the output includes C2 (VIP), its Burger bill of $13.50, and C4
(Critic), its Pasta bill of $18.00. C6 ends seated with a READY Salad but has
not paid. The game does not finish early when a target is reached.

## 3. Check the game interactively

```sh
bash run.sh
```

Enter `help` to see the commands; it must preserve the prompt and staff task.
Enter `status` to inspect the current state without consuming a task. To reproduce
the victory manually, follow this table. Enter the Waiter command first and then
the Chef command for each turn.

| Turn | Waiter input | Chef input | Behaviour to observe |
| --- | --- | --- | --- |
| 1 | `seat 1 1` | `wait` | Regular C1 ends at satisfaction 92 |
| 2 | `order 1` | `cook 1` | C1 Pasta is PREPARING, 1/2 units |
| 3 | `wait` | `cook 1` | C1 Pasta becomes READY; not served yet |
| 4 | `serve 1` | `wait` | VIP C2 arrives at 100, ends waiting at 95; C1 starts eating |
| 5 | `seat 2 2` | `wait` | C2 ends at 90; C1 becomes READY_TO_PAY |
| 6 | `order 2` | `cook 2` | C2 chooses Burger, bill $13.50, READY, satisfaction 85; C1 pays $18.00 |
| 7 | `serve 2` | `wait` | C2 starts eating at 85; regular C3 arrives |
| 8 | `seat 3 1` | `wait` | C2 becomes READY_TO_PAY at 85 |
| 9 | `order 3` | `cook 3` | C2 pays $13.50; total revenue $31.50, paid average 80.50 |
| 10 | `serve 3` | `wait` | Critic C4 arrives at 100, ends waiting at 88 |
| 11 | `seat 4 2` | `wait` | C4 ends seated at 76; C3 becomes READY_TO_PAY |
| 12 | `order 4` | `cook 4` | C4 chooses Pasta at full $18.00; 1/2 units; satisfaction 64; C3 pays $9.00 |
| 13 | `seat 5 1` | `cook 4` | C4 Pasta becomes READY, satisfaction 52; regular C5 arrives |
| 14 | `serve 4` | `wait` | C4 starts eating; satisfaction remains 52 |
| 15 | `order 5` | `cook 5` | C4 becomes READY_TO_PAY at 52; C5 Burger becomes READY |
| 16 | `serve 5` | `wait` | C4 pays full $18.00; regular C6 arrives |
| 17 | `seat 6 2` | `wait` | C5 becomes READY_TO_PAY |
| 18 | `order 6` | `cook 6` | C5 pays $15.00; C6 Salad READY; final VICTORY metrics as above |

Check these rules throughout:

- A Waiter has one task and a Chef advances one preparation unit per turn.
- A dish finished this turn is served next turn at earliest.
- A newly served customer finishes eating next turn and pays the following turn
  at earliest. The table remains occupied until payment or abandonment.
- Regular/VIP/Critic waiting losses are 8/5/12 respectively. Served, eating, and
  READY_TO_PAY customers lose nothing.
- Order bills display two decimals and revenue includes payments only.

The Critic rule is: choose the dish with the most preparation units, taking the
first item in menu order on ties. With the base menu it chooses Pasta. The automated
checks also use a differently ordered menu and tied dishes to verify this rule.

## 4. Invalid choices and retries

Start a fresh game. At the first `Waiter>` prompt, try these separately:

```text
order 1
serve 1
seat x 1
seat 1 3
seat 99 1
```

Expected: every failure explains the issue and returns to the same Waiter prompt.
Then `seat 1 1` must succeed. At `Chef>`, try `cook 1`: it must reject the missing
order and still let you choose `wait`.

On turn 2 enter `order 1` then `cook 1`. On turn 3, `serve 1` must reject the
unfinished Pasta; `wait` followed by `cook 1` must still finish it. Continue with
turn 4 onward in the interactive table if desired.

## 5. Reproduce defeat and type-specific abandonment

```sh
bash run.sh < tests/replays/stage2-defeat.txt
```

This selects Wait for both staff for all 18 turns. Expected final result:
**DEFEAT**, 0 served/paid, $0.00 revenue, N/A average satisfaction, 2 unhappy
departures, and 18/18 turns used.

| Customer | Final state | Final satisfaction | Important timing |
| --- | --- | --- | --- |
| C1 Regular | LEFT | 0 | Leaves at end of turn 13 |
| C2 VIP | WAITING | 25 | Slower loss allows it to remain through turn 18 |
| C3 Regular | WAITING | 4 | Arrives turn 7 |
| C4 Critic | LEFT | 0 | Leaves at end of turn 18; was at 4 after turn 17 |
| C5 Regular | WAITING | 52 | Arrives turn 13 |
| C6 Regular | WAITING | 76 | Arrives turn 16 |

For a seated Critic cancellation check, start a fresh game and Wait for both
staff through turn 9. On turn 10 enter `seat 4 1`, then `wait`. On turn 11 enter
`order 4`, then `wait`. Thereafter Wait for both staff through turn 18. C4 must
be reported as leaving at zero, and revenue must remain $0.00. Since this happens
at the final turn, the CLI has no next-turn display of its table or order. The
automated integration check verifies that C4's unprepared Pasta becomes CANCELLED
and table 1 becomes free. Abandonment alone never ends the game early.

## 6. Compare stages

The README records the files added and changed for the
[Stage 1 → Stage 2 comparison](https://github.com/Wilbert4213/SC2002_MACS1_RestaurantRush_Group_Assignment/compare/stage-1...codex/stage-2).
Stage 1 is already tagged and independently runnable; Stage 2's release tag is
created only after manual verification and merging. Approve the Stage 2 PR once
the terminal behaviour matches this plan, or report any mismatches for correction.
