package restaurantrush;

import java.util.Locale;

public final class Money {
    private Money() { }

    public static String format(int cents) {
        return String.format(Locale.ROOT, "$%.2f", cents / 100.0);
    }
}
