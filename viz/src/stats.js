var DATA_DIR = "elevation/";

var histogramTargets = function (targets) {
    var criticalityData = targets.features.map(function (feature) {
        return feature.properties.criticality;
    });

    var minCriticality = d3.min(criticalityData);
    var midPoint = d3.mean(criticalityData);
    var absMax = d3.max(criticalityData);
    var maxCriticality = Math.min(absMax, (midPoint - minCriticality) + midPoint);

    var color = d3.scale.linear()
        .domain([minCriticality, midPoint, maxCriticality])
        .range(["#FFFF00", "#FF8800", "#FF0000"]);

    console.log("Rendering histogram");
    // For each feature, extract the cose
    var data = targets.features.map(function (feature) {
        return costToHours(feature.properties.criticality);
    });

    var maxHours = costToHours(maxCriticality);
    var histogram = d3.layout.histogram().range([minCriticality, maxHours])
    (data);

    var rest = data.filter(function (d) {
        return d > maxHours;
    }).length;

    histogram.push({y: rest, x: maxHours, dx: (costToHours(absMax) - maxHours)});

    var dataset = new Plottable.Dataset(histogram);

    var xScale = new Plottable.Scales.Category();
    var yScale = new Plottable.Scales.Linear();
    var xAxis = new Plottable.Axes.Category(xScale, "bottom");
    //var yAxis = new Plottable.Axes.Numeric(yScale, "left");
    var titleLabel = new Plottable.Components.TitleLabel("Cost Distribution")
        .yAlignment("center");
    var xLabel = new Plottable.Components.AxisLabel("Rerouting Cost (hr)")
        .yAlignment("center");

    var plot = new Plottable.Plots.Bar();

    plot.x(function (d) {
            return Math.round(d.x) + " - " + Math.round(d.x + d.dx);
        }, xScale)
        .y(function (d) {
            return d.y;
        }, yScale)
        .labelsEnabled(true)
        .attr("fill", function (d) {
            var x = d.x + (d.dx / 2);
            return color(x * 100000);
        })
        .animated(true)
        .addDataset(dataset);

    var chart = new Plottable.Components.Table([
        [titleLabel],
        [plot],
        [xAxis],
        [xLabel]
    ]);

    chart.renderTo("svg#cost");
};

var costHistogram = function () {
    // Histogram of rerouting costs
    return loadTargets().then(histogramTargets);
};

// Comparison of rerouting costs to SAM threat
var annotateThreats = function (targets) {
    return loadMergedRangeRings().then(function (mergedRings) {
        // Mark the targets which are in range
        console.log("Computing active SAM threats");
        targets.features.forEach(function (feature) {
            var center = feature.properties.center ? feature.properties.center : turf.center(feature);
            feature.properties.center = center;
            if (turf.inside(center, mergedRings)) {
                feature.properties["activeSAM"] = true;
            }
        });
        return targets;
    }).then(loadSAMs).then(function (sams) {
        // Annotate the targets with SAM threat range
        console.log("Computing SAM ranges");
        targets.features.forEach(function (feature) {
            var center = feature.properties.center ? feature.properties.center : turf.center(feature);
            feature.properties.center = center;
            var nearest = turf.nearest(center, sams);
            var distance = turf.distance(nearest, center);
            feature.properties["nearestSAMDistance"] = distance;
        });
        return targets;
    });
};

var annotateBases = function (targets) {
    return loadAviation().then(function (airbases) {
        console.log("Computing airbase ranges");
        targets.features.forEach(function (feature) {
            var center = feature.properties.center ? feature.properties.center : turf.center(feature);
            var nearest = turf.nearest(center, airbases);
            var distance = turf.distance(nearest, center);
            feature.properties["nearestAirbaseDistance"] = distance;
        });
        return targets;
    });
};

var annotateStations = function (targets) {
    return loadStations().then(function (stations) {
        console.log("Computing station ranges");
        targets.features.forEach(function (feature) {
            var center = feature.properties.center ? feature.properties.center : turf.center(feature);
            var nearest = turf.nearest(center, stations);
            var distance = turf.distance(nearest, center);
            feature.properties["nearestStationDistance"] = distance;
        });
        return targets;
    });
};

var plotProximity = function (targets) {
    var criticalityData = targets.features.map(function (feature) {
        return feature.properties.criticality;
    });

    var minCriticality = d3.min(criticalityData);
    var midPoint = d3.mean(criticalityData);
    var absMax = d3.max(criticalityData);
    var maxCriticality = Math.min(absMax, (midPoint - minCriticality) + midPoint);

    console.log("Rendering plot");
    // For each feature, extract the properties
    var data = targets.features.map(function (feature) {
        return feature.properties;
    });

    var dataset = new Plottable.Dataset(data);

    var xScale = new Plottable.Scales.Linear().domain([minCriticality, maxCriticality].map(costToHours));
    var xAxis = new Plottable.Axes.Numeric(xScale, "bottom");
    var xLabel = new Plottable.Components.AxisLabel("Rerouting Cost (hr)")
        .yAlignment("center");

    // Plot SAMs
    var samYScale = new Plottable.Scales.Linear()//.domain([0, 100]);
    var samYAxis = new Plottable.Axes.Numeric(samYScale, "left");
    var samPlot = new Plottable.Plots.Scatter();
    var samTitleLabel = new Plottable.Components.TitleLabel("SAM Proximity")
        .yAlignment("center");
    var samYLabel = new Plottable.Components.AxisLabel("Nearest SAM/Radar (km)")
        .angle(-90);
    samPlot.x(function (d) {
            return costToHours(d.criticality);
        }, xScale)
        .y(function (d) {
            return d.nearestSAMDistance;
        }, samYScale)
        .attr("fill", function (d) {
            return d.activeSAM ? "#b00" : typeColors.radar; //  "#b00" : "#5279c7"
        })
        .animated(true)
        .addDataset(dataset);

    // Plot Bases
    var airbaseYScale = new Plottable.Scales.Linear()//.domain([0, 100]);
    var airbaseYAxis = new Plottable.Axes.Numeric(airbaseYScale, "left");
    var airbasePlot = new Plottable.Plots.Scatter();
    var airbaseTitleLabel = new Plottable.Components.TitleLabel("Airbase Proximity")
        .yAlignment("center");
    var airbaseYLabel = new Plottable.Components.AxisLabel("Nearest Airbase (km)")
        .angle(-90);
    airbasePlot.x(function (d) {
            return costToHours(d.criticality);
        }, xScale)
        .y(function (d) {
            return d.nearestAirbaseDistance;
        }, airbaseYScale)
        .attr("fill", function (d) {
            return typeColors.airbase; //  "#b00" : "#5279c7"
        })
        .animated(true)
        .addDataset(dataset);

    var titleLabel = new Plottable.Components.TitleLabel("Threat Proximity")
        .yAlignment("center");

    var chart = new Plottable.Components.Table([
        [null, null, titleLabel],
        [samYLabel, samYAxis, samPlot],
        [airbaseYLabel, airbaseYAxis, airbasePlot],
        [null, null, xAxis],
        [null, null, xLabel]
    ]);

    chart.renderTo("svg#proximity");
};

var costProximityPlot = function () {
    return loadTargets()
        .then(annotateThreats)
        .then(annotateBases)
        .then(plotProximity);
};

costHistogram()
    .then(costProximityPlot)
    .catch(function (error) {
        console.log("Failed to plot data ");
        console.log(error);
    });

