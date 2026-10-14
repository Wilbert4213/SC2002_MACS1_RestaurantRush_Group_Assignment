package restaurantrush;

/** An immutable dish definition. Prices are stored in cents. */
public class MenuItem {
    private final String name;
    private final int priceCents;
    private final int preparationUnits;

    public MenuItem(String name, int priceCents, int preparationUnits) {
        this.name = name;
        this.priceCents = priceCents;
        this.preparationUnits = preparationUnits;
    }

    public String getName() { return name; }
    public int getPriceCents() { return priceCents; }
    public int getPreparationUnits() { return preparationUnits; }
}
