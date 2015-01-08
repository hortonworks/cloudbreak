var $jq = jQuery.noConflict();

function addCrudControls(){

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
    // notification/filter field clearing on focus for filtering
    $jq("#notification-n-filtering").focusin(function () {
        $jq('.combo-box').removeClass('has-feedback has-error has-warning has-success');
    });
    // sorting
    $jq('#sort-clusters-btn + ul li').on('click', 'a', function (e) {
        var sortByValue = $jq(this).text();
        // set button label to selected sort mode
        $jq("#sort-clusters-btn span.title").text(sortByValue);
        // disable selected menu item
        $jq(this).parents().find('.disabled').removeClass("disabled");
        $jq(this).parent().addClass("disabled");
    });
}

function addClusterListPanelJQEventListeners() {
  // cluster-block hide/show
  $jq('#toggle-cluster-block-btn').click(function () {
      $jq('.cluster-block').collapse('toggle');
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
  // Bootstrap carousel as clusters / cluster details / create cluster slider init
  $jq('.carousel').carousel('pause');
  // show cluster details
  $jq(document).on("click", ".cluster h4 .btn-cluster", function() {
      $jq('.carousel').carousel(1);
      $jq('.carousel').on('slid.bs.carousel', function () {
          // unbind event
          $jq(this).off('slid.bs.carousel');
          $jq('#cluster-details-panel-collapse').collapse('show');
      });
      $jq('#toggle-cluster-block-btn').addClass('disabled');
      $jq('#sort-clusters-btn').addClass('disabled');
  });
}

function addClusterFormJQEventListeners() {
    $jq('#cluster-form-panel .panel-heading > h5 > a').click(function (e) {
        e.preventDefault();
        accordion = $jq(this).attr("data-parent");
        if (accordion != "") {
            $jq(accordion).find('.in').collapse('hide');
        }
        $jq(this).parent().parent().next().collapse('toggle');
    });
    // solo panel or in accordion shown
    $jq('#cluster-form-panel .panel-collapse').on('shown.bs.collapse', function (e) {
        e.stopPropagation();
        var panel = $jq(this).parent();		// panel
        var offset = panel.offset().top;
        if(offset) {
            $jq('html,body').animate({
                scrollTop: offset - 64
            }, 500);
        }
    });
    // solo panel or in accordion hidden
    $jq('#cluster-form-panel .panel-collapse').on('hidden.bs.collapse', function (e) {
        e.stopPropagation();
    });
    // show create cluster panel
    $jq('#create-cluster-btn').click(function () {
        $jq('.carousel').carousel(2);
        $jq('.carousel').on('slid.bs.carousel', function () {
            // unbind event
            $jq(this).off('slid.bs.carousel');
            $jq('#create-cluster-panel-collapse').collapse('show');
        });
        $jq(this).addClass('disabled');
        $jq('#toggle-cluster-block-btn').addClass('disabled');
        $jq('#sort-clusters-btn').addClass('disabled');
    });
    // back to clusters
    $jq("#create-cluster-back-btn").click(function () {
        $jq('.carousel').carousel(0);
        $jq('.carousel').on('slid.bs.carousel', function () {
            // unbind event
            $jq(this).off('slid.bs.carousel');
            $jq('#create-cluster-panel-collapse').collapse('hide');
        });
        $jq('#toggle-cluster-block-btn').removeClass('disabled');
        $jq('#create-cluster-btn').removeClass('disabled');
        $jq('#sort-clusters-btn').removeClass('disabled');
    });
}

function addActiveClusterJQEventListeners() {
    $jq('#active-cluster-panel .panel-heading > h5 > a').click(function (e) {
        e.preventDefault();
        accordion = $jq(this).attr("data-parent");
        if (accordion != "") {
            $jq(accordion).find('.in').collapse('hide');
        }
        $jq(this).parent().parent().next().collapse('toggle');
    });
    // solo panel or in accordion shown
    $jq('#active-cluster-panel .panel-collapse').on('shown.bs.collapse', function (e) {
        e.stopPropagation();
        var panel = $jq(this).parent();		// panel
        var offset = panel.offset().top;
        if(offset) {
            $jq('html,body').animate({
                scrollTop: offset - 64
            }, 500);
        }
    });
    // solo panel or in accordion hidden
    $jq('#active-cluster-panel .panel-collapse').on('hidden.bs.collapse', function (e) {
        e.stopPropagation();
    });
    // back to clusters
    $jq("#cluster-details-back-btn").click(function () {
        $jq('.carousel').carousel(0);
        $jq('.carousel').on('slid.bs.carousel', function () {
            // unbind event
            $jq(this).off('slid.bs.carousel');
            $jq('#cluster-details-panel-collapse').collapse('hide');
        });
        $jq('#toggle-cluster-block-btn').removeClass('disabled');
        $jq('#sort-clusters-btn').removeClass('disabled');
    });
    $jq('#terminateStackBtn').on('click', function () {
        $jq('.carousel').carousel(0);
        // enable toolbar buttons
        $jq('#toggle-cluster-block-btn').removeClass('disabled');
        $jq('#sort-clusters-btn').removeClass('disabled');
        $jq('#create-cluster-btn').removeClass('disabled');
    });
}

function addPanelJQueryEventListeners(panel){
    $jq('#panel-' + panel + ' .panel-heading > h5 > a').click(function (e) {
        e.preventDefault();
        accordion = $jq(this).attr("data-parent");
        if (accordion != "") {
            $jq(accordion).find('.in').collapse('hide');
        }
        $jq(this).parent().parent().next().collapse('toggle');
    });
    // solo panel or in accordion shown
    $jq('#panel-create-' + panel + '-collapse .panel-collapse').on('shown.bs.collapse', function (e) {
        e.stopPropagation();
        var panel = $jq(this).parent();		// panel
        var offset = panel.offset().top;
        if(offset) {
            $jq('html,body').animate({
                scrollTop: offset - 64
            }, 500);
        }
    });
    // solo panel or in accordion hidden
    $jq('#panel-create-' + panel + '-collapse .panel-collapse').on('hidden.bs.collapse', function (e) {
        e.stopPropagation();
    });
    // create panel click
    $jq('#panel-' + panel + '-collapse .btn-row-over-panel > a').click(function (e) {
        e.preventDefault();
        $jq(this).parent().parent().next().collapse('toggle');
    });
    // create template/blueprint/credential panel shown
    $jq('#panel-create-' + panel + '-collapse').on('shown.bs.collapse', function (e) {
        e.stopPropagation();
        // button switch
        $jq(this).parent().prev()
            .find('.btn').fadeTo("fast", 0, function () {
                $jq(this).removeClass('btn-success').addClass('btn-info')
                    .find('i').removeClass('fa-plus').addClass('fa-times').removeClass('fa-fw')
                    .parent().find('span').addClass('hidden');
                $jq(this).fadeTo("slow", 1);
            });
        // scroll
        var panel = $jq(this).parent().prev();	// btn-row-over-panel
        var offset = panel.offset().top;
        if(offset) {
            $jq('html,body').animate({
                scrollTop: offset - 64
            }, 500);
        }
    });
    // create template/blueprint/credential panel hidden
    $jq('#panel-create-' + panel + '-collapse').on('hidden.bs.collapse', function (e) {
        e.stopPropagation();
        $jq(this).parent().prev()
            .find('.btn').fadeTo("fast", 0, function () {
                $jq(this).removeClass('btn-info').addClass('btn-success')
                    .find('i').removeClass('fa-times').addClass('fa-plus').addClass('fa-fw')
                    .parent().find('span').removeClass('hidden');
                $jq(this).fadeTo("slow", 1);
            });
    });
    // management panel shown
    $jq('#panel-' + panel + '-collapse').on('shown.bs.collapse', function (e) {
        // button switch
        var thisPanel = $jq(this);
        if(thisPanel.context.id == e.target.id) {
            thisPanel.parent().find('.panel-heading .btn i').removeClass('fa-angle-down').addClass('fa-angle-up');
            // scroll
            var panel = $jq(this).parent().parent();	// panel
            var offset = panel.offset().top;
            if(offset) {
                $jq('html,body').animate({
                    scrollTop: offset - 64
                }, 500);
            }
        }
    });
    // management panel hidden
    $jq('#panel-' + panel + '-collapse').on('hidden.bs.collapse', function (e) {
        // button switch
        var thisPanel = $jq(this);
        if(thisPanel.context.id == e.target.id) {
            $jq(this).parent().find('.panel-heading .btn i').removeClass('fa-angle-up').addClass('fa-angle-down');
        }
    });

    $jq('#panel-' + panel + '-collapse .btn-segmented-control a').click(function (e) {
        var selected = 'btn-info';
        var active = 'btn-default';
        var control = $jq(this).parent().parent();
        e.preventDefault();
        control.find('a').each(function () {
            $jq(this).removeClass(selected).addClass(active);
        });
        $jq(this).removeClass(active).addClass(selected);
    });
}

function addDatePickerPanelJQueryEventListeners() {
    var timers = new Array;
    var timersIndex = 0;

    $jq('#btnGenReport').click(function (e) {
        var self = this;
        e.preventDefault();
        // disable button, start spinner
        $jq(this).addClass('disabled')
            .find('i').removeClass('fa-table').addClass('fa-circle-o-notch fa-spin');
        // simulated delay
        timers[timersIndex++] = window.setTimeout(function () {
            // enable button, stop spinner
            $jq(self).removeClass('disabled')
                .find('i').removeClass('fa-circle-o-notch fa-spin').addClass('fa-table');
        }, 1500);
    });

    $jq('#datePickerStart').datetimepicker({
        icons: {
            time: "fa fa-clock-o",
            date: "fa fa-calendar",
            up: "fa fa-arrow-up",
            down: "fa fa-arrow-down"
        },
        pickTime: false
    });

    $jq("#datePickerStart").on("dp.change", function (e) {
        // filter activated
        angular.element($jq("#datePickerStart")).scope().setStartDate(e.date._d.toString());
        $jq('.col-xs-6').has(this).addClass('active');
    });

    $jq('#datePickerEnd').datetimepicker({
      icons: {
        time: "fa fa-clock-o",
        date: "fa fa-calendar",
        up: "fa fa-arrow-up",
        down: "fa fa-arrow-down"
      },
      pickTime: false
    });

    $jq("#datePickerEnd").on("dp.change", function (e) {
      // filter activated
      angular.element($jq("#datePickerEnd")).scope().setEndDate(e.date._d.toString());
      $jq('.col-xs-6').has(this).addClass('active');
    });
}
