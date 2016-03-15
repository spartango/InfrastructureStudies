var DATA_DIR = "elevation/";

var histogramTargets = function (targets) {
    var criticalityData = targets.features.map(function (feature) {
        return feature.properties.criticality;
    });

    var minCriticality = d3.min(criticalityData);
    var midPoint = d3.mean(criticalityData);
    var maxCriticality = Math.min(d3.max(criticalityData), (midPoint - minCriticality) + midPoint);

    var color = d3.scale.linear()
        .domain([minCriticality, midPoint, maxCriticality])
        .range(["#FFFF00", "#FF8800", "#FF0000"]);

    console.log("Rendering histogram");
    // For each feature, extract the properties
    var data = targets.features.map(function (feature) {
        return costToHours(feature.properties.criticality);
    });

    var histogram = d3.layout.histogram()(data);
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
            var x = (d.x + (d.dx / 2));
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
            var center = turf.center(feature);
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
            var center = turf.center(feature);
            feature.properties.center = center;
            var nearest = turf.nearest(center, sams);
            var distance = turf.distance(nearest, center);
            feature.properties["nearestSAMDistance"] = distance;
        });
        return targets;
    });
};

var plotThreats = function (targets) {
    console.log("Rendering plot");
    // For each feature, extract the properties
    var data = targets.features.map(function (feature) {
        return feature.properties;
    });

    var dataset = new Plottable.Dataset(data);

    var xScale = new Plottable.Scales.ModifiedLog();
    var yScale = new Plottable.Scales.Linear();
    var xAxis = new Plottable.Axes.Numeric(xScale, "bottom");
    var yAxis = new Plottable.Axes.Numeric(yScale, "left");
    var plot = new Plottable.Plots.Scatter();
    var titleLabel = new Plottable.Components.TitleLabel("Air Defense Proximity")
        .yAlignment("center");
    var xLabel = new Plottable.Components.AxisLabel("Rerouting Cost (hr)")
        .yAlignment("center");
    var yLabel = new Plottable.Components.AxisLabel("Nearest SAM/Radar (km)")
        .angle(-90);


    plot.x(function (d) {
            return costToHours(d.criticality);
        }, xScale)
        .y(function (d) {
            return d.nearestSAMDistance;
        }, yScale)
        .attr("fill", function (d) {
            return d.activeSAM ? "#b00" : "#5279c7"
        })
        .animated(true)
        .addDataset(dataset);

    var chart = new Plottable.Components.Table([
        [null, null, titleLabel],
        [yLabel, yAxis, plot],
        [null, null, xAxis],
        [null, null, xLabel]
    ]);

    chart.renderTo("svg#sam");
};

var costSAMPlot = function () {
    return loadTargets().then(annotateThreats).then(plotThreats);
};

costHistogram().then(costSAMPlot).catch(function (error) {
    console.log("Failed to plot data " + error);
});

