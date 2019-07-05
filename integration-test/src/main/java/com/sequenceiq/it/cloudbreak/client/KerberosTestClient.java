package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.kerberos.KerberosCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.kerberos.KerberosDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.kerberos.KerberosGetAction;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;

@Service
public class KerberosTestClient {

    public Action<KerberosTestDto, FreeIPAClient> createV1() {
        return new KerberosCreateAction();
    }

    public Action<KerberosTestDto, FreeIPAClient> deleteV1() {
        return new KerberosDeleteAction();
    }

    public Action<KerberosTestDto, FreeIPAClient> describeV1() {
        return new KerberosGetAction();
    }

}