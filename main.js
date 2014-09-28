var express = require('express');
var app = express();
var cons = require('consolidate');
var path = require('path');
var favicon = require('serve-favicon');
var bodyParser = require('body-parser');
var check = require('validator').check;

var mailer = require('./mailer');

var uaaHost = process.env.UAA_HOST;
var uaaPort = process.env.UAA_PORT;

console.log("UAA server location: %s:%d", uaaHost, uaaPort)

app.engine('html', cons.underscore);

app.set('views', './app')
app.set('view engine', 'html')
app.use(express.static(path.join(__dirname, 'public')));
app.use(favicon(path.join(__dirname,'public','img','favicon.ico')));
app.use(bodyParser.json())

// index.html
app.get('/', function(req, res) {
  res.render('index',
  {
   emailErrorMsg: 'invalid email address',
   passwordErrorMsg: 'password is invalid (6 to 30 char)',
   confirmPasswordErrorMsg: 'passwords do not match!',
   firstNameErrorMsg: 'first name is empty',
   lastNameErrorMsg: 'last name is empty',
   companyErrorMsg: 'company name is empty'
   })
});

app.post('/registration', function(req, res){
    var templateFile = path.join(__dirname,'templates','confirmation-email.jade')
    mailer.sendMail(req.body.email, 'Registration' , templateFile, {user: req.body.firstName, confirm: 'link'})
    res.end('SUCCESS');
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
