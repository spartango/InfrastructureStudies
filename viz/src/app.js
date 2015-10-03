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

// Load up the geojson
var xhr = new XMLHttpRequest();
xhr.open('GET', 'link_graph.geojson', true);
xhr.onload = function (request) {
    if (xhr.readyState == 4) {
        console.log("Loaded data");
        var data = JSON.parse(xhr.responseText);
        L.geoJson(data).addTo(map);
    }
};
xhr.send();

var sXhr = new XMLHttpRequest();
sXhr.open('GET', 'all_station_graph.geojson', true);
sXhr.onload = function(request) {
    if (sXhr.readyState == 4) {
        var data = JSON.parse(sXhr.responseText);
        var markers = new L.MarkerClusterGroup();
        var geoJsonLayer = L.geoJson(data);
        markers.addLayer(geoJsonLayer);
        map.addLayer(markers);
        map.fitBounds(markers.getBounds());
    }
};
sXhr.send();