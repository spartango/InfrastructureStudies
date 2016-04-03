var costToHours = function (cost) {
    return cost / 100000; // meters @ 100kmph,
};

var addCommas = function (number) {
    return ("" + number).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
};

var formatTime = function (hours) {
    if (hours > 120) {
        return Math.round(hours / 24) + " days";
    } else if (hours < 1) {
        return Math.round(hours * 60) + " minutes";
    } else {
        return addCommas(Math.round(hours)) + " hours";
    }
};

var formatDistance = function (kilometers) {
    return kilometers > 1.0 ? addCommas(Math.round(kilometers)) + " km" : Math.round(kilometers * 1000) + " m";
};