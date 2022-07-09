package com.sequenceiq.it.cloudbreak.dto.idbmms;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import javax.inject.Inject;

import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.it.cloudbreak.IdbmmsClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class IdbmmsTestDto extends AbstractTestDto<IdBrokerMappingManagementProto.SetMappingsRequest, IdBrokerMappingManagementProto.SetMappingsResponse,
        IdbmmsTestDto, IdbmmsClient> {

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    private String crn;

    private IdBrokerMappingManagementProto.GetMappingsResponse getMappingsResponse;

    public IdbmmsTestDto(String newId) {
        super(newId);
    }

    public IdbmmsTestDto(IdBrokerMappingManagementProto.SetMappingsRequest request, TestContext testContext) {
        super(request, testContext);
    }

    public IdbmmsTestDto(TestContext testContext) {
        super(IdBrokerMappingManagementProto.SetMappingsRequest.newBuilder().build(), testContext);
    }

    public IdbmmsTestDto() {
        super(IdbmmsTestDto.class.getSimpleName().toUpperCase());
    }

    @Override
    public IdbmmsTestDto valid() {
        String environmentCrn = getTestContext().get(EnvironmentTestDto.class).getCrn();
        IdBrokerMappingManagementProto.SetMappingsRequest request = defaultRequest(environmentCrn);
        setRequest(request);
        return this;
    }

    private IdBrokerMappingManagementProto.SetMappingsRequest defaultRequest(String environmentCrn) {
        IdBrokerMappingManagementProto.SetMappingsRequest request = IdBrokerMappingManagementProto.SetMappingsRequest.newBuilder()
                .setAccountId(Crn.safeFromString(environmentCrn).getAccountId())
                .setEnvironmentNameOrCrn(environmentCrn)
                .setDataAccessRole(getCloudProvider().getDataAccessRole())
                .setBaselineRole(getCloudProvider().getRangerAuditRole())
                .build();
        return request;
    }

    @Override
    public String getCrn() {
        return crn;
    }

    public IdbmmsTestDto withCrn(String crn) {
        this.crn = crn;
        return this;
    }

    public void setMappingsDetails(IdBrokerMappingManagementProto.GetMappingsResponse getMappingsResponse) {
        this.getMappingsResponse = getMappingsResponse;
    }

    public IdBrokerMappingManagementProto.GetMappingsResponse getMappingsDetails() {
        return getMappingsResponse;
    }

    @Override
    public IdbmmsTestDto when(Action<IdbmmsTestDto, IdbmmsClient> action) {
        return getTestContext().when(this, IdbmmsClient.class, action, emptyRunningParameter());
    }

    @Override
    public IdbmmsTestDto then(Assertion<IdbmmsTestDto, IdbmmsClient> assertion) {
        return then(assertion, emptyRunningParameter());
    }

    @Override
    public IdbmmsTestDto then(Assertion<IdbmmsTestDto, IdbmmsClient> assertion, RunningParameter runningParameter) {
        return getTestContext().then(this, IdbmmsClient.class, assertion, runningParameter);
    }

}
