var mapmargin = 50;
$(window).on("resize", resize);
resize();
function resize() {
    $('#map').css("height", ($(window).height() - mapmargin ));
    $('#map').css("margin-top", -21);
}

$(function () {
    $('[data-toggle="tooltip"]').tooltip()
});

var urlHash = window.location.hash;

var DATA_DIR = "elevation/";
var debug = urlHash == "#debug";
var tutorial = urlHash == "#tutorial";

// Setup Map
var layer;
var hash;
var drawnItems;

var map = L.map('map', {
    zoomControl: false
}).setView([31.531634, 106.054523], 5);

// Tile sets
var mapboxLayer = L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}@2x.png?access_token={accessToken}', {
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
    maxZoom: 18,
    id: 'mapbox.streets',
    accessToken: 'pk.eyJ1Ijoic3BhcnRhbmdvIiwiYSI6IkFvOEpBcWcifQ.YJf-kBxkS9GYW2SFQ3Bpcg'
});

var satelliteMapboxLayer = L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}@2x.png?access_token={accessToken}', {
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
    maxZoom: 18,
    id: 'mapbox.satellite',
    accessToken: 'pk.eyJ1Ijoic3BhcnRhbmdvIiwiYSI6IkFvOEpBcWcifQ.YJf-kBxkS9GYW2SFQ3Bpcg'
});

var satelliteDigitalGlobeLayer = L.tileLayer('https://{s}.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}@2x.png?access_token={accessToken}', {
    attribution: '&copy; Mapbox &copy; OpenStreetMap &copy; DigitalGlobe',
    maxZoom: 20,
    id: 'digitalglobe.nal0g75k',
    accessToken: 'pk.eyJ1IjoiZGlnaXRhbGdsb2JlIiwiYSI6ImNpa3ZneDl4cTAwZnp1OWtzZmJjNHdvNmsifQ.evG8fSrSxdxBmez_564Pug'
});

var hybridDigitalGlobeLayer = L.tileLayer('https://{s}.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}@2x.png?access_token={accessToken}', {
    attribution: '&copy; Mapbox &copy; OpenStreetMap &copy; DigitalGlobe',
    maxZoom: 18,
    id: 'digitalglobe.nal0mpda',
    accessToken: 'pk.eyJ1IjoiZGlnaXRhbGdsb2JlIiwiYSI6ImNpa3ZneDl4cTAwZnp1OWtzZmJjNHdvNmsifQ.evG8fSrSxdxBmez_564Pug'
});

var topoMapboxLayer = L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}@2x.png?access_token={accessToken}', {
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
    maxZoom: 18,
    id: 'mapbox.outdoors',
    accessToken: 'pk.eyJ1Ijoic3BhcnRhbmdvIiwiYSI6IkFvOEpBcWcifQ.YJf-kBxkS9GYW2SFQ3Bpcg'
});

var hybridMapboxLayer = L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}@2x.png?access_token={accessToken}', {
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
    maxZoom: 18,
    id: 'mapbox.streets-satellite',
    accessToken: 'pk.eyJ1Ijoic3BhcnRhbmdvIiwiYSI6IkFvOEpBcWcifQ.YJf-kBxkS9GYW2SFQ3Bpcg'
});

var CartoDB_Positron = L.tileLayer('http://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}@2x.png', {
    attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="http://cartodb.com/attributions">CartoDB</a>',
    subdomains: 'abcd',
    maxZoom: 19
});

var CartoDB_DarkMatter = L.tileLayer('http://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png', {
    attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="http://cartodb.com/attributions">CartoDB</a>',
    subdomains: 'abcd',
    maxZoom: 19
});

var OpenWeatherMap_Clouds = L.tileLayer('http://{s}.tile.openweathermap.org/map/clouds/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: 'Map data &copy; <a href="http://openweathermap.org">OpenWeatherMap</a>',
    opacity: 0.5
});

var OpenWeatherMap_Precipitation = L.tileLayer('http://{s}.tile.openweathermap.org/map/precipitation/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: 'Map data &copy; <a href="http://openweathermap.org">OpenWeatherMap</a>',
    opacity: 0.5
});

var OpenWeatherMap_Pressure = L.tileLayer('http://{s}.tile.openweathermap.org/map/pressure/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: 'Map data &copy; <a href="http://openweathermap.org">OpenWeatherMap</a>',
    opacity: 0.5
});

var OpenWeatherMap_PressureContour = L.tileLayer('http://{s}.tile.openweathermap.org/map/pressure_cntr/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: 'Map data &copy; <a href="http://openweathermap.org">OpenWeatherMap</a>',
    opacity: 0.5
});

var OpenWeatherMap_Wind = L.tileLayer('http://{s}.tile.openweathermap.org/map/wind/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: 'Map data &copy; <a href="http://openweathermap.org">OpenWeatherMap</a>',
    opacity: 0.5
});
var OpenWeatherMap_Temperature = L.tileLayer('http://{s}.tile.openweathermap.org/map/temp/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: 'Map data &copy; <a href="http://openweathermap.org">OpenWeatherMap</a>',
    opacity: 0.5
});

var baseMaps = {
    "Streets": CartoDB_Positron,
    "Dark": CartoDB_DarkMatter,
    "Physical": mapboxLayer,
    "Topo": topoMapboxLayer,
    "Satellite": hybridMapboxLayer,
    "Latest Imagery": satelliteDigitalGlobeLayer
};

var defaultMap = baseMaps["Streets"];
defaultMap.addTo(map);

var overlayMaps = {
    //"Labels": MapQuestOpen_HybridOverlay,
    //"Clouds": OpenWeatherMap_Clouds,
    //"Precipitation": OpenWeatherMap_Precipitation,
    //"Pressure": OpenWeatherMap_Pressure,
    //"Pressure Contours": OpenWeatherMap_PressureContour,
    //"Wind": OpenWeatherMap_Wind,
    //"Temperature": OpenWeatherMap_Temperature
};

// Icon sets
var defaultColor = "white";
var typeColors = {
    "port": "#00b",
    "SAM": "#ffc000",
    "airbase": "#ff7500",
    "station": "#00C8EE",
    "nuclear": "#ff0000",
    "source": "#4dac26",
    "sink": "#d01c8b"
};

var clusterIcon = function (cluster) {
    var radius = 36;
    var strokeLength = 2 * 3.141592 * radius;
    var colorCount = {};
    var childCount = cluster.getChildCount();

    cluster.getAllChildMarkers().forEach(function (el) {
        var type = el.feature.properties.type;
        var color = typeColors[type] ? typeColors[type] : defaultColor;
        if (colorCount.hasOwnProperty(color))
            colorCount[color]++;
        else
            colorCount[color] = 1;
    });


    var textOpacity = 1;
    var dashoffsetSum = 0;
    var svgHtml = `<svg width="100%" height="100%" viewbox="0 0 100 100"> \
        <circle cx="50" cy="50" r="` + radius + `" fill="black" fill-opacity="0.5"/>`

    for (var color in colorCount) {
        svgHtml = svgHtml + `<circle cx="50" cy="50" r="` + radius + `" fill="transparent" stroke-width="10" stroke=` +
            color + ` stroke-dasharray=` + strokeLength + ` stroke-dashoffset="` + dashoffsetSum + `" stroke-opacity="` + textOpacity + `" />`
        var currLength = (1.0 * colorCount[color] / childCount) * strokeLength;
        dashoffsetSum += currLength;
    }

    var textX = 42;
    var fontSize = 32;
    if (childCount >= 10) {
        textX = 32;
    }
    if (childCount >= 100) {
        fontSize = 30;
        textX = 24;
    }
    if (childCount >= 1000) {
        textX = 20;
        fontSize = 28;
    }

    svgHtml += `<text x="` + textX + `" y="60" style="fill: white; font-size: ` + fontSize + `px; font-weight: bold; opacity: ` + textOpacity + `;">` + childCount + `</text>
        </svg>`;

    return new L.DivIcon({html: svgHtml, className: 'tiny-marker-cluster', iconSize: new L.Point(radius, radius)});
};

var glyphIcon = function (type, iconChar) {
    var radius = 36;
    var color = typeColors[type] ? typeColors[type] : type;
    var textOpacity = 1;

    var svgHtml = `<svg class="glyph-icon-` + type + `" width="100%" height="100%" viewbox="0 0 100 100"> \
        <circle cx="50" cy="50" r="` + radius + `" fill="` + color + `" fill-opacity="0.95"/>`

    var textX = 36;
    var fontSize = 32;

    svgHtml += `<text x="` + textX + `" y="60" style="fill: white; font-family: FontAwesome; font-size: ` + fontSize + `px; font-weight: bold; opacity: ` + textOpacity + `;">` + iconChar + `</text>
        </svg>`;

    return new L.DivIcon({html: svgHtml, className: 'tiny-marker-cluster', iconSize: new L.Point(radius, radius)});
};

// Data loading
var loadGeoJSON = function (path) {
    return new Promise(function (resolve, reject) {
        loadingControl.addLoader(path);
        var xhr = new XMLHttpRequest();
        xhr.open('GET', path, true);
        xhr.onload = function () {
            if (xhr.readyState == 4) {
                var data = JSON.parse(xhr.responseText);
                resolve(data);
                loadingControl.removeLoader(path);
            }
        };
        xhr.onerror = function () {
            reject(this.statusText);
            loadingControl.removeLoader(path);
        };
        xhr.send();
    });
};

var backgroundLayers = {};
var backgroundMarkers = new L.MarkerClusterGroup({
    iconCreateFunction: clusterIcon,
    maxClusterRadius: 50
}).addTo(map);

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
        var popupString = "<div style='height:100px;overflow:auto;'><table>";
        for (key in feature.properties) {
            popupString += "<tr><td>" + key + "</td><td>" + feature.properties[key] + "</td></tr>";
        }

        popupString += "</table></div>";
        if (feature.geometry) {
            popupString += "<div><button onclick='map.setView({lat:"
                + feature.geometry.coordinates[1]
                + ", lng:"
                + feature.geometry.coordinates[0]
                + "}, 16)'>Zoom</button></div>"
        }
        layer.bindPopup(popupString);
    }
};

var loadPorts = function () {
    return loadGeoJSON('background/WPI.geojson').then(function (data) {
        data.features.forEach(function (feature) {
            feature.properties.type = "port";
        });
        var icon = glyphIcon("port", "&#xf13d"); //L.MakiMarkers.icon({icon: "harbor", color: typeColors["port"], size: "s"});
        return L.geoJson(data, {
            onEachFeature: infraPopup,
            pointToLayer: function (feature, latlng) {
                return L.marker(latlng, {icon: icon});
            }
        })
    });
};

var togglePorts = function () {
    return toggleClusterLayer('ports', loadPorts());
};
var loadSAMs = function () {
    return loadGeoJSON('background/SAMs.geojson').then(function (data) {
        data.features.forEach(function (feature) {
            feature.properties.type = "SAM";
        });

        var icon = glyphIcon("SAM", "&#xf135"); //L.MakiMarkers.icon({icon: "rocket", color: typeColors["SAM"], size: "s"});
        //var heatPoints = [];
        return L.geoJson(data, {
            onEachFeature: infraPopup,
            pointToLayer: function (feature, latlng) {
                //heatPoints.push({lat: latlng.lat, lng: latlng.lng, count: 1});
                return L.marker(latlng, {icon: icon});
            }
        });
    })
};

var toggleSAMs = function () {
    return toggleClusterLayer('sams', loadSAMs());
};

var loadAviation = function () {
    return loadGeoJSON('background/ChineseMilitaryAviation.geojson').then(function (data) {
        data.features.forEach(function (feature) {
            feature.properties.type = "airbase";
        });

        var icon = glyphIcon("airbase", "&#xf072");
        //L.MakiMarkers.icon({icon: "airport", color: typeColors["airbase"], size: "s"});
        return L.geoJson(data, {
            onEachFeature: infraPopup,
            pointToLayer: function (feature, latlng) {
                return L.marker(latlng, {icon: icon});
            }
        });
    })
};

var toggleAviation = function () {
    return toggleClusterLayer('aviation', loadAviation());
};

var loadSecondArtillery = function () {
    return loadGeoJSON('background/2AOperationalSites.geojson').then(function (data) {
        data.features.forEach(function (feature) {
            feature.properties.type = "nuclear";
        });

        var icon = glyphIcon("nuclear", "&#xf1e2"); //L.MakiMarkers.icon({icon: "danger", color: typeColors["nuclear"], size: "s"});
        return L.geoJson(data, {
            filter: function (feature) {
                return feature.properties.name != "Garrison" && feature.properties.name != "UGF"
            },
            onEachFeature: infraPopup,
            pointToLayer: function (feature, latlng) {
                return L.marker(latlng, {icon: icon});
            }
        });
    });
};

var toggleSecondArtillery = function () {
    return toggleClusterLayer('missiles', loadSecondArtillery());
};

var stationPopup = function (feature, layer) {
    if (feature.properties) {
        var popupString = "<table>";
        for (var key in feature.properties) {
            popupString += "<tr><td>" + key + "</td> <td>" + feature.properties[key] + "</td></tr>";
        }

        if (feature.geometry) {
            popupString += "<tr><td><button onclick='map.setView({lat:"
                + feature.geometry.coordinates[1]
                + ", lng:"
                + feature.geometry.coordinates[0]
                + "}, 16)'>Zoom</button></td></tr>"
        }

        popupString += "</table>";
        layer.bindPopup(popupString);
    }
};

var loadStations = function () {
    return loadGeoJSON('background/stations.geojson').then(function (data) {
        data.features.forEach(function (feature) {
            feature.properties.type = "station";
        });

        var icon = glyphIcon("station", "&#xf238");//L.MakiMarkers.icon({icon: "rail", color: typeColors["station"], size: "s"});
        return L.geoJson(data, {
            onEachFeature: stationPopup,
            pointToLayer: function (feature, latlng) {
                return L.marker(latlng, {icon: icon});
            }
        });
    });
};

var toggleStations = function () {
    return toggleClusterLayer('stations', loadStations());
};

var pathPopup = function (feature, layer) {
    if (feature.properties) {
        var popupString = "<table>";
        for (var key in feature.properties) {
            popupString += "<tr><td>" + key + "</td><td>" + feature.properties[key] + "</td></tr>";
        }
        popupString += "</table>";
        layer.bindPopup(popupString);
    }
};

var loadPath = function (id) {
    return loadGeoJSON(DATA_DIR + id + '_path.geojson')
        .then(function (data) {
            var path = L.geoJson(data, {
                //onEachFeature: pathPopup
                style: {
                    "weight": 4,
                    "opacity": 0.5
                }
            });
            path.addTo(map);
        });
};

var loadPaths = function () {
    return loadGeoJSON(DATA_DIR + 'baseline.geojson').then(function (data) {
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
    return toggleMapLayer('paths', loadPaths());
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

var loadSources = function () {
    return loadGeoJSON(DATA_DIR + 'sources.geojson').then(function (data) {
        var icon = glyphIcon("source", '&#xf093'); // L.MakiMarkers.icon({icon: "rail", color: "#4dac26", size: "m"});
        $('#sourceCount').text(data.features.length);

        var geoJsonLayer = L.geoJson(data, {
            onEachFeature: stationPopup,
            pointToLayer: function (feature, latlng) {
                return L.marker(latlng, {icon: icon});
            }
        });
        map.fitBounds(geoJsonLayer.getBounds());
        return geoJsonLayer;
    });
};

var toggleSources = function () {
    return toggleMapLayer('sources', loadSources());
};

var loadSinks = function () {
    return loadGeoJSON(DATA_DIR + 'sinks.geojson').then(function (data) {
        var icon = glyphIcon("sink", '&#xf019'); //L.MakiMarkers.icon({icon: "rail", color: "#d01c8b", size: "m"});
        $('#sinkCount').text(data.features.length);

        var geoJsonLayer = L.geoJson(data, {
            onEachFeature: stationPopup,
            pointToLayer: function (feature, latlng) {
                return L.marker(latlng, {icon: icon});
            }
        });
        return geoJsonLayer;
    });
};

var toggleSinks = function () {
    return toggleMapLayer('sinks', loadSinks());
};


var loadRangeRings = function () {
    return loadGeoJSON('background/RangeRingsP.geojson').then(function (data) {
        // Merge the rings
        var merged = turf.merge(data);
        return L.geoJson(merged, {
            style: {
                "color": "#d00",
                "weight": 2,
                "opacity": 0.8,
                "fillOpacity": 0.10
            }
        });
    });
};

var toggleRangeRings = function () {
    return toggleMapLayer('rings', loadRangeRings());
};

var loadRangeFill = function () {
    return toggleMapLayer('rings',
        loadGeoJSON('background/RangeRingsP.geojson').then(function (data) {
            // Merge the rings
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
var loadSegments = function () {
    return loadGeoJSON(DATA_DIR + 'bridges.geojson')
        .then(function (data) {
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
                onEachFeature: pathPopup,
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
    return toggleMapLayer('segments', loadSegments());
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


var loadTargets = function () {
    return loadGeoJSON(DATA_DIR + 'damage.geojson')
        .then(function (data) {
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
                // TODO: call for the adjustments here
                onEachFeature: pathPopup,
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
        })
};

var toggleTargets = function () {
    return toggleMapLayer('targets', loadTargets());
};

var toggleFlows = function () {
    return togglePaths().then(function () {
        toggleAnimation('baseline')
    });
};

// Default controls
var loadingControl = L.Control.loading({
    separate: true,
    position: 'topright'
});
map.addControl(loadingControl);

L.control.scale().addTo(map);
var layerControl = L.control.layers(baseMaps, overlayMaps, {position: 'bottomleft'});
layerControl.addTo(map);

var bridgeButton = L.easyButton('fa-road', function (btn, map) {
    if (!allBridges && backgroundLayers['segments']) {
        // unload the layer to be reloaded
        toggleSegments();
    }

    allBridges = true;
    // Load or unload the layer
    toggleSegments();
});

// Advanced controls
var portButton = L.easyButton('fa-anchor', function (btn, map) {
    togglePorts();
});
var aviationButton = L.easyButton('fa-plane', function (btn, map) {
    toggleAviation();
});
var nuclearButton = L.easyButton('fa-bomb', function (btn, map) {
    toggleSecondArtillery();
});

var stationButton = L.easyButton('fa-train', function (btn, map) {
    toggleStations();
});

var SAMButton = L.easyButton('fa-rocket', function (btn, map) {
    toggleSAMs();
});

var flowButton = L.easyButton('fa-exchange', function (btn, map) {
    toggleFlows();
});

var rangeRingButton = L.easyButton('fa-warning', function (btn, map) {
    toggleRangeRings();
});
var priorityButton = L.easyButton('fa-sort-numeric-desc', function (btn, map) {
    if (allBridges && backgroundLayers['segments']) {
        // unload the layer to be reloaded
        toggleSegments();
    }

    allBridges = false;
    // Load or unload the layer
    toggleSegments();
});
var damageButton = L.easyButton('fa-crosshairs', function (btn, map) {
    toggleTargets();
});

var fastButton = L.easyButton('fa-bolt', function (btn, map) {
    if (backgroundLayers['animation']) {
        // unload the layer to be reloaded
        toggleAnimation('baseline');
    }

    fastAnimation = !fastAnimation;
    // Load or unload the layer
    toggleAnimation('baseline');
}, {
    position: 'topright'
});

L.easyBar([
    flowButton,
    priorityButton,
    damageButton
]);

var enableDrawing = function () {
    drawnItems = L.featureGroup().addTo(map);
    map.addControl(new L.Control.Draw({
        position: 'topleft',
        edit: {
            featureGroup: drawnItems
        }
    }));
    map.on('draw:created', function (event) {
        var layer = event.layer;
        drawnItems.addLayer(layer);
    });
};

L.easyButton('fa-pencil', function (btn) {
    btn.removeFrom(map);
    enableDrawing();
}, {
    position: 'topleft'
}).addTo(map);

L.easyButton('fa-cogs', function (btn) {
    btn.removeFrom(map);
    L.easyBar([
        bridgeButton,
        portButton,
        stationButton,
        aviationButton,
        nuclearButton,
        SAMButton,
        rangeRingButton
    ], {
        position: 'bottomright'
    }).addTo(map);

    layerControl.addOverlay(OpenWeatherMap_Clouds, 'Clouds');
    layerControl.addOverlay(OpenWeatherMap_Precipitation, 'Precipitation');
    layerControl.addOverlay(OpenWeatherMap_Pressure, 'Pressure');
    layerControl.addOverlay(OpenWeatherMap_PressureContour, 'Pressure Contours');
    layerControl.addOverlay(OpenWeatherMap_Wind, 'Wind');
    layerControl.addOverlay(OpenWeatherMap_Temperature, 'Temperature');
}, {
    position: 'bottomright'
}).addTo(map);

var refreshTargets = function () {
    setTimeout(function () {
        hideMapLayer('targets')   // Unload the layer
            .then(toggleTargets); // Reload the layer

        // Schedule another run in 60s
        refreshTargets();
    }, 60000);
};

var startTutorial = function () {
    tutorial = true;
    var intro = introJs();
    var steps = [
        {
            intro: "<h5>Introduction</h5><p>This is an analysis of supply network resilience. "
            + "It explores the flow of supplies from producers to consumers, "
            + "as well as vulnerabilities in the infrastructure supporting that movement.</p>",
            position: 'bottom'
        },
        {
            intro: "<h5>Suppliers</h5><p>The green markers identify suppliers of resources, such as food, fuel, and parts. "
            + "Trains load supplies at and depart from stations near these locations. </p>",
            position: 'bottom'
        },
        {
            intro: "<h5>Consumers</h5><p>The purple markers indicate consumers of those resources, "
            + "such as maintenance facilities, populations, or units. "
            + "Trains carrying supplies arrive and are offloaded at stations near these locations. </p>",
            position: 'bottom'
        },
        {
            intro: "<h5>Flow</h5><p>The blue paths represent the railroad links connecting suppliers to consumers, "
            + "with small dots indicating flow. "
            + "Each path is optimized, minimizing the costs associated with traveling across terrain. "
            + "</p>",
            position: 'bottom'
        },
        {
            intro: "<h5>Resilience</h5><p>The highlighted segments are vulnerable bridges along the rail routes. "
            + "Their relative vulnerability is indicated by color from green (low) to red (high) "
            + "and is determined by simulating damage to each bridge. </p>",
            position: 'bottom'
        }
    ];
    intro.setOptions({
        steps: steps,
        scrollToElement: false,
        showStepNumbers: false,
        overlayOpacity: 0.0,
        showBullets: false,
        exitOnOverlayClick: false,
        disableInteraction: false,
        skipLabel: 'Done',
        tooltipClass: 'custom-tooltip'
    }).onbeforechange(function (targetElement) {
        // Fetch the relevant data
        switch (this._currentStep) {
            case 0: // Nothing
                hideMapLayer('sources');
                hideMapLayer('sinks');
                hideMapLayer('paths');
                hideMapLayer('targets');
                clearAnimation();
                break;
            case 1: // Sources
                // Hide everything
                hideMapLayer('sinks');
                hideMapLayer('paths');
                hideMapLayer('targets');
                clearAnimation();
                showMapLayer('sources', loadSources());
                break;
            case 2: // Sinks
                hideMapLayer('sources');
                hideMapLayer('paths');
                hideMapLayer('targets');
                clearAnimation();
                showMapLayer('sinks', loadSinks());
                break;

            case 3: // Paths
                hideMapLayer('targets');
                showMapLayer('sources', loadSources()).then(function () {
                    showMapLayer('sinks', loadSinks());
                }).then(function () {
                    showMapLayer('paths', loadPaths());
                }).then(function () {
                    showAnimation('baseline');
                });
                break;

            case 4: // Targets
                toggleTargets();
                break;
        }
    }).onafterchange(function () {
        var element = document.querySelector('.introjs-tooltipReferenceLayer')
        if (element) {
            element.style.setProperty('top', '120px');
        }
    }).onexit(function () {
        showMapLayer('sinks', loadSinks()).then(function () {
            return showMapLayer('sources', loadSources());
        }).then(function () {
            return showMapLayer('paths', loadPaths());
        }).then(function () {
            return showMapLayer('targets', loadTargets());
        }).then(function () {
            return showAnimation('baseline');
        });

    });
    intro.start();
};

if (debug) {
    refreshTargets();
}

if (tutorial) {
    startTutorial();
} else {
    // Default layers
    toggleSources()
        .then(toggleSinks)
        .then(toggleFlows)
        .then(toggleTargets);
}

