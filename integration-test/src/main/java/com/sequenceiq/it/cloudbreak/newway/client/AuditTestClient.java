package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.v4.audit.AuditGetAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.audit.AuditListAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.audit.AuditListZipAction;
import com.sequenceiq.it.cloudbreak.newway.dto.audit.AuditTestDto;

@Service
public class AuditTestClient {

    public Action<AuditTestDto> getV4() {
        return new AuditGetAction();
    }

    public Action<AuditTestDto> listV4() {
        return new AuditListAction();
    }

    public Action<AuditTestDto> listZipV4() {
        return new AuditListZipAction();
    }
}
