mapboxgl.accessToken = 'pk.eyJ1Ijoic3BhcnRhbmdvIiwiYSI6IkFvOEpBcWcifQ.YJf-kBxkS9GYW2SFQ3Bpcg';

var map = new mapboxgl.Map({
    container: 'map',
    style: 'mapbox://styles/mapbox/streets-v8',
    center: [103.486052, 35.00],
    zoom: 3
});

var loadStations = function(){
    var sXhr = new XMLHttpRequest();
    sXhr.open('GET', 'stations.geojson', true);
    sXhr.onload = function () {
        if (sXhr.readyState == 4) {
            var data = JSON.parse(sXhr.responseText);

            map.addSource("stations", {
                "type": "geojson",
                "data": data
            });

            map.addLayer({
                "id": "targets",
                "type": "circle",
                "source": "stations"
            });

        }
    };
    sXhr.send();
};

map.on('style.load', function () {
    // Load data
    loadStations();
});

map.on('click', function (e) {
    map.featuresAt(e.point, {layer: 'targets', radius: 10, includeGeometry: true}, function (err, features) {
        console.log(features);
        if (err || !features.length)
            return;

        var feature = features[0];

        var popupString = "<table><tr>";
        for (var key in feature.properties) {
            popupString += "<td>" + feature.properties[key] + "</td>";
        }

        if (feature.properties.id || feature.geometry) {
            popupString += "</tr><tr>"
        }

        popupString += "</tr></table>";
        new mapboxgl.Popup()
            .setLngLat(feature.geometry.coordinates)
            .setHTML(popupString)
            .addTo(map);
    });
});

map.on('mousemove', function (e) {
    map.featuresAt(e.point, {layer: 'targets', radius: 10}, function (err, features) {
        map.getCanvas().style.cursor = (!err && features.length) ? 'pointer' : '';
    });
});