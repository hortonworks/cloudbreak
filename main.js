var express = require('express');
var app = express();
var uid = require('uid2');
var cons = require('consolidate');
var path = require('path');
var favicon = require('serve-favicon');
var session = require('express-session');
var bodyParser = require('body-parser');
var needle = require('needle');
var md5 = require('MD5');

var domain = require('domain'),
d = domain.create();

var mailer = require('./mailer');
var validator = require('./validator');

var uaaAddress = process.env.SL_UAA_ADDRESS;

var clientId = process.env.SL_CLIENT_ID;
var clientSecret = process.env.SL_CLIENT_SECRET;

console.log("UAA server location: %s", uaaAddress)

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
app.use(bodyParser.urlencoded({ extended: false }))
app.use(bodyParser.json())

// login.html
app.get('/', function(req, res) {
    var logout = req.query.logout
    if (logout != null && logout == 'true'){
        req.session.destroy(function() {
            res.clearCookie('connect.sid', { path: '/' });
            res.clearCookie('JSESSIONID', { path: '/' });
            res.clearCookie('uaa_cookie', { path: '/' });
            res.render('login',{ errorMessage: "" });
        })
    } else {
        res.render('login',{ errorMessage: "" });
    }
});

app.get('/dashboard', function(req, res) {
    res.render('dashboard',
    {
       cloudbreakAddress: process.env.SL_CB_ADDRESS
    })
});

// register.html
app.get('/register', function(req, res) {
  res.render('register',
  {
   emailErrorMsg: 'invalid email address',
   passwordErrorMsg: 'password is invalid (6 to 200 char)',
   confirmPasswordErrorMsg: 'passwords do not match!',
   firstNameErrorMsg: 'first name is empty',
   lastNameErrorMsg: 'last name is empty',
   companyErrorMsg: 'company name is empty'
   })
});

// reset.html
app.get('/reset/:resetToken', function(req, res) {
  res.render('reset')
});


app.post('/', function(req, res){
    var username = req.body.email
    var password = req.body.password
    var userCredentials = {username: username, password: password}
    needle.post(uaaAddress + '/login.do', userCredentials,
       function(err, tokenResp) {
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
            if (req.session.client_id == null) {
                res.redirect('dashboard')
            } else {
                //res.end('SUCCESS')
                res.redirect('confirm')
            }
        } else {
            //res.end('Authentication failed.')
            res.render('login',{ errorMessage: "The email or password you entered is incorrect." });
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
            res.redirect('/')
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
    var confirmParams = 'client_id=' + req.session.client_id
                        + '&response_type=' + req.session.response_type
                        + '&scope=' + req.session.scope;
                        + '&redirect_uri=' + req.session.redirect_uri;
    var confirmOptions = {
                      headers: {
                        'Cookie': 'JSESSIONID=' + getCookie(req, 'uaa_cookie')
                         }
                  }
    needle.get(uaaAddress + '/oauth/authorize?' + confirmParams, confirmOptions,
        function(err, confirmResp) {
            if (confirmResp.statusCode == 200){
                res.cookie('JSESSIONID', getCookie(req, 'uaa_cookie'))
                res.render('confirm', {client_id : req.session.client_id})
            } else if (confirmResp.statusCode == 302){
                if (endsWith(confirmResp.headers.location, '/login')){ // when redirects to UAA API login page
                  res.render('login',{ errorMessage: "" });
                } else {
                  res.cookie('JSESSIONID', getCookie(req, 'uaa_cookie'))
                  res.redirect(confirmResp.headers.location)
                }
            } else {
                res.end('Login/confirm: Error from token server, code: ' + confirmResp.statusCode)
            }
        });
  } else {
     res.statusCode = 500
     res.send('Invalid state');
  }
});

endsWith = function (str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
}

app.post('/confirm', function(req, res){
    var confirmOptions = {
        headers: {
               'Accept' : 'text/html,application/xhtml+xml,application/xml',
               'Cookie' : 'JSESSIONID=' + getCookie(req, 'uaa_cookie'),
               'Content-Type' : 'application/x-www-form-urlencoded'
        }
    }
    var formData = '';
    var scopes = req.session.scope.split(' ')
    for (var i = 0; i < scopes.length; i++) {
       formData = formData + 'scope.' + i.toString() + '=scope.' + scopes[i] + '&'
    }
    formData = formData + 'user_oauth_approval=true'
    needle.post(uaaAddress + '/oauth/authorize', formData, confirmOptions,
           function(err, confirmResp){
               if (confirmResp.statusCode == 302){
                   res.cookie('JSESSIONID', getCookie(req, 'uaa_cookie'))
                   res.redirect(confirmResp.headers.location)
               } else {
                   res.render('login',{ errorMessage: "" });
               }
    });
});

app.post('/reset/:resetToken', function(req, res) {
    var resetToken = req.param('resetToken')
    var email = req.body.email
    var errorResult = validateReset(email, req.body.password)
    if (errorResult){
    var options = {
      headers: { 'Authorization': 'Basic ' + new Buffer(clientId + ':'+ clientSecret).toString('base64') }
    }
    needle.post(uaaAddress + '/oauth/token', 'grant_type=client_credentials',
       options, function(err, tokenResp) {
       if (tokenResp.statusCode == 200){
          var token = tokenResp.body.access_token;
          var usrInfoOptions = {
              headers: {
               'Accept' : 'application/json',
               'scope': 'scim.read',
               'aud' : 'scim',
               'Authorization' : 'Bearer ' + token,
               'Content-Type' : 'application/json' }
          }
           needle.get(uaaAddress + '/Users/?attributes=id,userName,familyName,givenName,version,emails,meta.lastModified&filter=userName eq "' + email + '"', usrInfoOptions ,
            function(err, infoResp){
             if (infoResp.statusCode == 200){
              if (infoResp.body.resources.length > 0 && resetToken == md5(infoResp.body.resources[0].id + infoResp.body.resources[0]['meta.lastModified'])) {
                var userOptions = {
                           headers: {
                             'Accept' : 'application/json',
                             'scope': 'password.write',
                             'aud' : 'password',
                             'Authorization' : 'Bearer ' + token,
                             'Content-Type' : 'application/json' }
                }
                var newPasswordData = {'password' : req.body.password}
                var userId = infoResp.body.resources[0].id
                needle.put(uaaAddress + '/Users/' + userId + '/password', JSON.stringify(newPasswordData),
                         userOptions, function(err, resetResp) {
                             if (resetResp.statusCode = 200){
                                 res.end('SUCCESS');
                             } else {
                                 res.end('Password update failed.')
                             }
                 });
                 } else {
                 res.statusCode = 400
                 res.end('Reset URL is obsolete.');
                 }
             } else {
                 res.statusCode = 400
                 res.end('Bad Request. Cannot retrieve token from server')
             }
           });
       } else {
          res.statusCode = 400
          res.end('No token for client');
       }
   });
   } else {
    res.statusCode = 400
    console.log(errorResult)
    res.end(errorResult);
   }
});

// forget for login
app.post('/forget', function(req, res){
    var userName = req.body.email
    var errorResult = validator.validateForget(userName)
    if (errorResult == null) {
    var options = {
        headers: { 'Authorization': 'Basic ' + new Buffer(clientId + ':'+ clientSecret).toString('base64') }
    }
    needle.post(uaaAddress + '/oauth/token', 'grant_type=client_credentials',
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
                needle.get(uaaAddress + '/Users/?attributes=id,givenName,meta.lastModified,userName&filter=userName eq "' + userName + '"', usrOptions , function(err, usrResp){
                    if (usrResp.statusCode == 200){
                        if (usrResp.body.resources.length == 1){
                            var usrIdAndLastModified = usrResp.body.resources[0].id + usrResp.body.resources[0]['meta.lastModified']
                            var resetToken = md5(usrIdAndLastModified)
                            var templateFile = path.join(__dirname,'templates','reset-password-email.jade')
                            mailer.sendMail(req.body.email, 'Password reset' , templateFile, {user: usrResp.body.resources[0].givenName,
                                confirm: process.env.SL_ADDRESS + '/reset/' + resetToken + '?email=' + req.body.email})
                            res.end('SUCCESS');
                        } else {
                            console.log('Forget: User Not Found')
                            res.end('User Not Found');
                        }
                    } else {
                       console.log('Forget - Could not find user.')
                       res.end('Could not find user.');
                    }
                });
            } else {
                console.log('Forget - No token for client')
                res.end('No token for client');
            }
        }
    );
    } else {
        console.log(errorResult)
        res.end(errorResult);
    }
});

app.post('/register', function(req, res){
    var errorResult = validator.validateRegister(req.body.email, req.body.password, req.body.firstName, req.body.lastName, req.body.company)
    if (errorResult == null){
    var options = {
        headers: { 'Authorization': 'Basic ' + new Buffer(clientId + ':'+ clientSecret).toString('base64') }
    }
    needle.post(uaaAddress + '/oauth/token', 'grant_type=client_credentials',
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
            needle.post(uaaAddress + '/Users', JSON.stringify(userData), regOptions, function(err, createResp) {
                if (createResp.statusCode == 201) {
                    console.log('User created with ' + createResp.body.id + '(id) and name: ' + req.body.email)
                    var templateFile = path.join(__dirname,'templates','confirmation-email.jade')
                    mailer.sendMail(req.body.email, 'Registration' , templateFile, {user: req.body.firstName,
                        confirm: process.env.SL_ADDRESS + '/confirm/' + createResp.body.id})
                    updateAndPostSequenceIqGroups(token, createResp.body.id, req.body.company)
                    updateCloudbreakGroups(token, createResp.body.id)
                    res.end('SUCCESS')
                } else {
                    res.end('Registration failed. ' + createResp.body.message)
                }
            })
        } else {
            console.log("Register: Cannot retrieve token.")
            res.end("Cannot retrieve token.")
        }
    });
    } else {
        console.log(errorResult)
        res.end(errorResult)
    }
});

postGroup = function(token, userId, displayName){
        var groupOptions = {
              headers: {
                 'Accept' : 'application/json',
                 'scope': 'scim.write',
                 'aud' : 'scim',
                 'Authorization' : 'Bearer ' + token,
                 'Content-Type' : 'application/json'
                  }
        }
        var groupData = {
          "schemas":["urn:scim:schemas:core:1.0"],
          "displayName": displayName,
          "members":[
              { "type":"USER", "value": userId }
          ]
        }
        needle.post(uaaAddress + '/Groups', JSON.stringify(groupData), groupOptions,
            function(err, groupResp){
                if (groupResp.statusCode != 201 && groupResp.statusCode != 200) {
                  console.log('failed group creation ' + groupResp.statusCode + ', for user id: ' + userId)
                }
        });
}

updateAndPostSequenceIqGroups = function (token, userId, company){
    updateGroup(token, userId, 'sequenceiq.cloudbreak.user')
    updateGroup(token, userId, 'sequenceiq.cloudbreak.admin')
    postGroup(token, userId, 'sequenceiq.account.' + userId + '.' + company)
}

updateCloudbreakGroups = function (token, userId) {
    updateGroup(token, userId, 'cloudbreak.templates')
    updateGroup(token, userId, 'cloudbreak.stacks')
    updateGroup(token, userId, 'cloudbreak.blueprints')
    updateGroup(token, userId, 'cloudbreak.credentials')
}

updateGroup = function(token, userId, displayName) {
        var getGroupOptions = {
                      headers: {
                         'Accept' : 'application/json',
                         'scope': 'scim.read',
                         'aud' : 'scim',
                         'Authorization' : 'Bearer ' + token,
                         'Content-Type' : 'application/json'
                          }
        }
        needle.get(uaaAddress + '/Groups?attributes=id,displayName,members,meta&filter=displayName eq "' + displayName +'"', getGroupOptions,
            function(err, groupResp) {
                if (groupResp.statusCode == 200 && groupResp.body.resources.length > 0){
                    var id = groupResp.body.resources[0].id
                    var displayName = groupResp.body.resources[0].displayName
                    var members = groupResp.body.resources[0].members
                    var meta = groupResp.body.resources[0].meta

                var updateGroupOptions = {
                     headers: {
                      'Accept' : 'application/json',
                      'scope': 'scim.write',
                      'aud' : 'scim',
                      'Authorization' : 'Bearer ' + token,
                      'Content-Type' : 'application/json',
                      'If-Match' : meta.version
                      }
                }

                var newMembers = [];
                for (var i = 0; i <  members.length ; i++){
                    newMembers.push({"type":"USER","value": members[i].value})
                }
                newMembers.push({"type":"USER","value":userId})

                var updateGroupData = {
                    "schemas":["urn:scim:schemas:core:1.0"],
                    "id": id,
                    "displayName": displayName,
                    "members" : newMembers
                }

                needle.put(uaaAddress + '/Groups/' + id, JSON.stringify(updateGroupData), updateGroupOptions,
                 function(err, updateResp) {
                    if (updateResp.statusCode == 200) {
                        console.log("PUT - update group (id:"+ id + ") is successful (registration)")
                    } else {
                        console.log("PUT - failed to update group (id:"+ id + ", registration), code: " + updateResp.statusCode)
                    }
                 });
                } else {
                    console.log("GET - cannot retrieve group (registration)")
                }

        });
}

// confirm registration
app.get('/confirm/:confirm_token', function(req, res){
   var confirmToken = req.param("confirm_token")
   var options = {
     headers: { 'Authorization': 'Basic ' + new Buffer(clientId + ':'+ clientSecret).toString('base64') }
   }
   needle.post(uaaAddress + '/oauth/token', 'grant_type=client_credentials',
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
            needle.get(uaaAddress + '/Users/' + confirmToken,
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
                        needle.put(uaaAddress + '/Users/' + confirmToken, JSON.stringify(userData),
                        updateOptions, function(err, updateResp){
                            res.render('login',{ errorMessage: "confirmation successful" });
                        });
                    } else {
                     res.end('Cannot retrieve user by confirm token.')
                    }
                   } else {
                    res.end('Cannot retrieve user.')
                   }
            });
        } else {
          res.end('Cannot retrieve token')
        }
   });

});

// errors
/*
app.use(function(err, req, res, next){
  res.status(err.status);
  res.json({ error: {status: err.status, message: err.message} });
});*/

d.on('error', function(err) {
  console.error(err);
});

// listen
var port = process.env.SL_PORT || 8080;
server = app.listen(port);

console.log('Server listening on port %d in %s mode', server.address().port, app.settings.env);
