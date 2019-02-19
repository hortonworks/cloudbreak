package com.sequenceiq.it.cloudbreak.newway.entity.mpack;

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
public class MPackTestDto extends AbstractCloudbreakEntity<ManagementPackV4Request, ManagementPackV4Response, MPackTestDto>
        implements Purgable<ManagementPackV4Response> {

    public MPackTestDto(TestContext testContext) {
        super(new ManagementPackV4Request(), testContext);
    }

    public MPackTestDto valid() {
        return withName(getNameCreator().getRandomNameForMock())
                .witMpackUrl("http://public-repo-1.hortonworks.com/HDF/centos7/3.x/updates/3.2.0.0/tars/hdf_ambari_mp/hdf-ambari-mpack-3.2.0.0-520.tar.gz");
    }

    public MPackTestDto withName(String name) {
        getRequest().setName(name);
        return this;
    }

    public MPackTestDto withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public MPackTestDto withForce(boolean force) {
        getRequest().setForce(force);
        return this;
    }

    public MPackTestDto witMpackUrl(String mpackUrl) {
        getRequest().setMpackUrl(mpackUrl);
        return this;
    }

    public MPackTestDto withPurge(boolean purge) {
        getRequest().setPurge(purge);
        return this;
    }

    public MPackTestDto withPurgeList(List<String> purgeList) {
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
    public void delete(TestContext testContext, ManagementPackV4Response entity, CloudbreakClient client) {
        client.getCloudbreakClient().managementPackV4Endpoint().deleteInWorkspace(client.getWorkspaceId(), entity.getName());
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        delete(context, getResponse(), cloudbreakClient);
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
