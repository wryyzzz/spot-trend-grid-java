package request;


import java.time.LocalDateTime;

public class GetKlinesRequest {
    public String symbol;

    public String interval;

    public Integer limit;

    public LocalDateTime startTime;

    public LocalDateTime endTime;
}
