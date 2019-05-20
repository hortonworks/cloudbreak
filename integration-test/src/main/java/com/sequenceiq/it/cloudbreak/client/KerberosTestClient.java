package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.kerberos.KerberosCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.kerberos.KerberosDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.kerberos.KerberosGetAction;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;

@Service
public class KerberosTestClient {

    public Action<KerberosTestDto> createV4() {
        return new KerberosCreateAction();
    }

    public Action<KerberosTestDto> deleteV4() {
        return new KerberosDeleteAction();
    }

    public Action<KerberosTestDto> getV4() {
        return new KerberosGetAction();
    }

}