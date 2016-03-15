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
                hideLegend();
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
            + `<p> The blue markers identify suppliers of resources, such as food, fuel, and parts. `
            + `Trains load supplies at and depart from stations near these sites. `
            + `Click on any supplier for more information about it.</p>`,
            position: 'bottom',
            before: function () {
                hideMapLayer('sinks');
                showSources();
            }
        },
        {
            intro: `<h5><i class="fa fa-` + typeIcons['consumer']
            + `"></i> | Consumers</h5><p>The purple markers indicate consumers of those resources, `
            + "such as maintenance facilities, populations, or military units. "
            + "Trains carrying supplies are offloaded at stations near these locations. "
            + `Click on any consumer for more information about it.</p>`,
            position: 'bottom',
            before: function () {
                hideMapLayer('sources');
                hideMapLayer('paths');
                clearAnimation();
                showSinks()
            }
        },
        {
            intro: `<h5><i class="fa fa-` + typeIcons['flow']
            + `"></i> | Flows</h5><p>The black paths represent the best railroad routes connecting suppliers to consumers, `
            + "with small dots indicating flow direction. "
            + "Each path is optimized for speed, minimizing the costs associated with traveling across the terrain. </p>",
            position: 'bottom',
            before: function () {
                hideMapLayer('targets')
                    .then(hideLegend)
                    .then(showSources)
                    .then(showSinks)
                    .then(showPaths)
                    .then(function () {
                        showAnimation('baseline');
                    })
            }
        },
        {
            intro: `<h5><i class="fa fa-` + typeIcons['target']
            + `"></i> | Resilience</h5><p>The highlighted segments are vulnerable bridges along the rail routes. `
            + "Their relative vulnerability is indicated by color from yellow (low) to red (high) "
            + "and is determined by simulating damage to each bridge. "
            + `Click on any bridge for more information about it. </p>`,
            position: 'bottom',
            before: function () {
                hideClusterLayer('sams')
                    .then(function () {
                        return hideMapLayer('rings');
                    })
                    .then(showTargets)
                    .then(clearAnimation);
            }
        },
        {
            intro: `<h5><i class="fa fa-` + typeIcons['SAM']
            + `"></i> | Defense</h5><p>`
            + `The green markers indicate Surface-to-Air Missile (SAM) sites, radars, and airbases`
            + " protecting the rail network from aerial attack. Selecting a bridge will suggest the nearest "
            + " SAM/Radar site. "
            + `Click on any site for more information about it.</p>`,
            position: 'bottom',
            before: function () {
                showSAMThreats();
            }
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
    }).oncomplete(function () {
        window.location.hash = "#";
        hideClusterLayer('sams')
            .then(function () {
                return hideMapLayer('rings');
            }).then(showDefaultLayers);
    }).onexit(function () {
        window.location.hash = "#";
        hideClusterLayer('sams')
            .then(function () {
                return hideMapLayer('rings');
            }).then(showDefaultLayers);
    });
    intro.start();
};

if (tutorial) {
    startTutorial();
}