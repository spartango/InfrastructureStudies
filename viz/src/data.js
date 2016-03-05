var DATA_DIR = "elevation/";

// Data loading
var loadGeoJSON = function (path) {
    return new Promise(function (resolve, reject) {
        loadingControl.addLoader(path);
        var xhr = new XMLHttpRequest();
        xhr.open('GET', path, true);
        xhr.onload = function () {
            if (xhr.readyState == 4) {
                try {
                    var data = JSON.parse(xhr.responseText);
                    resolve(data);
                } catch (e) {
                    reject(this.statusText);
                }
                loadingControl.removeLoader(path);
            }
        };
        xhr.onerror = function () {
            reject(this.statusText);
            loadingControl.removeLoader(path);
        };
        xhr.send();
    });
};

var loadPorts = function () {
    return loadGeoJSON('background/WPI.geojson');
};

var loadSAMs = function () {
    return loadGeoJSON('background/SAMs.geojson');
};

var loadAviation = function () {
    return loadGeoJSON('background/ChineseMilitaryAviation.geojson');
};

var loadSecondArtillery = function () {
    return loadGeoJSON('background/2AOperationalSites.geojson');
};

var loadStations = function () {
    return loadGeoJSON('background/stations.geojson');
};

var loadPath = function (id) {
    return loadGeoJSON(DATA_DIR + id + '_path.geojson')
        .then(function (data) {
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
    return loadGeoJSON(DATA_DIR + 'baseline.geojson');
};

var loadSources = function () {
    return loadGeoJSON(DATA_DIR + 'sources.geojson');
};

var loadSinks = function () {
    return loadGeoJSON(DATA_DIR + 'sinks.geojson');
};

var loadRangeRings = function () {
    return loadGeoJSON('background/RangeRingsP.geojson');
};

var loadMergedRangeRings = function () {
    return loadGeoJSON('background/RangeRingsP.geojson').then(turf.merge)
};

var loadBridges = function () {
    return loadGeoJSON(DATA_DIR + 'bridges.geojson');
};

var loadTargets = function () {
    return loadGeoJSON(DATA_DIR + 'damage.geojson');
};