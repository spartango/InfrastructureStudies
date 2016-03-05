var urlHash = window.location.hash;

var interactiveMode = urlHash == "#interactive";

// Generic post
var postData = function (path, data) {
    return new Promise(function (resolve, reject) {
        loadingControl.addLoader(path);
        var xhr = new XMLHttpRequest();
        xhr.open('POST', path, true);
        xhr.setRequestHeader('Content-Type', 'application/json');
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
        xhr.send(JSON.stringify(data));
    });
};

// Get nearest station for points
var nearestStations = function (locations) {
    return loadStations().then(function (stations) {
        locations.features.forEach(function (location) {
            var nearest = turf.nearest(location, stations);
            location.properties["nearestStation"] = nearest;
        });
        return locations;
    });
};

// Strip everything away except the station IDs for sending
var buildIdRequest = function (locations) {
    return nearestStations(locations).then(function (locations) {
        return locations.features.map(function (location) {
            return location.properties.nearestStation.properties.id;
        });
    });
};

// Seed a simulation. The server should give us back an identifier to track
var requestSimulation = function (ids) {
    return postData('/simulate', {sinks: ids});
};

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

var refreshResults = function (interval) {
    var time = interval ? interval : 60000;
    setTimeout(function () {
        console.log("Refreshing, next in " + interval);
        showDefaultLayers();
        refreshResults(time);
    }, time);
};

var simulate = function () {
    var geojson = drawnItems.toGeoJSON();
    console.log(geojson);
    return buildIdRequest(geojson).then(requestSimulation).then(function (response) {
        var simulationId = response.id;
        DATA_DIR = "elevation/" + simulationId + "/";

        // Try to load up the default layers. We may need to keep trying
        refreshResults(5000);
    });
};

L.easyButton('fa-pencil', function (btn) {
    btn.removeFrom(map);
    enableDrawing();

    L.easyButton('fa-play', function (btn) {
        btn.removeFrom(map);
        simulate();
    }, {
        position: 'topleft'
    }).addTo(map);
}, {
    position: 'topleft'
}).addTo(map);
