package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.ldap.LdapCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.ldap.LdapCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.action.v4.ldap.LdapDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.ldap.LdapGetAction;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;

@Service
public class LdapTestClient {
    public Action<LdapTestDto, FreeIPAClient> createV1() {
        return new LdapCreateAction();
    }

    public Action<LdapTestDto, FreeIPAClient> createIfNotExistV1() {
        return new LdapCreateIfNotExistsAction();
    }

    public Action<LdapTestDto, FreeIPAClient> describeV1() {
        return new LdapGetAction();
    }

    public Action<LdapTestDto, FreeIPAClient> deleteV1() {
        return new LdapDeleteAction();
    }
}
