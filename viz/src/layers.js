var backgroundLayers = {};
var backgroundMarkers = new L.MarkerClusterGroup({
    iconCreateFunction: clusterIcon,
    maxClusterRadius: 50
});

var showMapLayer = function (layerName, layerPromise) {
    // Add the layer to the map
    return layerPromise.then(function (layer) {
        if (!backgroundLayers[layerName]) {
            layer.addTo(map);
            backgroundLayers[layerName] = layer;
        }
    });
};

var hideMapLayer = function (layerName) {
    return new Promise(function (resolve, reject) {
        if (backgroundLayers[layerName]) {
            map.removeLayer(backgroundLayers[layerName]);
            delete backgroundLayers[layerName];
        }
        resolve();
    });
};

var toggleMapLayer = function (layerName, layerPromise) {
    if (!backgroundLayers[layerName]) {
        return showMapLayer(layerName, layerPromise);
    } else {
        return hideMapLayer(layerName);
    }
};

var showClusterLayer = function (layerName, layerPromise) {
    // Add the layer to the map
    return layerPromise.then(function (layer) {
        backgroundMarkers.addLayer(layer);
        backgroundLayers[layerName] = layer;
    });
};

var hideClusterLayer = function (layerName) {
    // Hide the layer(s)
    return new Promise(function (resolve, reject) {
        if (backgroundLayers[layerName]) {
            backgroundMarkers.removeLayer(backgroundLayers[layerName]);
            delete backgroundLayers[layerName];
        }
        resolve();
    });
};

var toggleClusterLayer = function (layerName, layerPromise) {
    if (!backgroundLayers[layerName]) {
        return showClusterLayer(layerName, layerPromise);
    } else {
        return hideClusterLayer(layerName);
    }
};

var infraPopup = function (feature, layer) {
    if (feature.properties) {
        var popupString = `<div><div class="row" style='max-height:250px; max-width: 200px;overflow:auto;'><table class="table table-condensed">`;
        if (feature.properties.type) {
            var type = feature.properties.type;
            var niceType = type.charAt(0).toUpperCase() + type.slice(1);
            var iconString = typeIcons[type] ? `<i class="fa fa-` + typeIcons[type] + `"></i>` : "";
            popupString += `<thead><tr><th>` + iconString + ` ` + niceType + `</th></tr></thead>`;
        }

        for (key in feature.properties) {
            var niceKey = key.charAt(0).toUpperCase() + key.slice(1);
            var niceValue = feature.properties[key];
            if (key == 'elevation') {
                niceValue += " m";
            } else if (key == 'type') {
                continue;
            }
            popupString += "<tr><td><strong>" + niceKey + "</strong></td><td>" + niceValue + "</td></tr>";
        }

        if (feature.geometry) {
            popupString += "<tr><td><strong>MGRS</strong></td><td>"
                + mgrs.forward(feature.geometry.coordinates)
                + "</td></tr>";
        }

        popupString += "</table></div>";

        if (feature.geometry) {
            popupString += `<div class="row"><button class="btn btn-default btn-xs col-xs-12" onclick='map.setView({lat:`
                + feature.geometry.coordinates[1]
                + ", lng:"
                + feature.geometry.coordinates[0]
                + `}, 16)' ><i class="fa fa-search-plus"></i> Zoom</button></div></div>`
        }
        layer.bindPopup(popupString);
    }
};

var buildDataLayer = function (type, dataSource) {
    return dataSource.then(function (data) {
        data.features.forEach(function (feature) {
            feature.properties.type = type;
        });
        var icon = glyphIcon(type);
        return L.geoJson(data, {
            onEachFeature: infraPopup,
            pointToLayer: function (feature, latlng) {
                return L.marker(latlng, {icon: icon});
            }
        })
    });
};

var loadPortLayer = function () {
    return buildDataLayer('port', loadPorts());
};

var togglePorts = function () {
    return toggleClusterLayer('ports', loadPortLayer());
};

var loadSAMLayer = function () {
    return buildDataLayer('SAM', loadSAMs());
};

var toggleSAMs = function () {
    return toggleClusterLayer('sams', loadSAMLayer());
};

var loadAviationLayer = function () {
    return buildDataLayer('airbase', loadAviation());
};

var toggleAviation = function () {
    return toggleClusterLayer('aviation', loadAviationLayer());
};

var loadSecondArtilleryLayer = function () {
    return buildDataLayer('nuclear', loadSecondArtillery());
};

var toggleSecondArtillery = function () {
    return toggleClusterLayer('missiles', loadSecondArtilleryLayer());
};

var loadStationLayer = function () {
    return buildDataLayer('station', loadStations());
};

var toggleStations = function () {
    return toggleClusterLayer('stations', loadStationLayer());
};

var bridgePopup = function (feature, layer) {
    if (feature.properties) {
        var popupString = '<div class="row"><table class="table table-condensed"><thead><tr><th><i class="fa fa-road"></i> Bridge</th></tr></thead>';
        for (var key in feature.properties) {
            var prettyKey;
            var rowClass = "";
            var prettyValue = feature.properties[key];
            if (key == 'criticality') {
                prettyKey = 'Bottleneck';
                prettyValue = prettyValue + " sources";
            } else if (key == 'elevations') {
                prettyKey = 'Elevation';
                var array = JSON.parse(prettyValue);
                var count = array.length;
                prettyValue = array.reduce(function (a, b) {
                        return a + b;
                    }) / count;
                prettyValue += " m";
            } else {
                prettyKey = key.charAt(0).toUpperCase() + key.slice(1);
                // If we still haven't made a string out of this
                if (Number.isFinite(prettyValue)) {
                    prettyValue = Math.round(prettyValue) + " km";
                } else if (typeof prettyValue == 'boolean') {
                    prettyValue = prettyValue ? "Yes" : "No";
                }
            }

            popupString += `<tr class="` + rowClass + `"><td><strong>` + prettyKey + "</strong></td><td>" + prettyValue + "</td></tr>";
        }
        popupString += "</table></div>";

        feature.properties.center = turf.center(feature);
        if (feature.properties.center) {
            popupString += `<div class="row"><button class="btn btn-default btn-xs col-xs-12" onclick='map.setView({lat:`
                + feature.properties.center.geometry.coordinates[1]
                + ", lng:"
                + feature.properties.center.geometry.coordinates[0]
                + `}, 16)' ><i class="fa fa-search-plus"></i> Zoom</button></div></div>`
        }

        layer.bindPopup(popupString);
    }
};

var loadPathLayer = function () {
    return loadPaths().then(function (data) {
        // Update the top bar count
        $('#flowCount').text(data.features.length);
        return L.geoJson(data, {
            style: {
                "weight": 4,
                "opacity": 0.5
            }
        });
        //map.fitBounds(path.getBounds());
        //hash = new L.Hash(map);
    });
};

var togglePaths = function () {
    return toggleMapLayer('paths', loadPathLayer());
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
                            var time = distance * (fastAnimation ? 3 : 10); // 100 km/s
                            durations.push(time);
                        }
                        last = point;
                        return [coord[1], coord[0], (coord.length > 2 ? coord[2] : 0)];
                    }).reverse();
                    durations.reverse();

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

var loadSourceLayer = function () {
    return loadSources().then(function (data) {
        var icon = glyphIcon("supplier"); // L.MakiMarkers.icon({icon: "rail", color: "#4dac26", size: "m"});
        $('#sourceCount').text(data.features.length);
        data.features.forEach(function (feature) {
            feature.properties.type = "supplier";
        });
        var geoJsonLayer = L.geoJson(data, {
            onEachFeature: infraPopup,
            pointToLayer: function (feature, latlng) {
                return L.marker(latlng, {icon: icon});
            }
        });
        map.fitBounds(geoJsonLayer.getBounds());
        return geoJsonLayer;
    });
};

var toggleSources = function () {
    return toggleMapLayer('sources', loadSourceLayer());
};

var loadSinkLayer = function () {
    return loadSinks().then(function (data) {
        var icon = glyphIcon("consumer"); //L.MakiMarkers.icon({icon: "rail", color: "#d01c8b", size: "m"});
        $('#sinkCount').text(data.features.length);
        data.features.forEach(function (feature) {
            feature.properties.type = "consumer";
        });
        var geoJsonLayer = L.geoJson(data, {
            onEachFeature: infraPopup,
            pointToLayer: function (feature, latlng) {
                return L.marker(latlng, {icon: icon});
            }
        });
        return geoJsonLayer;
    });
};

var toggleSinks = function () {
    return toggleMapLayer('sinks', loadSinkLayer());
};

var loadMergedRangeLayer = function () {
    return loadMergedRangeRings().then(function (merged) {
        var layer = L.geoJson(merged, {
            style: {
                "color": "#d00",
                "weight": 2,
                "opacity": 0.8,
                "fillOpacity": 0.10
            }
        });
        return layer;
    });
};

var toggleRangeRings = function () {
    return toggleMapLayer('rings', loadMergedRangeLayer());
};

var loadRangeFill = function () {
    return toggleMapLayer('rings', loadRangeRings().then(function (data) {
        return L.geoJson(data, {
            style: {
                "color": "#d00",
                "weight": 0,
                "opacity": 0.5,
                "fillOpacity": 0.05
            }
        });
    }));
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
    return toggleMapLayer('segments', loadSegmentLayer());
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

var targetPopup = function (feature, layer) {
    if (feature.properties) {
        var popupString = '<div class="row"><table class="table table-condensed"><thead><tr><th><i class="fa fa-crosshairs"></i> Critical Bridge</th></tr></thead>';
        for (var key in feature.properties) {
            var prettyKey;
            var rowClass = "";
            var prettyValue = feature.properties[key];
            if (key == 'criticality') {
                prettyKey = 'Cost of Destruction';
                // Format this cost in terms of hours, with nice commas and rounding
                prettyValue = ("" + Math.round(prettyValue / 100)).replace(/\B(?=(\d{3})+(?!\d))/g, ",") + " hrs";
            } else if (key == 'elevations') {
                prettyKey = 'Elevation';
                var array = JSON.parse(prettyValue);
                var count = array.length;
                // Compute the average elevation
                prettyValue = array.reduce(function (a, b) {
                        return a + b;
                    }) / count;
                prettyValue += " m";
            } else if (key == 'activeSAM') {
                continue; // We'll handle activeSAM elsewhere
            } else if (key == 'nearestStation') {
                prettyKey = `<a href="#" onclick="toggleStations()"> Nearest Station</a>`;
                var distance = turf.distance(feature.properties['nearestStation'], feature.properties['center']);
                prettyValue = Math.round(distance) + " km";
            } else if (key == 'nearestSAM') {
                // Prep a function to show just the nearest SAM on click


                prettyKey = `<a href="#" onclick="toggleSAMThreats()">`;
                if (feature.properties.activeSAM) {
                    prettyKey += `SAM Threat</a>`;
                    rowClass = "danger"
                } else {
                    prettyKey = `Nearest Radar</a>`;
                }
                var distance = turf.distance(feature.properties['nearestSAM'], feature.properties['center']);
                prettyValue = Math.round(distance) + " km";
            } else if (key == 'center') {
                continue; // Skip center
                //prettyKey = "MGRS";
                //prettyValue = mgrs.forward(prettyValue.geometry.coordinates);
            } else if (key == 'nearestAirbase') {
                rowClass = "warning";
                prettyKey = `<a href="#" onclick="toggleAviation()"> Nearest Airbase</a>`;
                var distance = turf.distance(feature.properties['nearestAirbase'], feature.properties['center']);
                prettyValue = Math.round(distance) + " km";
            } else {
                prettyKey = key.charAt(0).toUpperCase() + key.slice(1);
                // If we still haven't made a string out of this
                if (Number.isFinite(prettyValue)) {
                    prettyValue = Math.round(prettyValue) + " km";
                } else if (typeof prettyValue == 'boolean') {
                    prettyValue = prettyValue ? "Yes" : "No";
                }
            }

            popupString += `<tr class="` + rowClass + `"><td><strong>` + prettyKey + "</strong></td><td>" + prettyValue + "</td></tr>";
        }
        popupString += "</table></div>";

        if (feature.properties.center) {
            popupString += `<div class="row"><button class="btn btn-default btn-xs col-xs-12" onclick='map.setView({lat:`
                + feature.properties.center.geometry.coordinates[1]
                + ", lng:"
                + feature.properties.center.geometry.coordinates[0]
                + `}, 16)' ><i class="fa fa-search-plus"></i> Zoom</button></div></div>`
        }

        layer.bindPopup(popupString);
    }
};

var loadTargetLayer = function () {
    return loadTargets().then(function (data) {
        // Load up the merged SAM threats
        return loadMergedRangeRings().then(function (mergedRings) {
            // Mark the targets which are in range
            console.log("Computing active SAM threats");
            loadingControl.addLoader("SAM");
            data.features.forEach(function (feature) {
                var center = turf.center(feature);
                feature.properties.center = center;
                if (turf.inside(center, mergedRings)) {
                    feature.properties["activeSAM"] = true;
                }
            });
            loadingControl.removeLoader("SAM");
            return data;
        }).then(loadSAMs).then(function (sams) {
            console.log("Computing SAM ranges");
            loadingControl.addLoader("Range");
            data.features.forEach(function (feature) {
                var center = feature.properties.center;
                var nearest = turf.nearest(center, sams);
                feature.properties["nearestSAM"] = nearest;
            });
            loadingControl.removeLoader("Range");
            return data;
        }).then(loadAviation).then(function (airbases) {
            console.log("Computing airbase ranges");
            loadingControl.addLoader("Air");
            data.features.forEach(function (feature) {
                var center = feature.properties.center;
                var nearest = turf.nearest(center, airbases);
                feature.properties["nearestAirbase"] = nearest;
            });
            loadingControl.removeLoader("Air");
            return data;
        });
    }).then(function (data) {
        // Have a quick look through the data and figure out what the range of criticality
        var criticalityData = data.features.map(function (feature) {
            return feature.properties.criticality;
        });

        var minCriticality = d3_array.min(criticalityData);
        var midPoint = d3_array.median(criticalityData);
        var maxCriticality = Math.min(d3_array.max(criticalityData), (midPoint - minCriticality) + midPoint);

        // Update the top bar count
        $('#targetCount').text(data.features.length);

        return L.geoJson(data, {
            onEachFeature: targetPopup,
            style: function (feature) {
                var criticality = feature.properties.criticality;
                var color = d3_scale.scaleLinear()
                    .domain([minCriticality, midPoint, maxCriticality])
                    .range(["#00FF00", "#FFFF00", "#FF0000"]);
                return {
                    "color": color(criticality),
                    "weight": 8,
                    "opacity": 0.66
                }
            }
        });
    });
};

var toggleTargets = function () {
    return toggleMapLayer('targets', loadTargetLayer());
};

var toggleFlows = function () {
    return togglePaths().then(function () {
        toggleAnimation('baseline')
    });
};

var toggleSAMThreats = function () {
    return toggleRangeRings().then(toggleSAMs);
};

var showSinks = function () {
    return showMapLayer('sinks', loadSinkLayer());
};
var showSources = function () {
    return showMapLayer('sources', loadSourceLayer());
};
var showPaths = function () {
    return showMapLayer('paths', loadPathLayer());
};
var showTargets = function () {
    return showMapLayer('targets', loadTargetLayer());
};
var showBaselineAnimation = function () {
    return showAnimation('baseline');
};