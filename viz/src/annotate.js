/**
 * Created by spartango on 4/27/16.
 */


var annotateInfrastructure = function (data) {
    // Load up the merged SAM threats
    return loadGeoJSON(dataLayers.rangeRings.url)
        .then(turf.merge)
        .then(function (mergedRings) {
            // Mark the targets which are in range
            console.log("Computing active SAM threats");
            loadingControl.addLoader("SAM");
            data.features.forEach(function (feature) {
                feature.properties.center = feature.properties.center ? feature.properties.center : turf.center(feature);
                if (turf.inside(feature.properties.center, mergedRings)) {
                    feature.properties["activeSAM"] = true;
                }
            });
            loadingControl.removeLoader("SAM");
            return data;
        }).then(function () {
            return loadGeoJSON(dataLayers.SAMs.url);
        }).then(function (sams) {
            console.log("Computing SAM ranges");
            loadingControl.addLoader("Range");
            data.features.forEach(function (feature) {
                if (!feature.properties["nearestSAM"]) {
                    feature.properties.center = feature.properties.center ? feature.properties.center : turf.center(feature);
                    feature.properties["nearestSAM"] = turf.nearest(feature.properties.center, sams);
                }
            });
            loadingControl.removeLoader("Range");
            return data;
        }).then(function () {
            return loadGeoJSON(dataLayers.airBases.url);
        }).then(function (airbases) {
            console.log("Computing airbase ranges");
            loadingControl.addLoader("Air");
            data.features.forEach(function (feature) {
                if (!feature.properties["nearestAirbase"]) {
                    feature.properties.center = feature.properties.center ? feature.properties.center : turf.center(feature);
                    feature.properties["nearestAirbase"] = turf.nearest(feature.properties.center, airbases);
                }
            });
            loadingControl.removeLoader("Air");
            return data;
        }).then(function () {
            return loadGeoJSON(dataLayers.USBases.url);
        }).then(function (airbases) {
            console.log("Computing US Base ranges");
            loadingControl.addLoader("US");
            data.features.forEach(function (feature) {
                if (!feature.properties["nearestUSBase"]) {
                    feature.properties.center = feature.properties.center ? feature.properties.center : turf.center(feature);
                    feature.properties["nearestUSBase"] = turf.nearest(feature.properties.center, airbases);
                }
            });
            loadingControl.removeLoader("US");
            return data;
        });
};

var annotateDimensions = function (data) {
    data.features.forEach(function (feature) {
        var coordinates = feature.geometry.coordinates;
        if (!feature.properties.span) {
            feature.properties.span = turf.distance(turf.point(coordinates[0]), turf.point(coordinates[coordinates.length - 1]));
        }
        feature.properties.type = 'bridge';
    });
    return data;
};


var nearestDistance = function (feature, featureSet) {
    if (!feature.properties.center) {
        feature.properties.center = turf.center(feature);
    }
    var nearest = turf.nearest(feature.properties.center, featureSet);
    var distance = turf.distance(nearest, feature.properties.center);
    return distance;
};

// Comparison of rerouting costs to SAM threat
var annotateThreats = function (targets) {
    return loadGeoJSON(dataLayers.rangeRings.url).then(turf.merge)
        .then(function (mergedRings) {
            // Mark the targets which are in range
            console.log("Computing active SAM threats");
            targets.features.forEach(function (feature) {
                if (!feature.properties.center) {
                    feature.properties.center = turf.center(feature);
                }
                if (turf.inside(feature.properties.center, mergedRings)) {
                    feature.properties["activeSAM"] = true;
                }
            });
            return targets;
        }).then(function () {
            return loadGeoJSON(dataLayers.SAMs.url);
        }).then(function (sams) {
            // Annotate the targets with SAM threat range
            console.log("Computing SAM ranges");
            targets.features.forEach(function (feature) {
                if (!feature.properties["nearestSAMDistance"]) {
                    feature.properties["nearestSAMDistance"] = nearestDistance(feature, sams);
                }
            });
            return targets;
        });
};

var annotateUSBases = function (targets) {
    return loadGeoJSON(dataLayers.USBases.url).then(function (airbases) {
        console.log("Computing US Bases ranges");
        targets.features.forEach(function (feature) {
            if (!feature.properties["nearestUSBaseDistance"]) {
                feature.properties["nearestUSBaseDistance"] = nearestDistance(feature, airbases);
            }
        });
        return targets;
    });
};

var annotateBases = function (targets) {
    return loadGeoJSON(dataLayers.airBases.url).then(function (airbases) {
        console.log("Computing airbase ranges");
        targets.features.forEach(function (feature) {
            if (!feature.properties["nearestAirbaseDistance"]) {
                feature.properties["nearestAirbaseDistance"] = nearestDistance(feature, airbases);
            }
        });
        return targets;
    });
};

var annotateStations = function (targets) {
    return loadGeoJSON(dataLayers.stations.url).then(function (stations) {
        console.log("Computing station ranges");
        targets.features.forEach(function (feature) {
            if (!feature.properties["nearestStationDistance"]) {
                feature.properties["nearestStationDistance"] = nearestDistance(feature, stations);
            }
        });
        return targets;
    });
};

var annotateSources = function (targets) {
    return loadGeoJSON(dataLayers.suppliers.url).then(function (sources) {
        console.log("Computing source ranges");
        targets.features.forEach(function (feature) {
            if (!feature.properties["nearestSourceDistance"]) {
                feature.properties["nearestSourceDistance"] = nearestDistance(feature, sources);
            }
        });
        return targets;
    });
};

var annotateSecondArtillery = function (targets) {
    return loadGeoJSON(dataLayers.missileBases.url).then(function (sources) {
        console.log("Computing 2nd Artillery ranges");
        targets.features.forEach(function (feature) {
            feature.properties["nearest2ADistance"] = nearestDistance(feature, sources);
        });
        return targets;
    });
};

var annotateSinks = function (targets) {
    return loadGeoJSON(dataLayers.consumers.url).then(function (sinks) {
        console.log("Computing sink ranges");
        targets.features.forEach(function (feature) {
            feature.properties["nearestSinkDistance"] = nearestDistance(feature, sinks);
        });
        return targets;
    });
};
