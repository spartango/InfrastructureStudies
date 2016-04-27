var dataLayers = {
    airBases: {
        description: 'PLAAF Bases',
        displayType: 'Air Base',
        url: 'background/PLAAFBases.geojson',
        color: '#006837',
        icon: 'plane',
        character: '&#xf072',
        layerType: 'cluster'
    },
    refineries: {
        description: 'Oil Refineries',
        displayType: 'Refinery',
        url: 'background/PLAAFBases.geojson',
        color: '#DB4A04',
        icon: 'database',
        character: '&#xf1c0',
        layerType: 'cluster'
    },
    ports: {
        description: 'Ports',
        displayType: 'Port',
        url: 'background/WPI.geojson',
        color: '#253494',
        icon: 'anchor',
        character: '&#xf13d',
        layerType: 'cluster'
    },
    navalBases: {
        description: 'PLAN bases',
        displayType: 'Naval Base',
        url: 'background/PLANBases.geojson',
        color: '#176FAD',
        icon: 'ship',
        character: '&#xf21a',
        layerType: 'cluster'
    },
    missileBases: {
        description: 'PLRF bases',
        displayType: 'Missile Base',
        url: 'background/2AOperationalSites.geojson',
        color: '#993404',
        icon: 'bomb',
        character: '&#xf1e2',
        layerType: 'cluster'
    },
    stations: {
        description: 'Train Stations',
        displayType: 'Station',
        url: 'background/stations.geojson',
        color: '#4eb3d3',
        icon: 'train',
        character: '&#xf238',
        layerType: 'cluster'
    },
    USBases: {
        description: 'US Bases',
        displayType: 'U.S. Base',
        url: 'background/USbases.geojson',
        color: '#909599',
        icon: 'flag',
        character: '&#xf024',
        layerType: 'cluster'
    }
};

var tileLayers = {
    "Streets": CartoDB_Positron,
    "Dark": CartoDB_DarkMatter,
    "Physical": mapboxLayer,
    "Topo": topoMapboxLayer,
    "Satellite": hybridMapboxLayer,
    "Latest Imagery": satelliteDigitalGlobeLayer
};

var overlayLayers = {};

var defaultLayers = [
    'airBases',
    'navalBases',
    'refineries',
    'USBases',
];