var express  = require('express');
var app = express();
var uid = require('uid2');
var sessionSecret = uid(30);
var server = require('http').Server(app);
var io = require('socket.io')(server);
var session = require('express-session');
var sessionStore = new session.MemoryStore();
var cookieParser = require('cookie-parser')(sessionSecret)
var sessionSocketIo = require('session.socket.io');
var sessionSockets = new sessionSocketIo(io, sessionStore, cookieParser);
var morgan = require('morgan');
var bodyParser = require('body-parser');
var methodOverride = require('method-override');
var restClient = require('node-rest-client');
var path = require('path');
var cons = require('consolidate');
var dns = require('dns');

// general config ==============================================================

app.engine('html', cons.underscore);

app.set('views', './app')
app.set('view engine', 'html')
app.use(express.static(path.join(__dirname, 'app/static')));
app.use(cookieParser);
app.use(session({
  genid: function(req) {
    return uid(30);
  },
  secret: sessionSecret,
  resave: true,
  saveUninitialized: true,
  cookie: {},
  store: sessionStore
}))
app.use(morgan('dev'));
app.use(bodyParser.urlencoded({'extended':'true', limit: '50mb'}));
app.use(bodyParser.json({ limit: '50mb' }));
app.use(bodyParser.json({ limit: '50mb', type: 'application/vnd.api+json' }));
app.use(methodOverride());

// oauth2 and identity server config ===========================================

var config = {
  environmentSet: true,
  clientId: process.env.ULU_OAUTH_CLIENT_ID,
  clientSecret: process.env.ULU_OAUTH_CLIENT_SECRET,
  clientScopes: 'openid' +
    '+cloudbreak.templates' +
    '+cloudbreak.credentials' +
    '+cloudbreak.blueprints' +
    '+cloudbreak.stacks' +
    '+periscope.cluster' +
    '+cloudbreak.events+cloudbreak.usages.account+cloudbreak.usages.user',
  hostAddress: process.env.ULU_HOST_ADDRESS,
  subscriptionAddress: null,
  identityServerAddress: null,
  sultansAddress: null,
  cloudbreakAddress: null,
  periscopeAddress: null,
}

function setResolvedAddress(configKey, resolvedAddress) {
  config[configKey] = resolvedAddress;
  console.log(configKey + " = " + resolvedAddress);
}

function resolveError(err) {
  console.log(err);
  config.environmentSet = false;
}

retryingResolve('http', 'ULU_PERISCOPE_SERVICEID', 'ULU_PERISCOPE_ADDRESS', 'periscopeAddress', setResolvedAddress, resolveError);
retryingResolve('http', 'ULU_IDENTITY_SERVICEID', 'ULU_IDENTITY_ADDRESS', 'identityServerAddress', setResolvedAddress, resolveError);
retryingResolve('http', 'ULU_CLOUDBREAK_SERVICEID', 'ULU_CLOUDBREAK_ADDRESS', 'cloudbreakAddress', setResolvedAddress, resolveError);
retryingResolve('http', 'ULU_SULTANS_SERVICEID', 'ULU_SULTANS_ADDRESS', 'sultansAddress', setResolvedAddress, resolveError);

if (!config.clientSecret || !config.hostAddress || !config.clientId) {
  console.log("ULU_HOST_ADDRESS, ULU_OAUTH_CLIENT_ID and ULU_OAUTH_CLIENT_SECRET must be specified!");
  config.environmentSet = false;
}

waitingForAddressesAndContinue();

function waitingForAddressesAndContinue() {
  if ((!config.periscopeAddress || !config.sultansAddress || !config.cloudbreakAddress || !config.identityServerAddress) && config.environmentSet) {
    setTimeout(waitingForAddressesAndContinue, 2000);
  } else if (config.environmentSet) {
    continueInit();
  } else {
    process.exit(1);
  }
}

function continueInit() {
  if (config.hostAddress.slice(-1) !== '/') {
    config.hostAddress += '/';
  }

  var redirectUri = config.hostAddress + 'authorize';

  var optionsAuth={user: config.clientId, password: config.clientSecret};
  var identityServerClient = new restClient.Client(optionsAuth);
  identityServerClient.registerMethod("retrieveToken", config.identityServerAddress + "oauth/token", "POST");

  var proxyRestClient = new restClient.Client();
  proxyRestClient.registerMethod("subscribe", config.cloudbreakAddress + "subscriptions", "POST");

  // cloudbreak config ===========================================================

  var cbRequestArgs = {
    headers:{
      "Content-Type": "application/json"
    }
  }

  // routes ======================================================================

  // info ========================================================================

  var pjson = require('./package.json');

  app.get('/info', function(req, res) {
      res.json({name: pjson.name, version: pjson.version, sultansAddress: config.sultansAddress});
  });

  // oauth  ======================================================================

  app.get('/authorize', function(req, res, next){
    var args = {
      headers:{
        "Content-Type": "application/x-www-form-urlencoded",
      },
      data:
        'grant_type=authorization_code'
        + '&redirect_uri=' + redirectUri
        + '&code=' + req.query.code
    }
    identityServerClient.methods.retrieveToken(args, function(data, response){
      if (response.statusCode != 200 || !data.access_token){
        var error = new Error("Couldn't retrieve access token from identity server.");
        error.status = 500;
        return next(error);
      }
      console.log(data)
      req.session.token=data.access_token;
      res.redirect(config.hostAddress);
    });
  });

  // cloudbreak notifications ====================================================

  app.post('/notifications', function(req, res, next){
    // console.log(req.body)
    if (req.body.owner){
      io.to(req.body.owner).emit('notification', req.body);
      res.send();
    } else {
      console.log('No username in request body, nothing to do.')
    }
  });

  // main page ===================================================================

  app.get('/', function(req, res) {
    if (config.subscriptionAddress == null) {
      subscribe()
    }
    var oauthFlowUrl = config.sultansAddress
        + 'oauth/authorize?response_type=code'
        + '&client_id=' + config.clientId
        + '&scope=' + config.clientScopes
        + '&redirect_uri=' + redirectUri
    if (!req.session.token){
      res.redirect(oauthFlowUrl)
    } else {
      res.render('index', { authorizeUrl : oauthFlowUrl });
    }
  });

  app.get('/logout', function(req, res){
      var sourceUrl = req.protocol + '://' + req.headers.host;
      var source = new Buffer(sourceUrl).toString('base64')
      req.session.destroy(function() {
          res.clearCookie('connect.sid', { path: '/' });
          res.clearCookie('JSESSIONID', { path: '/' });
          res.clearCookie('uaa_cookie', { path: '/' });
          res.redirect(config.sultansAddress + '?logout=true&source=' + source)
      })
  })

  app.get('/user', function(req,res) {
    retrieveUserByToken(req.session.token, function(data){
      res.json(data);
    });
  });

  function retrieveUserByToken(token, success){
    var requestArgs = {
      headers: {
        "Authorization": "Bearer " + token
      }
    }
    identityServerClient.get(config.identityServerAddress + "userinfo", requestArgs, function(data,response){
      success(data);
    });
  }

  // file download

  app.get('*/credentials/certificate/*', function(req, res){
    if (req.body){
      cbRequestArgs.data = req.body;
    }
    cbRequestArgs.headers.Authorization = "Bearer " + req.session.token;
    proxyRestClient.get(config.cloudbreakAddress + req.url, cbRequestArgs, function(data,response){
      if (data != null) {
        res.setHeader('Content-disposition', 'attachment; filename=azure.cer');
        res.setHeader('Content-type', 'application/x-x509-ca-cert');
        res.charset = 'UTF-8';
        res.write(data);
        res.end();
      } else {
         res.statusCode = 404
         res.json({ error: {status: 404, message: 'Cannot download azure certificate.'} })
      }
    });
  });

  // delete user

  app.delete('/users/:userId', function(req, res){
      var token = req.session.token
      var userId = req.param('userId')
      cbRequestArgs.headers.Authorization = "Bearer " + req.session.token;
      proxyRestClient.get(config.cloudbreakAddress + req.url + "/resources", cbRequestArgs, function(data,response){
          if (data === false) {
              proxyRestClient.delete(config.sultansAddress + 'users/' + userId, cbRequestArgs, function(data, response){
                res.status(response.statusCode).send(data);
              }).on('error', function(err){
                res.status(500).send("Uluwatu could not connect to Sultans.");
              });
          } else if (data === true){
              res.status(500).send("Delete owned resources first for user.");
          } else {
             res.status(response.statusCode).send(data);
          }
      }).on('error', function(err){
          res.status(500).send("Uluwatu could not connect to Cloudbreak.");
      });
  })

  function preventNoCachInResponse(res) {
    res.header("Cache-Control", "no-cache, no-store, must-revalidate");
    res.header("Pragma", "no-cache");
    res.header("Expires",0); 
  }

  // wildcards should be proxied =================================================

  app.get('*/periscope/*', function(req,res){
    preventNoCachInResponse(res);
    proxyPeriscopeRequest(req, res, proxyRestClient.get);
  });

  app.post('*/periscope/*', function(req,res){
    proxyPeriscopeRequest(req, res, proxyRestClient.post);
  });

  app.put('*/periscope/*', function(req,res){
    proxyPeriscopeRequest(req, res, proxyRestClient.put);
  });

  app.delete('*/periscope/*', function(req,res){
    proxyPeriscopeRequest(req, res, proxyRestClient.delete);
  });

  app.get('*/sultans/*', function(req,res){
      preventNoCachInResponse(res);
      proxySultansRequest(req, res, proxyRestClient.get);
  });

  app.get('*', function(req,res){
    preventNoCachInResponse(res);
    proxyCloudbreakRequest(req, res, proxyRestClient.get);
  });

  app.post('*/sultans/*', function(req,res){
      proxySultansRequest(req, res, proxyRestClient.post);
  });

  app.post('*', function(req,res){
    proxyCloudbreakRequest(req, res, proxyRestClient.post);
  });

  app.delete('*/sultans/*', function(req,res){
      proxySultansRequest(req, res, proxyRestClient.delete);
  });

  app.delete('*', function(req,res){
    proxyCloudbreakRequest(req, res, proxyRestClient.delete);
  });

  app.put('*/sultans/*', function(req,res){
      proxySultansRequest(req, res, proxyRestClient.put);
  });

  app.put('*', function(req,res){
    proxyCloudbreakRequest(req, res, proxyRestClient.put);
  });

  // proxy =======================================================================

  function proxyCloudbreakRequest(req, res, method){
    if (req.body){
      cbRequestArgs.data = req.body;
    }
    cbRequestArgs.headers.Authorization = "Bearer " + req.session.token;
    method(config.cloudbreakAddress + req.url, cbRequestArgs, function(data,response){
      res.status(response.statusCode).send(data);
    }).on('error', function(err){
      res.status(500).send("Uluwatu could not connect to Cloudbreak.");
    });
  }

  function proxySultansRequest(req, res, method){
      if (req.body){
          cbRequestArgs.data = req.body;
      }
      cbRequestArgs.headers.Authorization = "Bearer " + req.session.token;
      var req_url = req.url.replace("/sultans/", "");
      console.log("Sultans request to: "+ config.sultansAddress + req_url);
      method(config.sultansAddress + req_url, cbRequestArgs, function(data, response){
          res.status(response.statusCode).send(data);
      }).on('error', function(err){
        res.status(500).send("Uluwatu could not connect to Sultans.");
      });
  }

  function proxyPeriscopeRequest(req, res, method){
    if (req.body){
      cbRequestArgs.data = req.body;
    }
    cbRequestArgs.headers.Authorization = "Bearer " + req.session.token;
    var req_url = req.url.replace("/periscope/", "");
    console.log("Periscope request to: "+ config.periscopeAddress + req_url);
    method(config.periscopeAddress + req_url, cbRequestArgs, function(data, response){
      res.status(response.statusCode).send(data);
    }).on('error', function(err){
      res.status(500).send("Uluwatu could not connect to Periscope.");
    });
  }

  // subscription ======================================================================

  function subscribe() {
      console.log("Subscribing client to Cloudbreak notifications");
      var getClientTokenArgs = {
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        data: 'grant_type=client_credentials'
      }
      identityServerClient.methods.retrieveToken(getClientTokenArgs, function(data, response){
        if (response.statusCode != 200 || !data.access_token){
          console.log("Couldn't retrieve access token for Uluwatu, couldn't subscribe to Cloudbreak notifications.")
          console.log(data)
        } else {
          var subscribeArgs = {
            headers: {
               "Authorization": "Bearer " + data.access_token,
               "Content-Type": "application/json"
            },
            data: { "endpointUrl": config.hostAddress + "notifications" }
          }
          proxyRestClient.methods.subscribe(subscribeArgs, function(data, response){
            if (response.statusCode === 201 && data.id){
              console.log("Subscribed to Cloudbreak notifications. Subscription id: [" + data.id + "]");
              config.subscriptionAddress = config.hostAddress
            } else if (response.statusCode === 409){
              console.log("Subscription already exist.");
              config.subscriptionAddress = config.hostAddress
            } else {
              console.log("Something unexpected happened. Couldn't subscribe to Cloudbreak notifications.")
              console.log(data)
            }
          })
        }
      });
  }

  // socket ======================================================================

  sessionSockets.on('connection', function (err, socket, session) {
    if (session){
      retrieveUserByToken(session.token, function(data){
        socket.join(data.user_id)
      });
    } else {
      console.log("No session found, websocket notifications won't work [socket ID: " + socket.id + "] " + err)
    }
  });

  // errors  =====================================================================

  app.use(function(err, req, res, next){
    res.status(err.status);
    res.json({ error: {status: err.status, message: err.message} });
  });

  process.on('uncaughtException', function (err) {
    if (err.code == 'ECONNREFUSED' || err.code == 'ECONNRESET' || err.code == 'ENETUNREACH') {
       console.log('Exception error occurred: ' + err.code + ' when try to connect: ' + err.request.options.host + ':' + err.request.options.port + err.request.options.path);
   } else {
       console.log(err)
   }
  });

  // listen ======================================================================

  serverPort = process.env.ULU_SERVER_PORT ? process.env.ULU_SERVER_PORT : 3000;
  server.listen(serverPort);
  console.log("App listening on port " + serverPort);
}

function retryingResolve(protocol, serviceIdKey, serverUrlKey, configKey, resolveFn, errFn, attemptNum) {
  var maxRetry = 10;
  if (typeof(attemptNum)==='undefined') attemptNum = 0;
  resolve(protocol, serviceIdKey, serverUrlKey, configKey, resolveFn, function(err) {
    if (attemptNum < maxRetry) {
      setTimeout(retryingResolve(protocol, serviceIdKey, serverUrlKey, configKey, resolveFn, errFn, attemptNum+1), 2000);
    } else {
      errFn(err);
    }
  })
}

function resolve(protocol, serviceIdKey, serverUrlKey, configKey, resolveFn, errFn) {
  serviceId = process.env[serviceIdKey];
  serverUrl = process.env[serverUrlKey];
  if (serverUrl) {
    if (checkUrl(serverUrl)) {
      if (serverUrl.slice(-1) !== '/') {
        serverUrl += '/';
      }
      resolveFn(configKey, serverUrl);
    } else {
      errFn(serverUrlKey + " must be a standard URL: 'http[s]://host[:port]/, current value: " + serverUrl);
    }
  } else if (protocol && serviceId) {
    dns.resolve(serviceId, 'SRV', function onResolve(err, addresses) {
      if (err) {
        errFn('Error during resolving ' + serviceIdKey + ':' + serviceId + ': ' + err);
      } else if (!addresses || addresses.length == 0) {
        errFn(serviceIdKey + ':' + serviceId + ' cannot be resolved!');
      } else {
        resolveFn(configKey, protocol + "://" + addresses[0].name + ":" + addresses[0].port + "/");
      }
    });
  } else {
    errFn(serviceIdKey + ' or ' + serverUrlKey + ' must be specified!');
  }
}

function checkUrl(serviceUrl) {
  return serviceUrl && (serviceUrl.substring(0, 7) == "http://" || serviceUrl.substring(0, 8) == "https://");
}
