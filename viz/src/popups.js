var targetPopup = function (feature, layer) {
    if (feature.properties) {
        var popupString = '<div class="row"><table class="table table-condensed"><thead><tr><th><i class="fa fa-crosshairs"></i> Critical Bridge</th></tr></thead>';
        var center = feature.properties['center'];
        for (var key in feature.properties) {
            var prettyKey;
            var rowClass = "";
            var prettyValue = feature.properties[key];
            if (key == 'criticality') {
                prettyKey = 'Rerouting Cost';
                var hours = costToHours(prettyValue);
                prettyValue = formatTime(hours);
            } else if (key == 'elevations') {
                prettyKey = 'Elevation';
                var array = JSON.parse(prettyValue);
                // Compute the average elevation
                prettyValue = d3_array.mean(array);
                prettyValue += " m";
            } else if (key == 'nearestStation') {
                var station = feature.properties['nearestStation'];
                var extent = turf.extent(turf.featurecollection([station, center]));
                var bounds = [[extent[1], extent[0]], [extent[3], extent[2]]];
                prettyKey = `<a href="#" onclick="showAndFocus(showStations(),` + JSON.stringify(bounds) + `)"> Nearest Station</a>`;
                var distance = turf.distance(station, center);
                prettyValue = formatDistance(distance);
            } else if (key == 'nearestSAM') {
                var sam = feature.properties['nearestSAM'];
                var extent = turf.extent(turf.featurecollection([sam, center]));
                var bounds = [[extent[1], extent[0]], [extent[3], extent[2]]];
                prettyKey = `<a href="#" onclick="showAndFocus(showSAMThreats(),` + JSON.stringify(bounds) + `)">`;
                if (feature.properties.activeSAM) {
                    prettyKey += `SAM Threat</a>`;
                    rowClass = "danger"
                } else {
                    prettyKey += `Nearest Radar</a>`;
                }
                var distance = turf.distance(sam, center);
                prettyValue = formatDistance(distance);
            } else if (key == 'nearestAirbase') {
                var airbase = feature.properties['nearestAirbase'];
                var extent = turf.extent(turf.featurecollection([airbase, center]));
                var bounds = [[extent[1], extent[0]], [extent[3], extent[2]]];
                prettyKey = `<a href="#" onclick="showAndFocus(showAviation(),` + JSON.stringify(bounds) + `)"> Nearest Airbase</a>`;
                rowClass = "warning";
                var distance = turf.distance(airbase, center);
                prettyValue = formatDistance(distance);
            } else if (key == 'center') {
                prettyKey = "MGRS";
                prettyValue = mgrs.forward(center.geometry.coordinates);
            } else if (key == 'activeSAM') {
                continue; // We'll handle activeSAM elsewhere
            } else if (key == 'color') {
                continue; // We'll handle color elsewhere
            } else {
                prettyKey = key.charAt(0).toUpperCase() + key.slice(1);
                // If we still haven't made a string out of this
                if (Number.isFinite(prettyValue)) {
                    prettyValue = formatDistance(prettyValue);
                } else if (typeof prettyValue == 'boolean') {
                    prettyValue = prettyValue ? "Yes" : "No";
                }
            }

            popupString += `<tr class="` + rowClass + `"><td><strong>` + prettyKey + "</strong></td><td>" + prettyValue + "</td></tr>";
        }
        popupString += "</table></div>";

        if (feature.properties.center) {
            popupString += `<div class="row"><button class="btn btn-default btn-xs col-xs-6" onclick='map.setView({lat:`
                + feature.properties.center.geometry.coordinates[1]
                + ", lng:"
                + feature.properties.center.geometry.coordinates[0]
                + `}, 16)' ><i class="fa fa-search-plus"></i> Zoom</button>`;
            popupString += `<button class="btn btn-danger btn-xs col-xs-6" onclick='showReroute(` + feature.id + `)'><i class="fa fa-fire"></i> Reroute</button>`;
        }
        popupString += `</div></div>`;

        layer.bindPopup(popupString);
    }
};


var infrastructurePopup = function (feature, layer, config) {
    if (feature.properties) {
        var popupString = `<div><div class="row" style='max-height:250px; max-width: 200px;overflow:auto;'><table class="table table-condensed">`;
        popupString += `<thead><tr><th><i class="fa fa-` + config.icon + `"></i>` + ` ` + config.displayType + `</th></tr></thead>`;

        for (var key in feature.properties) {
            var niceKey = key.charAt(0).toUpperCase() + key.slice(1);
            var niceValue = feature.properties[key];
            if (key == 'elevation') {
                niceValue += " m";
            } else if (key == 'color') {
                continue;
            }
            popupString += "<tr><td><strong>" + niceKey + "</strong></td><td>" + niceValue + "</td></tr>";
        }

        if (feature.geometry) {
            popupString += "<tr><td><strong>MGRS</strong></td><td>"
                + mgrs.forward(feature.geometry.coordinates)
                + "</td></tr>";
        }

        popupString += "</table></div>";

        if (feature.geometry) {
            popupString += `<div class="row"><button class="btn btn-default btn-xs col-xs-12" onclick='map.setView({lat:`
                + feature.geometry.coordinates[1]
                + ", lng:"
                + feature.geometry.coordinates[0]
                + `}, 16)' ><i class="fa fa-search-plus"></i> Zoom</button></div></div>`
        }
        layer.bindPopup(popupString);
    }
};

/**
 * @deprecated
 * @param feature
 * @param layer
 */
var infraPopup = function (feature, layer) {
    if (feature.properties) {
        var popupString = `<div><div class="row" style='max-height:250px; max-width: 200px;overflow:auto;'><table class="table table-condensed">`;
        if (feature.properties.type) {
            var type = feature.properties.type;
            var niceType = type.charAt(0).toUpperCase() + type.slice(1);
            var iconString = typeIcons[type] ? `<i class="fa fa-` + typeIcons[type] + `"></i>` : "";
            popupString += `<thead><tr><th>` + iconString + ` ` + niceType + `</th></tr></thead>`;
        }

        for (key in feature.properties) {
            var niceKey = key.charAt(0).toUpperCase() + key.slice(1);
            var niceValue = feature.properties[key];
            if (key == 'elevation') {
                niceValue += " m";
            } else if (key == 'type') {
                continue;
            }
            popupString += "<tr><td><strong>" + niceKey + "</strong></td><td>" + niceValue + "</td></tr>";
        }

        if (feature.geometry) {
            popupString += "<tr><td><strong>MGRS</strong></td><td>"
                + mgrs.forward(feature.geometry.coordinates)
                + "</td></tr>";
        }

        popupString += "</table></div>";

        if (feature.geometry) {
            popupString += `<div class="row"><button class="btn btn-default btn-xs col-xs-12" onclick='map.setView({lat:`
                + feature.geometry.coordinates[1]
                + ", lng:"
                + feature.geometry.coordinates[0]
                + `}, 16)' ><i class="fa fa-search-plus"></i> Zoom</button></div></div>`
        }
        layer.bindPopup(popupString);
    }
};


var bridgePopup = function (feature, layer) {
    if (feature.properties) {
        var popupString = '<div><div class="row"><table class="table table-condensed"><thead><tr><th><i class="fa fa-road"></i> Bridge</th></tr></thead>';
        for (var key in feature.properties) {
            var prettyKey;
            var rowClass = "";
            var prettyValue = feature.properties[key];
            if (key == 'criticality') {
                prettyKey = 'Bottleneck';
                prettyValue = prettyValue + " sources";
            } else if (key == 'elevations') {
                prettyKey = 'Elevation';
                var array = JSON.parse(prettyValue);
                prettyValue = d3_array.mean(array);
                prettyValue += " m";
            } else {
                prettyKey = key.charAt(0).toUpperCase() + key.slice(1);
                // If we still haven't made a string out of this
                if (Number.isFinite(prettyValue)) {
                    prettyValue = formatDistance(prettyValue);
                } else if (typeof prettyValue == 'boolean') {
                    prettyValue = prettyValue ? "Yes" : "No";
                }
            }

            popupString += `<tr class="` + rowClass + `"><td><strong>` + prettyKey + "</strong></td><td>" + prettyValue + "</td></tr>";
        }
        popupString += "</table></div>";

        feature.properties.center = turf.center(feature);
        if (feature.properties.center) {
            popupString += `<div class="row"><button class="btn btn-default btn-xs col-xs-12" onclick='map.setView({lat:`
                + feature.properties.center.geometry.coordinates[1]
                + ", lng:"
                + feature.properties.center.geometry.coordinates[0]
                + `}, 16)' ><i class="fa fa-search-plus"></i> Zoom</button></div></div>`
        }

        layer.bindPopup(popupString);
    }
};

var reroutePopup = function (feature, layer) {
    var popupString = `<div>`
        + `<div class="row"><table class="table table-condensed"><thead><tr><th><i class="fa fa-exchange"></i> Adjusted Route</th></tr></thead>`;

    for (var key in feature.properties) {
        var prettyKey;
        var rowClass = "";
        var prettyValue = feature.properties[key];
        if (key == 'cost') {
            prettyKey = 'Travel Time';
            prettyValue = formatTime(costToHours(prettyValue));
        } else {
            prettyKey = key.charAt(0).toUpperCase() + key.slice(1);
            // If we still haven't made a string out of this
            if (Number.isFinite(prettyValue)) {
                prettyValue = formatDistance(prettyValue);
            } else if (typeof prettyValue == 'boolean') {
                prettyValue = prettyValue ? "Yes" : "No";
            }
        }

        popupString += `<tr class="` + rowClass + `"><td><strong>` + prettyKey + "</strong></td><td>" + prettyValue + "</td></tr>";
    }
    popupString += "</table></div>";

    popupString += `<div class="row"><button class="btn btn-default btn-xs col-xs-12" onclick='hideReroute()'>`
        + `<i class="fa fa-close"></i> Hide</button></div></div>`;
    layer.bindPopup(popupString);
};