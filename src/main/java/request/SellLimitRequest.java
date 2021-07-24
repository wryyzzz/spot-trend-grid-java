package request;

import java.math.BigDecimal;

public class SellLimitRequest {
    public String symbol;

    public String type;

    public String timeInForce;

    public BigDecimal quantity;

    public BigDecimal price;
}
