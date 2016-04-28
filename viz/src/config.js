var DATA_DIR = "testing/";

var dataLayers = {
    suppliers: {
        description: 'Resource Suppliers',
        displayType: 'Loading Point',
        url: DATA_DIR + 'sources.geojson',
        color: '#0868ac',
        icon: 'upload',
        character: '&#xf093',
        layerType: 'marker',
        popup: 'infrastructure',
        handleData: function (data) {
            $('#sourceCount').text(data.features.length);
            return data;
        }
    },
    consumers: {
        description: 'Resource Consumers',
        displayType: 'Offload Point',
        url: DATA_DIR + 'sinks.geojson',
        color: '#d01c8b',
        icon: 'download',
        character: '&#xf019',
        layerType: 'marker',
        popup: 'infrastructure',
        handleData: function (data) {
            $('#sinkCount').text(data.features.length);
            return data;
        }
    },
    airBases: {
        description: 'PLAAF Bases',
        displayType: 'Air Base',
        url: 'background/PLAAFBases.geojson',
        color: '#006837',
        icon: 'plane',
        character: '&#xf072',
        layerType: 'cluster',
        popup: 'infrastructure'
    },
    refineries: {
        description: 'Oil Refineries',
        displayType: 'Refinery',
        url: 'background/SurveyRefineries.geojson',
        color: '#DB4A04',
        icon: 'database',
        character: '&#xf1c0',
        layerType: 'cluster',
        popup: 'infrastructure'
    },
    ports: {
        description: 'Ports',
        displayType: 'Port',
        url: 'background/WPI.geojson',
        color: '#253494',
        icon: 'anchor',
        character: '&#xf13d',
        layerType: 'cluster',
        popup: 'infrastructure'
    },
    navalBases: {
        description: 'PLAN bases',
        displayType: 'Naval Base',
        url: 'background/PLANBases.geojson',
        color: '#176FAD',
        icon: 'ship',
        character: '&#xf21a',
        layerType: 'cluster',
        popup: 'infrastructure'
    },
    missileBases: {
        description: 'PLRF bases',
        displayType: 'Missile Base',
        url: 'background/2AOperationalSites.geojson',
        color: '#993404',
        icon: 'bomb',
        character: '&#xf1e2',
        layerType: 'cluster',
        popup: 'infrastructure'
    },
    stations: {
        description: 'Train Stations',
        displayType: 'Station',
        url: 'background/stations.geojson',
        color: '#4eb3d3',
        icon: 'train',
        character: '&#xf238',
        layerType: 'cluster',
        popup: 'infrastructure'
    },
    USBases: {
        description: 'US Bases',
        displayType: 'U.S. Base',
        url: 'background/USbases.geojson',
        color: '#909599',
        icon: 'flag',
        character: '&#xf024',
        layerType: 'cluster',
        popup: 'infrastructure'
    },
    SAMs: {
        description: 'SAM Sites',
        displayType: 'SAM',
        url: 'background/SAMs.geojson',
        color: '#31a354',
        icon: 'rocket',
        character: '&#xf135',
        layerType: 'cluster',
        popup: 'infrastructure'
    },
    rangeRings: {
        description: 'SAM Range Rings',
        displayType: 'Range Ring',
        url: 'background/RangeRingsP.geojson',
        color: '#31a354',
        icon: 'rocket',
        character: '&#xf135',
        style: {
            "color": "#d00",
            "weight": 2,
            "opacity": 0.8,
            "fillOpacity": 0.10,
            "clickable": false
        },
        layerType: 'polygon',
        handleData: turf.merge
    }
};

var defaultLayers = [
    'airBases',
    'refineries',
    'suppliers'
];