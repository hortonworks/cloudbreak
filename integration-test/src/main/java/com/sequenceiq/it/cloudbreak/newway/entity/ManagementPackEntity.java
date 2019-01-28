package com.sequenceiq.it.cloudbreak.newway.entity;

import java.util.Collection;
import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.request.ManagementPackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.response.ManagementPackV4Response;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.Purgable;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ManagementPackEntity extends AbstractCloudbreakEntity<ManagementPackV4Request, ManagementPackV4Response, ManagementPackEntity>
        implements Purgable<ManagementPackV4Response> {

    public ManagementPackEntity(TestContext testContext) {
        super(new ManagementPackV4Request(), testContext);
    }

    public ManagementPackEntity valid() {
        return withName(getNameCreator().getRandomNameForMock())
                .witMpackUrl("http://some.mpack/url");
    }

    public ManagementPackEntity withName(String name) {
        getRequest().setName(name);
        return this;
    }

    public ManagementPackEntity withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public ManagementPackEntity withForce(boolean force) {
        getRequest().setForce(force);
        return this;
    }

    public ManagementPackEntity witMpackUrl(String mpackUrl) {
        getRequest().setMpackUrl(mpackUrl);
        return this;
    }

    public ManagementPackEntity withPurge(boolean purge) {
        getRequest().setPurge(purge);
        return this;
    }

    public ManagementPackEntity withPurgeList(List<String> purgeList) {
        getRequest().setPurgeList(purgeList);
        return this;
    }

    @Override
    public Collection<ManagementPackV4Response> getAll(CloudbreakClient client) {
        return client.getCloudbreakClient().managementPackV4Endpoint().listByWorkspace(client.getWorkspaceId()).getResponses();
    }

    @Override
    public boolean deletable(ManagementPackV4Response entity) {
        return entity.getName().startsWith("mock-");
    }

    @Override
    public void delete(ManagementPackV4Response entity, CloudbreakClient client) {
        client.getCloudbreakClient().managementPackV4Endpoint().deleteInWorkspace(client.getWorkspaceId(), entity.getName());
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        delete(getResponse(), cloudbreakClient);
    }

    @Override
    public int order() {
        return 500;
    }

    @Override
    public String getName() {
        return getRequest().getName();
    }
}
