package com.sequenceiq.it.cloudbreak.assertion.audit;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ListAuditEvent;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
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

    public DistroXTestDto upgradeClusterByNameInternal(TestContext testContext, DistroXTestDto testDto) {
        List<AuditProto.CdpAuditEvent> cdpAuditEvents = getAuditClient().listEvents(ListAuditEvent.builder()
                .actor(ActorCrn.builder().withActorCrn(testContext.getActingUserCrn().toString()).build())
                .eventSource(getService()).build());
        validateEventList(cdpAuditEvents, testDto, "UpgradeDatahubCluster");
        return testDto;
    }

    @Override
    protected Crn.Service getService() {
        return Crn.Service.DATAHUB;
    }
}
