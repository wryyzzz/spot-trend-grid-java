package response;

import java.math.BigDecimal;
import java.util.List;

public class SellMarketResponse {
    public String symbol;
    public Integer orderId;
    public Integer orderListId;
    public String clientOrderId;
    public Long transactTime;
    public BigDecimal price;
    public BigDecimal origQty;
    public BigDecimal executedQty;
    public BigDecimal cummulativeQuoteQty;
    public String status;
    public String timeInForce;
    public String type;
    public String side;
    public List<Fill> fills;

    public static final class Fill {
        public BigDecimal price;
        public BigDecimal qty;
        public BigDecimal commission;
        public String commissionAsset;
    }
}
