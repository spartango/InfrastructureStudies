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
    // For each feature, extract the cost
    var data = criticalityData.map(costToHours);

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
    var xAxis = new Plottable.Axes.Category(xScale, "bottom").tickLabelAngle(-90);
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
        //.animated(true)
        .addDataset(dataset);

    var chart = new Plottable.Components.Table([
        //[titleLabel],
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

var annotateSources = function (targets) {
    return loadSources().then(function (sources) {
        console.log("Computing source ranges");
        targets.features.forEach(function (feature) {
            var center = feature.properties.center ? feature.properties.center : turf.center(feature);
            var nearest = turf.nearest(center, sources);
            var distance = turf.distance(nearest, center);
            feature.properties["nearestSourceDistance"] = distance;
        });
        return targets;
    });
};

var annotateSinks = function (targets) {
    return loadSources().then(function (sinks) {
        console.log("Computing sink ranges");
        targets.features.forEach(function (feature) {
            var center = feature.properties.center ? feature.properties.center : turf.center(feature);
            var nearest = turf.nearest(center, sinks);
            var distance = turf.distance(nearest, center);
            feature.properties["nearestSinkDistance"] = distance;
        });
        return targets;
    });
};

var buildPlot = function (featureKey, targets, title, color) {
    var criticalityData = targets.features.map(function (feature) {
        return feature.properties.criticality;
    });

    var minCriticality = d3.min(criticalityData);
    var midPoint = d3.mean(criticalityData);
    var absMax = d3.max(criticalityData);
    var maxCriticality = Math.min(absMax, (midPoint - minCriticality) + midPoint);
    var xRange = [minCriticality, maxCriticality].map(costToHours);

    // For each feature, extract the properties
    var data = targets.features.map(function (feature) {
        return feature.properties;
    });

    var regressionTarget = data.map(function (d) {
        return [costToHours(d.criticality), d[featureKey]];
    });
    var regression = ss.linearRegression(regressionTarget);
    var regressionFunc = ss.linearRegressionLine(regression);
    var rSquared = ss.rSquared(regressionTarget, regressionFunc);

    console.log("Regression: " + regression.m + "x + " + regression.b + "; R^2 = " + rSquared);

    var regressionData = new Plottable.Dataset(xRange.map(function (x) {
        return {x: x, y: regressionFunc(x)};
    }));
    var dataset = new Plottable.Dataset(data);

    var xScale = new Plottable.Scales.Linear().domain(xRange);
    var xAxis = new Plottable.Axes.Numeric(xScale, "bottom");
    var xLabel = new Plottable.Components.AxisLabel("Rerouting Cost (hr)");

    var yScale = new Plottable.Scales.Linear()//.domain([0, 100]);
    var yAxis = new Plottable.Axes.Numeric(yScale, "left");
    var plot = new Plottable.Plots.Scatter();
    var regressionPlot = new Plottable.Plots.Line();
    var yLabel = new Plottable.Components.AxisLabel(title ? title : "Proximity (km)").angle(-90);

    plot.x(function (d) {
            return costToHours(d.criticality);
        }, xScale)
        .y(function (d) {
            return d[featureKey];
        }, yScale)
        .attr("fill", function (d) {
            if (color) {
                return typeColors[color] ? typeColors[color] : color;
            } else {
                return d.activeSAM ? "#b00" : typeColors.radar; //  "#b00" : "#5279c7"
            }
        })
        //.animated(true)
        .addDataset(dataset);

    regressionPlot.x(function (d) {
            return d.x;
        }, xScale)
        .y(function (d) {
            return d.y;
        }, yScale)
        //.animated(true)
        .addDataset(regressionData);

    // Plot
    var colorScale = new Plottable.Scales.Color();
    var legend = new Plottable.Components.Legend(colorScale);
    colorScale.domain(["R2 = "+ Math.round(1000 * rSquared)/1000]);

    var plots = new Plottable.Components.Group([plot, regressionPlot]);
    var chart = new Plottable.Components.Table([
        [null, null, legend],
        [yLabel, yAxis, plots],
        [null, null, xAxis],
        [null, null, xLabel]
    ]);

    return chart;
};

var plotRadars = function (targets) {
    var chart = buildPlot("nearestSAMDistance", targets, "Nearest Radar (km)", "radar");
    chart.renderTo("svg#radars");
    return targets;
};

var plotSAMs = function (targets) {
    var activeTargets = {
        features: targets.features.filter(function (target) {
            return target.properties.activeSAM;
        })
    };
    var chart = buildPlot("nearestSAMDistance", activeTargets, "Nearest SAM (km)", "#b00");
    chart.renderTo("svg#sams");
    return targets;
};

var plotBases = function (targets) {
    var chart = buildPlot("nearestAirbaseDistance", targets, "Nearest Airbase (km)", "airbase");
    chart.renderTo("svg#bases");
    return targets;
};

var plotStations = function (targets) {
    var chart = buildPlot("nearestStationDistance", targets, "Nearest Station (km)", "station");
    chart.renderTo("svg#stations");
    return targets;
};

var plotSources = function (targets) {
    var chart = buildPlot("nearestSourceDistance", targets, "Nearest Supplier (km)", "supplier");
    chart.renderTo("svg#sources");
    return targets;
};

var plotSinks = function (targets) {
    var chart = buildPlot("nearestSinkDistance", targets, "Nearest Consumer (km)", "consumer");
    chart.renderTo("svg#sinks");
    return targets;
};

var costProximityPlot = function () {
    return loadTargets()
        .then(annotateThreats)
        .then(annotateBases)
        .then(annotateSinks)
        .then(plotBases)
        .then(plotSAMs)
        .then(plotRadars)
        .then(plotSinks);
};

costHistogram()
    .then(costProximityPlot)
    .catch(function (error) {
        console.log("Failed to plot data ");
        console.log(error);
    });

