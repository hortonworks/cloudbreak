package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.ldap.LdapCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.ldap.LdapCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.action.v4.ldap.LdapDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.ldap.LdapGetAction;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;

@Service
public class LdapTestClient {

    public Action<LdapTestDto, CloudbreakClient> createV4() {
        return new LdapCreateAction();
    }

    public Action<LdapTestDto, CloudbreakClient> createIfNotExistV4() {
        return new LdapCreateIfNotExistsAction();
    }

    public Action<LdapTestDto, CloudbreakClient> getV4() {
        return new LdapGetAction();
    }

    public Action<LdapTestDto, CloudbreakClient> deleteV4() {
        return new LdapDeleteAction();
    }

}