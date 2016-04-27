var urlHash = window.location.hash;

var DATA_DIR = "testing/";
var standardMode = !urlHash || urlHash == "#" || urlHash == "";

// Setup Map
var layer;
var hash;
var drawnItems;

var map = L.map('map', {}).setView([31.531634, 106.054523], 5);

var defaultMap = tileLayers["Physical"];
defaultMap.addTo(map);

backgroundMarkers.addTo(map);

// Default controls
loadingControl = L.Control.loading({
    separate: true,
    position: 'topright'
});
map.addControl(loadingControl);

L.control.scale().addTo(map);
var layerControl = L.control.layers(tileLayers, overlayLayers, {position: 'bottomleft'});
layerControl.addTo(map);

var showBackgroundLayers = function () {
    return Promise.all(defaultLayers.map(showLayer));
};

var showDefaultLayers = function () {
    return Promise.all([
        showBackgroundLayers()
        //showSinks(),
        //showSources(),
        //showPaths().then(showBaselineAnimation).then(showTargets)
    ]);
};

if (standardMode) {
    showDefaultLayers();
}
