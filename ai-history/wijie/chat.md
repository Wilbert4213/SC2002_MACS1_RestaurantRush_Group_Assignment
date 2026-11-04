**Stage 1:** We considered how to represent customers and orders as the game moves from arrival through payment. I questioned whether a single game-state variable could capture both the customer’s situation and the order’s progress without creating ambiguous combinations. We discussed separating those responsibilities and checking that transitions, such as serving and payment, happen only when their prerequisites are met.

We also discussed how `Waiter`, `Chef`, `Cashier`, `Table` and `Order` should interact. I considered whether staff should directly control the whole turn or perform narrower tasks coordinated by a controller. The discussion focused on keeping responsibilities clear while preserving the required order of actions.

**Stage 2:** We considered how `VIPCustomer` and `CriticCustomer` could extend the regular customer behaviour. I questioned whether putting customer-type checks throughout the controller would make later changes harder. We discussed using customer methods for differences such as satisfaction loss and pricing, while keeping the common service flow shared.

For integration, we considered which existing Stage 1 classes needed changes and how to check that regular customers, payment and the turn sequence still worked after adding the subclasses.

# Conversation

Stage 1: Basic Restaurant Operation
1. Representing game states
Student: I need to represent the game from a customer arriving until they pay or leave. My first idea is to use one status for the whole process: WAITING, SEATED, ORDERED, READY, SERVED, READY_TO_PAY and PAID. Would that be sufficient?

AI: It could work, but first ask what each status describes. Does READY describe the customer or the order? Could the customer still be seated while their order changes from preparing to ready?

Try tracing a customer through the process and noting which facts must be visible at the same time. That should help you decide whether one status is clear enough.

Student: The customer is still seated while the order is being prepared, so I think those are two separate things. I could have a CustomerStatus for whether the customer is waiting, seated, ready to pay, paid or left, and an OrderStatus for whether the food is preparing, ready, served or paid.

But if I separate them, the statuses could contradict each other. For example, the customer might be marked as paid while the order is still preparing.

AI: That is the main trade-off. Separate statuses describe the two objects more clearly, but you need to control the operations that update them. Consider these questions:
- Which object or operation can mark an order as served?
- Which operation updates both the customer and order after payment?
- What should happen if an operation is attempted in the wrong state?
You could use those questions to define valid transitions before implementing them.

Student: I think the waiter’s serving action should check that the order is already READY. For payment, the cashier should check that the customer is READY_TO_PAY, then update the order, customer, revenue and table together.

I also need to be careful with timing: the chef cannot finish a dish and have the waiter serve it in the same turn, because the waiter acts first.

AI: That follows the brief. How will you distinguish an order that was ready before the waiter acted from one that becomes ready later in the same turn?

Student: If I execute the waiter action before the chef action, the waiter can only see the order’s state before the chef works on it. That seems simpler than storing an extra “turn became ready” value for serving.

For payment, though, I may need to record when the customer became ready to pay, because the brief says they cannot pay until the next turn.

AI: That distinction is worth testing. The order of operations may enforce the serving rule naturally, while payment needs enough information to distinguish someone who became ready this turn from someone who was already ready.

A useful test would follow one customer turn by turn and check the earliest possible serving and payment turns.
2. Dividing responsibilities between classes
Student: I’m considering putting most game logic in RestaurantRush.java. It would contain the main loop, customers, tables, orders and all the staff actions. That seems easier to get working quickly. Is there a reason to split it?

AI: A single class might be quick initially, but consider how you would locate and change one rule later. For example, if the chef’s preparation logic changes, would it be easy to find without affecting input handling or payment?

Before deciding on classes, list the responsibilities you need. Which ones represent game entities, which coordinate the turn, and which interact with the player?

Student: I think the main class should start the program. A controller could coordinate the turn and the order of actions. Customer, Order and Table would hold the relevant state. The waiter, chef and cashier would perform their respective tasks.

I’m less sure where input validation belongs. Some input is invalid because it is not a number, but other input is invalid because the chosen table is occupied.

AI: Those are two different checks. The input layer can check whether it can read a choice in the expected format. The game logic can check whether that choice is allowed in the current state.

For the occupied table example, think about what the interface needs back from the game logic so it can explain the problem and let the player choose again.

Student: The game logic could return a result that says why the action failed. The interface can display that message and ask again. The waiter’s task should only count as used if the action succeeds.

That also means the interface should not decide whether a customer is eligible to be served just by looking at what it printed.

AI: Exactly. That separation lets you test game rules without typing into the command line. It also gives you a way to check the brief’s requirement that an invalid choice does not consume the staff task.
3. Integrating the turn sequence
Student: I’ve separated the staff and domain classes, but I’m not sure how to link them together. Should the waiter automatically tell the chef when an order is taken?

AI: Consider who chooses the chef’s task. Does the brief say the chef always prepares the newest order, or does the manager choose which order to advance?

Student: The manager chooses the kitchen priority, so the waiter should not automatically assign the chef. The waiter can create or update an order, and then the controller can present eligible orders for the chef’s action.

The chef may work on an order taken in the same turn, so I need to make sure the new order is available before the chef makes a choice.

AI: That fits the required sequence. You could trace the integration as:
1. The controller receives the waiter choice.
2. The waiter performs the valid task, which may create an order.
3. The controller presents orders eligible for preparation.
4. The chef advances the selected order by one unit.
5. The controller continues with eating, payment and end-of-turn effects.
What should happen if the player selects an order that is already ready?

Student: The action should fail with an explanation, and the player should choose again without losing the chef’s task. I should also check that a two-unit Pasta order does not become ready after only one chef action.

AI: Those are useful checks. You can test each staff action independently and then run a complete customer scenario to verify that the pieces work together.
4. Victory, defeat and test cases
Student: For the result, I was going to end the game as soon as the player reaches four paid customers and $45. But the objective also includes average satisfaction. Can I declare victory early if all three are reached?

AI: Check the timing stated in the brief. Is the result based on the first moment the targets are reached, or on the state at the end of turn 18?

Student: It says the game always runs all 18 turns, with no early ending. So even if the targets are reached earlier, I should continue and check all three at the end.

Also, I should calculate average satisfaction only from customers who paid, using their satisfaction when payment happened. Waiting or abandoned customers should not be included.

AI: That gives you several useful test cases: no paid customers, enough revenue but fewer than four paid customers, and meeting the customer and revenue targets but missing the satisfaction target. You should also check a reproducible victory and a reproducible defeat, as required for Stage 1.
Stage 2: Customer Behaviour Extension
1. Adding customer subclasses
Student: For Stage 2, I need regular, VIP and Critic customers. I could keep one Customer class with a type field and use if statements whenever I calculate satisfaction loss or a bill. Would that be reasonable?

AI: It could produce the required behaviour, but compare it with a subclass approach. Where would the type checks appear, and what would you need to modify if another customer type were added later?

Also, which behaviours are genuinely different, and which should stay shared?

Student: The shared part is the service flow: all customers arrive, wait, order, get served and pay. The differences are satisfaction loss, the VIP discount, and whatever distinct preference or reaction we define for a Critic.

If I put type checks in the controller, I might end up changing it for every new customer type. I could instead define methods on Customer and override them in VIPCustomer and CriticCustomer.

AI: That would allow the controller to work with customers through the same general operations while the objects supply their own behaviour. Before implementing it, check the brief’s values and decide how you will document and test the Critic’s distinct behaviour.

Student: For example, the Critic could prefer Pasta according to a deterministic rule. I would document that rule so a run can be reproduced. I should not change the overall ordering flow just because the customer is a Critic.

AI: That is a possible design, provided the rule is implemented and documented consistently. You should also check that a customer still chooses their own food: the manager selects the waiter’s task, but does not choose the customer’s meal.
2. Testing polymorphism and avoiding duplicated logic
Student: If VIPCustomer extends Customer, should I copy the whole payment method into the subclass and change the price there?

AI: Consider what would happen if the payment process later changed. Would you need to update both copies? Which part of payment is actually specific to a VIP?

Student: Only the bill calculation is different. The steps that mark payment, update revenue and free the table should be shared. So the cashier could use a method provided by the customer to calculate the bill, then run the same payment process for everyone.

AI: That reduces duplication. Make sure the discount is applied to the correct amount and stored in integer cents, as required by the brief. It would be useful to test a regular and VIP customer ordering the same item and compare their final bills.

Student: I should also test that the VIP loses five satisfaction per waiting turn, the Critic loses twelve, and the regular customer still loses eight. The controller should call the customer’s behaviour rather than check the customer’s type.

AI: Yes. A further question is whether a subclass can still participate in the same service flow without special handling. Testing arrival through payment for each type would give evidence that the shared flow still works.
3. Integrating Stage 2 with Stage 1
Student: The brief requires six arrivals, including at least one VIP and one Critic. Should I add two extra arrivals to the Stage 1 schedule?

AI: Recheck the arrival requirement: does Stage 2 increase the total, or can the new customer types replace some regular arrivals?

Student: It says I may replace two regular arrivals, but there must still be six. I’ll keep the same arrival turns and make two of the scheduled customers a VIP and a Critic.

That means I need to change the customer creation or setup code, but I should avoid rewriting the turn sequence.

AI: That is a focused change. When you document the transition, identify which files were added, which existing files changed and why. Also consider how you can show that the Stage 1 behaviour remains intact.

Student: I can rerun the regular-customer tests and the existing victory and defeat replays where they still apply. I’ll also add a reproducible Stage 2 run that includes both new customer types. If a Stage 1 replay relies on all six customers being regular, I shouldn’t assume it produces the same result under the Stage 2 arrival schedule.

AI: Good distinction. You can preserve the original Stage 1 evidence at the stage-1 tag and separately document the Stage 2 run and checks. For the transition, explain the effect of the changed arrival schedule as well as the new subclass behaviour.