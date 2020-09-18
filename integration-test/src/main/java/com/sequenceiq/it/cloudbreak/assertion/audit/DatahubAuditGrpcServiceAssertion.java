package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;

@Component
public class DatahubAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<DistroXTestDto, CloudbreakClient> {

    @Override
    protected String getStopEventName() {
        return "StopDatahubCluster";
    }

    @Override
    protected String getDeleteEventName() {
        return "DeleteDatahubCluster";
    }

    @Override
    protected String getStartEventName() {
        return "StartDatahubCluster";
    }

    @Override
    protected String getCreateEventName() {
        return "CreateDatahubCluster";
    }

    @Override
    protected Crn.Service getService() {
        return Crn.Service.DATAHUB;
    }
}
