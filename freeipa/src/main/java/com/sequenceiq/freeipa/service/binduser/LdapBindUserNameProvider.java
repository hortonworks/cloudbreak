package com.sequenceiq.freeipa.service.binduser;

import org.springframework.stereotype.Component;

@Component
public class LdapBindUserNameProvider {

    public String createBindUserName(String postfix) {
        return "ldapbind-" + postfix;
    }
}
