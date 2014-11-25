var express  = require('express');
var uid = require('uid2');
var session = require('express-session');
var morgan = require('morgan');
var bodyParser = require('body-parser');
var methodOverride = require('method-override');
var restClient = require('node-rest-client');
var path = require('path');
var cons = require('consolidate');

var app = express();
var cloudbreakClient = new restClient.Client();
var sultansClient = new restClient.Client();


// general config ==============================================================

app.engine('html', cons.underscore);

app.set('views', './app')
app.set('view engine', 'html')
app.use(express.static(path.join(__dirname, 'app/static')));

app.use(session({
  genid: function(req) {
    return uid(30);
  },
  secret: uid(30),
  resave: true,
  saveUninitialized: true,
  cookie: {}
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
var redirectUri = process.env.ULU_OAUTH_REDIRECT_URI;
var clientScopes = 'openid' +
    '+cloudbreak.templates' +
    '+cloudbreak.credentials' +
    '+cloudbreak.blueprints' +
    '+cloudbreak.stacks' +
    '+cloudbreak.events+cloudbreak.usages.account+cloudbreak.usages.user';
var identityServerAddress = process.env.ULU_IDENTITY_ADDRESS
var sultansAddress = process.env.ULU_SULTANS_ADDRESS
var cloudbreakAddress = process.env.ULU_CLOUDBREAK_ADDRESS

if (!clientSecret || !redirectUri || !clientId) {
  console.log("ULU_CLIENT_SECRET and ULU_OAUTH_REDIRECT_URI and ULU_OAUTH_CLIENT_ID must be specified!");
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

if (identityServerAddress.slice(-1) !== '/') {
  identityServerAddress += '/';
}

if (cloudbreakAddress.slice(-1) !== '/') {
  cloudbreakAddress += '/';
}

if (sultansAddress.slice(-1) !== '/') {
  sultansAddress += '/';
}

var optionsAuth={user: clientId, password: clientSecret};
var identityServerClient = new restClient.Client(optionsAuth);
identityServerClient.registerMethod("retrieveToken", identityServerAddress + "oauth/token", "POST");

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
    req.session.token=data.access_token;
    res.redirect('/');
  });
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
  var requestArgs = {
    headers:{
      "Authorization": "Bearer " + req.session.token,
    }
  }
  identityServerClient.get(identityServerAddress + "userinfo", requestArgs, function(data,response){
    res.json(data);
  });
});

// wildcards should be proxied =================================================

app.get('*', function(req,res){
  proxyCloudbreakRequest(req, res, cloudbreakClient.get);
});

app.post('*/sultans/invite', function(req,res){
    proxySultansRequest(req, res, cloudbreakClient.post);
});

app.post('*', function(req,res){
  proxyCloudbreakRequest(req, res, cloudbreakClient.post);
});

app.delete('*', function(req,res){
  proxyCloudbreakRequest(req, res, cloudbreakClient.delete);
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
    method(sultansAddress + req_url, cbRequestArgs, function(data,response){
        res.status(response).send(data);
    });
}

// errors  =====================================================================

app.use(function(err, req, res, next){
  res.status(err.status);
  res.json({ error: {status: err.status, message: err.message} });
});

// listen ======================================================================

serverPort = process.env.ULU_SERVER_PORT ? process.env.ULU_SERVER_PORT : 3000;
app.listen(serverPort);
console.log("App listening on port " + serverPort);
