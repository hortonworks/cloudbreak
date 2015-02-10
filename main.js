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
var request = require('request');

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

var emailErrorMsg = 'invalid email address'
var passwordErrorMsg = 'password is invalid (6 to 200 char)'
var confirmPasswordErrorMsg = 'passwords do not match!'
var firstNameErrorMsg = 'first name is empty'
var lastNameErrorMsg = 'last name is empty'
var companyErrorMsg = 'company name is empty'
// register.html
app.get('/register', function(req, res) {
  res.render('register',
  {
   emailErrorMsg: emailErrorMsg,
   passwordErrorMsg: passwordErrorMsg,
   confirmPasswordErrorMsg: confirmPasswordErrorMsg,
   firstNameErrorMsg: firstNameErrorMsg,
   lastNameErrorMsg: lastNameErrorMsg,
   companyErrorMsg: companyErrorMsg
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
        if (err == null){
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
                res.redirect('confirm')
            }
        } else {
            res.render('login',{ errorMessage: "Incorrect email/password or account is disabled." });
        }
        } else {
            console.log("Client cannot access resource server. Check UAA address.");
            res.render('login',{ errorMessage: "Client cannot access resource server." });
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
    var confirmData = {
        'client_id' : req.session.client_id,
        'response_type': req.session.response_type,
        'scope' : req.session.scope,
        'redirect_uri' : req.session.redirect_uri
    }
    var confirmOptions = {
                      headers: {
                        'Accept' : 'application/json',
                        'Cookie': 'JSESSIONID=' + getCookie(req, 'uaa_cookie')
                         }
                  }
     needle.post(uaaAddress + '/oauth/authorize', confirmData, confirmOptions, function (err, confirmResp){
                    if (confirmResp.statusCode == 200){
                        req.session.userScopes = confirmResp.body.auth_request.scope
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
                        console.log('Confirm error - code: ' + confirmResp.statusCode +', message: ' + confirmResp.message)
                        res.end('Login/confirm: Error from token server, code: ' + confirmResp.statusCode)
                    }
     })
  } else {
     res.statusCode = 500
     console.log('Invalid state at confirm.')
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
    var scopes = req.session.userScopes
    if (scopes != undefined) {
        for (var i = 0; i < scopes.length; i++) {
            formData = formData + 'scope.' + i.toString() + '=scope.' + scopes[i] + '&'
        }
    }
    formData = formData + 'user_oauth_approval=true'
    needle.post(uaaAddress + '/oauth/authorize', formData, confirmOptions,
           function(err, confirmResp){
               if (confirmResp.statusCode == 302){
                   res.cookie('JSESSIONID', getCookie(req, 'uaa_cookie'))
                   res.redirect(confirmResp.headers.location)
               } else {
                   console.log('Authorization failed - ' + confirmResp.message)
                   res.render('login',{ errorMessage: "" });
               }
    });
});

postGroup = function(token, userId, displayName){
        var groupOptions = {
              headers: {
                 'Accept' : 'application/json',
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
    updateGroup(token, userId, 'cloudbreak.usages.account')
    postGroup(token, userId, 'sequenceiq.account.' + userId + '.' + company)
}

updateCloudbreakGroups = function (token, userId) {
    updateGroup(token, userId, 'cloudbreak.templates')
    updateGroup(token, userId, 'cloudbreak.stacks')
    updateGroup(token, userId, 'cloudbreak.blueprints')
    updateGroup(token, userId, 'cloudbreak.credentials')
    updateGroup(token, userId, 'cloudbreak.events')
    updateGroup(token, userId, 'cloudbreak.recipes')
    updateGroup(token, userId, 'cloudbreak.usages.user')
    updateGroup(token, userId, 'periscope.cluster')
}

updateGroup = function(token, userId, displayName) {
        var getGroupOptions = {
                      headers: {
                         'Accept' : 'application/json',
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
                        console.log("PUT - update group (id:"+ id + ", name: " + displayName + ") is successful.")
                    } else {
                        console.log("PUT - failed to update group (id:"+ id + ", name: " + displayName + "), code: " + updateResp.statusCode)
                    }
                 });
                } else {
                    console.log("GET - cannot retrieve group: " + displayName)
                }
        });
}

app.post('/reset/:resetToken', function(req, res) {
    var resetToken = req.param('resetToken')
    var email = req.body.email
    var errorResult = validator.validateReset(email, req.body.password)
    if (errorResult == null){
        getToken(req, res, function(token){
            getUserByName(req, res, token, email, 'id,userName,familyName,givenName,version,emails,meta.lastModified', function(userData){
                if (resetToken == md5(userData.id + userData['meta.lastModified'])){
                    var userOptions = {
                         headers: {
                            'Accept' : 'application/json',
                            'scope': 'password.write',
                            'aud' : 'password',
                            'Authorization' : 'Bearer ' + token,
                            'Content-Type' : 'application/json' }
                    }
                    var newPasswordData = {'password' : req.body.password}
                    needle.put(uaaAddress + '/Users/' + userData.id + '/password', JSON.stringify(newPasswordData),
                         userOptions, function(err, resetResp) {
                       if (resetResp.statusCode == 200){
                            res.json({message: 'SUCCESS'})
                       } else {
                            res.statusCode = resetResp.statusCode
                            res.json({message: 'Password update failed.'})
                       }
                    });
                } else {
                    res.statusCode = 401
                    res.json({message: 'Bad token for user.'})
                }
            })
        })
   } else {
    res.statusCode = 401
    res.json({message:'Failed to reset password. Check inputs'});
   }
});

app.post('/register', function(req, res){
    var errorResult = validator.validateRegister(req.body.email, req.body.password, req.body.firstName, req.body.lastName, req.body.company)
    if (errorResult == null){
        getToken(req, res, function(token){
            registerUser(req, res, token)
        })
    } else {
        console.log(errorResult)
        res.statusCode = 400
        res.json({message: 'Failed to send register email. Check inputs'})
    }
});

// confirm registration
app.get('/confirm/:confirm_token', function(req, res){
   var confirmToken = req.param("confirm_token")
   getToken(req, res, function(token) {
        getUserById(req, res, token, confirmToken, function(userData){
            if (confirmToken == userData.id) {
                var updateOptions = {
                     headers: {
                       'Accept' : 'application/json',
                       'Authorization' : 'Bearer ' + token,
                       'Content-Type' : 'application/json',
                       'If-Match': userData.meta.version}
                    }
                var updateData = {
                       'userName' : userData.userName,
                       'active' : true,
                       'name' : {
                         'familyName': userData.name.familyName,
                         'givenName' : userData.name.givenName
                        },
                        'emails':[
                        {
                         'value': userData.emails[0].value
                        }]
                    }
                updateUserData(req, res, token, userData.id, updateData, updateOptions, function(updateResp){
                   res.render('login',{ errorMessage: "confirmation successful" });
                })
            } else {
                res.statusCode = 401
                res.json({message: 'Cannot retrieve user by confirm token.'})
            }
        });
   });
});

app.post('/invite', function (req, res){
    var inviteEmail = req.body.invite_email
    if (validator.validateEmail(inviteEmail)){
        getUserName(req, res, function(adminUserName){
           getToken(req, res, function(token){
              getUserByName(req, res, token, adminUserName, 'id,userName,groups', function(userData){
                isUserAdmin(req, res, userData, function(companyId){
                   inviteUser(req, res, token, inviteEmail, adminUserName, companyId)
                });
              });
            });
        });
    }
    else {
        res.statusCode = 400
        res.json({message: 'Email is not valid'})
    }
});

app.post('/forget', function(req, res){
    var userName = req.body.email
    var errorResult = validator.validateForget(userName)
    if (errorResult == null) {
        getToken(req, res, function(token){
            getUserByName(req, res, token, userName, 'id,givenName,meta.lastModified,userName', function(userData){
               var usrIdAndLastModified = userData.id + userData['meta.lastModified']
               var resetToken = md5(usrIdAndLastModified)
               var templateFile = path.join(__dirname,'templates','reset-password-email.jade')
               mailer.sendMail(req.body.email, 'Password reset' , templateFile, {user: userData.givenName,
                  confirm: process.env.SL_ADDRESS + '/reset/' + resetToken + '?email=' + req.body.email})
               res.json({message: 'SUCCESS'})
            });
        });
    } else {
        console.log(errorResult)
        res.statusCode = 400
        res.json({message: 'Failed to send reset password email. Check inputs'})
    }
});

app.get('/account/register', function(req, res){
    req.session.acc_token = req.param('token')
    req.session.acc_email = req.param('email')
    var inviter = req.param('inviter')
    getToken(req, res, function(token) {
        getUserByName(req, res, token, inviter, 'id,userName,groups', function(userData){
            isUserAdmin(req, res, userData, function(companyId){
                var company = companyId.split(".")[3]
                res.render('regacc',
                  {
                    token: req.session.acc_token,
                    email: req.session.acc_email,
                    inviter: inviter,
                    company: company,
                    passwordErrorMsg: passwordErrorMsg,
                    confirmPasswordErrorMsg: confirmPasswordErrorMsg,
                    firstNameErrorMsg: firstNameErrorMsg,
                    lastNameErrorMsg: lastNameErrorMsg
                  })
            });
        });
    });
});

app.post('/account/register', function(req, res){
    var regToken = req.session.acc_token
    var email = req.session.acc_email
    if (regToken == null || email == null){
        res.end('Session has expired.')
    } else {
        var errorResult = validator.validateRegister(req.body.email, req.body.password, req.body.firstName, req.body.lastName, req.body.company)
        if (errorResult == null) {
            getToken(req, res, function(token) {
                getUserByName(req, res, token, email, 'id,userName,familyName,givenName,version,emails,active',
                    function(userData){
                     if(userData.active == false){
                        var updateOptions = {
                             headers: {
                               'Accept' : 'application/json',
                               'Authorization' : 'Bearer ' + token,
                               'Content-Type' : 'application/json',
                               'If-Match': userData.version}
                        }
                        var updateData = {
                           'userName' : req.body.email,
                           'active' : false,
                           'name' : {
                               'familyName': req.body.lastName,
                               'givenName' : req.body.firstName
                            },
                           'emails':[
                              {'value': req.body.email}
                            ]
                        }
                        updateUserData(req, res, token, userData.id, updateData, updateOptions, function(updateResp){
                            updatePassword(req, res, token, userData.id, req.body.password, function(pwdResp){
                                var templateFile = path.join(__dirname,'templates','confirmation-email.jade')
                                mailer.sendMail(req.body.email, 'Registration' , templateFile, {user: req.body.firstName,
                                  confirm: process.env.SL_ADDRESS + '/confirm/' + userData.id})
                                res.json({message: 'SUCCESS'});
                            });
                        });
                     } else {
                       console.log('User already created.')
                       res.statusCode = 400
                       res.json({message: 'User already created.'})
                     }
                });
            });
        } else {
          res.statusCode = 400
          res.json({message: 'Invalid input data.'})
        }
    }
});

app.get('/account/details', function(req, res){
    getUserName(req, res, function(userName){
        getToken(req, res, function(token){
            getUserByName(req, res, token, userName, 'userName,familyName,givenName,groups', function(userData){
                var companyName = null
                var groups = userData.groups
                var adminUserId = null
                for (var i = 0; i < groups.length; i++ ){
                    if (groups[i].display.lastIndexOf('sequenceiq.account', 0) === 0) {
                        var splittedGroup = groups[i].display.split('.')
                        adminUserId = splittedGroup[2]
                        companyName = splittedGroup[3]
                    }
                }
                getUserById(req, res, token, adminUserId, function(adminUserData){
                    res.json({userName: userData.userName, givenName: userData.givenName, familyName: userData.familyName, company: companyName, companyOwner: adminUserData.userName})
                });
            });
        });
    });
});

app.post('/activate', function(req, res){
    var activate = req.body.activate;
    var email = req.body.email

    if (activate != null && (activate === true || activate === false) && validator.validateEmail(email)) {
        getUserName(req, res, function(adminUserName){
            getToken(req, res, function(token){
                getUserByName(req, res, token, adminUserName, 'id,userName,groups', function(adminUserData){
                    isUserAdmin(req, res, adminUserData, function(companyId){
                        getUserByName(req, res, token, email, 'id,userName,familyName,givenName,version,groups', function(userData){
                            var userCompanyId = null
                            var groups = userData.groups
                            for (var i = 0; i < groups.length; i++ ){
                               if (groups[i].display.lastIndexOf('sequenceiq.account', 0) === 0) {
                                 userCompanyId = groups[i].display
                               }
                            }
                            if (userCompanyId != null && userCompanyId == companyId) {
                                var userToActivateId = userData.id
                                var userActivateOptions = {
                                    headers: {
                                      'Accept' : 'application/json',
                                      'Authorization' : 'Bearer ' + token,
                                      'Content-Type' : 'application/json',
                                      'If-Match': userData.version
                                    }
                                }
                                var userActivateData = {
                                    'userName' : email,
                                    'active' : activate,
                                    'name' : {
                                        'familyName': userData.familyName,
                                        'givenName' : userData.givenName
                                    },
                                    'emails':[
                                      {
                                       'value': email
                                      }]
                                }
                                updateUserData(req, res, token, userToActivateId, userActivateData, userActivateOptions, function(updateResp){
                                    console.log('User activation/deactivation successful on user with id: ' + userToActivateId)
                                    res.json({message: 'SUCCESS'})
                                });
                            } else {
                                console.log('User and admin company id is not the same.')
                                res.statusCode = 403
                                res.json({message: 'User and admin company id is not the same.'})
                            }
                        });
                    });
                });
            });
        });
    } else {
        console.log('Invalid activate or email parameter.')
        res.statusCode = 400
        res.json({message: 'Invalid activate or email parameter.'})
    }
});

app.get('/users', function(req, res){
    getUserName(req, res, function(adminUserName){
        getToken(req, res, function(token){
            getUserByName(req, res, token, adminUserName, 'id,userName,groups', function(adminUserData){
                isUserAdmin(req, res, adminUserData, function(companyId){
                    getGroupByName(req, res, token, 'sequenceiq.cloudbreak.admin', 'members', function(adminGroupData){
                        var adminGroupMembers = adminGroupData.members
                        var adminGroupMemberIds = [];
                        adminGroupMembers.forEach(function(adminGroupMember) {
                            adminGroupMemberIds.push(adminGroupMember.value);
                        });
                        getGroupByName(req, res, token, companyId, 'members', function(groupData){
                            var groupMemberIds = groupData.members
                            var completed_requests = 0;
                            var users = [];
                            if (groupMemberIds.length != 0) {
                                groupMemberIds.forEach(function(groupMember) {
                                    request({
                                    method: 'GET',
                                    url: uaaAddress + '/Users?attributes=id,userName,active&filter=id eq  "' + groupMember.value + '"',
                                    headers: {'Accept' : 'application/json',
                                         'Authorization' : 'Bearer ' + token,
                                          'Content-Type' : 'application/json'
                                    }
                                    },function (error, response, body) {
                                        if (response.statusCode == 200){
                                            var resultResource = JSON.parse(body).resources[0]
                                            var isAdmin = (adminGroupMemberIds.indexOf(resultResource.id) == -1) ? false : true
                                            users.push({id: resultResource.id, username: resultResource.userName, active: resultResource.active, admin: isAdmin})
                                        }
                                        completed_requests++;
                                        if (completed_requests == groupMemberIds.length){
                                            res.json(users)
                                        }
                                    });
                                });
                        } else {
                          console.log('No users found for this company.')
                          res.statusCode = 400
                          res.json({message: 'No users found for this company.'})
                        }
                    })
                    })
                });
            });
        });
    });
});

app.post('/permission', function(req, res){
    var role = req.body.role
    var userId = req.body.id
    if (role == 'admin') {
         getUserName(req, res, function(adminUserName){
            getToken(req, res, function(token){
                getUserByName(req, res, token, adminUserName, 'id,userName,groups', function(adminUserData){
                    isUserAdmin(req, res, adminUserData, function(companyId){
                        updateGroup(token, userId, 'sequenceiq.cloudbreak.admin')
                        res.json({user: userId, admin: true})
                    });
                });
            });
         });
    } else {
        res.statusCode = 400
        res.json({message: 'Not existing permission type.'})
    }
});

app.get('/permission', function(req, res){
        getUserName(req, res, function(userName){
                    getToken(req, res, function(token){
                        getUserByName(req, res, token, userName, 'id,userName,groups', function(userData){
                            var groups = userData.groups
                            var isAdmin = false
                            for (var i = 0; i < groups.length; i++ ){
                                   if (groups[i].display.lastIndexOf('sequenceiq.cloudbreak.admin', 0) === 0){
                                      isAdmin = true
                                   }
                            }
                            res.json({admin: isAdmin})
                        });
                    });
        });
});

// service methods

getToken = function(req, res, callback) {
    var options = {
        headers: { 'Authorization': 'Basic ' + new Buffer(clientId + ':'+ clientSecret).toString('base64') }
    }
    needle.post(uaaAddress + '/oauth/token', 'grant_type=client_credentials',
        options, function(err, tokenResp) {
            if (tokenResp.statusCode == 200){
                var token = tokenResp.body.access_token;
                callback(token)
            } else {
                res.statusCode = tokenResp.statusCode
                res.json({message: 'Cannot retrieve token'})
            }
    });
}

getUserByName = function(req, res, token, userName, attributes, callback) {
    var usrOptions = {
          headers: {
            'Accept' : 'application/json',
            'Authorization' : 'Bearer ' + token,
            'Content-Type' : 'application/json' }
    }
    needle.get(uaaAddress + '/Users?attributes=' + attributes + '&filter=userName eq "' + userName + '"', usrOptions , function(err, usrResp){
       if (usrResp.statusCode == 200){
            if (usrResp.body.resources.length == 1){
                callback(usrResp.body.resources[0])
            } else {
            console.log('User Not Found')
            res.statusCode = 400
            res.json({message: 'User not found.'})
          }
       } else {
        console.log('Could not find user - bad request.')
        res.statusCode = 400
        res.json({message: 'Could not find user - bad request.'})
       }
    });
}

getUserById = function(req, res, token, userId, callback) {
    var usrOptions = {
         headers: {
           'Accept' : 'application/json',
           'Authorization' : 'Bearer ' + token,
           'Content-Type' : 'application/json'
         }
    }
    needle.get(uaaAddress + '/Users/' + userId, usrOptions, function(err, userResp) {
         if (userResp.statusCode == 200) {
            callback(userResp.body)
         } else {
           console.log('Cannot retrieve user by id - bad request.')
           res.statusCode = 400
           res.json({message: 'Cannot retrieve user by id - bad request.'})
         }
    });
}

updateUserData = function(req, res, token, userId, updateData, updateOptions, callback) {
    needle.put(uaaAddress + '/Users/' + userId, JSON.stringify(updateData), updateOptions,
        function(err, updateResp){
          if (updateResp.statusCode == 200){
            callback(updateResp);
          } else {
            console.log('User update failed.')
            res.statusCode = 400
            res.json({message: 'User update failed.'})
          }
    });
}

updatePassword = function(req, res, token, userId, newPassword, callback) {
        var passwordUpdateOptions = {
            headers: {
               'Accept' : 'application/json',
               'Authorization' : 'Bearer ' + token,
               'Content-Type' : 'application/json' }
        }
        var newPasswordData = {'password' : newPassword}
        needle.put(uaaAddress + '/Users/' + userId + '/password', JSON.stringify(newPasswordData),
             passwordUpdateOptions, function(err, resetResp) {
                 if (resetResp.statusCode == 200){
                   callback(resetResp)
                 } else {
                   console.log('Password update failed.')
                   res.statusCode = 400
                   res.json({message: 'Password update failed.'})
                 }
        });
}

registerUser = function(req, res, token) {
            var regOptions = {
                 headers: {
                     'Accept' : 'application/json',
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
                     res.json({status: 200, message: 'SUCCESS'})
                 } else {
                     res.statusCode = 400
                     res.json({message: 'Registration failed. '+ createResp.body.message})
                 }
             })
}

getUserName = function(req, res, callback) {
    var authHeader = req.headers['authorization']
    var options = {
        headers: { 'Authorization': authHeader }
    }
    if (authHeader != null && authHeader.split(' ').length > 1) {
        var token = authHeader.split(' ')[1];
        var checkTokenRespOption = {
            headers : {
                    'Content-Type' : 'application/x-www-form-urlencoded',
                    'Authorization' : 'Basic ' + new Buffer(clientId + ':'+ clientSecret).toString('base64')
            }
        }
        needle.post(uaaAddress + "/check_token", 'token=' + token, checkTokenRespOption, function(err, checkTokenResp){
            if (err == null) {
            if (checkTokenResp.statusCode == 200){
               var userName = checkTokenResp.body.user_name
               callback(userName)
            } else {
                console.log('Cannot retrieve user name from token.')
                res.statusCode = checkTokenResp.statusCode
                res.json({message: 'Cannot retrieve user name from token.'})
            }
            } else {
                console.log('Authorization: cannot access UAA server.')
                res.statusCode = 400
                res.json({message: 'Cannot access resource server.'})
            }
        })
    } else {
        console.log('Authorization token not found')
        res.statusCode = 400
        res.json({message: 'Authorization token not found'})
    }
}

isUserAdmin = function(req, res, userData, callback) {
    var groups = userData.groups
    var isAdmin = false
    var companyId = null
    for (var i = 0; i < groups.length; i++ ){
       if (groups[i].display.lastIndexOf('sequenceiq.cloudbreak.admin', 0) === 0){
          isAdmin = true
       }
       if (groups[i].display.lastIndexOf('sequenceiq.account', 0) === 0) {
          companyId = groups[i].display
       }
    }
    if (isAdmin == false) {
      res.statusCode = 403
      res.json({message: 'User is not an admin.'})
    } else if (companyId == null){
      res.statusCode = 400
      res.json({message: 'Company not found for admin user.'})
    } else {
      callback(companyId)
    }
}

inviteUser = function(req, res, token, inviteEmail, adminUserName, companyId) {
    var userTempToken = Math.random().toString(20)
    var tempRegOptions = {
        headers: {
           'Accept' : 'application/json',
           'Authorization' : 'Bearer ' + token,
           'Content-Type' : 'application/json' }
    }
    var tempUserData = {
          'userName' : inviteEmail,
          'active' : false,
          'name' : {
          'familyName': userTempToken,
          'givenName' : userTempToken
           },
          'password' : userTempToken,
          'emails':[
            {
             'value': inviteEmail
            }]
    }
    needle.post(uaaAddress + '/Users', JSON.stringify(tempUserData), tempRegOptions, function(err, createResp) {
       if (createResp.statusCode == 201) {
           console.log('User created with ' + createResp.body.id + '(id) and name: ' + inviteEmail)
           updateGroup(token, createResp.body.id, 'sequenceiq.cloudbreak.user')
           updateGroup(token, createResp.body.id, companyId)
           updateCloudbreakGroups(token, createResp.body.id)

           var templateFile = path.join(__dirname,'templates','invite-email.jade')
           mailer.sendMail(req.body.invite_email, 'Cloudbreak invite' , templateFile, {user: adminUserName,
           invite: process.env.SL_ADDRESS + '/account/register?token=' + userTempToken + '&email=' + inviteEmail + '&inviter=' + adminUserName})
           res.json({message: 'SUCCESS'})
       } else {
          res.statusCode = 400
          res.json({message: 'Temporary registration failed. ' + createResp.body.message})
       }
    });
}

getGroupByName = function(req, res, token, displayName, attributes, callback) {
    var usrOptions = {
         headers: {
           'Accept' : 'application/json',
           'Authorization' : 'Bearer ' + token,
           'Content-Type' : 'application/json' }
    }
    needle.get(uaaAddress + '/Groups?attributes=' + attributes + '&filter=displayname eq "' + displayName + '"', usrOptions , function(err, groupResp){
          if (groupResp != null && groupResp.body.resources.length == 1) {
            callback(groupResp.body.resources[0])
          } else {
            res.statusCode = 400
            res.json({message: 'Cannot retrieve group by id: '+ displayName})
          }
    });
}

// errors

app.use(function(err, req, res, next){
  res.status(err.status);
  res.json({ error: {status: err.status, message: err.message} });
});

d.on('error', function(err) {
  console.error(err);
});

process.on('uncaughtException', function (err) {
    if (err.code == 'ECONNREFUSED' || err.code == 'ECONNRESET' || err.code == 'ENETUNREACH') {
        console.log('Exception error occurred: ' + err.code + ' when try to connect: ' + err.request.options.host + ':' + err.request.options.port + err.request.options.path);
    } else {
        console.log(err)
    }
 });

// listen
var port = process.env.SL_PORT || 8080;
server = app.listen(port);

console.log('Server listening on port %d in %s mode', server.address().port, app.settings.env);
