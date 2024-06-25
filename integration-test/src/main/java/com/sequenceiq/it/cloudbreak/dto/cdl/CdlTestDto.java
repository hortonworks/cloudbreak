package com.sequenceiq.it.cloudbreak.dto.cdl;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CdlClient;

@Prototype
public class CdlTestDto extends AbstractTestDto<CdlCrudProto.CreateDatalakeRequest, CdlCrudProto.DatalakeResponse, CdlTestDto, CdlClient> {

    public CdlTestDto(TestContext testContext) {
        super(CdlCrudProto.CreateDatalakeRequest.newBuilder().build(), testContext);
    }

    @Override
    public CdlTestDto valid() {
        return this;
    }

    public CdlTestDto withEnvironmentKey(RunningParameter key) {
        setRequest(CdlCrudProto.CreateDatalakeRequest.newBuilder()
                .setEnvironmentName(getTestContext().given(key.getKey(), EnvironmentTestDto.class).getResourceCrn())
                .setDatalakeName("cdl-test")
                .build());
        return this;
    }

    @Override
    public CdlTestDto when(Action<CdlTestDto, CdlClient> action) {
        return getTestContext().when(this, CdlClient.class, action, emptyRunningParameter());
    }
}
