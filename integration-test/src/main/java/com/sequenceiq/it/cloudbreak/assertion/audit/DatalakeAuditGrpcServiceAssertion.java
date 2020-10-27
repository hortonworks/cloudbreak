package com.sequenceiq.it.cloudbreak.assertion.audit;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ListAuditEvent;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;

@Component
public class DatalakeAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<SdxTestDto, SdxClient> {

    public SdxTestDto rotateAutotlsCertificates(TestContext testContext, SdxTestDto testDto, SdxClient client) {
        List<AuditProto.CdpAuditEvent> cdpAuditEvents = getAuditClient().listEvents(ListAuditEvent.builder()
                .actor(ActorCrn.builder().withActorCrn(testContext.getActingUserCrn().toString()).build())
                .eventSource(getService()).build());
        validateEventList(cdpAuditEvents, testDto, "RotateDatalakeClusterCertificates");
        return testDto;
    }

    @Override
    protected String getStopEventName() {
        return "StopDatalakeCluster";
    }

    @Override
    protected String getDeleteEventName() {
        return "DeleteDatalakeCluster";
    }

    @Override
    protected String getStartEventName() {
        return "StartDatalakeCluster";
    }

    @Override
    protected String getCreateEventName() {
        return "CreateDatalakeCluster";
    }

    @Override
    protected Crn.Service getService() {
        return Crn.Service.DATALAKE;
    }
}
