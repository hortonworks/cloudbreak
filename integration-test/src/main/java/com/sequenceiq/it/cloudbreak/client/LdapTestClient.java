package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.ldap.LdapCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.ldap.LdapCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.action.v4.ldap.LdapDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.ldap.LdapGetAction;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;

@Service
public class LdapTestClient {
    public Action<LdapTestDto, FreeIpaClient> createV1() {
        return new LdapCreateAction();
    }

    public Action<LdapTestDto, FreeIpaClient> createIfNotExistV1() {
        return new LdapCreateIfNotExistsAction();
    }

    public Action<LdapTestDto, FreeIpaClient> describeV1() {
        return new LdapGetAction();
    }

    public Action<LdapTestDto, FreeIpaClient> deleteV1() {
        return new LdapDeleteAction();
    }
}
