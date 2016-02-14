/**
 * Created by spartango on 10/2/15.
 */
var DATA_DIR = "testing/";

// Setup Map
var layer;
var hash;

var map = L.map('map', {
    zoomControl: false
}).setView([51.505, -0.09], 13);
L.control.fullscreen({
    position: 'bottomright'
}).addTo(map);

var loadingControl = L.Control.loading({
    separate: true,
    position: 'bottomright'
});
map.addControl(loadingControl);

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

var hybridMapboxLayer = L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}@2x.png?access_token={accessToken}', {
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
    maxZoom: 18,
    id: 'mapbox.streets-satellite',
    accessToken: 'pk.eyJ1Ijoic3BhcnRhbmdvIiwiYSI6IkFvOEpBcWcifQ.YJf-kBxkS9GYW2SFQ3Bpcg'
});

var Esri_WorldImagery = L.tileLayer('http://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
    detectRetina: true,
    attribution: 'Tiles &copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community'
});

var Esri_WorldPhysical = L.tileLayer('http://server.arcgisonline.com/ArcGIS/rest/services/World_Physical_Map/MapServer/tile/{z}/{y}/{x}', {
    attribution: 'Tiles &copy; Esri &mdash; Source: US National Park Service',
    detectRetina: true,
    maxZoom: 8
});

var Esri_WorldTopoMap = L.tileLayer('http://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}', {
    attribution: 'Tiles &copy; Esri &mdash; Esri, DeLorme, NAVTEQ, TomTom, Intermap, iPC, USGS, FAO, NPS, NRCAN, GeoBase, Kadaster NL, Ordnance Survey, Esri Japan, METI, Esri China (Hong Kong), and the GIS User Community'
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

var OpenStreetMap_Mapnik = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
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

var MapQuestOpen_HybridOverlay = L.tileLayer('http://otile{s}.mqcdn.com/tiles/1.0.0/{type}/{z}/{x}/{y}.{ext}', {
    type: 'hyb',
    ext: 'png',
    attribution: 'Tiles Courtesy of <a href="http://www.mapquest.com/">MapQuest</a> &mdash; Map data &copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    subdomains: '1234',
    opacity: 0.9
});

var baseMaps = {
    "Streets": CartoDB_Positron,
    //"Dark": CartoDB_DarkMatter,
    "Physical": mapboxLayer,
    //"Physical": Esri_WorldPhysical,
    //"Topo": Esri_WorldTopoMap,
    //"Imagery": Esri_WorldImagery,
    "Hybrid": hybridMapboxLayer,
    //"OpenStreetMap": OpenStreetMap_Mapnik,
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

var layerControl = L.control.layers(baseMaps, overlayMaps);
layerControl.addTo(map);
L.control.scale().addTo(map);

var backgroundLayers = {};

var loadGeoJSON = function (path, callback) {
    loadingControl.addLoader(path);
    var xhr = new XMLHttpRequest();
    xhr.open('GET', path, true);
    xhr.onload = function () {
        if (xhr.readyState == 4) {
            var data = JSON.parse(xhr.responseText);
            callback(data);
            loadingControl.removeLoader(path);
        }
    };
    xhr.send();
};

var showRoutes = function (id) {
    loadGeoJSON(DATA_DIR + '20_' + id + '_path.geojson',
        function (data) {
            if (layer && data) {
                map.removeLayer(layer);
            }

            layer = L.geoJson(data);
            layer.addTo(map);
        });
};

var infraPopup = function (feature, layer) {
    // does this feature have a property named popupContent?
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
    if (!backgroundLayers['ports']) {
        loadGeoJSON('background/WPI.geojson', function (data) {
            var markers = new L.MarkerClusterGroup({
                iconCreateFunction: function (cluster) {
                    return L.MakiMarkers.icon({icon: "ferry", color: "#00b", size: "m"});
                }
            });
            var icon = L.MakiMarkers.icon({icon: "ferry", color: "#00b", size: "s"});
            var geoJsonLayer = L.geoJson(data, {
                onEachFeature: infraPopup,
                pointToLayer: function (feature, latlng) {
                    return L.marker(latlng, {icon: icon});
                }
            });
            markers.addLayer(geoJsonLayer);
            backgroundLayers['ports'] = markers;
            map.addLayer(markers);
        });
    } else {
        map.removeLayer(backgroundLayers['ports']);
        delete backgroundLayers['ports'];
    }
};

var loadSAMs = function () {
    if (!backgroundLayers['sams']) {
        loadGeoJSON('background/SAMs.geojson', function (data) {
            var markers = new L.MarkerClusterGroup({
                iconCreateFunction: function (cluster) {
                    return L.MakiMarkers.icon({icon: "triangle", color: "#fb0", size: "m"});
                }
            });

            var icon = L.MakiMarkers.icon({icon: "triangle", color: "#fb0", size: "s"});
            //var heatPoints = [];
            var geoJsonLayer = L.geoJson(data, {
                onEachFeature: infraPopup,
                pointToLayer: function (feature, latlng) {
                    //heatPoints.push({lat: latlng.lat, lng: latlng.lng, count: 1});
                    return L.marker(latlng, {icon: icon});
                }
            });
            var heatmapLayer = new HeatmapOverlay({
                "radius": 1,
                "maxOpacity": .33,
                "scaleRadius": true,
                "useLocalExtrema": false,
                latField: 'lat',
                lngField: 'lng',
                valueField: 'count'
            });
            //map.addLayer(heatmapLayer);
            //heatmapLayer.setData({
            //    max: 3,
            //    data: heatPoints
            //});
            markers.addLayer(geoJsonLayer);
            map.addLayer(markers);
            backgroundLayers['sams'] = markers;
            backgroundLayers['heat'] = heatmapLayer;
        });
    } else {
        map.removeLayer(backgroundLayers['sams']);
        //map.removeLayer(backgroundLayers['heat']);
        delete backgroundLayers['sams'];
        delete backgroundLayers['heat'];
    }
};

var loadAviation = function () {
    if (!backgroundLayers['aviation']) {
        loadGeoJSON('background/ChineseMilitaryAviation.geojson', function (data) {
            var markers = new L.MarkerClusterGroup({
                iconCreateFunction: function (cluster) {
                    return L.MakiMarkers.icon({icon: "airport", color: "#f70", size: "m"});
                }
            });
            var icon = L.MakiMarkers.icon({icon: "airport", color: "#f70", size: "s"});
            var geoJsonLayer = L.geoJson(data, {
                onEachFeature: infraPopup,
                pointToLayer: function (feature, latlng) {
                    return L.marker(latlng, {icon: icon});
                }
            });
            markers.addLayer(geoJsonLayer);
            map.addLayer(markers);
            backgroundLayers['aviation'] = markers;
        });
    } else {
        map.removeLayer(backgroundLayers['aviation']);
        delete backgroundLayers['aviation'];
    }
};

var loadSecondArtillery = function () {
    if (!backgroundLayers['missiles']) {
        loadGeoJSON('background/2AOperationalSites.geojson', function (data) {
            var markers = new L.MarkerClusterGroup({
                iconCreateFunction: function (cluster) {
                    return L.MakiMarkers.icon({icon: "rocket", color: "#f30", size: "m"});
                }
            });
            var icon = L.MakiMarkers.icon({icon: "rocket", color: "#f30", size: "s"});
            var geoJsonLayer = L.geoJson(data, {
                filter: function (feature) {
                    return feature.properties.name != "Garrison" && feature.properties.name != "UGF"
                },
                onEachFeature: infraPopup,
                pointToLayer: function (feature, latlng) {
                    return L.marker(latlng, {icon: icon});
                }
            });
            markers.addLayer(geoJsonLayer);
            map.addLayer(markers);
            backgroundLayers['missiles'] = markers;
        });
    } else {
        map.removeLayer(backgroundLayers['missiles']);
        delete backgroundLayers['missiles'];
    }
};

var stationPopup = function (feature, layer) {
    // does this feature have a property named popupContent?
    if (feature.properties) {
        var popupString = "<table><tr>";
        for (var key in feature.properties) {
            popupString += "<td>" + feature.properties[key] + "</td>";
        }

        if (feature.properties.id || feature.geometry) {
            popupString += "</tr><tr>"
        }

        if (feature.geometry) {
            popupString += "<td><button onclick='map.setView({lat:"
                + feature.geometry.coordinates[1]
                + ", lng:"
                + feature.geometry.coordinates[0]
                + "}, 16)'>Zoom</button></td>"
        }
        if (false && feature.properties.id) {
            popupString += "<td><button onclick='showRoutes(" + feature.properties.id + ")'>Routes</button></td>";
        }

        popupString += "</tr></table>";
        layer.bindPopup(popupString);
    }
};

var loadStations = function () {
    if (!backgroundLayers['stations']) {
        loadGeoJSON('background/stations.geojson', function (data) {
            var markers = new L.MarkerClusterGroup({
                iconCreateFunction: function (cluster) {
                    return L.MakiMarkers.icon({icon: "rail", color: "#00b", size: "m"});
                }
            });
            var icon = L.MakiMarkers.icon({icon: "rail", color: "#00b", size: "s"});
            var geoJsonLayer = L.geoJson(data, {
                onEachFeature: stationPopup,
                pointToLayer: function (feature, latlng) {
                    return L.marker(latlng, {icon: icon});
                }
            });
            markers.addLayer(geoJsonLayer);
            map.addLayer(markers);
            backgroundLayers['stations'] = markers;
        })
    } else {
        map.removeLayer(backgroundLayers['stations']);
        delete backgroundLayers['stations'];
    }
};

var pathPopup = function (feature, layer) {
    if (feature.properties) {
        var popupString = "<table><tr>";
        for (var key in feature.properties) {
            popupString += "<td>" + feature.properties[key] + "</td>";
        }

        if (feature.properties.id || feature.geometry) {
            popupString += "</tr><tr>"
        }
        popupString += "</tr></table>";
        layer.bindPopup(popupString);
    }
};

var loadPath = function (id) {
    loadGeoJSON(DATA_DIR + id + '_path.geojson',
        function (data) {
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
    loadGeoJSON(DATA_DIR + 'baseline.geojson',
        function (data) {
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

var loadSources = function () {
    loadGeoJSON(DATA_DIR + 'sources.geojson', function (data) {
        //var markers = new L.MarkerClusterGroup();
        var icon = L.MakiMarkers.icon({icon: "rail", color: "#0b0", size: "m"});
        var geoJsonLayer = L.geoJson(data, {
            onEachFeature: stationPopup,
            pointToLayer: function (feature, latlng) {
                return L.marker(latlng, {icon: icon});
            }
        });
        //markers.addLayer(geoJsonLayer);
        map.addLayer(geoJsonLayer);
        map.fitBounds(geoJsonLayer.getBounds());

        hash = new L.Hash(map);
    })
};

var loadSinks = function () {
    loadGeoJSON(DATA_DIR + 'sinks.geojson', function (data) {
        //var markers = new L.MarkerClusterGroup();
        var icon = L.MakiMarkers.icon({icon: "rail", color: "#b00", size: "m"});
        var geoJsonLayer = L.geoJson(data, {
            onEachFeature: stationPopup,
            pointToLayer: function (feature, latlng) {
                return L.marker(latlng, {icon: icon});
            }
        });
        //markers.addLayer(geoJsonLayer);
        map.addLayer(geoJsonLayer);
        //map.fitBounds(geoJsonLayer.getBounds());
    })
};

var loadRangeRings = function () {
    if (!backgroundLayers['rings']) {
        loadGeoJSON('background/RangeRingsP.geojson', function (data) {
            // Merge the rings
            var merged = turf.merge(data);
            var fillLayer = L.geoJson(data, {
                style: {
                    "color": "#d00",
                    "weight": 0,
                    "opacity": 0.5,
                    "fillOpacity": 0.05
                }
            });
            var geoJsonLayer = L.geoJson(merged, {
                style: {
                    "color": "#d00",
                    "weight": 2,
                    "opacity": 0.8,
                    "fillOpacity": 0.10
                }
            });
            map.addLayer(geoJsonLayer);
            //map.addLayer(fillLayer);
            backgroundLayers['rings'] = geoJsonLayer;
            backgroundLayers['ringFill'] = fillLayer;
        });
    } else {
        map.removeLayer(backgroundLayers['rings']);
        //map.removeLayer(backgroundLayers['ringFill']);
        delete backgroundLayers['rings'];
        delete backgroundLayers['ringFill'];
    }
};

var allBridges = false;

var loadSegments = function () {
    if (!backgroundLayers['segments']) {
        loadGeoJSON(DATA_DIR + 'bridges.geojson',
            function (data) {
                // Have a quick look through the data and figure out what the range of criticality is
                var criticalityData = data.features.map(function (feature) {
                    return feature.properties.criticality;
                });

                var minCriticality = d3_array.min(criticalityData);
                //var maxCriticality = d3_array.max(criticalityData);

                //var criticalityRange = maxCriticality - minCriticality;
                var midPoint = d3_array.median(criticalityData);
                var maxCriticality = midPoint * 2;

                var path = L.geoJson(data, {
                    filter: function (feature) {
                        return allBridges || feature.properties.criticality >= midPoint;
                    },
                    //onEachFeature: pathPopup,
                    style: function (feature) {
                        var criticality = feature.properties.criticality;
                        var color = d3_scale.scaleLinear()
                            .domain([minCriticality, midPoint, maxCriticality])
                            .range(["#00FF00", "#FFFF00", "#FF0000"]);
                        var weight = criticality < midPoint || allBridges ? 4 : 8;
                        return {
                            "color": color(criticality),
                            "weight": weight,
                            "opacity": 0.66
                        }
                    }
                });
                path.addTo(map);
                backgroundLayers['segments'] = path;
            });
    } else {
        map.removeLayer(backgroundLayers['segments']);
        delete backgroundLayers['segments'];
    }
};

var clearDamagedPath = function () {
    if (backgroundLayers['damagedPath']) {
        map.removeLayer(backgroundLayers['damagedPath']);
        delete backgroundLayers['damagedPath'];
    }

};

var loadDamagedPath = function (id) {
    if (backgroundLayers['damagedPath']) {
        clearDamagedPath();
    }

    loadGeoJSON(DATA_DIR + id + '_damage.geojson',
        function (data) {
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
        });
};

var damagePopup = function (feature, layer) {
    // does this feature have a property named popupContent?
    if (feature.properties) {
        var popupString = "<table><tr>";
        for (var key in feature.properties) {
            popupString += "<td>" + feature.properties[key] + "</td>";
        }

        if (feature.id || feature.geometry) {
            popupString += "</tr><tr>"
        }

        if (feature.id) {
            popupString += "<td><button onclick='loadDamagedPath(" + feature.id + ")'>Reroutes</button></td>";
        }

        popupString += "</tr></table>";
        layer.bindPopup(popupString);
    }
};

var loadTargets = function () {
    if (!backgroundLayers['targets']) {
        loadGeoJSON(DATA_DIR + 'damage.geojson',
            function (data) {
                // Have a quick look through the data and figure out what the range of criticality
                var criticalityData = data.features.map(function (feature) {
                    return feature.properties.criticality;
                });

                var minCriticality = d3_array.min(criticalityData);
                //var maxCriticality = d3_array.max(criticalityData);
                var midPoint = d3_array.median(criticalityData);
                var maxCriticality = midPoint * 2;

                var path = L.geoJson(data, {
                    // TODO: call for the adjustments here
                    onEachFeature: damagePopup,
                    style: function (feature) {
                        var criticality = feature.properties.criticality;
                        var color = d3_scale.scaleLinear()
                            .domain([minCriticality, midPoint, maxCriticality])
                            .range(["#00FF00", "#FFFF00", "#FF0000"]);
                        return {
                            "color": color(criticality),
                            "weight": 12,
                            "opacity": 0.8
                        }
                    }
                });
                path.addTo(map);
                backgroundLayers['targets'] = path;
            });
    } else {
        map.removeLayer(backgroundLayers['targets']);
        delete backgroundLayers['targets'];
    }
};

// Default layers
loadSources();
loadSinks();
loadRangeRings();
loadPaths();

// Default controls
var bridgeButton = L.easyButton('fa-road', function (btn, map) {
    if (!allBridges && backgroundLayers['segments']) {
        // unload the layer to be reloaded
        loadSegments();
    }

    allBridges = true;
    // Load or unload the layer
    loadSegments();
});

L.easyBar([
    L.easyButton('fa-crosshairs', function (btn, map) {
        allBridges = false;
        loadSegments();
    }),
    L.easyButton('fa-fire', function (btn, map) {
        loadTargets();
    })]).addTo(map);

L.easyButton('fa-warning', function (btn, map) {
    loadRangeRings();
}).addTo(map);

// Advanced controls
var portButton = L.easyButton('fa-ship', function (btn, map) {
    loadPorts();
});
var aviationButton = L.easyButton('fa-plane', function (btn, map) {
    loadAviation();
});
var nuclearButton = L.easyButton('fa-rocket', function (btn, map) {
    loadSecondArtillery();
});

var stationButton = L.easyButton('fa-train', function (btn, map) {
    loadStations();
});

var SAMButton = L.easyButton('fa-warning', function (btn, map) {
    loadSAMs();
});

var advancedMode = false;
L.easyButton('fa-building', function (btn) {
    if (!advancedMode) {
        btn.removeFrom(map);
        advancedMode = true;
        L.easyBar([
            bridgeButton,
            portButton,
            aviationButton,
            nuclearButton,
            stationButton,
            SAMButton
        ], {
            position: 'topright'
        }).addTo(map);
        layerControl.addOverlay(OpenWeatherMap_Clouds, 'Clouds');
        layerControl.addOverlay(OpenWeatherMap_Precipitation, 'Precipitation');
        layerControl.addOverlay(OpenWeatherMap_Pressure, 'Pressure');
        layerControl.addOverlay(OpenWeatherMap_PressureContour, 'Pressure Contours');
        layerControl.addOverlay(OpenWeatherMap_Wind, 'Wind');
        layerControl.addOverlay(OpenWeatherMap_Temperature, 'Temperature');
    }
}, {
    position: 'topright'
}).addTo(map);

