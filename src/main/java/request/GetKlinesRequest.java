package request;


import java.time.LocalDateTime;

public class GetKlinesRequest {
    public String symbol;

    public String interval;

    public Integer limit;

    public LocalDateTime startTime;

    public LocalDateTime endTime;


    @Override
    public String toString() {
        return "GetKlinesRequest{" +
                "symbol='" + symbol + '\'' +
                ", interval='" + interval + '\'' +
                ", limit=" + limit +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
