package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;

@Component
public class ClusterTemplateAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<ClusterTemplateTestDto, CloudbreakClient> {

    @Override
    protected OperationInfo getCreateOperationInfo() {
        return OperationInfo.builder().withEventName("CreateClusterTemplate").build();
    }

    @Override
    protected OperationInfo getDeleteOperationInfo() {
        return OperationInfo.builder().withEventName("DeleteClusterTemplate").build();
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
