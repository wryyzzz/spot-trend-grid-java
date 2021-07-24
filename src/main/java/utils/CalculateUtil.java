package utils;

import api.BinanceApi;
import request.GetKlinesRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CalculateUtil {
    private static BinanceApi binanceApi;

    public static void setBinanceApi(BinanceApi binanceApi) {
        CalculateUtil.binanceApi = binanceApi;
    }

    public static boolean calculateAngle(String coinType, String interval, boolean direction, int point) {
        var ret = calcSlopeMA5(coinType, interval, point);
        var lastMa5 = ret[0];
        var nextMa5 = ret[1];
        if (direction) {
            return lastMa5.compareTo(nextMa5) > -1;
        } else {
            return lastMa5.compareTo(nextMa5) < 1;
        }
    }

    public static int calculateMA10() {
        return 0;
    }

    public static BigDecimal[] calcSlopeMA5(String coinType, String interval, int point) {
        BigDecimal lastMa5 = BigDecimal.ZERO;
        BigDecimal nextMa5 = BigDecimal.ZERO;
        GetKlinesRequest request = new GetKlinesRequest();
        request.symbol = coinType;
        request.interval = interval;
        request.limit = 6;
        List<List<BigDecimal>> klines = binanceApi.getKlines(request);
        BigDecimal[] ret = new BigDecimal[2];
        for (int i = 0; i < klines.size(); i++) {
            if (i == 0) {
                lastMa5 = lastMa5.add(klines.get(i).get(4));
            } else if (i == 5) {
                nextMa5 = nextMa5.add(klines.get(i).get(4));
            } else {
                lastMa5 = lastMa5.add(klines.get(i).get(4));
                nextMa5 = nextMa5.add(klines.get(i).get(4));
            }
        }
        ret[0] = lastMa5.setScale(point, RoundingMode.HALF_UP);
        ret[1] = nextMa5.setScale(point, RoundingMode.HALF_UP);
        return ret;
    }
}
