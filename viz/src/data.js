// Data loading
var loadingControl = null;

var loadGeoJSON = function (path) {
    return new Promise(function (resolve, reject) {
        if (loadingControl) {
            loadingControl.addLoader(path);
        }
        var xhr = new XMLHttpRequest();
        xhr.open('GET', path, true);
        xhr.onload = function () {
            if (xhr.readyState == 4) {
                var data = JSON.parse(xhr.responseText);
                resolve(data);
                if (loadingControl) {
                    loadingControl.removeLoader(path);
                }
            }
        };
        xhr.onerror = function () {
            if (loadingControl) {
                loadingControl.removeLoader(path);
            }
            reject(this.statusText);
        };
        xhr.send();
    });
};

var loadPath = function (id) {
    return loadGeoJSON(DATA_DIR + id + '_damage.geojson');
};

var loadPaths = function () {
    return loadGeoJSON(DATA_DIR + 'baseline.geojson');
};

var loadBridges = function () {
    return loadGeoJSON(DATA_DIR + 'bridges.geojson');
};

var loadTargets = function () {
    return loadGeoJSON(DATA_DIR + 'damage.geojson');
};