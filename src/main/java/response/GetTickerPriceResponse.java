package response;

public class GetTickerPriceResponse {
    public String symbol;

    public String price;

    @Override
    public String toString() {
        return "GetTickerPriceResponse{" +
                "symbol='" + symbol + '\'' +
                ", price='" + price + '\'' +
                '}';
    }
}
