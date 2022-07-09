package com.sequenceiq.it.cloudbreak.dto.idbmms;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import javax.ws.rs.NotFoundException;

import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.microservice.IdbmmsClient;

@Prototype
public class IdbmmsTestDto extends AbstractTestDto<SetMappingsRequest, SetMappingsResponse,
        IdbmmsTestDto, IdbmmsClient> {

    private String crn;

    private GetMappingsResponse getMappingsResponse;

    private DeleteMappingsResponse deleteMappingsResponse;

    public IdbmmsTestDto(SetMappingsRequest request, TestContext testContext) {
        super(request, testContext);
    }

    public IdbmmsTestDto(TestContext testContext) {
        super(SetMappingsRequest.newBuilder().build(), testContext);
    }

    @Override
    public IdbmmsTestDto valid() {
        String environmentCrn = getTestContext().get(EnvironmentTestDto.class).getCrn();
        SetMappingsRequest request = defaultRequest(environmentCrn);
        setRequest(request);
        return this;
    }

    private SetMappingsRequest defaultRequest(String environmentCrn) {
        SetMappingsRequest request = SetMappingsRequest.newBuilder()
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

    public void setDeleteMappingsDetails(DeleteMappingsResponse deleteMappingsResponse) {
        this.deleteMappingsResponse = deleteMappingsResponse;
    }

    public DeleteMappingsResponse getDeleteMappingsDetails() {
        return deleteMappingsResponse;
    }

    public void setMappingsDetails(GetMappingsResponse getMappingsResponse) {
        this.getMappingsResponse = getMappingsResponse;
    }

    public GetMappingsResponse getMappingsDetails() {
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

    @Override
    public void deleteForCleanup() {
        try {
            String environmentCrn = getTestContext().get(EnvironmentTestDto.class).getCrn();
            getClientForCleanup().getDefaultClient().deleteMappings(getTestContext().getActingUser().getCrn(), environmentCrn);
        } catch (NotFoundException nfe) {
            LOGGER.info("IDBMMS resource not found, thus cleanup not needed.");
        }
    }
}
