package com.sequenceiq.it.cloudbreak.newway.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.ambarirepository.AmbariRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
    public class AmbariRepositoryV4TestDto extends AbstractCloudbreakTestDto<AmbariRepositoryV4Request, AmbariRepositoryV4Response, AmbariRepositoryV4TestDto> {

    protected AmbariRepositoryV4TestDto(TestContext testContext) {
        super(new AmbariRepositoryV4Request(), testContext);
    }

    @Override
    public AmbariRepositoryV4TestDto valid() {
        return withVersion("2.7.2.2")
                .withGpgKeyUrl("http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins")
                .withBaseUrl("http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.7.2.2");
    }

    public AmbariRepositoryV4TestDto withVersion(String version) {
        getRequest().setVersion(version);
        return this;
    }

    public AmbariRepositoryV4TestDto withBaseUrl(String baseUrl) {
        getRequest().setBaseUrl(baseUrl);
        return this;
    }

    public AmbariRepositoryV4TestDto withGpgKeyUrl(String gpgKeyUrl) {
        getRequest().setGpgKeyUrl(gpgKeyUrl);
        return this;
    }
}
