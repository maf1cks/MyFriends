package com.example.map1;
public class SimpleDistanceCalculator {
    private static final double METERS_PER_DEGREE_LATITUDE = 111139;
    private static final double METERS_PER_DEGREE_LONGITUDE_EQUATOR = 111319;


    public static String calculateDistanceAlongCathetuses(double lat1, double lon1, double lat2, double lon2) {
        double deltaLatDeg = Math.abs(lat2 - lat1);
        double deltaLonDeg = Math.abs(lon2 - lon1);
        double distanceLatMeters = deltaLatDeg * METERS_PER_DEGREE_LATITUDE;
        double averageLatRad = Math.toRadians((lat1 + lat2) / 2.0);
        double distanceLonMeters = deltaLonDeg * METERS_PER_DEGREE_LONGITUDE_EQUATOR * Math.cos(averageLatRad);

        int t;
        String t1;
        double totalDistanceMeters = Math.sqrt(distanceLatMeters*distanceLatMeters + distanceLonMeters*distanceLonMeters);
        if (totalDistanceMeters>10000){
            t = (int) Math.round(totalDistanceMeters/1000);
            t1 = String.valueOf(t)+"км";
        } else {
            t = (int) Math.round(totalDistanceMeters);
            t1 = String.valueOf(t)+"м";
        }

        return t1;
    }
}
