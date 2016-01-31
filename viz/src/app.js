/**
 * Created by spartango on 10/2/15.
 */

// Setup Map

var layer;
var hash;

var map = L.map('map', {
    fullscreenControl: true
}).setView([51.505, -0.09], 13);

var mapboxLayer = L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
    maxZoom: 18,
    id: 'spartango.njkjli28',
    accessToken: 'pk.eyJ1Ijoic3BhcnRhbmdvIiwiYSI6IkFvOEpBcWcifQ.YJf-kBxkS9GYW2SFQ3Bpcg'
}).addTo(map);

var Esri_WorldStreetMap = L.tileLayer('http://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}', {
    attribution: 'Tiles &copy; Esri &mdash; Source: Esri, DeLorme, NAVTEQ, USGS, Intermap, iPC, NRCAN, Esri Japan, METI, Esri China (Hong Kong), Esri (Thailand), TomTom, 2012'
});

var Esri_WorldImagery = L.tileLayer('http://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
    attribution: 'Tiles &copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community'
});

var Esri_WorldPhysical = L.tileLayer('http://server.arcgisonline.com/ArcGIS/rest/services/World_Physical_Map/MapServer/tile/{z}/{y}/{x}', {
    attribution: 'Tiles &copy; Esri &mdash; Source: US National Park Service',
    maxZoom: 8
});

var Esri_WorldTopoMap = L.tileLayer('http://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}', {
    attribution: 'Tiles &copy; Esri &mdash; Esri, DeLorme, NAVTEQ, TomTom, Intermap, iPC, USGS, FAO, NPS, NRCAN, GeoBase, Kadaster NL, Ordnance Survey, Esri Japan, METI, Esri China (Hong Kong), and the GIS User Community'
});

var OpenStreetMap_Mapnik = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
});

var Thunderforest_Transport = L.tileLayer('http://{s}.tile.thunderforest.com/transport/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="http://www.opencyclemap.org">OpenCycleMap</a>, &copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    maxZoom: 19
});

var OpenTopoMap = L.tileLayer('http://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', {
    maxZoom: 16,
    attribution: 'Map data: &copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>, <a href="http://viewfinderpanoramas.org">SRTM</a> | Map style: &copy; <a href="https://opentopomap.org">OpenTopoMap</a> (<a href="https://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA</a>)'
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
    "MapBox": mapboxLayer,
    "OpenStreetMap": OpenStreetMap_Mapnik,
    "TF Transport": Thunderforest_Transport,
    "Esri Street Map": Esri_WorldStreetMap,
    "Esri Imagery": Esri_WorldImagery,
    "Esri Physical": Esri_WorldPhysical,
    "Esri Topo": Esri_WorldTopoMap,
};

var overlayMaps = {
    "Labels": MapQuestOpen_HybridOverlay,
    "Clouds": OpenWeatherMap_Clouds,
    "Precipitation": OpenWeatherMap_Precipitation,
    "Pressure": OpenWeatherMap_Pressure,
    "Pressure Contours": OpenWeatherMap_PressureContour,
    "Wind": OpenWeatherMap_Wind,
    "Temperature": OpenWeatherMap_Temperature
};

L.control.layers(baseMaps, overlayMaps).addTo(map);
L.control.scale().addTo(map);

var backgroundLayers = {};

var loadGeoJSON = function (path, callback) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', path, true);
    xhr.onload = function () {
        if (xhr.readyState == 4) {
            var data = JSON.parse(xhr.responseText);
            callback(data);
        }
    };
    xhr.send();
};

var showRoutes = function (id) {
    loadGeoJSON('2020/20_' + id + '_path.geojson',
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
                    return L.MakiMarkers.icon({icon: "ferry", color: "#00b", size: "l"});
                }
            });
            var icon = L.MakiMarkers.icon({icon: "ferry", color: "#00b", size: "m"});
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
                    return L.MakiMarkers.icon({icon: "danger", color: "#ab0", size: "m"});
                }
            });
            var icon = L.MakiMarkers.icon({icon: "danger", color: "#ab0", size: "s"});
            var heatPoints = [];
            var geoJsonLayer = L.geoJson(data, {
                onEachFeature: infraPopup,
                pointToLayer: function (feature, latlng) {
                    heatPoints.push({lat: latlng.lat, lng: latlng.lng, count: 1});
                    return L.marker(latlng, {icon: icon});
                }
            });
            var heatmapLayer = new HeatmapOverlay({
                // radius should be small ONLY if scaleRadius is true (or small radius is intended)
                // if scaleRadius is false it will be the constant radius used in pixels
                "radius": 1,
                "maxOpacity": .33,
                // scales the radius based on map zoom
                "scaleRadius": true,
                // if set to false the heatmap uses the global maximum for colorization
                // if activated: uses the data maximum within the current map boundaries
                //   (there will always be a red spot with useLocalExtremas true)
                "useLocalExtrema": false,
                // which field name in your data represents the latitude - default "lat"
                latField: 'lat',
                // which field name in your data represents the longitude - default "lng"
                lngField: 'lng',
                // which field name in your data represents the data value - default "value"
                valueField: 'count'
            });
            map.addLayer(heatmapLayer);
            heatmapLayer.setData({
                max: 3,
                data: heatPoints
            });
            markers.addLayer(geoJsonLayer);
            map.addLayer(markers);
            backgroundLayers['sams'] = markers;
            backgroundLayers['heat'] = heatmapLayer;
        });
    } else {
        map.removeLayer(backgroundLayers['sams']);
        map.removeLayer(backgroundLayers['heat']);
        delete backgroundLayers['sams'];
        delete backgroundLayers['heat'];
    }
};

var loadAviation = function () {
    if (!backgroundLayers['aviation']) {
        loadGeoJSON('background/ChineseMilitaryAviation.geojson', function (data) {
            var markers = new L.MarkerClusterGroup({
                iconCreateFunction: function (cluster) {
                    return L.MakiMarkers.icon({icon: "airport", color: "#d90", size: "l"});
                }
            });
            var icon = L.MakiMarkers.icon({icon: "airport", color: "#d90", size: "m"});
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
                    return L.MakiMarkers.icon({icon: "rocket", color: "#e70", size: "m"});
                }
            });
            var icon = L.MakiMarkers.icon({icon: "rocket", color: "#e70", size: "s"});
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
    loadGeoJSON('background/stations.geojson', function (data) {
        var markers = new L.MarkerClusterGroup();
        var icon = L.MakiMarkers.icon({icon: "rail", color: "#00b", size: "m"});
        var geoJsonLayer = L.geoJson(data, {
            onEachFeature: stationPopup,
            pointToLayer: function (feature, latlng) {
                return L.marker(latlng, {icon: icon});
            }
        });
        markers.addLayer(geoJsonLayer);
        map.addLayer(markers);
        map.fitBounds(markers.getBounds());
        hash = new L.Hash(map);
    })
};

var loadPath = function (id) {
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
    loadGeoJSON('2020/20_' + id + '_path.geojson',
        function (data) {
            var path = L.geoJson(data, {
                onEachFeature: pathPopup
            });
            path.addTo(map);
        });
};

var loadSources = function () {
    loadGeoJSON('2020/sources.geojson', function (data) {
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

        // Load up the routes
        data.features.forEach(function (feature) {
            loadPath(feature.properties.id);
        });
    })
};

var loadSinks = function () {
    loadGeoJSON('2020/sinks.geojson', function (data) {
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
        map.fitBounds(geoJsonLayer.getBounds());
        hash = new L.Hash(map);
    })
};

var loadRangeRings = function () {
    loadGeoJSON('background/RangeRings.geojson', function (data) {
        var markers = new L.MarkerClusterGroup();
        var geoJsonLayer = L.geoJson(data, {});
        markers.addLayer(geoJsonLayer);
        map.addLayer(markers);
    });
};

loadSources();
loadSinks();
L.easyButton('fa-warning', function (btn, map) {
    loadSAMs();
}).addTo(map);
L.easyButton('fa-ship', function (btn, map) {
    loadPorts();
}).addTo(map);
L.easyButton('fa-plane', function (btn, map) {
    loadAviation();
}).addTo(map);
L.easyButton('fa-rocket', function (btn, map) {
    loadSecondArtillery();
}).addTo(map);