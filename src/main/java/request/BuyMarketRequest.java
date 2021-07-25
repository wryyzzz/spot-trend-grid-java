package request;

import java.math.BigDecimal;

public class BuyMarketRequest {
    public String symbol;

    public String type;

    public String quantity;

    @Override
    public String toString() {
        return "BuyMarketRequest{" +
                "symbol='" + symbol + '\'' +
                ", type='" + type + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}
