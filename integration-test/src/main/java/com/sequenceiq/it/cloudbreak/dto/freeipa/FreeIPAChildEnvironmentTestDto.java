package com.sequenceiq.it.cloudbreak.dto.freeipa;

import java.util.Collection;

import javax.inject.Inject;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.FreeIPATestClient;
import com.sequenceiq.it.cloudbreak.context.Purgable;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIPATestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.search.Searchable;

@Prototype
public class FreeIPAChildEnvironmentTestDto extends AbstractFreeIPATestDto<AttachChildEnvironmentRequest, Void, FreeIPAChildEnvironmentTestDto>
        implements Purgable<ListFreeIpaResponse, FreeIPAClient>, Searchable {

    public static final String CHILD_ENVIRONMENT_KEY = "childEnv";

    @Inject
    private FreeIPATestClient freeIPATestClient;

    public FreeIPAChildEnvironmentTestDto(TestContext testContext) {
        super(new AttachChildEnvironmentRequest(), testContext);
    }

    @Override
    public FreeIPAChildEnvironmentTestDto valid() {
        getRequest().setParentEnvironmentCrn(getParentEnvironmentCrn());
        getRequest().setChildEnvironmentCrn(getChildEnvironmentCrn());
        return this;
    }

    private String getChildEnvironmentCrn() {
        EnvironmentTestDto childEnvironment = getTestContext().get(CHILD_ENVIRONMENT_KEY);
        return childEnvironment.getResponse().getCrn();
    }

    private String getParentEnvironmentCrn() {
        return getTestContext().get(EnvironmentTestDto.class).getResponse().getCrn();
    }

    @Override
    public Collection<ListFreeIpaResponse> getAll(FreeIPAClient client) {
        return client.getFreeIpaClient().getFreeIpaV1Endpoint().list();
    }

    @Override
    public boolean deletable(ListFreeIpaResponse entity) {
        return entity.getName().startsWith(getResourcePropertyProvider().prefix());
    }

    @Override
    public void delete(TestContext testContext, ListFreeIpaResponse entity, FreeIPAClient client) {
        DetachChildEnvironmentRequest request = new DetachChildEnvironmentRequest();
        request.setParentEnvironmentCrn(getParentEnvironmentCrn());
        request.setChildEnvironmentCrn(getChildEnvironmentCrn());
        client.getFreeIpaClient().getFreeIpaV1Endpoint().detachChildEnvironment(request);
    }

    @Override
    public Class<FreeIPAClient> client() {
        return FreeIPAClient.class;
    }

    @Override
    public String getSearchId() {
        return getName();
    }
}
