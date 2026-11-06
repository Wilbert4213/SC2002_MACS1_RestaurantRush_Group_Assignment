AI Interaction History  
User: Hong Yi  
AI assistant: ChatGPT  

=======================================================================================
1. Planning the Stage 3 extension
   
> User: I have completed and tested Stages 1 and 2. I want to implement Stage 3 on top of the existing Java game. What features must be added?

> AI: Stage 3 adds three main mechanics: a Host who seats customers independently of the Waiter, ComboMeal menu items that combine two dishes with summed preparation requirements and a 10% discount, and a once-per-game Happy Hour lasting two turns, with order discounts and satisfaction restoration.

-----------------------------------------------------------------------------------------------

2. Key ideas — Organising the Stage 3 design discussion
   
User: What key design ideas should I discuss for Stage 3, particularly the Host, ComboMeal, and Happy Hour features?

AI: Organise the design discussion around three concerns: separating seating duties from other staff tasks, representing combinations of existing menu items, and introducing a limited-time promotion. Explain how each feature relates to the existing game structure and its object-oriented design.

-----------------------------------------------------------------------------------------------
3. Key ideas — Selecting OOP examples for the report
   
User: Which object-oriented principles can I discuss using examples from Stage 3? Please keep the explanation grounded in the classes we actually used.

AI: The Host can illustrate a specialised staff role within the existing class hierarchy. ComboMeal can illustrate inheritance from MenuItem and composition through its component dishes. Explain these relationships without claiming that unsupported principles were implemented.

-----------------------------------------------------------------------------------------------
4. Refining syntax and grammar — Report wording
   
User: Refine my explanation of why separate classes were chosen over placing all classes in one file.

AI: Separate classes make individual components easier to maintain and review. Combining unrelated classes into one file can make changes harder to isolate and may increase the risk of unintended errors.

-----------------------------------------------------------------------------------------------
