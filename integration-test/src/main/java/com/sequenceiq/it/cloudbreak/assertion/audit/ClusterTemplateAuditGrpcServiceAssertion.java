package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;

@Component
public class ClusterTemplateAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<ClusterTemplateTestDto, CloudbreakClient> {

    @Override
    protected String getCreateEventName() {
        return "CreateClusterTemplate";
    }

    @Override
    protected String getDeleteEventName() {
        return "DeleteClusterTemplate";
    }

    @Override
    protected Crn.Service getService() {
        return Crn.Service.DATAHUB;
    }

    @Override
    protected boolean shouldCheckFlowEvents() {
        return false;
    }
}
