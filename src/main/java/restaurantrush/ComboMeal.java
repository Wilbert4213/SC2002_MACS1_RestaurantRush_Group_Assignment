package restaurantrush;

import java.util.Objects;

public class ComboMeal extends MenuItem {

    private final MenuItem firstItem;
    private final MenuItem secondItem;

    public ComboMeal(MenuItem firstItem, MenuItem secondItem) {
        super(
            Objects.requireNonNull(firstItem).getName()
                + " + "
                + Objects.requireNonNull(secondItem).getName()
                + " Combo",
            (int) (((long) firstItem.getPriceCents()
                + secondItem.getPriceCents()) * 90 + 50) / 100,
            firstItem.getPreparationUnits()
                + secondItem.getPreparationUnits()
        );

        this.firstItem = firstItem;
        this.secondItem = secondItem;
    }

    public MenuItem getFirstItem() {
        return firstItem;
    }

    public MenuItem getSecondItem() {
        return secondItem;
    }
}