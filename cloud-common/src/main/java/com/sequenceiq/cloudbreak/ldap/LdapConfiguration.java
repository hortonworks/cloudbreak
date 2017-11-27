package com.sequenceiq.cloudbreak.ldap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

@Configuration
public class LdapConfiguration {

    @Value("${cb.ldap.url}")
    private String ldapUrl;

    @Value("${cb.ldap.userDN}")
    private String userDn;

    @Value("${cb.ldap.password}")
    private String password;

    @Value("${cb.ldap.baseDN}")
    private String baseDN;

    @Bean
    public LdapTemplate createLdapTemplate() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapUrl);
        contextSource.setUserDn(userDn);
        contextSource.setPassword(password);
        contextSource.setBase(baseDN);
        contextSource.setAnonymousReadOnly(false);
        contextSource.afterPropertiesSet();
        LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
        ldapTemplate.setIgnorePartialResultException(true);
        return ldapTemplate;
    }

}
