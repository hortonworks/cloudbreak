package com.sequenceiq.it.cloudbreak.newway.entity;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
    public class AmbariRepoDetailsEntity extends AbstractCloudbreakEntity<AmbariRepoDetailsJson, Response, AmbariRepoDetailsEntity> {

    protected AmbariRepoDetailsEntity(TestContext testContext) {
        super(new AmbariRepoDetailsJson(), testContext);
    }

    @Override
    public AmbariRepoDetailsEntity valid() {
        return withVersion("2.6.2.2")
                .withGpgKeyUrl("http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins")
                .withBaseUrl("http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.6.2.2");
    }

    public AmbariRepoDetailsEntity withVersion(String version) {
        getRequest().setVersion(version);
        return this;
    }

    public AmbariRepoDetailsEntity withBaseUrl(String baseUrl) {
        getRequest().setBaseUrl(baseUrl);
        return this;
    }

    public AmbariRepoDetailsEntity withGpgKeyUrl(String gpgKeyUrl) {
        getRequest().setGpgKeyUrl(gpgKeyUrl);
        return this;
    }
}
