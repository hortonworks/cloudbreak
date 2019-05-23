package com.sequenceiq.freeipa.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.controller.exception.BadRequestException;

@Component
public class LdapConfigValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigValidator.class);

    public void validateLdapConnection(LdapConfig ldapConfig) {
        if (ldapConfig != null) {
            validateLdapConnection(ldapConfig.getProtocol(),
                    ldapConfig.getServerHost(),
                    ldapConfig.getServerPort(),
                    ldapConfig.getBindDn(),
                    ldapConfig.getBindPassword());
        }
    }

    private void validateLdapConnection(String protocol, String serverHost, Integer serverPort, String bindDn, String bindPassword) {
        try {
            LOGGER.debug("Validate connection to LDAP host: '{}', port: '{}', protocol: '{}'.", serverHost, serverPort, protocol);
            //BEGIN GENERATED CODE
            Hashtable<String, String> env = new Hashtable<>();
            //END GENERATED CODE
            env.put("com.sun.jndi.ldap.read.timeout", "1000");
            env.put("com.sun.jndi.ldap.connect.timeout", "5000");
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            String url = new StringBuilder(protocol).
                    append("://").
                    append(serverHost).
                    append(':').
                    append(serverPort).toString();
            env.put(Context.PROVIDER_URL, url);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, bindDn);
            env.put(Context.SECURITY_CREDENTIALS, bindPassword);
            Context ctx = new InitialDirContext(env);
            ctx.close();

        } catch (NamingException e) {
            throw new BadRequestException("Failed to connect to LDAP server: " + e.getMessage(), e);
        }
    }
}
