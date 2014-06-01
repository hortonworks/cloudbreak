			$(document).ready(function() {
				
// copy credential menu selection to menu label, remove alert color and enable create cluster button
				$("#menu-credential ul li a").click(function() {
  				if ( !($(this).hasClass( "not-option" ) ) ) { 
						$(this).parents("#menu-credential").find('.dropdown-toggle span').text($(this).text()).removeClass('text-danger');
						$('#create-cluster-btn').removeClass('disabled');
					}
				});
				
// switch cluster title to normal word break if title has space in the middle 
				$(".cluster h4 .btn-cluster").each(function() {
					var s = $(this).text().trim();
					if ( s.indexOf(" ") > -1 && ( s.indexOf(" ") > 4 || s.lastIndexOf(" ") < 15 ) ) {
						$(this).css( "word-break", "normal" );
					}
				});
				
// initialize Isotope with sort and filter
				var qsRegex;
				var $container = $('.isotope-wrapper').isotope({
					itemSelector: '.cluster',
					layoutMode: 'masonry',
					masonry: {
						columnWidth: 156,
						gutter: 0
					},
					filter: function() {
						return qsRegex ? $(this).find('h4 a').text().match( qsRegex ) : true;
					},
					getSortData: {
						// cluster name
						name: function( itemElem ) {
							var str = $( itemElem ).find('h4 a').text();
							return str.toLowerCase();
						},
						// cluster node number
						nodes: function( itemElem ) { 
							var str = $( itemElem ).find('.mod-nodes dd').text();
							return parseInt( str );
						},
						// cluster state 
						state: '[data-state]',
						// cluster uptime
						uptime: function( itemElem ) {
							var str = $( itemElem ).find('.mod-uptime dd').text();
							return parseInt( str );
						}
					}
				});
				$container.isotope();
				// sorting
				$('#sort-clusters-btn + ul li').on( 'click', 'a', function(e) {
					var sortByValue = $(this).attr('data-sort-by');
					var sortAsc = $(this).attr('data-sort-asc');
					$container.isotope({ sortBy: sortByValue, sortAscending: sortAsc });
					// set button label to selected sort mode
					$("#sort-clusters-btn span.title").text( sortByValue );
					// disable selected menu item
					$(this).parents().find('.disabled').removeClass( "disabled" );
					$(this).parent().addClass( "disabled" );
				});
				// filtering
				var $quicksearch = $('#notification-n-filtering').keyup( debounce( function() {
						qsRegex = new RegExp( $quicksearch.val(), 'gi' );
						$container.isotope();
					}, 300 ) );
				// debounce so filtering doesn't happen every millisecond
				function debounce( fn, threshold ) {
					var timeout;
					return function debounced() {
						if ( timeout ) {
							clearTimeout( timeout );
						}
						function delayed() {
							fn();
							timeout = null;
						}
						timeout = setTimeout( delayed, threshold || 100 );
					}
				}
// notification/filter field clearing on focus for filtering
				$( "#notification-n-filtering" ).focusin(function() {
					$(this).val("").trigger( "keyup" )
						// delete warning sign
						.parent().find('> i').remove();
					// delete error classes
					$(this).parent().parent().removeClass('has-feedback').removeClass('has-error');
				});
				
// cluster-block hide/show
				$('#toggle-cluster-block-btn').click(function() {
					$('.cluster-block').collapse('toggle');
					// must force isotope redraw, its container height set 0 by some fucking shite
					$container.isotope();
				});
				$('.cluster-block').on('hidden.bs.collapse', function() {
  				$('#toggle-cluster-block-btn i').removeClass('fa-angle-up').addClass('fa-angle-down');
					$('#sort-clusters-btn').addClass('disabled');
				});
				$('.cluster-block').on('shown.bs.collapse', function() {
  				$('#toggle-cluster-block-btn i').removeClass('fa-angle-down').addClass('fa-angle-up');
					$('#sort-clusters-btn').removeClass('disabled');
				});
				
// Bootstrap carousel as clusters / cluster details / create cluster slider
				// init
				$('.carousel').carousel('pause');
				// show cluster details
				$(".cluster h4 .btn-cluster").click(function() {
					$('.carousel').carousel(1);
					$('#toggle-cluster-block-btn').addClass('disabled');
					$('#sort-clusters-btn').addClass('disabled');
					$("#notification-n-filtering").prop( "disabled", true );
				});
				// back to clusters
				$("#cluster-details-back-btn").click(function() {
					$('.carousel').carousel(0);
					// must force isotope redraw, its container height set 0 by by some fucking shite
					$container.isotope();
					$('#toggle-cluster-block-btn').removeClass('disabled');
					$('#sort-clusters-btn').removeClass('disabled');
					$("#notification-n-filtering").prop( "disabled", false );
				});
				// show create cluster panel
				$('#create-cluster-btn').click(function() {
					$('.carousel').carousel(2);
					$(this).addClass('disabled');
					$('#toggle-cluster-block-btn').addClass('disabled');
					$('#sort-clusters-btn').addClass('disabled');
					$("#notification-n-filtering").prop( "disabled", true );
				});
				// back to clusters
				$("#create-cluster-back-btn").click(function() {
					$('.carousel').carousel(0);
					// must force isotope redraw, .isotope-wrapper's height set 0 by by some fucking shite
					$container.isotope();
					$('#toggle-cluster-block-btn').removeClass('disabled');
					$('#create-cluster-btn').removeClass('disabled');
					$('#sort-clusters-btn').removeClass('disabled');
					$("#notification-n-filtering").prop( "disabled", false );
				});
				
// main template/blueprint/credential panels show hide
				$('#panel-templates-collapse').on('hidden.bs.collapse', function() {
  				$(this).parent().find('.panel-heading .btn i').removeClass('fa-angle-up').addClass('fa-angle-down');
				});
				$('#panel-templates-collapse').on('shown.bs.collapse', function() {
  				$(this).parent().find('.panel-heading .btn i').removeClass('fa-angle-down').addClass('fa-angle-up');
				});
// create panels
				$('.panel-under-btn-collapse').on('shown.bs.collapse', function() {
  				$(this).parent().prev()
						.find('.btn').fadeTo("fast", 0, function() { 
							$(this).removeClass('btn-success').addClass('btn-info')
							.find('i').removeClass('fa-plus').addClass('fa-times').removeClass('fa-fw')
							.parent().find('span').addClass('hidden');
							$(this).fadeTo("slow", 1);
					});
				});
				$('.panel-under-btn-collapse').on('hidden.bs.collapse', function() {
  				$(this).parent().prev()
						.find('.btn').fadeTo("fast", 0, function() { 
							$(this).removeClass('btn-info').addClass('btn-success')
							.find('i').removeClass('fa-times').addClass('fa-plus').addClass('fa-fw')
							.parent().find('span').removeClass('hidden');
							$(this).fadeTo("slow", 1);
					});
				});			
			});