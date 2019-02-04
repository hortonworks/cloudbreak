package com.sequenceiq.it.cloudbreak.newway.entity;

import java.util.Collection;
import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v3.ManagementPackV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackRequest;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackResponse;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.Purgable;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ManagementPackEntity extends AbstractCloudbreakEntity<ManagementPackRequest, ManagementPackResponse, ManagementPackEntity>
        implements Purgable<ManagementPackResponse> {

    public ManagementPackEntity(TestContext testContext) {
        super(new ManagementPackRequest(), testContext);
    }

    public ManagementPackEntity valid() {
        return withName(getNameCreator().getRandomNameForMock())
                .witMpackUrl("http://public-repo-1.hortonworks.com/HDF/centos7/3.x/updates/3.2.0.0/tars/hdf_ambari_mp/hdf-ambari-mpack-3.2.0.0-520.tar.gz");
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
    public Collection<ManagementPackResponse> getAll(CloudbreakClient client) {
        ManagementPackV3Endpoint managementPackV3Endpoint = client.getCloudbreakClient().managementPackV3Endpoint();
        return managementPackV3Endpoint.listByWorkspace(client.getWorkspaceId());
    }

    @Override
    public boolean deletable(ManagementPackResponse entity) {
        return entity.getName().startsWith("mock-");
    }

    @Override
    public void delete(ManagementPackResponse entity, CloudbreakClient client) {
        client.getCloudbreakClient().managementPackV3Endpoint().deleteInWorkspace(client.getWorkspaceId(), entity.getName());
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
