
var bridgeButton = L.easyButton('fa-' + typeIcons['bridge'], function (btn, map) {
    if (!allBridges && backgroundLayers['segments']) {
        // unload the layer to be reloaded
        toggleSegments();
    }

    allBridges = true;
    // Load or unload the layer
    toggleSegments();
});

// Advanced controls
var buildButton = function (type, toggle) {
    return L.easyButton('fa-' + typeIcons[type], toggle);
};

var portButton = buildButton('port', togglePorts);
var aviationButton = buildButton('airbase', toggleAviation);
var nuclearButton = buildButton('nuclear', toggleSecondArtillery);
var stationButton = buildButton('station', toggleStations);
var SAMButton = buildButton('SAM', toggleSAMs);
var flowButton = buildButton('flow', toggleFlows);
var rangeRingButton = buildButton('rangering', toggleRangeRings);
var damageButton = buildButton('target', toggleTargets);

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