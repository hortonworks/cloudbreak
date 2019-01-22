package com.sequenceiq.it.cloudbreak.newway.entity;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.RepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.stackrepository.StackRepositoryV4Response;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class StackRepositoryEntity extends AbstractCloudbreakEntity<StackRepositoryV4Request, StackRepositoryV4Response, StackRepositoryEntity> {

    protected StackRepositoryEntity(TestContext testContext) {
        super(new StackRepositoryV4Request(), testContext);
    }

    @Override
    public CloudbreakEntity valid() {
        return withVersion("2.6")
                .withStack("HDP")
                .withRepoId("HDP")
                .withRepositoryVersion("2.6.5.0-292")
                .withVersionDefinitionFileUrl("http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.5.0/HDP-2.6.5.0-292.xml")
                .withVerify(true);
    }

    public StackRepositoryEntity withVersion(String version) {
        getRequest().setVersion(version);
        return this;
    }

    public StackRepositoryEntity withOs(String os) {
        getRequest().setOs(os);
        return this;
    }

    public StackRepositoryEntity withOsType(String osType) {
        getRequest().setOsType(osType);
        return this;
    }

    public StackRepositoryEntity withRepoId(String stackRepoId) {
        getRequest().setRepoId(stackRepoId);
        return this;
    }

    public StackRepositoryEntity withStackBaseURL(String stackBaseURL) {
        if (getRequest().getRepository() == null) {
            getRequest().setRepository(new RepositoryV4Request());
        }
        getRequest().getRepository().setBaseUrl(stackBaseURL);
        return this;
    }

    public StackRepositoryEntity withUtilsRepoId(String utilsRepoId) {
        getRequest().setUtilsRepoId(utilsRepoId);
        return this;
    }

    public StackRepositoryEntity withUtilsBaseURL(String utilsBaseURL) {
        getRequest().setUtilsBaseURL(utilsBaseURL);
        return this;
    }

    public StackRepositoryEntity withEnableGplRepo(boolean enableGplRepo) {
        getRequest().setEnableGplRepo(enableGplRepo);
        return this;
    }

    public StackRepositoryEntity withVerify(boolean verify) {
        getRequest().setVerify(verify);
        return this;
    }

    public StackRepositoryEntity withRepositoryVersion(String repositoryVersion) {
        if (getRequest().getRepository() == null) {
            getRequest().setRepository(new RepositoryV4Request());
        }
        getRequest().getRepository().setVersion(repositoryVersion);
        return this;
    }

    public StackRepositoryEntity withVersionDefinitionFileUrl(String versionDefinitionFileUrl) {
        getRequest().setVersionDefinitionFileUrl(versionDefinitionFileUrl);
        return this;
    }

    public StackRepositoryEntity withMpackUrl(String mpackUrl) {
        getRequest().setMpackUrl(mpackUrl);
        return this;
    }

    public StackRepositoryEntity withMpacks(String... mpacks) {
        getRequest().setMpacks(Stream.of(mpacks).map(key -> {
            ManagementPackDetailsEntity entity = getTestContext().get(key);
            return entity.getRequest();
        }).collect(Collectors.toList()));
        return this;
    }

    public StackRepositoryEntity withGpgKeyUrl(String gpgKeyUrl) {
        if (getRequest().getRepository() == null) {
            getRequest().setRepository(new RepositoryV4Request());
        }
        getRequest().getRepository().setGpgKeyUrl(gpgKeyUrl);
        return this;
    }

    public StackRepositoryEntity withStack(String stack) {
        getRequest().setStack(stack);
        return this;
    }
}
