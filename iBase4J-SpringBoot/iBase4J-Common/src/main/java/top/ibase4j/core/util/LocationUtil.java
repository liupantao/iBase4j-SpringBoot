package top.ibase4j.core.util;

import java.math.BigDecimal;

/**
 * @author ShenHuaJie
 * @since 2018年5月16日 上午11:16:49
 */
public class LocationUtil {
    private static final double EARTH_RADIUS = 6378137;// 赤道半径

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    /** 根据经纬度获取两点的距离(单位米) */
    public static Double getDistance(double lon1, double lat1, double lon2, double lat2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lon1) - rad(lon2);
        double s = 2 * Math.asin(Math.sqrt(
            Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        return s;
    }

    /** 根据经纬度获取两点的距离(单位米) */
    public static Double getDistance(BigDecimal lon1, BigDecimal lat1, BigDecimal lon2, BigDecimal lat2) {
        return getDistance(lon1.doubleValue(), lat1.doubleValue(), lon2.doubleValue(), lat2.doubleValue());
    }
}
