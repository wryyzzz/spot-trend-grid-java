package request;

import java.math.BigDecimal;

public class BuyLimitRequest {
    public String symbol;

    public String type;

    public String timeInForce;

    public String quantity;

    public String price;

    @Override
    public String toString() {
        return "BuyLimitRequest{" +
                "symbol='" + symbol + '\'' +
                ", type='" + type + '\'' +
                ", timeInForce='" + timeInForce + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }
}
