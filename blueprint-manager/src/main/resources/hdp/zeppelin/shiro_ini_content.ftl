
[users]
# List of users with their password allowed to access Zeppelin.
# To use a different strategy (LDAP / Database / ...) check the shiro doc at http://shiro.apache.org/configuration.html#Configuration-INISections

<#if !knoxGateway>
admin = ${ zeppelin_admin_password }
</#if>

# Sample LDAP configuration, for user Authentication, currently tested for single Realm
[main]
<#if knoxGateway>
ldapRealm = org.apache.shiro.realm.ldap.JndiLdapRealm
ldapRealm.userDnTemplate = uid={0},ou=Users,dc=hadoop,dc=apache,dc=org
ldapRealm.contextFactory.url = ldap://localhost:33389
ldapRealm.contextFactory.authenticationMechanism = SIMPLE
</#if>
sessionManager = org.apache.shiro.web.session.mgt.DefaultWebSessionManager
securityManager.sessionManager = $sessionManager
# 86,400,000 milliseconds = 24 hour
securityManager.sessionManager.globalSessionTimeout = 86400000
shiro.loginUrl = /api/login

[urls]
# anon means the access is anonymous.
# authcBasic means Basic Auth Security
# To enfore security, comment the line below and uncomment the next one
/api/version = anon
#/** = anon
/** = authc