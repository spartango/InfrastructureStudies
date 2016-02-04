package com.spartango.infra.geom;

import com.spartango.infra.osm.type.NodeStub;
import org.geotools.referencing.GeodeticCalculator;

import java.util.Iterator;
import java.util.List;

/**
 * Author: spartango
 * Date: 9/1/15
 * Time: 22:21.
 */
public class ShapeUtils {
    public static double calculateLength(List<NodeStub> path) {
        final Iterator<NodeStub> iterator = path.iterator();
        double sum = 0;
        NodeStub current = iterator.hasNext() ? iterator.next() : null;
        while (current != null && iterator.hasNext()) {
            NodeStub next = iterator.next();
            GeodeticCalculator calculator = new GeodeticCalculator();
            calculator.setStartingGeographicPoint(current.getLongitude(), current.getLatitude());
            calculator.setDestinationGeographicPoint(next.getLongitude(), next.getLatitude());
            double distance = calculator.getOrthodromicDistance();
            sum += distance;
            current = next;
        }
        return sum;
    }

    public static double calculateDistance(NodeStub current, NodeStub next) {
        return calculateDistance(current.getLatitude(),
                                 current.getLongitude(),
                                 next.getLatitude(),
                                 next.getLongitude());
    }

    public static double calculateDistance(double startLatitude,
                                           double startLongitude,
                                           double endLatitude,
                                           double endLongitude) {
        GeodeticCalculator calculator = new GeodeticCalculator();
        calculator.setStartingGeographicPoint(startLongitude, startLatitude);
        calculator.setDestinationGeographicPoint(endLongitude, endLatitude);
        return calculator.getOrthodromicDistance();
    }
}
