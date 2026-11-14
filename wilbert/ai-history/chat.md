# AI Interaction History

User: Wilbert  
AI assistant: ChatGPT  
Project: SC2002 Restaurant Rush — UML Sequence Diagram  

## 1. Key ideas — Identifying objects for the Stage 1 sequence diagram

> User: For the Stage 1 restaurant game, which objects should appear in a sequence diagram showing a customer from arrival to payment?

> AI: Use `RestaurantRush` as the command-line boundary, `GameController` to coordinate turns and staff choices, and `Waiter`, `Chef`, and `Cashier` for their separate tasks. Include the customer, their `Order`, and the occupied `Table` because they hold the state needed to show ordering, preparation, serving, payment, and release. Focus on messages needed for one concrete scenario instead of trying to show every possible method.

---

## 2. Key ideas — Mapping the turn-based flow

> User: How should I organise the interactions so the sequence diagram follows the required order of events in each turn?

> AI: Start with `beginTurn()`, then show the manager's waiter and chef assignments, followed by eating and automatic cashier activity when the customer becomes eligible. Finish with satisfaction updates and `finishTurn()`. Read the messages from top to bottom and check that a customer is not served, charged, or removed before the necessary state transition.

---

## 3. Key ideas — Representing the Pasta preparation delay

> User: A regular customer orders Pasta, which takes two preparation units. How do I make the READY-to-SERVED timing clear?

> AI: Show the chef advancing the Pasta order once in Turn 2 and once in Turn 3. At the end of the second preparation unit, the order becomes `READY`. Since the waiter acts before the chef during each turn, the waiter cannot serve food completed in Turn 3 until Turn 4. Use an annotation or order-state response to make this timing constraint easy to see.

---

## 4. Key ideas — Showing the payment lifecycle

> User: What interaction should the diagram include between serving the customer and freeing their table?

> AI: A customer served in Turn 4 finishes eating in Turn 5 and becomes `READY_TO_PAY`. The cashier can then collect payment in Turn 6, after which the customer and order become `PAID` and the table is released. The diagram should make it clear that payment is not allowed immediately upon service or in the same turn the customer becomes ready to pay.

---

## 5. Key ideas — Extending the diagram for a VIP in Stage 2

> User: How can I show Stage 2's VIP behaviour without creating a completely different service flow?

> AI: Keep the same customer lifecycle used in Stage 1 and add a `VIPCustomer` lifeline for the new arrival. For example, a VIP ordering a $15 Burger receives a 10% discount, so the bill is $13.50 (1,350 cents). The discount should be calculated through customer-specific behaviour, such as an overridden `calculateBillCents()` method, and the resulting bill stored on the `Order`. The waiter, chef, and cashier still carry out the usual service sequence.

---

## 6. Key ideas — Demonstrating polymorphism rather than type checking

> User: What would make the VIP section demonstrate object-oriented design instead of simply showing a discounted number?

> AI: Show a message to the VIP customer's overridden billing operation, rather than putting an `if VIP` discount rule in `GameController`. That demonstrates dynamic dispatch: the controller and staff work with customers through the common `Customer` behaviour, while the VIP object supplies its specialised pricing rule. Only show method calls that correspond to the Java implementation.

---

## 7. Key ideas — Choosing UML interaction fragments

> User: My diagram is becoming very long. Which UML fragments help simplify it without losing the important logic?

> AI: Use `loop` for repeated turn processing, `opt` for conditional actions, and `alt` for mutually exclusive outcomes such as `VICTORY` or `DEFEAT`. A `ref` fragment should reference another named interaction; it should not be used as a placeholder that merely says the remaining actions are similar. Keep the detailed customer journeys visible and summarise the later turns carefully.

---

## 8. Key ideas — Connecting manager choices to the game result

> Use: What should I emphasise so the sequence diagram shows that the manager's choices affect the outcome?

> AI: Explain how the manager assigns the waiter's limited task and the chef's preparation work. Choosing to prepare or serve earlier can preserve satisfaction and allow payment before Turn 18. Delays leave customers waiting, reduce satisfaction, and may cause abandonment. At the end, the controller checks all three conditions: at least four paid customers, revenue of at least $45, and average paid-customer satisfaction of at least 60.

---

## 9. Key ideas — Checking the sequence diagram against the code

> User: What should I verify before submitting the Stage 2 sequence diagram?

> AI: Check that message names and receivers agree with the Stage 2 class design and Java methods. Confirm arrivals on the scheduled turns, the chef's second preparation step, the VIP discount calculation, the READY/SERVED/eating/payment timing, and the final result check. If a long section is abbreviated with a loop, make sure it does not imply an unsupported victory outcome. Export the diagram at readable resolution.

---
