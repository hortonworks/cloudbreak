var express = require('express');
var app = express();
var cons = require('consolidate');
var path = require('path');
var favicon = require('serve-favicon');
var check = require('validator').check;

var uaaHost = process.env.UAA_HOST;
var uaaPort = process.env.UAA_PORT;


console.log("UAA server location: %s:%d", uaaHost, uaaPort)

app.engine('html', cons.underscore);

app.set('views', './app')
app.set('view engine', 'html')
app.use(express.static(path.join(__dirname, 'public')));
app.use(favicon(path.join(__dirname,'public','img','favicon.ico')));

// index.html
app.get('/', function(req, res) {
  res.render('index')
});

// errors
app.use(function(err, req, res, next){
  res.status(err.status);
  res.json({ error: {status: err.status, message: err.message} });
});

// listen
var port = process.env.SEQ_REG_PORT || 8080;
server = app.listen(port);

console.log('Server listening on port %d in %s mode', server.address().port, app.settings.env);
