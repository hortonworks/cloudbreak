var $jq = jQuery.noConflict();
$jq(document).ready(function () {
    $jq("#menu-credential ul li a").click(function () {
        if (!($jq(this).hasClass("not-option") )) {
            $jq(this).parents("#menu-credential").find('.dropdown-toggle span').text($jq(this).text()).removeClass('text-danger');
            $jq('#create-cluster-btn').removeClass('disabled');
        }
    });

// switch cluster title to normal word break if title has space in the middle
    $jq(".cluster h4 .btn-cluster").each(function () {
        var s = $jq(this).text().trim();
        if (s.indexOf(" ") > -1 && ( s.indexOf(" ") > 4 || s.lastIndexOf(" ") < 15 )) {
            $jq(this).css("word-break", "normal");
        }
    });

    // initialize Isotope with sort and filter
    var qsRegex;
    var $container = $jq('.isotope-wrapper').isotope({
        itemSelector: '.cluster',
        layoutMode: 'masonry',
        masonry: {
            columnWidth: 156,
            gutter: 0
        },
        filter: function () {
            return qsRegex ? $jq(this).find('h4 a').text().match(qsRegex) : true;
        },
        getSortData: {
            // cluster name
            name: function (itemElem) {
                var str = $jq(itemElem).find('h4 a').text();
                return str.toLowerCase();
            },
            // cluster node number
            nodes: function (itemElem) {
                var str = $jq(itemElem).find('.mod-nodes dd').text();
                return parseInt(str);
            },
            // cluster state
            state: '[data-state]',
            // cluster uptime
            uptime: function (itemElem) {
                var str = $jq(itemElem).find('.mod-uptime dd').text();
                return parseInt(str);
            }
        }
    });
    $container.isotope();
    // sorting
    $jq('#sort-clusters-btn + ul li').on('click', 'a', function (e) {
        var sortByValue = $jq(this).attr('data-sort-by');
        var sortAsc = $jq(this).attr('data-sort-asc');
        $container.isotope({ sortBy: sortByValue, sortAscending: sortAsc });
        // set button label to selected sort mode
        $jq("#sort-clusters-btn span.title").text(sortByValue);
        // disable selected menu item
        $jq(this).parents().find('.disabled').removeClass("disabled");
        $jq(this).parent().addClass("disabled");
    });
    // filtering
    var $quicksearch = $jq('#notification-n-filtering').keyup(debounce(function () {
        qsRegex = new RegExp($quicksearch.val(), 'gi');
        $container.isotope();
    }, 300));
    // debounce so filtering doesn't happen every millisecond
    function debounce(fn, threshold) {
        var timeout;
        return function debounced() {
            if (timeout) {
                clearTimeout(timeout);
            }
            function delayed() {
                fn();
                timeout = null;
            }

            timeout = setTimeout(delayed, threshold || 100);
        }
    }

// notification/filter field clearing on focus for filtering
    $jq("#notification-n-filtering").focusin(function () {
        $jq(this).val("").trigger("keyup")
            // delete warning sign
            .parent().find('> i').remove();
        // delete error classes
        $jq(this).parent().parent().removeClass('has-feedback').removeClass('has-error');
    });

// cluster-block hide/show
    $jq('#toggle-cluster-block-btn').click(function () {
        $jq('.cluster-block').collapse('toggle');
        // must force isotope redraw, its container height set 0 by some fucking shite
        $container.isotope();
    });
    // toggle fa-angle-up/down icon and sort button
    $jq('.cluster-block').on('hidden.bs.collapse', function () {
        $jq('#toggle-cluster-block-btn i').removeClass('fa-angle-up').addClass('fa-angle-down');
        $jq('#sort-clusters-btn').addClass('disabled');
    });
    $jq('.cluster-block').on('shown.bs.collapse', function () {
        $jq('#toggle-cluster-block-btn i').removeClass('fa-angle-down').addClass('fa-angle-up');
        $jq('#sort-clusters-btn').removeClass('disabled');
    });

// Bootstrap carousel as clusters / cluster details / create cluster slider
    // init
    $jq('.carousel').carousel('pause');
    // show cluster details
    $jq(document).on("click", ".cluster h4 .btn-cluster", function() {
        $jq('.carousel').carousel(1);
        $jq('#toggle-cluster-block-btn').addClass('disabled');
        $jq('#sort-clusters-btn').addClass('disabled');
        $jq("#notification-n-filtering").prop("disabled", true);
    });
    // back to clusters
    $jq("#cluster-details-back-btn").click(function () {
        $jq('.carousel').carousel(0);
        // must force isotope redraw, its container height set 0 by by some fucking shite
        $container.isotope();
        $jq('#toggle-cluster-block-btn').removeClass('disabled');
        $jq('#sort-clusters-btn').removeClass('disabled');
        $jq("#notification-n-filtering").prop("disabled", false);
    });
    // show create cluster panel
    $jq('#create-cluster-btn').click(function () {
        $jq('.carousel').carousel(2);
        $jq(this).addClass('disabled');
        $jq('#toggle-cluster-block-btn').addClass('disabled');
        $jq('#sort-clusters-btn').addClass('disabled');
        $jq("#notification-n-filtering").prop("disabled", true);
    });
    // back to clusters
    $jq("#create-cluster-back-btn").click(function () {
        $jq('.carousel').carousel(0);
        // must force isotope redraw, .isotope-wrapper's height set 0 by by some fucking shite
        $container.isotope();
        $jq('#toggle-cluster-block-btn').removeClass('disabled');
        $jq('#create-cluster-btn').removeClass('disabled');
        $jq('#sort-clusters-btn').removeClass('disabled');
        $jq("#notification-n-filtering").prop("disabled", false);
    });

// main template/blueprint/credential panels icon toggle
    $jq('.panel-btn-in-header-collapse').on('hidden.bs.collapse', function () {
        $jq(this).parent().find('.panel-heading .btn i').removeClass('fa-angle-up').addClass('fa-angle-down');
    });
    $jq('.panel-btn-in-header-collapse').on('shown.bs.collapse', function () {
        $jq(this).parent().find('.panel-heading .btn i').removeClass('fa-angle-down').addClass('fa-angle-up');
    });
// create * panels
    $jq('.panel-under-btn-collapse').on('shown.bs.collapse', function () {
        $jq(this).parent().prev()
            .find('.btn').fadeTo("fast", 0, function () {
                $jq(this).removeClass('btn-success').addClass('btn-info')
                    .find('i').removeClass('fa-plus').addClass('fa-times').removeClass('fa-fw')
                    .parent().find('span').addClass('hidden');
                $jq(this).fadeTo("slow", 1);
            });
    });
    $jq('.panel-under-btn-collapse').on('hidden.bs.collapse', function () {
        $jq(this).parent().prev()
            .find('.btn').fadeTo("fast", 0, function () {
                $jq(this).removeClass('btn-info').addClass('btn-success')
                    .find('i').removeClass('fa-times').addClass('fa-plus').addClass('fa-fw')
                    .parent().find('span').removeClass('hidden');
                $jq(this).fadeTo("slow", 1);
            });
    });
});