var urlHash = window.location.hash;
var tutorial = urlHash == "#tutorial";

var startTutorial = function () {
    tutorial = true;
    var intro = introJs();
    var steps = [
        {
            intro: "<h5>Introduction</h5><p>This is an analysis of supply network resilience. "
            + "It explores the flow of supplies from suppliers to consumers, "
            + "as well as vulnerabilities in the infrastructure supporting that movement.</p>",
            position: 'bottom',
            before: function () {
                hideMapLayer('sources');
                hideMapLayer('sinks');
                hideMapLayer('paths');
                hideMapLayer('targets');
                clearAnimation();
            }
        },
        {
            intro: `<h5><i class="fa fa-` + typeIcons['supplier']
            + `"></i> | Suppliers</h5>`
            + `<p> The green markers identify suppliers of resources, such as food, fuel, and parts. `
            + `Trains load supplies at and depart from stations near these locations. </p>`,
            position: 'bottom',
            before: function () {
                hideMapLayer('sinks');
                hideMapLayer('paths');
                hideMapLayer('targets');
                clearAnimation();
                showSources();
            }
        },
        {
            intro: `<h5><i class="fa fa-` + typeIcons['consumer']
            + `"></i> | Consumers</h5><p>The purple markers indicate consumers of those resources, `
            + "such as maintenance facilities, populations, or military units. "
            + "Trains carrying supplies arrive and are offloaded at stations near these locations. </p>",
            position: 'bottom',
            before: function () {
                hideMapLayer('sources');
                hideMapLayer('paths');
                hideMapLayer('targets');
                clearAnimation();
                showSinks()
            }
        },
        {
            intro: `<h5><i class="fa fa-` + typeIcons['flow']
            + `"></i> | Flows</h5><p>The blue paths represent the railroad links connecting suppliers to consumers, `
            + "with small dots indicating flow. "
            + "Each path is optimized, minimizing the costs associated with traveling across terrain. "
            + "</p>",
            position: 'bottom',
            before: function () {
                hideMapLayer('targets')
                    .then(showSources)
                    .then(showSinks).then(showPaths).then(function () {
                    showAnimation('baseline');
                });
            }
        },
        {
            intro: `<h5><i class="fa fa-` + typeIcons['target']
            + `"></i> | Resilience</h5><p>The highlighted segments are vulnerable bridges along the rail routes. `
            + "Their relative vulnerability is indicated by color from green (low) to red (high) "
            + "and is determined by simulating damage to each bridge. </p>",
            position: 'bottom',
            before: function () {
                hideClusterLayer('sams').then(showTargets)
            }
        },
        {
            intro: `<h5><i class="fa fa-` + typeIcons['SAM']
            + `"></i> | Defense</h5><p>`
            + `The yellow and red markers indicate Surface-to-Air Missile (SAM) sites and early warning `
            + " radars protecting the rail network from aerial attack. Clicking on a bridge will indicate the nearest "
            + " SAM/Radar site.</p>",
            position: 'bottom',
            before: showSAMs
        }
    ];
    intro.setOptions({
        steps: steps,
        scrollToElement: false,
        showStepNumbers: false,
        overlayOpacity: 0.0,
        showBullets: false,
        exitOnOverlayClick: false,
        disableInteraction: false,
        skipLabel: 'Done',
        tooltipClass: 'custom-tooltip'
    }).onbeforechange(function (targetElement) {
        // Fetch the relevant data
        steps[this._currentStep].before();
    }).onafterchange(function () {
        var element = document.querySelector('.introjs-tooltipReferenceLayer');
        if (element) {
            element.style.setProperty('top', '120px');
        }
    }).onexit(function () {
        window.location.hash = "#";
        showDefaultLayers();
    });
    intro.start();
};

if (tutorial) {
    startTutorial();
}