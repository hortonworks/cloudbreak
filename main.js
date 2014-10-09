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
            res.end('Authentication failed.')
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
    var confirmParams = 'client_id=' + req.session.client_id
                        + '&response_type=' + req.session.response_type
                        + '&scope=' + req.session.scope;
                        + '&redirect_uri=' + req.session.redirect_uri;
    var confirmOptions = {
                      headers: {
                        'Cookie': 'JSESSIONID=' + getCookie(req, 'uaa_cookie')
                         }
                  }
    needle.get('http://' + uaaHost + ':' + uaaPort + '/oauth/authorize?' + confirmParams, confirmOptions,
        function(err, confirmResp) {
            if (confirmResp.statusCode == 200){
                res.cookie('JSESSIONID', getCookie(req, 'uaa_cookie'))
                res.render('confirm', {client_id : req.session.client_id})
            } else if (confirmResp.statusCode == 302){
                if (endsWith(confirmResp.headers.location, '/login')){ // when redirects to UAA API login page
                  res.render('login')
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
    var choose = req.param('choose');
    if (choose == 'yes') {
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
       needle.post('http://' + uaaHost + ':' + uaaPort + '/oauth/authorize', formData, confirmOptions,
           function(err, confirmResp){
               if (confirmResp.statusCode == 302){
                   res.cookie('JSESSIONID', getCookie(req, 'uaa_cookie'))
                   res.end(confirmResp.headers.location)
               } else {
                   res.render('login')
               }
        });
    } else {
        res.render('login')
    }
});

app.post('/reset/:resetToken', function(req, res) {
    var resetToken = req.param('resetToken')
    if (resetToken != null && resetToken.split('-').length == 6) {
    var split = resetToken.split('-')
    var version = split[5];
    var userId = resetToken.substring(0, resetToken.length - split[5].length - 1);
    var options = {
      headers: { 'Authorization': 'Basic ' + new Buffer(clientId + ':'+ clientSecret).toString('base64') }
    }
    needle.post('http://' + uaaHost + ':' + uaaPort + '/oauth/token', 'grant_type=client_credentials',
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
           needle.get('http://' + uaaHost + ':' + uaaPort + '/Users/?attributes=version,id&filter=id eq "' + userId + '"', usrInfoOptions ,
            function(err, infoResp){
             if (infoResp.statusCode == 200){
              if (infoResp.body.resources.length > 0 && infoResp.body.resources[0].version == version) {
                var userOptions = {
                           headers: {
                             'Accept' : 'application/json',
                             'scope': 'password.write',
                             'aud' : 'password',
                             'Authorization' : 'Bearer ' + token,
                             'Content-Type' : 'application/json' }
                }
                var newPasswordData = {'password' : req.body.password}
                needle.put('http://' + uaaHost + ':' + uaaPort + '/Users/' + userId + '/password', JSON.stringify(newPasswordData),
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
    res.end('Bad Request. Cannot read token.')
   }
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
                needle.get('http://' + uaaHost + ':' + uaaPort + '/Users/?attributes=id,givenName,version,userName&filter=userName eq "' + userName + '"', usrOptions , function(err, usrResp){
                    if (usrResp.statusCode == 200){
                        console.log(usrResp.body)
                        if (usrResp.body.resources.length == 1){
                            var templateFile = path.join(__dirname,'templates','reset-password-email.jade')
                            mailer.sendMail(req.body.email, 'Password reset' , templateFile, {user: usrResp.body.resources[0].givenName,
                                confirm: 'http://' +  process.env.UR_HOST + ':' + process.env.UR_PORT + '/reset/' + usrResp.body.resources[0].id + '-' + usrResp.body.resources[0].version})
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
});

app.post('/register', function(req, res){
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
                    // updateAndPostSequenceIqGroups(token, createResp.body.id, req.body.company) // TODO: uncomment this before production
                    updateCloudbreakGroups(token, createResp.body.id)
                    res.end('SUCCESS')
                } else {
                    res.end('Registration failed. Status Code: ' + createResp.statusCode)
                }
            })
        } else {
            res.end('Cannot get token from server')
        }
    });
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
        needle.post('http://' + uaaHost + ':' + uaaPort + '/Groups', JSON.stringify(groupData), groupOptions,
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
        needle.get('http://' + uaaHost + ':' + uaaPort + '/Groups?attributes=id,displayName,members,meta&filter=displayName eq "' + displayName +'"', getGroupOptions,
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

                needle.put('http://' + uaaHost + ':' + uaaPort + '/Groups/' + id, JSON.stringify(updateGroupData), updateGroupOptions,
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

app.get('/confirm/:confirm_token', function(req, res){
   var confirmToken = req.param("confirm_token")
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
                            res.render('login')
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
app.use(function(err, req, res, next){
  res.status(err.status);
  res.json({ error: {status: err.status, message: err.message} });
});

// listen
var port = process.env.UR_PORT || 8080;
server = app.listen(port);

console.log('Server listening on port %d in %s mode', server.address().port, app.settings.env);
