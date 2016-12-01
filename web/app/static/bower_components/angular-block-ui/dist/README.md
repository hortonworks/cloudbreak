angular-block-ui
============
An AngularJS module that allows you to block user interaction on AJAX requests. Blocking is done automatically for each http request and/or manually via an injectable service. 

#### Dependencies
Besides AngularJS (~1.2.4), none.  

#### Demos
Live demos can be found on the [block-ui website](http://angular-block-ui.nullest.com) or by executing the website included in the [GitHub project](https://github.com/McNull/angular-block-ui) .

#### Breaking Changes
There are two breaking changes for users upgrading from `0.0.x` to `0.1.x`.

1. The `blockUIConfig` is no longer a provider instance but a plain simple javascript object.
2. The markdown has been simplified.

#### Installation
Either copy the contents of the `dist` directory of the [Github](https://github.com/McNull/angular-block-ui) project or install with _bower_ from the command line (**recommended**):
    
    bower install angular-block-ui

Include both the JS and CSS file in your html:

    <link rel="stylesheet" href="path-to-block-ui/angular-block-ui.min.css"/>
    <!-- After AngularJS -->
	<script src="path-to-block-ui/angular-block-ui.min.js"></script>
Create a dependency on `blockUI` in your main Angular module:

    angular.module('myApp', ['blockUI'])

#### Usage
By default the module will block the user interface on each pending request made from the browser. This behaviour can be modified in the configuration.
 
It's also possible to do the blocking manually. The blockUI module exposes a service by the same name. Access to the service is gained by injecting it into your controller or directive:

    angular.module('myApp').controller('MyController', function($scope, blockUI) {
      // A function called from user interface, which performs an async operation.
      $scope.onSave = function(item) {
    
        // Block the user interface
        blockUI.start();

        // Perform the async operation    
        item.$save(function() {
      
          // Unblock the user interface
          blockUI.stop();
          
        });
      };
    });

BlockUI service methods
=======================

#### start
The start method will start the user interface block. Because multiple user interface elements can request a user interface block at the same time, the service keeps track of the number of start calls. Each call to `start()` will increase the count and every call to `stop()` will decrease the value. Whenever the count reaches 0 the block will end.

*Note: By default the block is immediately active after calling this method, but to prevent trashing the user interface each time a button is pressed, the block is visible after a short delay. This behaviour can be modified in the configuration.*

**Arguments:**

* **message** (string)
Indicates the message to be shown in the overlay. If none is provided, the default message from the configuration is used.

#### stop
This will decrease the block count. The block will end if the count is 0.

#### reset
The reset will force an unblock by setting the block count to 0.

#### message
Allows the message shown in the overlay to be updated while to block is active.

#### done
Queues a callback function to be called when the block has finished. This can be useful whenever you wish to redirect the user to a different location while there are still pending AJAX requests.

**Arguments:**

* **callback** (function)
The callback function to queue.

Blocking individual elements
============================

Instead of blocking the whole page, it's also possible to block individual elements. Just like the main `blockUI` service, this can be done either manually or automatically. Elements can be made _block ui enabled_ by wrapping them in a `block-ui` element.

#### Manual blocking
```
<div block-ui="myBlockUI">
  <p> ... I'm blockable ... </p>
</div>
```

The `block-ui` directive takes an optional value, which can be used to get an instance of the associated `blockUI` service.

```
// Get the reference to the block service.
var myBlockUI = blockUI.instances.get('myBlockUI');

// Start blocking the element.
myBlockUI.start();

$timeout(function() {
  // Stop the block after some async operation.
  myBlockUI.stop();
}, 1000);
```
#### Automatic blocking

Automatic blocking elements can be done by providing the `block-ui` directive a `block-ui-pattern` attribute. This attribute should contain a valid regular expression, which indicates the requests that are associated with the specific element.

```
<!-- Initiated the UI block whenever a request to '/api/quote' is performed -->
<div block-ui block-ui-pattern="/^\/api\/quote($|\/).*/"></div>
  <p> ... I'm blockable ... </p>
</div>
```

BlockUI module configuration
============================

The configuration of the BlockUI module can be modified via the `blockUIConfig` during the config phase of your Angular application:

    angular.module('myApp').config(function(blockUIConfig) {
      
      // Change the default overlay message
      blockUIConfig.message = 'Please stop clicking!';
      
      // Change the default delay to 100ms before the blocking is visible
      blockUIConfig.delay = 100;
      
    });

### Properties

#### message
Changes the default message to be used when no message has been provided to the *start* method of the service. Default value is *'Loading ...'*.

    // Change the default overlay message
    blockUIConfig.message = 'Please wait';

#### delay
Specifies the amount in milliseconds before the block is visible to the user. By delaying a visible block your application will appear more responsive. The default value is *250*.

    // Change the default delay to 100ms before the blocking is visible ...
    blockUIConfig.delay = 100;
    
    // ... or completely remove the delay
    blockUIConfig.delay = 0;
    
#### template
Specifies a custom template to use as the overlay.

    // Provide a custom template to use
    blockUIConfig.template = '<pre><code>{{ state | json }}</code></pre>';

#### templateUrl
Specifies a url to retrieve the template from. *The current release only works with pre-cached templates, which means that this url should be known in the $templateCache service of Angular. If you're using the grunt with html2js or angular-templates, which I highly recommend, you're already set.*

    // Provide the custom template via a url
    blockUIConfig.templateUrl = 'my-templates/block-ui-overlay.html';

#### autoBlock
By default the BlockUI module will start a block whenever the Angular *$http* service has an pending request. If you don't want this behaviour and want to do all the blocking manually you can change this value to *false*.

    // Disable automatically blocking of the user interface
    blockUIConfig.autoBlock = false;

#### resetOnException
By default the BlockUI module will reset the block count and hide the overlay whenever an exception has occurred. You can set this value to *false* if you don't want this behaviour.

    // Disable clearing block whenever an exception has occurred
    blockUIConfig.resetOnException = false;
    
#### requestFilter
Allows you to specify a filter function to exclude certain ajax requests from blocking the user interface. The function is passed the [Angular request config object](http://docs.angularjs.org/api/ng/service/$http). The blockUI service will ignore requests when the function returns `false`.

	// Tell the blockUI service to ignore certain requests
    blockUIConfig.requestFilter = function(config) {
	  // If the request starts with '/api/quote' ...
      if(config.url.match(/^\/api\/quote($|\/).*/)) {
        return false; // ... don't block it.
      }
    };
    
If the filter function returns a _string_ it will be passed as the `message` argument to the `start` method of the service.

    // Change the displayed message based on the http verbs being used.    
    blockUIConfig.requestFilter = function(config) {
    
	  var message;
	
	  switch(config.method) {
        case 'GET':
          message = 'Getting ...';
          break;
          
        case 'POST':
          message = 'Posting ...';
          break;

	    case 'DELETE':
          message = 'Deleting ...';
          break;

	    case 'PUT':
          message = 'Putting ...';
          break;
	  };
	  
	  return message;
	  
	};


#### autoInjectBodyBlock
When the module is started it will inject the _main block element_ by adding the `block-ui` directive to the `body` element.

    <body block-ui="main">
    </body>
    
This behaviour can be disabled if there no need for any _fullscreen_ blocking or if there's more control required. For instance when your `ng-app` directive is a child element of the `body` element it is impossible for the `blockUI` resolve the main instance. In such a case the auto injection of the main block scope should be disabled and the main block element should be relocated.

	// Disable auto body block
    blockUIConfig.autoInjectBodyBlock = false;

	<div ng-app="myApp">
		<div block-ui="main" class="block-ui-main"></div>
	</div>
	
More information and an example can be found in this [plunker](http://plnkr.co/edit/F9UauI?p=preview).

#### cssClass
A string containing the default css classes, separated by spaces, that should be applied to each block-ui element. The default value is `'block-ui block-ui-anim-fade'`. 

If this needs to be overridden for a certain element; set the desired classes on the element including the `block-ui` class. This way the directive will not apply the configured classes to the element.

    blockUIConfig.cssClass = 'block-ui my-custom-class'; // Apply these classes to al block-ui elements
