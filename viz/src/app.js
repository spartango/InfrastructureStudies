var urlHash = window.location.hash;

var DATA_DIR = "elevation/";
var standardMode = !urlHash || urlHash == "#" || urlHash == "";
var milMode = urlHash == "#mil";
var civMode = urlHash == "#civ";

// Setup Map
var layer;
var hash;
var drawnItems;

var map = L.map('map', {
    zoomControl: false
}).setView([31.531634, 106.054523], 5);

var baseMaps = {
    "Streets": CartoDB_Positron,
    "Dark": CartoDB_DarkMatter,
    "Physical": mapboxLayer,
    "Topo": topoMapboxLayer,
    "Satellite": hybridMapboxLayer,
    "Latest Imagery": satelliteDigitalGlobeLayer
};
var overlayMaps = {};

var defaultMap = baseMaps["Physical"];
defaultMap.addTo(map);

backgroundMarkers.addTo(map);

// Default controls
loadingControl = L.Control.loading({
    separate: true,
    position: 'topright'
});
map.addControl(loadingControl);

L.control.scale().addTo(map);
var layerControl = L.control.layers(baseMaps, overlayMaps, {position: 'bottomleft'});
layerControl.addTo(map);

var showDefaultLayers = function () {
    return Promise.all([
        showSinks(),
        showSources(),
        showPaths().then(showBaselineAnimation).then(showTargets)
    ]);
};

if (standardMode) {
    showDefaultLayers();
} else if (milMode) {
    showSAMThreats();
    showAviation();
    toggleSecondArtillery();
} else if (civMode) {
    showStations();
    togglePorts();
}
