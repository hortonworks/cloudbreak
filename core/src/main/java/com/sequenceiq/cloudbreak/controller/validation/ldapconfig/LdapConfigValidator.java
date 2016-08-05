package com.sequenceiq.cloudbreak.controller.validation.ldapconfig;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

@Component
public class LdapConfigValidator {

    public void validateLdapConnection(LdapConfig ldapConfig) {

        if (ldapConfig != null) {

            try {
                //BEGIN GENERATED CODE
                Hashtable<String, String> env = new Hashtable<String, String>();
                //END GENERATED CODE
                env.put("com.sun.jndi.ldap.read.timeout", "1000");
                env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                String url = new StringBuilder(ldapConfig.getServerSSL() ? "ldaps://" : "ldap://").
                        append(ldapConfig.getServerHost()).
                        append(":").
                        append(ldapConfig.getServerPort()).toString();
                env.put(Context.PROVIDER_URL, url);
                env.put(Context.SECURITY_AUTHENTICATION, "simple");
                env.put(Context.SECURITY_PRINCIPAL, ldapConfig.getBindDn());
                env.put(Context.SECURITY_CREDENTIALS, ldapConfig.getBindPassword());
                DirContext ctx = new InitialDirContext(env);
                ctx.close();

            } catch (NamingException e) {
                throw new BadRequestException("Failed to connect to LDAP server: " + e.getMessage(), e);
            }
        }
    }



}
