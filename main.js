var express = require('express');
var app = express();
var uid = require('uid2');
var cons = require('consolidate');
var path = require('path');
var favicon = require('serve-favicon');
var session = require('express-session');
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
app.use(session({
  genid: function(req) {
    return uid(30);
  },
  secret: uid(30),
  resave: true,
  saveUninitialized: true,
  cookie: {}
}))
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

// login.html
app.get('/login', function(req, res) {
    res.render('login')
});

app.post('/logout', function(req, res) {
   req.session = null
   res.end('SUCCESS')
});

// reset.html
app.get('/reset/:resetToken', function(req, res) {
  res.render('reset')
});


app.post('/login', function(req, res){
    var username = req.body.username
    var password = req.body.password
    var userCredentials = {username: username, password: password}
    needle.post('http://' + uaaHost + ':' + uaaPort + '/login.do', userCredentials,
       function(err, tokenResp) {
       console.log('set-cookie' + tokenResp.headers.location)
        var splittedLocation = tokenResp.headers.location.split('?')
        if (splittedLocation.length == 1 || splittedLocation[1] != 'error=true'){
            var cookies = tokenResp.headers['set-cookie'][0].split(';')
            var sessionId;
            for (var i = 0 ; i < cookies.length; i++) {
                var cookie = cookies[i].split('=')
                if (cookie.length == 2 && cookie[0] == 'JSESSIONID'){
                   sessionId = cookie[1]
                }
            }
            res.cookie('uaa_cookie', sessionId) // TODO check sessionId
            res.end('SUCCESS')
        } else {
            res.end('authentication failed')
        }
    });
});

app.get('/oauth/authorize', function(req, res){
    if (req.param('client_id') && req.param('response_type') && req.param('scope') && req.param('redirect_uri')){
        req.session.client_id = req.param('client_id')
        req.session.response_type = req.param('response_type')
        req.session.scope = req.param('scope')
        req.session.redirect_uri = req.param('redirect_uri')
        if (isUaaSession(req)) {
            res.redirect('/confirm')
        } else {
            res.redirect('/login')
        }
    } else {
        res.statusCode = 404
        res.send('Error 404 check client_id, response_type, scope and redirect_uri params')
    }
});

isUaaSession = function(req) {
    return (getCookie(req, 'uaa_cookie') != null)
}

parseCookies = function (request) {
    var list = {},
        rc = request.headers.cookie;
    rc && rc.split(';').forEach(function(cookie) {
        var parts = cookie.split('=');
        list[parts.shift().trim()] = unescape(parts.join('='));
    });
    return list;
}

getCookie = function(request, cookie) {
    return parseCookies(request)[cookie]
}

app.get('/confirm', function(req, res){
  if (isUaaSession(req)){
    // TODO: handle when there is
    var confirmParams = 'client_id=' + req.session.client_id
                        + '&response_type=' + req.session.response_type
                        + '&scope=' + req.session.scope
                        + '&redirect_uri=' + req.session.redirect_uri;
    var confirmOptions = {
                      headers: {
                        'Cookie': 'JSESSIONID=' + getCookie(req, 'uaa_cookie')
                         }
                  }
    needle.get('http://' + uaaHost + ':' + uaaPort + '/oauth/authorize?' + confirmParams, confirmOptions,
        function(err, confirmResp) {
            if (confirmResp.statusCode == 200){
                // TODO: set confirmation key
                console.log(JSON.stringify(confirmResp.body))
                res.end()
            } else if (confirmResp.statusCode == 302){
                res.cookie('JSESSIONID', getCookie(req, 'uaa_cookie'))
                res.redirect(confirmResp.headers.location)
            } else {

            }
        });
  } else {
     res.statusCode = 500
     res.send('Invalid state');
  }
});

app.post('/confirm', function(req, res){
    var choose = req.param('choose');
    if (choose == 'yes') {
        // TODO: rewrite it
        var confirmParams = {client_id: req.session.client_id, response_type: req.session.response_type,
                          scope: req.session.scope, redirect_uri: req.session.redirect_uri}
        var confirmOptions = {
                          headers: {
                            'Accept' : 'application/json',
                            'Set-Cookie': 'JSESSIONID=' + getCookie(req, 'uaa_cookie'),
                            'Content-Type' : 'application/json' }
                      }
        needle.post('http://' + uaaHost + ':' + uaaPort + '/oauth/authorize', confirmParams,
                    JSON.stringify(confirmOptions), function(err, confirmResp) {
                res.redirect(res.headers.location) // TODO check
        });
    } else {
        res.statusCode = 400
        res.end()
    }
});

app.post('/reset/:resetToken', function(req, res) {
    var resetToken = req.param('resetToken')
    var options = {
      headers: { 'Authorization': 'Basic ' + new Buffer(clientId + ':'+ clientSecret).toString('base64') }
    }
    needle.post('http://' + uaaHost + ':' + uaaPort + '/oauth/token', 'grant_type=client_credentials',
       options, function(err, tokenResp) {
       if (tokenResp.statusCode == 200){
          var token = tokenResp.body.access_token;
          var userOptions = {
              headers: {
                'Accept' : 'application/json',
                'scope': 'password.write',
                'aud' : 'password',
                'Authorization' : 'Bearer ' + token,
                'Content-Type' : 'application/json' }
          }
          var newPasswordData = {'password' : req.body.password}
          needle.put('http://' + uaaHost + ':' + uaaPort + '/Users/' + resetToken + '/password', JSON.stringify(newPasswordData),
            userOptions, function(err, resetResp) {
                if (resetResp.statusCode = 200){
                    res.end('SUCCESS');
                } else {
                    res.end('Password update failed.')
                }
            })
       } else {
          res.end('No token for client');
       }
   });
});

// forget for login
app.post('/forget', function(req, res){
    var userName = req.body.email
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
                needle.get('http://' + uaaHost + ':' + uaaPort + '/Users/?attributes=id,givenName&filter=userName eq "' + userName + '"', usrOptions , function(err, usrResp){
                    if (usrResp.statusCode == 200){
                        console.log(usrResp.body)
                        if (usrResp.body.resources.length == 1){
                            var templateFile = path.join(__dirname,'templates','reset-password-email.jade')
                            mailer.sendMail(req.body.email, 'Password reset' , templateFile, {user: usrResp.body.resources[0].givenName,
                                confirm: 'http://' +  process.env.UR_HOST + ':' + process.env.UR_PORT + '/reset/' + usrResp.body.resources[0].id })
                            res.end('SUCCESS');
                        } else {
                            res.end('User Not Found');
                        }
                    } else {
                       res.end('Could not access for database');
                    }
                });
            } else { res.end('No token for client'); }
        }
    );
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
