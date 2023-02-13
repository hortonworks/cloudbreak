package com.sequenceiq.it.cloudbreak.dto.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto;
import com.cloudera.thunderhead.service.sdxsvccommon.SDXSvcCommonProto;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxSaasItClient;

@Prototype
public class SdxSaasTestDto extends AbstractTestDto<SDXSvcAdminProto.CreateInstanceRequest, SDXSvcCommonProto.Instance, SdxSaasTestDto, SdxSaasItClient> {

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    private String crn;

    public SdxSaasTestDto(SDXSvcAdminProto.CreateInstanceRequest request, TestContext testContext) {
        super(request, testContext);
    }

    public SdxSaasTestDto(TestContext testContext) {
        super(SDXSvcAdminProto.CreateInstanceRequest.newBuilder().build(), testContext);
    }

    @Override
    public SdxSaasTestDto valid() {
        String environmentCrn = getTestContext().get(EnvironmentTestDto.class).getCrn();
        SDXSvcAdminProto.CreateInstanceRequest request = defaultRequest(environmentCrn);
        setRequest(request);
        return this;
    }

    private SDXSvcAdminProto.CreateInstanceRequest defaultRequest(String environmentCrn) {
        SDXSvcAdminProto.CreateInstanceRequest request = SDXSvcAdminProto.CreateInstanceRequest.newBuilder()
                .setCloudPlatform(SDXSvcCommonProto.CloudPlatform.Value.AWS)
                .setCloudRegion(regionAwareInternalCrnGeneratorFactory.getRegion())
                .setEnvironment(environmentCrn)
                .setAccountId(Crn.safeFromString(environmentCrn).getAccountId())
                .setName(getResourcePropertyProvider().getName())
                .build();
        return request;
    }

    @Override
    public String getCrn() {
        return crn;
    }

    public SdxSaasTestDto withCrn(String crn) {
        this.crn = crn;
        return this;
    }

    @Override
    public SdxSaasTestDto when(Action<SdxSaasTestDto, SdxSaasItClient> action) {
        return getTestContext().when(this, SdxSaasItClient.class, action, emptyRunningParameter());
    }

    @Override
    public void deleteForCleanup() {
        try {
            getClientForCleanup().getDefaultClient().deleteInstance(crn);
        } catch (NotFoundException nfe) {
            LOGGER.info("SDX SaaS resource not found, thus cleanup not needed.");
        }
    }
}
