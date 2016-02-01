/**
 * Created by spartango on 10/2/15.
 */

// Setup Map

var layer;
var hash;

var map = L.map('map', {
    fullscreenControl: true
}).setView([51.505, -0.09], 13);

var mapboxLayer = L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}@2x.png?access_token={accessToken}', {
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
    maxZoom: 18,
    id: 'mapbox.streets',
    accessToken: 'pk.eyJ1Ijoic3BhcnRhbmdvIiwiYSI6IkFvOEpBcWcifQ.YJf-kBxkS9GYW2SFQ3Bpcg'
}).addTo(map);

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
    "Light": CartoDB_Positron,
    "Dark": CartoDB_DarkMatter,
    "Standard": mapboxLayer,
    "Physical": Esri_WorldPhysical,
    "Topo": Esri_WorldTopoMap,
    "Imagery": Esri_WorldImagery,
    "Hybrid": hybridMapboxLayer,
    "OpenStreetMap": OpenStreetMap_Mapnik,
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
            // Iconography
            var symbol = new MS.symbol(
                "10042000001213090000", {
                    size: 15
                });
            var icon = L.icon({
                iconUrl: symbol.getMarker().asImage(),
                iconAnchor: [symbol.markerAnchor.x, symbol.markerAnchor.y],
            });

            var largeSymbol = new MS.symbol(
                "10042000001213090000", {
                    size: 20
                });
            var largeIcon = L.icon({
                iconUrl: largeSymbol.getMarker().asImage(),
                iconAnchor: [largeSymbol.markerAnchor.x, largeSymbol.markerAnchor.y],
            });

            var markers = new L.MarkerClusterGroup({
                iconCreateFunction: function (cluster) {
                    return largeIcon;
                    //return L.MakiMarkers.icon({icon: "ferry", color: "#00b", size: "m"});
                }
            });
            //var icon = L.MakiMarkers.icon({icon: "ferry", color: "#00b", size: "s"});
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
            // Iconography
            var symbol = new MS.symbol(
                "10061500001111000000", {
                    size: 15
                });
            var icon = L.icon({
                iconUrl: symbol.getMarker().asImage(),
                iconAnchor: [symbol.markerAnchor.x, symbol.markerAnchor.y],
            });

            var largeSymbol = new MS.symbol(
                "10061500001111000000", {
                    size: 20
                });
            var largeIcon = L.icon({
                iconUrl: largeSymbol.getMarker().asImage(),
                iconAnchor: [largeSymbol.markerAnchor.x, largeSymbol.markerAnchor.y],
            });

            var markers = new L.MarkerClusterGroup({
                iconCreateFunction: function (cluster) {
                    return largeIcon;
                    //return L.MakiMarkers.icon({icon: "triangle", color: "#fb0", size: "m"});
                }
            });

            //var icon = L.MakiMarkers.icon({icon: "triangle", color: "#fb0", size: "s"});
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
            // Iconography
            var symbol = new MS.symbol(
                "10062000001213010000", {
                    size: 15
                });
            var icon = L.icon({
                iconUrl: symbol.getMarker().asImage(),
                iconAnchor: [symbol.markerAnchor.x, symbol.markerAnchor.y],
            });

            var largeSymbol = new MS.symbol(
                "10062000001213010000", {
                    size: 20
                });
            var largeIcon = L.icon({
                iconUrl: largeSymbol.getMarker().asImage(),
                iconAnchor: [largeSymbol.markerAnchor.x, largeSymbol.markerAnchor.y],
            });

            var markers = new L.MarkerClusterGroup({
                iconCreateFunction: function (cluster) {
                    return largeIcon;
                    //return L.MakiMarkers.icon({icon: "airport", color: "#f70", size: "m"});
                }
            });
            //var icon = L.MakiMarkers.icon({icon: "airport", color: "#f70", size: "s"});
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
            // Iconography
            var symbol = new MS.symbol(
                "10061500001113030000", {
                    size: 15
                });
            var icon = L.icon({
                iconUrl: symbol.getMarker().asImage(),
                iconAnchor: [symbol.markerAnchor.x, symbol.markerAnchor.y],
            });

            var largeSymbol = new MS.symbol(
                "10061500001113030000", {
                    size: 20
                });

            var largeIcon = L.icon({
                iconUrl: largeSymbol.getMarker().asImage(),
                iconAnchor: [largeSymbol.markerAnchor.x, largeSymbol.markerAnchor.y],
            });

            var markers = new L.MarkerClusterGroup({
                iconCreateFunction: function (cluster) {
                    return largeIcon;
                    //return L.MakiMarkers.icon({icon: "rocket", color: "#f30", size: "m"});
                }
            });
            //var icon = L.MakiMarkers.icon({icon: "rocket", color: "#f30", size: "s"});
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
    })
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
    loadGeoJSON('2020/20_' + id + '_path.geojson',
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
    loadGeoJSON('2020/sources.geojson', function (data) {
        //var markers = new L.MarkerClusterGroup();
        //var icon = L.MakiMarkers.icon({icon: "rail", color: "#0b0", size: "m"});
        var symbol = new MS.symbol(
            "10062000001213070000", {
                size: 20
            });
        var icon = L.icon({
            iconUrl: symbol.getMarker().asImage(),
            iconAnchor: [symbol.markerAnchor.x, symbol.markerAnchor.y],
        });
        var geoJsonLayer = L.geoJson(data, {
            onEachFeature: stationPopup,
            pointToLayer: function (feature, latlng) {
                return L.marker(latlng, {icon: icon});
            }
        });
        //markers.addLayer(geoJsonLayer);
        map.addLayer(geoJsonLayer);
        map.fitBounds(geoJsonLayer.getBounds());

        // Load up the routes
        data.features.forEach(function (feature) {
            loadPath(feature.properties.id);
        });

        hash = new L.Hash(map);
    })
};

var loadSinks = function () {
    loadGeoJSON('2020/sinks.geojson', function (data) {
        //var markers = new L.MarkerClusterGroup();
        // Iconography
        var symbol = new MS.symbol(
            "10062000001213070000", {
                size: 20
            });
        var icon = L.icon({
            iconUrl: symbol.getMarker().asImage(),
            iconAnchor: [symbol.markerAnchor.x, symbol.markerAnchor.y],
        });
        //var icon = L.MakiMarkers.icon({icon: "rail", color: "#b00", size: "m"});
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

var loadSegments = function () {
    if (!backgroundLayers['segments']) {
        loadGeoJSON('2020/segments.geojson',
            function (data) {
                //var combined = turf.combine(data);
                var path = L.geoJson(data, {
                    onEachFeature: pathPopup,
                    style: function (feature) {
                        var scaled = (feature.properties.criticality - 10) / 10;
                        var green = Math.round(255 * (1 - scaled));
                        return {
                            "color": "rgb(255, " + green + ", 0)",
                            "weight": 6,
                            "opacity": 0.8
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

loadSources();
loadSinks();

L.easyButton('fa-bullseye', function (btn, map) {
    loadSegments();
}).addTo(map);
L.easyButton('fa-warning', function (btn, map) {
    loadSAMs();
    loadRangeRings();
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
