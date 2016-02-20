package com.spartango.infra.elevation;

import java.util.Optional;

/**
 * Author: spartango
 * Date: 2/20/16
 * Time: 14:45.
 */
public interface ElevationSource {

    public Optional<Double> getElevation(double latitude, double longitude);
}
