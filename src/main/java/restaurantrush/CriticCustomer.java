package restaurantrush;

import java.util.List;

/** An impatient critic who prefers the dish requiring the most preparation. */
public class CriticCustomer extends Customer {
    public CriticCustomer(int id) { super(id); }

    @Override
    public String getTypeName() { return "Critic"; }

    @Override
    public int getWaitingSatisfactionLoss() { return 12; }

    @Override
    public MenuItem chooseMeal(List<MenuItem> menu) {
        MenuItem preferred = menu.get(0);
        for (MenuItem item : menu) {
            if (item.getPreparationUnits() > preferred.getPreparationUnits()) {
                preferred = item;
            }
        }
        return preferred;
    }
}
