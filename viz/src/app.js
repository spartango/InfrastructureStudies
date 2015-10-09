/**
 * Created by spartango on 10/2/15.
 */

 // Setup Map

var layer;
var hash;

var map = L.map('map',  {
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
    "MapBox" : mapboxLayer,
    "OpenStreetMap" : OpenStreetMap_Mapnik,
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

function showRoutes(id) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', '20_run/' + id + '_path.geojson', true);
    xhr.onload = function () {
        if (xhr.readyState == 4) {
            console.log("Loaded data");
            var data = JSON.parse(xhr.responseText);
            if (layer && data) {
                map.removeLayer(layer);
            }

            layer = L.geoJson(data);
            layer.addTo(map);
        }
    };
    xhr.send();
}

function stationPopup(feature, layer) {
    // does this feature have a property named popupContent?
    if (feature.properties) {
        var popupString = "<table><tr>";
        for (key in feature.properties) {
            popupString += "<td>" + feature.properties[key] + "</td>";
        }
        if (feature.properties.id) {
            popupString += "</tr><tr><td><button onclick='showRoutes(" + feature.properties.id + ")'>Routes</button></td>";
        }
        popupString += "</tr></table>";
        layer.bindPopup(popupString);
    }
}

var sXhr = new XMLHttpRequest();
sXhr.open('GET', 'stations.geojson', true);
sXhr.onload = function () {
    if (sXhr.readyState == 4) {
        var data = JSON.parse(sXhr.responseText);
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
    }
};
sXhr.send();