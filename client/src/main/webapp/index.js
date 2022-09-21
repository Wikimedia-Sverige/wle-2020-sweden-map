var map;
var searchResultLayer;

var geolocationLayer;
var geolocation;

var mappos;

$(function () {

  trackUser('entered page');

  mappos = L.Permalink.getMapLocation(0, [0, 0]);
  var hasPermalinkMapLocation = mappos.center[0] !== 0 && mappos.center[1] !== 0;
  if (mappos.center[0] === 0 && mappos.center[1] === 0 && mappos.zoom === 0) {
    mappos = {zoom: 5, center: [60.128162, 18.643501]};
    trackUser('initialization', 'no permalink location');
  } else {
    trackUser('initialization', 'permalink location');
  }
  initializeMap();
  L.Permalink.setup(map);

  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(function (position) {
      var positionLatLng = L.latLng(position.coords.latitude, position.coords.longitude);
      if (!hasPermalinkMapLocation) {
        var swedenEnvelope = L.latLngBounds(
            L.latLng(55.3617373725, 11.0273686052),
            L.latLng(69.1062472602, 23.9033785336)
        );
        if (swedenEnvelope.contains(positionLatLng)) {
          map.setView(positionLatLng, 12);
          trackUser('initialization', 'geolocation', 'located in sweden');
        } else {
          trackUser('initialization', 'geolocation', 'not located in sweden');
        }
      }
      L.marker(positionLatLng, {
        icon: L.VectorMarkers.icon({
          icon: "street-view",
          markerColor: 'pink'
        })
      }).addTo(geolocationLayer);
      search();
    }, function (err) {
      search();
    }, {
      enableHighAccuracy: true,
      timeout: 5000,
      maximumAge: 0
    });
  } else {
    search();
  }
});

function initializeMap() {
  map = L.map('map', {
    zoomControl: false,
    center: mappos.center,
    zoom: mappos.zoom
  });

  map.createPane('base');
  map.createPane('geolocation');
  map.createPane('searchResults');
  map.createPane('labels');

  map.getPane('base').style.zIndex = 100;
  map.getPane('geolocation').style.zIndex = 200;
  map.getPane('searchResults').style.zIndex = 300;
  map.getPane('labels').style.zIndex = 400;

  var attribution = '<a href="https://www.openstreetmap.org">OSM</a> | <a href="https://wikimedia.se/">WMSE</a> | Läs mer om <a href="https://commons.wikimedia.org/wiki/Commons:Wiki_Loves_Earth_2021_in_Sweden/sv">Wiki Loves Earth</a>';

  // var base = new L.TileLayer('https://maps.wikimedia.org/osm-intl/{z}/{x}/{y}.png', {
  var base = new L.TileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
  // var base = new L.TileLayer('https://maps.keb.kodapan.se/tiles/hydda/v2/base/{z}/{x}/{y}.png', {
    subdomains: "abc",
    attribution: attribution,
    maxZoom: 20,
    maxNativeZoom: 19,
    pane: "base"
  });

  // var labels = new L.TileLayer('https://maps.keb.kodapan.se/tiles/hydda/v2/places_roads_and_labels/int/{z}/{x}/{y}.png', {
  //   subdomains: "abc",
  //   attribution: attribution,
  //   maxZoom: 20,
  //   maxNativeZoom: 19,
  //   pane: "labels"
  // });

  searchResultLayer = L.layerGroup([], {
    pane: "searchResults"
  });

  geolocationLayer = L.layerGroup([], {
    pane: "geolocation"
  });

  map.addLayer(base);
  map.addLayer(geolocationLayer);
  map.addLayer(searchResultLayer);
  // map.addLayer(labels);


  /** this is to handle a bug that occurs sometimes when clicking on an icon one happens to move the map really quick instead. */
  var moveStarted;
  var moveSearchTimer;
  map.on('movestart', function (mouseEvent) {
    moveStarted = new Date().getTime();
    moveSearchTimer = moveStarted;
    $("#tooltip").hide();
  });
  map.on('moveend', function (mouseEvent) {
    // todo did we move far enough? would be better
    var millisecondsSpent = new Date().getTime() - moveStarted;
    if (millisecondsSpent < 100) {
      return;
    }
    delayedSearch(100);
  });
  map.on('move', function (mouseevent) {
    var now = new Date().getTime();
    var millisecondsSpent = now - moveSearchTimer;
    if (millisecondsSpent > 1000) {
      delayedSearch(100);
      moveSearchTimer = now;
    }
  });
  map.on('zoomend', function (mouseEvent) {
    delayedSearch(200);
    $("#tooltip").hide();
  });
  map.on('resize', function (mouseEvent) {
    delayedSearch(500);
    $("#tooltip").hide();
  });

}

var delayedSearchTimeout = null;
var legendTimeout = null;

function delayedSearch(millisecondsDelay) {
  if (delayedSearchTimeout) {
    window.clearTimeout(delayedSearchTimeout);
  }
  delayedSearchTimeout = window.setTimeout(function () {
    search();
  }, millisecondsDelay);
}

// this needs to match the data in NaturvardsregistretGeometryManager
var distanceToleranceByZoom = {
  6: 0.1,
  7: 0.1,
  8: 0.05,
  9: 0.025,
  10: 0.005,
  11: 0.002,
  12: 0.001,
  13: 0.0005,
  14: 0.0001,
  15: null,
  16: null,
  17: null,
  18: null,
  19: null,
  20: null
};


function search() {
  var searchStarted = Date.now();
  if (map.getZoom() < 8) {
    searchResultLayer.clearLayers();
    $("#zoomMore").show();
    return;
  }
  $("#zoomMore").hide();
  var bounds = map.getBounds();
  $.ajax({
    type: "POST",
    url: "http://localhost:8080/api/nvr/search/envelope",
    contentType: "application/json",
    dataType: "json",
    data: JSON.stringify({
      boundingBox: {
        southLatitude: bounds.getSouth(),
        westLongitude: bounds.getWest(),
        northLatitude: bounds.getNorth(),
        eastLongitude: bounds.getEast()
      },
      distanceTolerance: distanceToleranceByZoom[map.getZoom()],
    }),
    success: function (searchResults) {

      if (searchResults.length > 0) {
        if (legendTimeout === null) {
          $("#legend").show();
          legendTimeout = window.setTimeout(function() {
            $("#legend").hide();
          }, 10000);
        }
      }

      searchResultLayer.clearLayers();
      searchResults.forEach(function (searchResult) {

        var uploadUrl;

        if ("biosphere reserve" === searchResult.stereotype) {
          uploadUrl = "https://commons.wikimedia.org/wiki/special:uploadWizard?campaign=wle-se-unesco&id=" + searchResult.q + "&descriptionlang=sv&description=";
        } else {
          uploadUrl = "https://commons.wikimedia.org/wiki/special:uploadWizard?campaign=wle-se&id=" + searchResult.nvrid + "&descriptionlang=sv&description=";
        }

        var markerColor = searchResult.images.length ===  0? '#0000FF' : 'green';

        var polyStyle = {
          fillColor: markerColor,
          weight: 2,
          opacity: 1,
          color: markerColor,  //Outline color
          fillOpacity: 0.5
        };

        var circleStyle = {
          fillColor: markerColor,
          weight: 2,
          opacity: 1,
          color: markerColor,  //Outline color
          fillOpacity: 0.5,
          radius: 250
        };

        var popupHtml = "<a href='https://www.wikidata.org/wiki/"+  searchResult.q +"' target='_blank' onclick='trackWikidataLinkClick(\""+searchResult.q+"\");'>" + searchResult.label + "</a>";
        popupHtml += "<br/><a href='" + uploadUrl + "' target='_blank' onclick='trackUploadLinkClick(\""+searchResult.q+"\");'>Ladda upp bild</a>";
        if (searchResult.images.length > 0) {
          popupHtml += "<div class='popup-images'>";
          // popupHtml += "Har "+ searchResult.images.length+" bild";
          // if (searchResult.images.length > 1) {
          //   popupHtml += "er";
          // }
          searchResult.images.forEach(function (image){
            popupHtml += "<br/><a href='https://commons.wikimedia.org/wiki/File:"+image.filename+"' target='_blank'><img src='"+image.url+"' width='"+image.width/2+"' height='"+image.height/2+"' onclick='trackImageClick(\""+searchResult.q+"\", \""+image.filename+"\")'></a>";
          });
          popupHtml += "</div>"
        } else {
          popupHtml += "<br/>Saknar bilder";
        }

        if (searchResult.feature.geometry.type === 'MultiPolygon'
          || searchResult.feature.geometry.type === 'Polygon') {

          var polygon = L.geoJSON(searchResult.feature.geometry, {style: polyStyle})
            .on("click", function(e){
              var timestampOpened = Date.now();
              L.popup()
                .setLatLng(L.latLng(searchResult.centroid.coordinates[1], searchResult.centroid.coordinates[0]))
                .setContent(popupHtml)
                .openOn(map);
              map
                  .once('popupclose', function (popup){
                    var millisecondsOpened = Date.now() - timestampOpened;
                    trackUser('user interaction', 'search result popup closed', searchResult.q, millisecondsOpened);
                  });
              trackUser('user interaction', 'search result popup opened', searchResult.q, searchResult.images.length);
            })
            .addTo(searchResultLayer);
          setTooltip(searchResult, polygon);

          if (map.getZoom() >= 12) {
            var marker = L.marker(L.latLng(searchResult.centroid.coordinates[1], searchResult.centroid.coordinates[0]), {
              icon: L.VectorMarkers.icon({
                icon: 'leaf',
                markerColor: markerColor
              })
            })
              .on("click", function(e){
                var timestampOpened = Date.now();
                L.popup()
                  .setLatLng(L.latLng(searchResult.centroid.coordinates[1], searchResult.centroid.coordinates[0]))
                  .setContent(popupHtml)
                  .openOn(map);
                map
                    .once('popupclose', function (popup){
                      var millisecondsOpened = Date.now() - timestampOpened;
                      trackUser('user interaction', 'search result popup closed', searchResult.q, millisecondsOpened);
                    });
                trackUser('user interaction', 'search result popup opened', searchResult.q, searchResult.images.length);
              })
              .addTo(searchResultLayer);
            setTooltip(searchResult, marker);
          }

        } else if (searchResult.feature.geometry.type === 'MultiPoint'
          || searchResult.feature.geometry.type === 'Point') {

          function addPoint(point) {
            var marker;
            if (map.getZoom() >= 12) {
              marker = L.marker(L.latLng(point[1], point[0]), {
                icon: L.VectorMarkers.icon({
                  icon: 'leaf',
                  markerColor: markerColor
                })
              });
            } else {
              marker = L.circle(L.latLng(point[1], point[0]), circleStyle);
            }
            marker
              .on("click", function(e){
                var timestampOpened = Date.now();
                var popup = L.popup()
                  .setLatLng(L.latLng(searchResult.centroid.coordinates[1], searchResult.centroid.coordinates[0]))
                  .setContent(popupHtml)
                  .openOn(map);
                map
                  .once('popupclose', function (popup){
                    var millisecondsOpened = Date.now() - timestampOpened;
                    trackUser('user interaction', 'search result popup closed', searchResult.q, millisecondsOpened);
                  });
                trackUser('user interaction', 'search result popup opened', searchResult.q, searchResult.images.length);
              })
              .addTo(searchResultLayer);
            setTooltip(searchResult, marker);

          }

          if (searchResult.feature.geometry.type === 'MultiPoint') {
            searchResult.feature.geometry.coordinates.forEach(function (point) {
              addPoint(point);
            });
          } else {
            addPoint(searchResult.feature.geometry.coordinates);
          }


        } else {
          console.log("unsupported " + searchResult.feature.geometry.type);
        }

      });

      var millisecondsSpentSearching = Date.now() - searchStarted;
      trackUser('user interaction', 'search results processed', 'millisecondsSpent', millisecondsSpentSearching);
      trackUser('user interaction', 'search results processed', 'numberOfResults', searchResults.length);
    }
  });
}

var startHoverTimestamp = null;
function setTooltip(searchResult, element, backgroundColor, textColor) {
  if (element === null) {
    console.log("Avoid adding tooltip to non existing elements!");
    return;
  }

  if (arguments.length === 2) {
    backgroundColor = "#0000FF";
    textColor = "#FFFFFF";
  } else if (arguments.length === 3) {
    textColor = "#FFFFFF";
  }

  var mapDiv = document.getElementById("map");
  var tooltip = document.getElementById("tooltip");
  element.on("mousemove", function (mouseEvent) {

    if (startHoverTimestamp === null) {
      startHoverTimestamp = Date.now();
      trackUser( 'user interaction','search result begin hover', searchResult.q);
    }

    tooltip.innerHTML = searchResult.label;
    tooltip.style.top = "0px";
    tooltip.style.left = "0px";
    tooltip.style.display = "block";
    tooltip.style["background-color"] = backgroundColor;
    tooltip.style.color = textColor;

    var mouseXY = {x: 0, y: 0};

    if (mouseEvent) {
      if (typeof (mouseEvent.pageX) == 'number') {
        //most browsers
        mouseXY.x = mouseEvent.pageX;
        mouseXY.y = mouseEvent.pageY;
      } else if (typeof (mouseEvent.clientX) == 'number') {
        //Internet Explorer 8- and older browsers
        //other browsers provide this, but follow the pageX/Y branch
        mouseXY.x = mouseEvent.clientX;
        mouseXY.y = mouseEvent.clientY;
        var badOldBrowser = (window.navigator.userAgent.indexOf('Opera') + 1) ||
          (window.ScriptEngine && ScriptEngine().indexOf('InScript') + 1) ||
          (navigator.vendor == 'KDE');
        if (!badOldBrowser) {
          if (document.body && (document.body.scrollLeft || document.body.scrollTop)) {
            //IE 4, 5 & 6 (in non-standards compliant mode)
            mouseXY.x += document.body.scrollLeft;
            mouseXY.y += document.body.scrollTop;
          } else if (document.documentElement && (document.documentElement.scrollLeft || document.documentElement.scrollTop)) {
            //IE 6 (in standards compliant mode)
            mouseXY.x += document.documentElement.scrollLeft;
            mouseXY.y += document.documentElement.scrollTop;
          }
        }
      } else if (typeof (mouseEvent.containerPoint) == 'object') {
        mouseXY.x = mouseEvent.containerPoint.x;
        mouseXY.y = mouseEvent.containerPoint.y;
      } else {
        //total failure, we have no way of obtaining the mouse coordinates
      }
    }

    var x = tooltip.offsetWidth + 20;
    if (mouseXY.x + x > mapDiv.offsetWidth) {
      x = mapDiv.offsetWidth - x;
    } else {
      x = mouseXY.x + 20;
    }

    var y = tooltip.offsetHeight + 20;
    if (mouseXY.y + y > mapDiv.offsetHeight) {
      y = mapDiv.offsetHeight - y;
    } else {
      y = mouseXY.y + 20;
    }

    tooltip.style.left = x + "px";
    tooltip.style.top = y + "px";
  });
  element.on("mouseout", function (e) {
    tooltip.style.display = "none";
    var millisecondsSpentHovering = Date.now() - startHoverTimestamp;
    trackUser( 'user interaction','search result end hover', searchResult.q, millisecondsSpentHovering);
    startHoverTimestamp = null;
  });
}


function trackImageClick(q, filename) {
  trackUser('user interaction', 'search result click image', q, filename);
}
function trackWikidataLinkClick(q, filename) {
  trackUser('user interaction', 'search result click wikidata link', q);
}
function trackUploadLinkClick(q, filename) {
  trackUser('user interaction', 'search result click upload link', q);
}

/**
 *
 * @param category Mandatory. This describes the type of events you want to track. For example, Link Clicks, Videos, Outbound Links, and Form Events.
 * @param action Mandatory. This is the specific action that is taken. For example, with the Video category, you might have a Play, Pause and Complete action.
 * @param name Optional, recommended. This is usually the title of the element that is being interacted with, to aid with analysis. For example, it could be the name of a Video that was played or the specific form that is being submitted.
 * @param value Optional. This is a numeric value and is often added dynamically. It could be the cost of a product that is added to a cart, or the completion percentage of a video.
 */
function trackUser(category, action, name, value) {
  console.log('[trackUser] ' +  JSON.stringify({
    category: category,
    action: action,
    name: name,
    value: value
  }));
  _paq.push(['trackEvent', category, action, name, value]);
}
