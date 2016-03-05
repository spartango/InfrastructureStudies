var urlHash = window.location.hash;
var debug = urlHash == "#debug";

var refreshTargets = function (interval) {
    var time = interval ? interval : 60000;
    setTimeout(function () {
        hideMapLayer('targets')   // Unload the layer
            .then(toggleTargets); // Reload the layer

        // Schedule another run in 60s
        refreshTargets(time);
    }, time);
};

if (debug) {
    refreshTargets();
}