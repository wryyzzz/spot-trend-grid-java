package request;

import java.math.BigDecimal;

public class SellMarketRequest {
    public String symbol;

    public String type;

    public String quantity;


    @Override
    public String toString() {
        return "SellMarketRequest{" +
                "symbol='" + symbol + '\'' +
                ", type='" + type + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}
