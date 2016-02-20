package com.spartango.infra.elevation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;

/**
 * Author: spartango
 * Date: 2/20/16
 * Time: 14:45.
 */
public class GeonamesSource implements ElevationSource {
    public static final String SRTM     = "srtm3";
    public static final String ASTER    = "astergdem";
    public static final String BASE_URL = "http://api.geonames.org/"; //srtm3?lat=50.01&lng=10.2&username=demo&style=full
    public static final String SRTM_VOID = "-32768";

    private String sourceName;
    private String username;

    public GeonamesSource(String username) {
        this(SRTM, username);
    }

    public GeonamesSource(String sourceName, String username) {
        this.sourceName = sourceName;
        this.username = username;
    }

    @Override public Optional<Double> getElevation(double latitude, double longitude) {
        // Request
        String urlString = BASE_URL
                           + sourceName + "?"
                           + "&lat=" + latitude
                           + "&lng=" + longitude
                           + "&username=" + username
                           + "&style=full";

        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(urlString).openStream()))) {
            String line = in.readLine();
            if (line.equals(SRTM_VOID)) { // This is SRTM's void value
                return Optional.empty();
            }
            return Optional.of(Double.parseDouble(line));

        } catch (IOException | NumberFormatException e) {
            System.err.println(e.getCause() + ": Failed to get elevation for ("
                               + latitude
                               + ", "
                               + longitude
                               + ") due to"
                               + e.getMessage());
        }

        return Optional.empty();
    }
}
