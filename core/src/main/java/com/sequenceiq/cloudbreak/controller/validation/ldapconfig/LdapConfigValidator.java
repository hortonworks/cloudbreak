package com.sequenceiq.cloudbreak.controller.validation.ldapconfig;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.LdapValidationRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

@Component
public class LdapConfigValidator {

    public void validateLdapConnection(LdapConfig ldapConfig) {
        if (ldapConfig != null) {
            validateLdapConnection(ldapConfig.getProtocol(),
                    ldapConfig.getServerHost(),
                    ldapConfig.getServerPort(),
                    ldapConfig.getBindDn(),
                    ldapConfig.getBindPassword());
        }
    }

    public void validateLdapConnection(LdapValidationRequest request) {
        if (request != null) {
            validateLdapConnection(request.getProtocol(),
                    request.getServerHost(),
                    request.getServerPort(),
                    request.getBindDn(),
                    request.getBindPassword());
        }
    }

    private void validateLdapConnection(String protocol, String serverHost, Integer serverPort, String bindDn, String bindPassword) {
        try {
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
            DirContext ctx = new InitialDirContext(env);
            ctx.close();

        } catch (NamingException e) {
            throw new BadRequestException("Failed to connect to LDAP server: " + e.getMessage(), e);
        }
    }


}
