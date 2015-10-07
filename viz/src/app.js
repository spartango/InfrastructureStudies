/**
 * Created by spartango on 10/2/15.
 */

var map = L.map('map').setView([51.505, -0.09], 13);
L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
    maxZoom: 18,
    id: 'spartango.njkjli28',
    accessToken: 'pk.eyJ1Ijoic3BhcnRhbmdvIiwiYSI6IkFvOEpBcWcifQ.YJf-kBxkS9GYW2SFQ3Bpcg'
}).addTo(map);

var layer;

function showRoutes(id) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', id + '_path.geojson', true);
    xhr.onload = function () {
        if (xhr.readyState == 4) {
            console.log("Loaded data");
            var data = JSON.parse(xhr.responseText);
            layer = L.geoJson(data);
            later.addTo(map);
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
            popupString += "</tr><tr><td><button onclick='showRoutes(feature.properties.id)'>Routes</button></td>";
        }
        popupString += "</tr></table>";
        layer.bindPopup(popupString);
    }
}

var sXhr = new XMLHttpRequest();
sXhr.open('GET', 's_station_graph.geojson', true);
sXhr.onload = function () {
    if (sXhr.readyState == 4) {
        var data = JSON.parse(sXhr.responseText);
        var markers = new L.MarkerClusterGroup();
        var geoJsonLayer = L.geoJson(data, {
            onEachFeature: stationPopup
        });
        markers.addLayer(geoJsonLayer);
        map.addLayer(markers);
        map.fitBounds(markers.getBounds());
    }
};
sXhr.send();