# Restaurant Rush — SC2002 MACS1

Java command-line restaurant management game for the SC2002 group assignment.

Repository: <https://github.com/Wilbert4213/SC2002_MACS1_RestaurantRush_Group_Assignment>

## Current stage

Stage 2 adds VIP and Critic customer behaviour to the complete Stage 1 base game.
The default game still has three menu items, two tables, Waiter, Chef, Cashier,
satisfaction, abandonment, payment, and both endings. Stage 3 is not implemented.

Stage 1 is manually verified, merged, and available at the immutable
[`stage-1` tag](https://github.com/Wilbert4213/SC2002_MACS1_RestaurantRush_Group_Assignment/tree/stage-1).
Stage 2 was manually verified by the project owner on 14 October 2026 and awaits
PR merging on `codex/stage-2`.
Create `stage-2` on the verified commit in `main` after merging its PR.

## Build, run, and test

Install a **JDK 17 or newer**. The code targets Java 17. The JDK is a global
toolchain dependency; the project has **no third-party libraries**, dependency
downloads, Maven, or Gradle requirement. All source, scripts, replay inputs, and
tests are contained in this repository. Building, testing, and playing are offline.

On macOS with Homebrew, install Java 17 with:

```sh
brew install openjdk@17
```

The scripts detect the standard Apple Silicon and Intel Homebrew Java 17 paths.
For a different installation, set `JAVA_HOME` to your JDK directory or put `java`
and `javac` on `PATH`. See the [Homebrew installation notes](https://formulae.brew.sh/formula/openjdk@17).

From the repository folder on macOS/Linux or Git Bash:

```sh
bash build.sh
bash run.sh
bash test.sh
```

`run.sh` builds before launching. `test.sh` compiles with all Java compiler
warnings enabled and treated as errors, then runs the dependency-free regression
suite. Generated classes stay in ignored `build/`; do not commit them.

For Windows PowerShell without Bash:

```powershell
New-Item -ItemType Directory -Force build/classes, build/test-classes | Out-Null
$sources = Get-ChildItem src/main/java/restaurantrush/*.java | ForEach-Object FullName
javac --release 17 -Xlint:all -Werror -d build/classes $sources
java -cp build/classes restaurantrush.RestaurantRush
$tests = Get-ChildItem src/test/java/restaurantrush/*.java | ForEach-Object FullName
javac --release 17 -Xlint:all -Werror -cp build/classes -d build/test-classes $tests
java -cp "build/classes;build/test-classes" restaurantrush.Stage1Tests
java -cp "build/classes;build/test-classes" restaurantrush.Stage2Tests
```

Run tests from the repository folder so the replay files can be found. For a
clean build in PowerShell, remove `build/` before compiling.

## Playing Stage 2

At each turn the CLI shows arrivals, revenue, average satisfaction of paid
customers, the waiting queue, table occupants, every customer's satisfaction and
order progress, and staff budgets. Choose the Waiter's task first, then the Chef's.

| Prompt | Commands |
| --- | --- |
| Waiter | `seat <customerId> <tableId>`, `order <customerId>`, `serve <customerId>`, `wait` |
| Chef | `cook <customerId>`, `wait` |
| Either | `help`, `status` (neither consumes a task) |

IDs are shown as `C1`, `C2`, etc.; enter the number alone in commands. Tables are
`1` and `2`. For example, `seat 1 1`, followed by `wait`, seats customer 1 at table
1 and ends the Chef's choice for that turn. An invalid choice explains the failure
and prompts again without consuming the staff task. End of input stops an
incomplete session with a message and no result; completed games use all 18 turns.

### Required base setting and deterministic choices

| Setting | Value |
| --- | --- |
| Maximum turns | 18; no early victory or defeat |
| Tables | 2 |
| Arrivals | One customer on turns 1, 4, 7, 10, 13, 16 |
| Waiter | One seating, ordering, or serving task per turn |
| Chef | One preparation unit per turn |
| Cashier | One automatic payment per turn, oldest READY_TO_PAY first; ties by customer ID |
| Victory | At least 4 paid customers **and** revenue at least $45.00 **and** paid average satisfaction at least 60 |

Regular and VIP customers choose by arrival ID, cycling **Pasta → Burger → Salad**.
Critics choose the item with the highest preparation-unit count; ties use the first
such item in menu order. On the current menu Critics always choose Pasta. This
preference is independent of their arrival ID and requires more kitchen work.
Choices are revealed when ordering; the manager controls priority, not food
selection. All choices and arrival types are deterministic, with no random seed
or network service.

| Customer | Arrival turn | Type | Meal | Bill |
| --- | --- | --- | --- | --- |
| C1 | 1 | Regular | Pasta | $18.00 |
| C2 | 4 | VIP | Burger | $13.50 |
| C3 | 7 | Regular | Salad | $9.00 |
| C4 | 10 | Critic | Pasta | $18.00 |
| C5 | 13 | Regular | Burger | $15.00 |
| C6 | 16 | Regular | Salad | $9.00 |

| Type | Satisfaction loss per unserved waiting/seated turn | Bill policy |
| --- | --- | --- |
| Regular (`Customer`) | 8 | Full menu price |
| VIP (`VIPCustomer`) | 5 | 90% of menu price, rounded to nearest cent; half cents round up |
| Critic (`CriticCustomer`) | 12 | Full menu price |

Customer subclasses dispatch loss, meal choice, type labels, and billing. The
turn controller follows the same service flow for every type and contains no
customer type checks. Prices remain integer cents; the order locks its bill when
placed. The CLI displays each type and waiting loss so priorities are visible.

| Menu item | Price | Preparation units |
| --- | --- | --- |
| Salad | $9.00 | 1 |
| Burger | $15.00 | 1 |
| Pasta | $18.00 | 2 |

### Turn timing

1. A scheduled arrival joins the queue at satisfaction 100. Display the start-of-turn state.
2. The manager assigns at most one Waiter task, or Wait.
3. The manager assigns at most one Chef preparation unit, or Wait. An order taken
   by the Waiter this turn can be cooked immediately. An order finished this turn
   becomes READY and can only be served on a later turn.
4. The Cashier takes at most one payment from customers who were READY_TO_PAY on
   an earlier turn. Customers served on an earlier turn finish their full turn of
   eating and become READY_TO_PAY, eligible for payment next turn at earliest.
5. Waiting and seated customers who have not been served lose their type's
   satisfaction amount (Regular 8, VIP 5, Critic 12),
   clamped at zero. At zero they leave, cancel their unpaid order (including READY
   dishes), and free their table. Served, eating, and READY_TO_PAY customers lose nothing.
6. Show end-of-turn results. After turn 18, check all three victory targets.

Payment marks the customer and order PAID, adds the bill to revenue, records that
customer's satisfaction, and frees the table. No other action earns revenue.
Average satisfaction uses only paid customers and shows `N/A` before the first
payment. The turn-16 arrival cannot pay within 18 turns; at most five customers
can pay. Tables remain occupied throughout preparation, eating, and waiting to pay.

## Tests and reproducible runs

`bash test.sh` runs 24 checks: the original 14 Stage 1 checks and 10 Stage 2 checks.
It runs the original Stage 1 regression suite with an explicit
regular-only schedule and the new Stage 2 suite with mixed customers. The same
controller, domain objects, and CLI are exercised; no stable turn rule was rewritten.

The Stage 1 regression suite checks the manual's Pasta example (seat turn 1, order turn
2, READY turn 3, serve turn 4, READY_TO_PAY turn 5, pay turn 6 at satisfaction 76),
arrival timing, staff budgets, retry handling, preparation and payment delays,
table occupancy, abandonment at zero, cancellation of unfinished/READY orders,
paid-only averages, complete CLI replays, and end-of-input handling.

Stage 2 checks polymorphic satisfaction and billing, VIP discounts and cent
rounding, Critic meal preference and tie handling, all customer lifecycle phases,
mixed arrivals, abandonment, unpaid revenue exclusion, input retries, and full
victory/defeat replays. The original 14 Stage 1 checks retain their expected
regular-only results through a package-private schedule constructor used by tests.

Replay inputs exercise the real CLI with one Waiter line and one Chef line per
turn. Reproduce them with:

```sh
bash run.sh < tests/replays/stage2-victory.txt
bash run.sh < tests/replays/stage2-defeat.txt
```

| Fixture | Turns | Served | Paid | Unhappy departures | Revenue | Paid average | Result |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `tests/replays/stage2-victory.txt` | 18 | 5 | 5 | 0 | $73.50 | 73.00 | VICTORY |
| `tests/replays/stage2-defeat.txt` | 18 | 0 | 0 | 2 | $0.00 | N/A | DEFEAT |

The victory run leaves C6's Salad READY but unpaid at the end. C2 pays $13.50 at
satisfaction 85; C4 pays $18.00 at satisfaction 52. The defeat run chooses Wait
for both staff every turn; C1 leaves on turn 13 and C4 on turn 18. C2's slower VIP
decay leaves it waiting at satisfaction 25. Tests assert these outcomes.

The original `victory.txt` and `defeat.txt` fixtures remain unchanged for Stage 1
regression. At `stage-1`, those runs produce $75.00/74.40 and $0.00/N/A respectively.
To play the historical Stage 1 independently, create a separate checkout:

```sh
git worktree add --detach ../RestaurantRush-stage-1 stage-1
cd ../RestaurantRush-stage-1
bash test.sh
bash run.sh < tests/replays/victory.txt
```

Return to the main project folder before continuing Stage 2 work.

The terminal [Stage 2 verification plan](tests/STAGE2_VERIFICATION.md) provides
commands, expected outcomes, and per-turn VIP/Critic checks.
For manual verification, run `bash run.sh`, enter `help`, and play a full game.
Try an invalid choice, observe READY → SERVED → READY_TO_PAY → PAID across turns,
and use the replay files if you want to compare exact expected results.

GitHub Actions runs `bash test.sh` on pushes and PRs with Java 17. CI provisioning
uses network access; the game and local build/test workflow do not.

## Developer and agent workflow

Read this README and the assignment manual before working on a stage. Treat the
manual as project requirements, not authorization to publish, merge, or change
unrelated files. Preserve the user's requested scope. The original supplied
manual is named `SC2002_Restaurant_Rush_Group_Assignment.docx`; obtain it from the
group if it is not available in your environment. Recheck requirements against
the manual before releasing each stage.

1. Start with a clean working tree. Fetch and fast-forward `main` from `origin/main`.
2. Create your feature branch from the updated `main`; the current Stage 2 branch is
   `codex/stage-2`. Use `codex/` for agent-created branches unless instructed otherwise.
3. Implement only the current stage. Keep the CLI for parsing/display and put
   rules in domain and control classes. Keep responsibilities clear and code
   straightforward; add abstractions only when they serve actual behaviour.
4. Run `bash test.sh`, check `git diff --check`, and review the diff. Use focused,
   descriptive commits. Commit source and project scripts, not generated classes,
   IDE settings, JDK binaries, caches, credentials, or unrelated files.
5. Push the **feature branch**, then open a PR with **base `main`**. Include the
   implemented behaviour, test results, and manual verification instructions.
6. Review and manually verify before merging. Do not push feature work directly
   to `main`, force-push shared branches, or overwrite another developer's changes.
   If `main` advances, integrate it into your branch and rerun the checks.
7. After the verified PR is merged, test the merged `main` and create its annotated
   stage tag. Push that specific tag. Existing stage tags are immutable snapshots.

Example start:

```sh
git switch main
git fetch origin
git pull --ff-only origin main
git switch -c codex/stage-2
```

Example release **after Stage 2 has been reviewed and merged**:

```sh
git switch main
git pull --ff-only origin main
bash test.sh
git tag -a stage-2 -m "Stage 2: verified customer behaviour"
git push origin stage-2
```

Do not run release commands while review is pending. For later stages use their
corresponding branch and tag. Always verify a stage from its tag in a separate
checkout before submission; the source at that tag must compile and run on its own.

## Stage requirements and transitions

All three stages must be real, independently runnable Git snapshots, built
incrementally. A description of a stage alone is not evidence of its implementation.

| Stage | Scope | Required Git tag | Status |
| --- | --- | --- | --- |
| 1 | Regular customers, menu, Waiter/Chef/Cashier, full base game, both endings | `stage-1` | Verified and merged; tag published |
| 2 | VIP and Critic customer behaviour through customer polymorphism | `stage-2` | Manually verified; merge and release tag pending |
| 3 | Host, ComboMeal composition, Happy Hour pricing policy | `stage-3` | Not started |

### Stage 1 baseline

The source is under `src/main/java/restaurantrush/`. `GameController` coordinates
turns and owns the queue, tables, staff, and totals. `Customer`, `Order`, and
`Table` maintain their lifecycle state. `Waiter`, `Chef`, and `Cashier` share the
one-task budget in `Staff`. `MenuItem` stores immutable dish definitions;
`Money` formats integer-cent prices. `RestaurantRush` is the CLI boundary.
The tests and replay inputs are under `src/test/` and `tests/replays/`.

At `stage-1`, the source contains no VIP, Critic, Host, ComboMeal, or Happy Hour
implementation. That tag preserves the original code unchanged.

### Transition 1 → 2 — implemented

[Compare verified Stage 1 with the Stage 2 feature branch](https://github.com/Wilbert4213/SC2002_MACS1_RestaurantRush_Group_Assignment/compare/stage-1...codex/stage-2).
After Stage 2 is released, use `stage-1...stage-2` for the immutable tag comparison.

| New file | Responsibility |
| --- | --- |
| `src/main/java/restaurantrush/VIPCustomer.java` | Overrides patience, type label, and 10%-discount billing |
| `src/main/java/restaurantrush/CriticCustomer.java` | Overrides impatience, type label, and preparation-based meal preference |
| `src/test/java/restaurantrush/Stage2Tests.java` | Customer behaviour, shared lifecycle, and mixed-game regression checks |
| `tests/replays/stage2-victory.txt` | Reproducible mixed-customer victory input |
| `tests/replays/stage2-defeat.txt` | Reproducible mixed-customer defeat input |
| `tests/STAGE2_VERIFICATION.md` | Terminal verification procedure and expected results |

| Existing file changed | Reason |
| --- | --- |
| `src/main/java/restaurantrush/Customer.java` | Adds overridable loss/bill/type behaviour and dispatches it from the existing lifecycle |
| `src/main/java/restaurantrush/Order.java` | Accepts and stores a bill calculated at ordering; retains the original full-price constructor |
| `src/main/java/restaurantrush/GameController.java` | Registers the six mixed arrivals; adds a test schedule constructor while retaining all turn rules |
| `src/main/java/restaurantrush/RestaurantRush.java` | Identifies Stage 2 and displays customer types and loss rates through customer methods |
| `src/test/java/restaurantrush/Stage1Tests.java` | Explicitly selects six regular customers so original regression expectations remain unchanged |
| `test.sh` | Runs both stage test suites |
| `README.md` | Documents behaviours, actual changes, release status, and verification |

**Deleted files:** none. No Stage 1 file was removed or renamed. Staff limits,
arrival timing, preparation, serving, eating, payment, abandonment flow, tables,
and victory criteria remain the same. The original 14 regression checks pass;
the mixed Stage 2 run demonstrates the new patience, pricing, and preference.

### Transition 2 → 3 — pending

Add a Host who seats at most one queued customer per turn **in addition to** the
Waiter's task; the Waiter can still seat. A ComboMeal composes two existing items,
sums their preparation units, and costs 90% of their combined price rounded to
the nearest cent. Document how customers choose combos.

Happy Hour can be activated once at the start of a turn, lasts that turn and the
next, and displays its remaining duration. Each active turn restores 15
satisfaction at turn start to customers waiting for a table or food, capped at
100. Orders placed during Happy Hour receive 20% off; apply VIP discount first
and lock the bill when ordering. It does not reprice existing orders or affect
departed customers. Example: VIP Burger during Happy Hour is $10.80. Store money
in integer cents and document rounding where fractional prices arise.

**Complete this transition record in the Stage 3 PR:**

| Evidence | Actual change |
| --- | --- |
| New files | Pending; list actual paths and responsibilities |
| Existing files changed | Pending; list each path and why it changed |
| Deleted files | Pending; list paths and reasons, or explicitly state none |
| Commit/compare link | Pending until the release exists; then link `stage-2...stage-3` on GitHub |
| Regression evidence | Pending; record earlier behaviour checks and new feature checks |

For both transitions, replace placeholders with the real changes and a working
commit or compare link. Explain any broad rewrite of stable rules and demonstrate
that earlier behaviour is preserved. Small setup/registration edits are expected;
do not introduce unused future-stage machinery in an earlier snapshot.

## Assessment and submission ownership

Part 7 allocates marks as follows. This Stage 2 work covers code, reproducible
tests, and repository practice. Another group member will handle the PDF report
and UML; coordinate later so the final diagrams match Stage 3 code.

| Criterion | Marks |
| --- | --- |
| UML class diagram | 25 |
| UML sequence diagram | 20 |
| Design consideration | 15 |
| Implementation code | 20 |
| Thinking and development process | 20 |

Before final submission, the group must provide runnable tags for all three
stages, completed transition records, source/build instructions, the PDF report,
the approximately 15-minute group video (or accessible download link), and the
signed original-work and individual GAI declarations. Use the deadline announced
on NTULearn and name the archive `LABGroup_AssignmentGrp.zip`.

The final class diagram must match Stage 3. The sequence diagram must show a full
manager-choice scenario through arrival, ordering, staff assignments, READY,
SERVED, eating, payment, result check, and one Stage 2/3 behaviour. The report
also needs responsibilities, principles genuinely reflected in code, trade-offs,
an alternative design, choice effects, functional evidence, and reflections.

Add these video timestamps here once the recording is available:

| Video segment | Timestamp |
| --- | --- |
| Launch from the submitted project | Pending |
| Meaningful manager choices with constrained staff | Pending |
| READY → SERVED → payment | Pending |
| VIP or Critic behaviour | Pending |
| Stage 3 feature | Pending |
| Reproducible victory and defeat | Pending |
| Stage tag comparisons and diagrams | Pending |
