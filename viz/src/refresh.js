var urlHash = window.location.hash;
var debug = urlHash == "#debug";

var refreshTargets = function (interval) {
    var time = interval ? interval : 60000;
    setTimeout(function () {
        hideMapLayer('targets')
            .then(hideLegend)   // Unload the layer
            .then(showTargets); // Reload the layer

        // Schedule another run in 60s
        refreshTargets();
    }, time);
};

if (debug) {
    showDefaultLayers().then(refreshTargets);
}