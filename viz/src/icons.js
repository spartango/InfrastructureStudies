var clusterIcon = function (cluster) {
    var radius = 36;
    var strokeLength = 2 * 3.141592 * radius;
    var colorCount = {};
    var childCount = cluster.getChildCount();

    cluster.getAllChildMarkers().forEach(function (el) {
        var type = el.feature.properties.type;
        var color = typeColors[type] ? typeColors[type] :
            el.feature.properties.color ? JSON.stringify(el.feature.properties.color) : defaultColor;
        if (colorCount.hasOwnProperty(color))
            colorCount[color]++;
        else
            colorCount[color] = 1;
    });


    var textOpacity = 1;
    var dashoffsetSum = 0;
    var svgHtml = `<svg width="100%" height="100%" viewbox="0 0 100 100"> \
        <circle cx="50" cy="50" r="` + radius + `" fill="black" fill-opacity="0.5"/>`

    for (var color in colorCount) {
        svgHtml = svgHtml + `<circle cx="50" cy="50" r="` + radius + `" fill="transparent" stroke-width="10" stroke=` +
            color + ` stroke-dasharray=` + strokeLength + ` stroke-dashoffset="` + dashoffsetSum + `" stroke-opacity="` + textOpacity + `" />`
        var currLength = (1.0 * colorCount[color] / childCount) * strokeLength;
        dashoffsetSum += currLength;
    }

    var textX = 42;
    var fontSize = 32;
    if (childCount >= 10) {
        textX = 32;
    }
    if (childCount >= 100) {
        fontSize = 30;
        textX = 24;
    }
    if (childCount >= 1000) {
        textX = 20;
        fontSize = 28;
    }

    svgHtml += `<text x="` + textX
        + `" y="60" style="fill: white; font-size: `
        + fontSize + `px; font-weight: bold; opacity: `
        + textOpacity + `;">` + childCount + `</text></svg>`;

    return new L.DivIcon({html: svgHtml, className: 'tiny-marker-cluster', iconSize: new L.Point(radius, radius)});
};

var glyphSvg = function (type, icon) {
    var radius = 36;
    var color = typeColors[type] ? typeColors[type] : type;
    var iconChar = typeChars[type] ? typeChars[type] : icon;
    var textOpacity = 1;

    var svgHtml = `<svg class="glyph-icon-` + type + `" width="100%" height="100%" viewbox="0 0 100 100"> \
        <circle cx="50" cy="50" r="` + radius + `" fill="` + color + `" fill-opacity="0.95"/>`

    var textX = 36;
    var fontSize = 32;

    svgHtml += `<text x="` + textX
        + `" y="60" style="fill: white; font-family: FontAwesome; font-size: ` + fontSize
        + `px; font-weight: bold; opacity: ` + textOpacity
        + `;">` + iconChar + `</text></svg>`;

    return svgHtml;
};

var glyphIcon = function (type, icon) {
    var radius = 36;
    var svgHtml = glyphSvg(type, icon);

    return new L.DivIcon({html: svgHtml, className: 'tiny-marker-cluster', iconSize: new L.Point(radius, radius)});
};