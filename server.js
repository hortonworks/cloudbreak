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
app.use(bodyParser.urlencoded({'extended':'true'}));
app.use(bodyParser.json());
app.use(bodyParser.json({ type: 'application/vnd.api+json' }));
app.use(methodOverride());

// oauth2 and identity server config ===========================================

var environmentSet = true;
var clientId = process.env.ULU_OAUTH_CLIENT_ID;
var clientSecret = process.env.ULU_OAUTH_CLIENT_SECRET;
var identityServerAddress = process.env.ULU_IDENTITY_ADDRESS
var sultansAddress = process.env.ULU_SULTANS_ADDRESS
var cloudbreakAddress = process.env.ULU_CLOUDBREAK_ADDRESS
var hostAddress = process.env.ULU_HOST_ADDRESS;
var clientScopes = 'openid' +
    '+cloudbreak.templates' +
    '+cloudbreak.credentials' +
    '+cloudbreak.blueprints' +
    '+cloudbreak.stacks' +
    '+cloudbreak.events+cloudbreak.usages.account+cloudbreak.usages.user';

if (!clientSecret || !hostAddress || !clientId) {
  console.log("ULU_HOST_ADDRESS, ULU_OAUTH_CLIENT_ID and ULU_OAUTH_CLIENT_SECRET must be specified!");
  environmentSet = false;
}

if (!identityServerAddress || (identityServerAddress.substring(0, 7) !== "http://" && identityServerAddress.substring(0, 8) !== "https://")){
  console.log("ULU_IDENTITY_ADDRESS must be specified and it must be a standard URL: 'http[s]://host[:port]/'");
  environmentSet = false;
}

if (!cloudbreakAddress || (cloudbreakAddress.substring(0, 7) !== "http://" && cloudbreakAddress.substring(0, 8) !== "https://")){
  console.log("ULU_CLOUDBREAK_ADDRESS must be specified and it must be a standard URL: 'http[s]://host[:port]/'");
  environmentSet = false;
}

if (!sultansAddress || (sultansAddress.substring(0, 7) !== "http://" && sultansAddress.substring(0, 8) !== "https://")){
  console.log("ULU_SULTANS_ADDRESS must be specified and it must be a standard URL: 'http[s]://host[:port]/'");
  environmentSet = false;
}

if (!environmentSet) {
  process.exit(1);
}

if (hostAddress.slice(-1) !== '/') {
  hostAddress += '/';
}

if (identityServerAddress.slice(-1) !== '/') {
  identityServerAddress += '/';
}

if (cloudbreakAddress.slice(-1) !== '/') {
  cloudbreakAddress += '/';
}

if (sultansAddress.slice(-1) !== '/') {
  sultansAddress += '/';
}

var redirectUri = hostAddress + 'authorize';

var optionsAuth={user: clientId, password: clientSecret};
var identityServerClient = new restClient.Client(optionsAuth);
identityServerClient.registerMethod("retrieveToken", identityServerAddress + "oauth/token", "POST");

var cloudbreakClient = new restClient.Client();
cloudbreakClient.registerMethod("subscribe", cloudbreakAddress + "subscriptions", "POST");

var sultansClient = new restClient.Client();

// cloudbreak config ===========================================================

var cbRequestArgs = {
  headers:{
    "Content-Type": "application/json"
  }
}

// routes ======================================================================

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
    res.redirect('/');
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
  var oauthFlowUrl = sultansAddress
      + 'oauth/authorize?response_type=code'
      + '&client_id=' + clientId
      + '&scope=' + clientScopes
      + '&redirect_uri=' + redirectUri
  if (!req.session.token){
    res.redirect(oauthFlowUrl)
  } else {
    res.render('index', { authorizeUrl : oauthFlowUrl });
  }
});

app.get('/logout', function(req, res){
    req.session.destroy(function() {
        res.clearCookie('connect.sid', { path: '/' });
        res.clearCookie('JSESSIONID', { path: '/' });
        res.clearCookie('uaa_cookie', { path: '/' });
        res.redirect(sultansAddress + '?logout=true')
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
  identityServerClient.get(identityServerAddress + "userinfo", requestArgs, function(data,response){
    success(data);
  });
}

// wildcards should be proxied =================================================

app.get('*/sultans/*', function(req,res){
    proxySultansRequest(req, res, cloudbreakClient.get);
});

app.get('*', function(req,res){
  proxyCloudbreakRequest(req, res, cloudbreakClient.get);
});

app.post('*/sultans/*', function(req,res){
    proxySultansRequest(req, res, cloudbreakClient.post);
});

app.post('*', function(req,res){
  proxyCloudbreakRequest(req, res, cloudbreakClient.post);
});

app.delete('*/sultans/*', function(req,res){
    proxySultansRequest(req, res, cloudbreakClient.delete);
});

app.delete('*', function(req,res){
  proxyCloudbreakRequest(req, res, cloudbreakClient.delete);
});

app.put('*/sultans/*', function(req,res){
    proxySultansRequest(req, res, cloudbreakClient.put);
});

app.put('*', function(req,res){
  proxyCloudbreakRequest(req, res, cloudbreakClient.put);
});

// proxy =======================================================================

function proxyCloudbreakRequest(req, res, method){
  if (req.body){
    cbRequestArgs.data = req.body;
  }
  cbRequestArgs.headers.Authorization = "Bearer " + req.session.token;
  method(cloudbreakAddress + req.url, cbRequestArgs, function(data,response){
    res.status(response.statusCode).send(data);
  });
}

function proxySultansRequest(req, res, method){
    if (req.body){
        cbRequestArgs.data = req.body;
    }
    cbRequestArgs.headers.Authorization = "Bearer " + req.session.token;
    var req_url = req.url.replace("/sultans/", "");
    method(sultansAddress + req_url, cbRequestArgs, function(data, response){
        res.status(response.statusCode).send(data);
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

// listen ======================================================================

serverPort = process.env.ULU_SERVER_PORT ? process.env.ULU_SERVER_PORT : 3000;
server.listen(serverPort);
console.log("App listening on port " + serverPort);

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
      data: { "endpointUrl": hostAddress + "notifications" }
    }
    cloudbreakClient.methods.subscribe(subscribeArgs, function(data, response){
      if (response.statusCode === 201 && data.id){
        console.log("Subscribed to Cloudbreak notifications. Subscription id: [" + data.id + "]");
      } else if (response.statusCode === 409){
        console.log("Subscription already exist.");
      } else {
        console.log("Something unexpected happened. Couldn't subscribe to Cloudbreak notifications.")
        console.log(data)
      }
    })
  }
});
