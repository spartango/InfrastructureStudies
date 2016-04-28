// All layers
var backgroundLayers = {};
// Clusters
var backgroundMarkers = new L.MarkerClusterGroup({
    iconCreateFunction: clusterIcon,
    maxClusterRadius: 50,
    showCoverageOnHover: false
});

// Legend
var legendShowing = false;
var legend = L.control({position: 'bottomright'});

// Building Layers
var buildLayerWithDataConfig = function (data, config) {
    var icon = glyphIcon(config.color, config.character);
    var options = {};
    if (config.style) {
        options.style = config.style
    }
    if (config.popup == 'infrastructure') {
        options.onEachFeature = function (feature, layer) {
            infrastructurePopup(feature, layer, config);
        }
    }
    if (config.layerType == 'cluster' || config.layerType == 'marker') {
        data.features.forEach(function (feature) {
            feature.properties.color = config.color;
        });
        options.pointToLayer = function (feature, latlng) {
            return L.marker(latlng, {icon: icon});
        }
    }
    return L.geoJson(data, options)
};

var buildLayerFromConfig = function (config) {
    var geojson = loadGeoJSON(config.url);
    if (config.handleData) {
        geojson = geojson.then(config.handleData);
    }
    return geojson.then(function (data) {
        return buildLayerWithDataConfig(data, config);
    });
};

var buildLayer = function (name) {
    var config = dataLayers[name];
    if (config) {
        return buildLayerFromConfig(config);
    } else {
        return Promise.reject("No config for name: " + name);
    }
};

// Displaying layers
var showLayer = function (name) {
    var config = dataLayers[name];

    // Check that this layer isn't already showing
    if (backgroundLayers[name]) {
        return Promise.resolve();
    }

    // Check that there's a config for this name
    if (!config) {
        return Promise.reject("No config for name: " + name);
    }

    // Build the layer
    var layerPromise = buildLayerFromConfig(config);

    if (config.layerType == 'cluster') {
        return layerPromise.then(function (layer) {
            // Add it to the cluster group if it needs to be clustered
            backgroundMarkers.addLayer(layer);
            backgroundLayers[name] = layer;
        });
    } else {
        return layerPromise.then(function (layer) {
            // Add the layer directly to the map
            layer.addTo(map);
            backgroundLayers[name] = layer;
        });
    }
};

var hideLayer = function (name) {
    var config = dataLayers[name];

    // Check that this layer isn't already hidden
    var backgroundLayer = backgroundLayers[name];
    if (!backgroundLayer) {
        return Promise.resolve();
    }

    // Check that there's a config for this name
    if (!config) {
        return Promise.reject("No config for name: " + name);
    }

    // Check the config to see if this is a clustered layer
    if (config.layerType == 'cluster') {
        // Remove the layer from the cluster group
        backgroundMarkers.removeLayer(backgroundLayer);
    } else {
        // Remove the layer from the map itself
        map.removeLayer(backgroundLayer);
    }

    // Delete this layer from the registry
    delete backgroundLayers[name];
    return Promise.resolve();
};

var toggleLayer = function (layerName) {
    if (!backgroundLayers[layerName]) {
        return showLayer(layerName);
    } else {
        return hideLayer(layerName);
    }
};

// Show and hide functions for special layers
var showMapLayer = function (layerName, layerSupplier) {
    // Add the layer to the map
    if (!backgroundLayers[layerName]) {
        var layerPromise = layerSupplier();
        return layerPromise.then(function (layer) {
            layer.addTo(map);
            backgroundLayers[layerName] = layer;
        });
    } else {
        return Promise.resolve();
    }
};

var hideMapLayer = function (layerName) {
    if (backgroundLayers[layerName]) {
        map.removeLayer(backgroundLayers[layerName]);
        delete backgroundLayers[layerName];
    }
    return Promise.resolve();
};

var toggleMapLayer = function (layerName, layerSupplier) {
    if (!backgroundLayers[layerName]) {
        return showMapLayer(layerName, layerSupplier);
    } else {
        return hideMapLayer(layerName);
    }
};

// Specialized Layers
var flowCount = 1;
var loadPathLayer = function () {
    return loadPaths().then(function (data) {
        // Update the top bar count
        flowCount = data.features.length;
        $('#flowCount').text(data.features.length);
        var outer = L.geoJson(data, {
            style: {
                "weight": 6,
                "color": '#FFFFFF',
                "opacity": 1.0,
                "clickable": false
            }
        });
        var inner = L.geoJson(data, {
            style: {
                "weight": 4,
                "color": '#3F3F3F',
                "opacity": 1.0,
                "clickable": false
            }
        });

        return L.layerGroup([inner, outer]);
        //map.fitBounds(path.getBounds());
        //hash = new L.Hash(map);
    });
};

var hideReroute = function () {
    return hideMapLayer('reroute');
};

var showReroute = function (id) {
    return hideReroute().then(function () {
        return showMapLayer('reroute', function () {
            return loadRerouteLayer(id);
        });
    });
};

var loadRerouteLayer = function (id) {
    return loadPath(id).then(function (data) {
        return L.geoJson(data, {
            onEachFeature: reroutePopup,
            style: {
                "color": "#f06eaa",
                "weight": 4,
                "opacity": 0.66
            }
        });
    });
};

var fastAnimation = false;
var clearAnimation = function () {
    if (backgroundLayers['animation']) {
        backgroundLayers['animation'].forEach(function (m) {
            map.removeLayer(m);
        });
        delete backgroundLayers['animation'];
    }
};

var showAnimation = function (flowName) {
    if (!backgroundLayers['animation']) {
        return loadGeoJSON(DATA_DIR + flowName + '.geojson')
            .then(function (data) {
                var animatedMarkers = [];
                var completed = 0;

                data.features.forEach(function (feature) {
                    // Create a little animated disk
                    var durations = [];
                    var last = null;
                    var latlngs = feature.geometry.coordinates.map(function (coord) {
                        var point = turf.point(coord);
                        if (last) {
                            var distance = turf.distance(last, point); // km
                            var time = distance * (fastAnimation ? 5 : 10); // 100 km/s
                            durations.push(time);
                        }
                        last = point;
                        return [coord[1], coord[0], (coord.length > 2 ? coord[2] : 0)];
                    });
                    //durations;

                    var aMarker = L.Marker.movingMarker(latlngs, durations, {
                        loop: !fastAnimation,
                        autostart: true,
                        icon: new L.DivIcon({
                            html: '<div></div>',
                            className: 'flow-marker',
                            iconSize: new L.Point(10, 10)
                        })
                    });
                    if (fastAnimation) {
                        aMarker.on('end', function () {
                            //console.log("Finished " + completed);
                            completed++;
                            aMarker.pause();
                            if (completed >= animatedMarkers.length) {
                                console.log("Restarting all animations " + completed);
                                completed = 0;
                                animatedMarkers.forEach(function (m) {
                                    m.start();
                                });
                            }
                        });
                    }
                    animatedMarkers.push(aMarker);
                });

                animatedMarkers.forEach(function (m) {
                    m.addTo(map);
                });

                backgroundLayers['animation'] = animatedMarkers;
            });
    } else {
        return new Promise(function (resolve, reject) {
            resolve();
        });
    }
};

var toggleAnimation = function (flowName) {
    if (!backgroundLayers['animation']) {
        return showAnimation(flowName);
    } else {
        clearAnimation();
    }
};

var toggleRangeRings = function () {
    return toggleMapLayer('rings', loadMergedRangeLayer);
};

var allBridges = false;
var loadSegmentLayer = function () {
    return loadBridges().then(function (data) {
        // Update the top bar count
        $('#bridgeCount').text(data.features.length);

        // Have a quick look through the data and figure out what the range of criticality is
        var criticalityData = data.features.map(function (feature) {
            return feature.properties.criticality;
        });

        var minCriticality = d3_array.min(criticalityData);
        var maxCriticality = d3_array.max(criticalityData);
        var midPoint = minCriticality + ( (maxCriticality - minCriticality) / 2);

        return L.geoJson(data, {
            filter: function (feature) {
                return allBridges || feature.properties.criticality >= 2;
            },
            onEachFeature: bridgePopup,
            style: function (feature) {
                var criticality = feature.properties.criticality;
                var color = d3_scale.scaleLinear()
                    .domain([minCriticality, midPoint, maxCriticality])
                    .range(["#00FF00", "#FFFF00", "#FF0000"]);
                var weight = 4;
                return {
                    "color": color(criticality),
                    "weight": weight,
                    "opacity": 0.66
                }
            }
        });
    });
};

var toggleSegments = function () {
    return toggleMapLayer('segments', loadSegmentLayer);
};

var clearDamagedPath = function () {
    if (backgroundLayers['damagedPath']) {
        map.removeLayer(backgroundLayers['damagedPath']);
        delete backgroundLayers['damagedPath'];
    }

    if (backgroundLayers['animation']) {
        // Clear any existing animation
        clearAnimation();
    }
};

var loadDamagedPath = function (id) {
    if (backgroundLayers['damagedPath']) {
        clearDamagedPath();
    }

    if (backgroundLayers['animation']) {
        // Clear any existing animation
        clearAnimation();
    }

    var flowName = id + '_damage';
    return loadGeoJSON(DATA_DIR + flowName + '.geojson')
        .then(function (data) {
            var path = L.geoJson(data, {
                onEachFeature: function (feature, layer) {
                    var popupString = "<button onclick='clearDamagedPath()'>Clear</button>";
                    layer.bindPopup(popupString);
                },
                style: {
                    color: "#0b0",
                    weight: 6,
                    opacity: 0.25
                }
            });
            path.addTo(map);
            backgroundLayers['damagedPath'] = path;
        }).then(function () {
            toggleAnimation(flowName);
        });
};

var drawAimPoints = false;
var drawTargets = function (data) {
    // Have a quick look through the data and figure out what the range of criticality
    var criticalityData = data.features.map(function (feature) {
        return feature.properties.criticality;
    });

    var minCriticality = d3_array.min(criticalityData);
    var midPoint = d3_array.mean(criticalityData);
    var maxCriticality = Math.min(d3_array.max(criticalityData), (midPoint - minCriticality) + midPoint);

    var color = d3_scale.scaleLinear()
        .domain([minCriticality, midPoint, maxCriticality])
        .range(["#FFFF00", "#FF8800", "#FF0000"]);
    //.interpolate(d3_interpolate.interpolateRgb);

    data.features.forEach(function (feature) {
        var criticality = feature.properties.criticality;
        feature.properties.color = color(criticality);
    });

    legend.onAdd = function (map) {
        var grades = [];
        var interval = (maxCriticality - minCriticality) / 7;
        for (var i = 0; i < 7; i++) {
            grades.push(minCriticality + (interval * i));
        }
        var div = L.DomUtil.create('div', 'info legend');
        //var grades = [minCriticality, midPoint, maxCriticality];
        var labels = grades.map(function (value) {
            return formatTime(costToHours(value));
        });

        // loop through our density intervals and generate a label with a colored square for each interval
        div.innerHTML += `<strong>Rerouting Delays</strong><br>`;
        for (var i = grades.length - 1; i >= 0; i--) {
            div.innerHTML +=
                '<i style="background:' + color(grades[i]) + '"></i>'
                + labels[i] + '<br>';
        }

        return div;
    };

    // Update the top bar count
    $('#targetCount').text(data.features.length);

    var segmentLayer = L.geoJson(data, {
        onEachFeature: targetPopup,
        filter: function (feature) {
            // Only draw segments if not drawing aimpoints
            return !drawAimPoints;
        },
        style: function (feature) {
            return {
                "color": feature.properties.color,
                "weight": drawAimPoints ? 4 : 6,
                "opacity": 0.66,
                "clickable": !drawAimPoints
            }
        }
    });

    if (drawAimPoints) {
        var pointData = turf.featurecollection(
            data.features.map(function (feature) {
                return {
                    type: feature.type,
                    id: feature.id,
                    geometry: feature.properties.center.geometry,
                    properties: feature.properties
                };
            }));

        var targetCluster = new L.MarkerClusterGroup({
            iconCreateFunction: clusterIcon,
            maxClusterRadius: 50,
            showCoverageOnHover: false
        });
        var pointLayer = L.geoJson(pointData, {
            onEachFeature: targetPopup,
            pointToLayer: function (feature, latlng) {
                var colorValue = feature.properties.color;
                var icon = glyphIcon(colorValue, typeChars['target']);
                return L.marker(latlng, {icon: icon});
            }
        });
        targetCluster.addLayer(pointLayer);

        return L.layerGroup([targetCluster, segmentLayer]);
    } else {
        return segmentLayer;
    }
};

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

var loadTargetLayer = function () {
    return loadTargets()
        .then(annotateInfrastructure)
        .then(annotateDimensions)
        .then(drawTargets);
};

function toggleLegend() {
    if (legendShowing) {
        hideLegend();
    } else {
        showLegend();
    }
}
var toggleTargets = function () {
    return toggleMapLayer('targets', loadTargetLayer).then(toggleLegend);
};

var toggleFlows = function () {
    var targetsShowing = backgroundLayers['targets'] != null;

    return togglePaths().then(function () {
        if (targetsShowing) {
            return hideMapLayer('targets');
        }
    }).then(function () {
        return toggleAnimation('baseline')
    }).then(function () {
        if (targetsShowing) {
            return showTargets();
        }
    });
};

var showLegend = function () {
    if (!legendShowing) {
        legendShowing = true;
        legend.addTo(map);
    }
};

var hideLegend = function () {
    if (legendShowing) {
        legendShowing = false;
        legend.removeFrom(map);
    }
};

var showAndFocus = function (showPromise, bounds) {
    return showPromise.then(function () {
        if (bounds) { // Focus on this target
            map.fitBounds(bounds);
        }
    });
};

var showPaths = function () {
    return showMapLayer('paths', loadPathLayer);
};
var showTargets = function () {
    return showMapLayer('targets', loadTargetLayer).then(showLegend);
};

var toggleAimpoints = function () {
    drawAimPoints = !drawAimPoints;
    hideMapLayer('targets').then(showTargets);
};

var showBaselineAnimation = function () {
    return showAnimation('baseline');
};

var togglePaths = function () {
    return toggleMapLayer('paths', loadPathLayer);
};
