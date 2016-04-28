var histogramTargets = function (targets) {
    var criticalityData = targets.features.map(function (feature) {
        return feature.properties.criticality;
    });

    var minCriticality = d3.min(criticalityData);
    var midPoint = d3.mean(criticalityData);
    var absMax = d3.max(criticalityData);
    var maxCriticality = absMax;//Math.min(absMax, (midPoint - minCriticality) + midPoint);

    var color = d3.scale.linear()
        .domain([minCriticality, midPoint, maxCriticality])
        .range(["#FFFF00", "#FF8800", "#FF0000"]);

    console.log("Rendering histogram");
    // For each feature, extract the cost
    var data = criticalityData.map(costToHours);

    var minHours = costToHours(minCriticality);
    var maxHours = costToHours(maxCriticality);
    var histogram = d3.layout.histogram().range([minHours, maxHours])(data);

    var rest = data.filter(function (d) {
        return d > maxHours;
    }).length;

    histogram.push({y: rest, x: maxHours, dx: (costToHours(absMax) - maxHours)});

    var dataset = new Plottable.Dataset(histogram);

    var xScale = new Plottable.Scales.Category();
    var yScale = new Plottable.Scales.Linear();
    var xAxis = new Plottable.Axes.Category(xScale, "bottom").tickLabelAngle(-90);
    var yAxis = new Plottable.Axes.Numeric(yScale, "left");
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
        [yAxis, plot],
        [null, xAxis],
        [null, xLabel]
    ]);

    chart.renderTo("svg#cost");
};

var costHistogram = function () {
    // Histogram of rerouting costs
    return loadTargets().then(histogramTargets);
};

var buildDualPlot = function (xKey, yKey, targets, xTitle, yTitle) {
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

    // For each feature, extract the properties
    var data = targets.features.map(function (feature) {
        return feature.properties;
    });
    var dataset = new Plottable.Dataset(data);

    var xScale = new Plottable.Scales.Linear();
    var xAxis = new Plottable.Axes.Numeric(xScale, "bottom");
    var xLabel = new Plottable.Components.AxisLabel(xTitle ? xTitle : "Proximity (km)");

    var yScale = new Plottable.Scales.Linear();
    var yAxis = new Plottable.Axes.Numeric(yScale, "left");
    var plot = new Plottable.Plots.Scatter();
    var yLabel = new Plottable.Components.AxisLabel(yTitle ? yTitle : "Proximity (km)").angle(-90);

    plot.x(function (d) {
            return d[xKey];
        }, xScale)
        .y(function (d) {
            return d[yKey];
        }, yScale)
        .attr("fill", function (d) {
            return color(d.criticality);
        })
        //.animated(true)
        .addDataset(dataset);

    var chart = new Plottable.Components.Table([
        [yLabel, yAxis, plot],
        [null, null, xAxis],
        [null, null, xLabel]
    ]);

    return chart;
};

var buildPlot = function (featureKey, targets, title, color) {
    var criticalityData = targets.features.map(function (feature) {
        return feature.properties.criticality;
    });

    var minCriticality = d3.min(criticalityData);
    var midPoint = d3.mean(criticalityData);
    var absMax = d3.max(criticalityData);
    var maxCriticality = absMax;//Math.min(absMax, (midPoint - minCriticality) + midPoint);
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
                return color;
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
    colorScale.domain(["R2 = " + Math.round(1000 * rSquared) / 1000]);

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
    var chart = buildPlot("nearestSAMDistance", targets, "Nearest Radar (km)", "#78c679");
    chart.renderTo("svg#radars");
    return targets;
};

var plotSAMs = function (targets) {
    var activeTargets = {
        features: targets.features.filter(function (target) {
            return target.properties.activeSAM;
        })
    };
    var chart = buildPlot("nearestSAMDistance", activeTargets, "Nearest SAM (km)", dataLayers.SAMs.color);
    chart.renderTo("svg#sams");
    return targets;
};

var plotBases = function (targets) {
    var chart = buildPlot("nearestAirbaseDistance", targets, "Nearest Airbase (km)", dataLayers.airBases.color);
    chart.renderTo("svg#bases");
    return targets;
};

var plotStations = function (targets) {
    var chart = buildPlot("nearestStationDistance", targets, "Nearest Station (km)", dataLayers.stations.color);
    chart.renderTo("svg#stations");
    return targets;
};

var plotSources = function (targets) {
    var chart = buildPlot("nearestSourceDistance", targets, "Nearest Supplier (km)", dataLayers.suppliers.color);
    chart.renderTo("svg#sources");
    return targets;
};

var plotSinks = function (targets) {
    var chart = buildPlot("nearestSinkDistance", targets, "Nearest Consumer (km)", dataLayers.consumers.color);
    chart.renderTo("svg#sinks");
    return targets;
};

var plotSecondArtillery = function (targets) {
    var chart = buildPlot("nearest2ADistance", targets, "Nearest Missile Site (km)", dataLayers.missileBases.color);
    chart.renderTo("svg#nuclear");
    return targets;
};

var plotUSBases = function (targets) {
    var chart = buildPlot("nearestUSBaseDistance", targets, "Nearest US Base (km)", dataLayers.USBases.color);
    chart.renderTo("svg#usbases");
    return targets;
};

var plotDual = function (targets) {
    //var activeTargets = {
    //    features: targets.features.filter(function (target) {
    //        return target.properties.activeSAM;
    //    })
    //};

    var chart = buildDualPlot("nearestSAMDistance",
        "nearestUSBaseDistance",
        targets,
        "Nearest SAM (km)",
        "Nearest US Base (km)");
    chart.renderTo("svg#dual");
    return targets;
};

var costProximityPlot = function () {
    return loadTargets()
        .then(annotateThreats).then(plotSAMs).then(plotRadars)
        .then(annotateUSBases).then(plotUSBases)
        //.then(annotateSecondArtillery)
        //.then(plotSecondArtillery)
        //.then(annotateStations)
        .then(annotateBases).then(plotBases).then(plotDual)
        .then(annotateSinks).then(plotSinks)
        .then(annotateSources).then(plotSources)
    //.then(cacheTargets)
};

costHistogram()
    .then(costProximityPlot)
    .catch(function (error) {
        console.log("Failed to plot data ");
        console.log(error);
    });

