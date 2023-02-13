package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.audit.AuditGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.audit.AuditListAction;
import com.sequenceiq.it.cloudbreak.action.v4.audit.AuditListZipAction;
import com.sequenceiq.it.cloudbreak.dto.audit.AuditTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

@Service
public class AuditTestClient {

    public Action<AuditTestDto, CloudbreakClient> getV4() {
        return new AuditGetAction();
    }

    public Action<AuditTestDto, CloudbreakClient> listV4() {
        return new AuditListAction();
    }

    public Action<AuditTestDto, CloudbreakClient> listZipV4() {
        return new AuditListZipAction();
    }
}
