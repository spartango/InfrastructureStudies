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
            } else if (key == 'nearestSAM') {
                var sam = feature.properties['nearestSAM'];
                var distance = turf.distance(sam, center);
                prettyKey = `<a href="#" onclick="showLayerAndArc(showLayer('SAMs'),`
                    + JSON.stringify(sam.geometry.coordinates) + `,`
                    + JSON.stringify(center.geometry.coordinates)
                    + `,` + distance + `)">`;
                if (feature.properties.activeSAM) {
                    prettyKey += `SAM Threat</a>`;
                    rowClass = "danger"
                } else {
                    prettyKey += `Nearest Radar</a>`;
                }
                prettyValue = formatDistance(distance);
            } else if (key == 'nearestUSBase') {
                var airbase = feature.properties['nearestUSBase'];
                var distance = turf.distance(airbase, center);
                prettyKey = `<a href="#" onclick="showLayerAndArc(showLayer('USBases'),`
                    + JSON.stringify(airbase.geometry.coordinates) + `,`
                    + JSON.stringify(center.geometry.coordinates)
                    + `,` + distance + `)"> Nearest U.S. Base</a>`;
                rowClass = "success";
                prettyValue = formatDistance(distance);
            } else if (key == 'nearestAirbase') {
                var airbase = feature.properties['nearestAirbase'];
                var distance = turf.distance(airbase, center);
                prettyKey = `<a href="#" onclick="showLayerAndArc(showLayer('airBases'),`
                    + JSON.stringify(airbase.geometry.coordinates) + `,`
                    + JSON.stringify(center.geometry.coordinates)
                    + `,` + distance + `)"> Nearest Airbase</a>`;
                rowClass = "warning";
                prettyValue = formatDistance(distance);
            } else if (key == 'center') {
                prettyKey = "MGRS";
                prettyValue = mgrs.forward(center.geometry.coordinates);
            } else if (key == 'activeSAM' || key == 'color' || key == 'type') {
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
        var popupString = `<div><div class="row" style='max-height:250px; max-width: 250px;overflow:auto;'><table class="table table-condensed">`;
        popupString += `<thead><tr><th>` + config.displayType + `</th></tr></thead>`;

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

var arcPopup = function (distance) {
    return `<div><div class="row"><table class="table table-condensed"><thead><tr><th><i class="fa fa-arrows-h"></i> Fight Path</th></tr></thead>`
        + `<tr><td><strong>Distance </strong></td><td>` + formatDistance(distance) + "</td></tr></table></div>"
        + `<div class="row"><button class="btn btn-default btn-xs col-xs-12" onclick='hideMapLayer("arc")'>`
        + `<i class="fa fa-close"></i> Hide</button></div></div>`;
};