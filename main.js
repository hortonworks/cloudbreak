var express = require('express');
var app = express();
var cons = require('consolidate');
var path = require('path');
var favicon = require('serve-favicon');
var bodyParser = require('body-parser');
var needle = require('needle');
var check = require('validator').check;

var mailer = require('./mailer');

var uaaHost = process.env.UAA_HOST;
var uaaPort = process.env.UAA_PORT;

var clientId = process.env.UR_CLIENT_ID;
var clientSecret = process.env.UR_CLIENT_SECRET;

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

app.post('/register', function(req, res){
    var result = 'FAILED'
    var options = {
        headers: { 'Authorization': 'Basic ' + new Buffer(clientId + ':'+ clientSecret).toString('base64') }
    }
    needle.post('http://' + uaaHost + ':' + uaaPort + '/oauth/token', 'grant_type=client_credentials',
        options, function(err, tokenResp) {
        if (tokenResp.statusCode == 200){
            var token = tokenResp.body.access_token;
            var regOptions = {
                headers: {
                    'Accept' : 'application/json',
                    'scope': 'scim.write',
                    'aud' : 'scim',
                    'Authorization' : 'Bearer ' + token,
                    'Content-Type' : 'application/json' }
            }
            var userData = {
                'schemas' : ["urn:scim:schemas:core:1.0"],
                'userName' : req.body.email,
                'password' : req.body.password,
                'active' : false,
                'name' : {
                    'familyName': req.body.lastName,
                    'givenName' : req.body.firstName
                },
                'emails':[
                      {
                          'value': req.body.email
                      }
                ]
            }
            needle.post('http://' + uaaHost + ':' + uaaPort + '/Users', JSON.stringify(userData), regOptions, function(err, createResp) {
                if (createResp.statusCode == 201) {
                    console.log(createResp.body.id)
                    var templateFile = path.join(__dirname,'templates','confirmation-email.jade')
                    mailer.sendMail(req.body.email, 'Registration' , templateFile, {user: req.body.firstName,
                        confirm: 'http://' +  process.env.UR_HOST + ':' + process.env.UR_PORT + '/confirm/' + createResp.body.id})
                    result = 'SUCCESS'
                    res.end(result)
                } else {
                    res.end(result)
                }
            })
        } else {
            res.end(result)
        }
    });
});

app.get('/confirm/:confirm_token', function(req, res){
   var confirmToken = req.param("confirm_token")
   var result = 'FAILED'
   var options = {
     headers: { 'Authorization': 'Basic ' + new Buffer(clientId + ':'+ clientSecret).toString('base64') }
   }
   needle.post('http://' + uaaHost + ':' + uaaPort + '/oauth/token', 'grant_type=client_credentials',
           options, function(err, tokenResp) {
        if (tokenResp.statusCode == 200){
            var token = tokenResp.body.access_token;
            var usrOptions = {
              headers: {
                'Accept' : 'application/json',
                'scope': 'scim.read',
                'aud' : 'scim',
                'Authorization' : 'Bearer ' + token,
                'Content-Type' : 'application/json' }
            }
            needle.get('http://' + uaaHost + ':' + uaaPort + '/Users/' + confirmToken,
                   usrOptions, function(err, userResp) {
                   if (userResp.statusCode == 200) {
                    if (confirmToken == userResp.body.id) {
                        var updateOptions = {
                           headers: {
                            'Accept' : 'application/json',
                            'scope': 'scim.write',
                            'aud' : 'scim',
                            'Authorization' : 'Bearer ' + token,
                            'Content-Type' : 'application/json',
                            'If-Match': userResp.body.meta.version}
                        }
                        var userData = {
                           'userName' : userResp.body.userName,
                           'active' : true,
                           'name' : {
                              'familyName': userResp.body.name.familyName,
                              'givenName' : userResp.body.name.givenName
                            },
                            'emails':[
                             {
                               'value': userResp.body.emails[0].value
                             }
                             ]
                        }
                        needle.put('http://' + uaaHost + ':' + uaaPort + '/Users/' + confirmToken, JSON.stringify(userData),
                        updateOptions, function(err, updateResp){
                            result = 'SUCCESS'
                            res.end(result)
                        });
                    } else {
                     res.end(result)
                    }
                   } else {
                    res.end(result)
                   }
            });
        } else {
          res.end(result)
        }
   });

});

// errors
app.use(function(err, req, res, next){
  res.status(err.status);
  res.json({ error: {status: err.status, message: err.message} });
});

// listen
var port = process.env.UR_PORT || 8080;
server = app.listen(port);

console.log('Server listening on port %d in %s mode', server.address().port, app.settings.env);
