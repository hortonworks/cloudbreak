package com.sequenceiq.it.cloudbreak.newway.entity;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsResponse;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class AmbariStackDetailsEntity extends AbstractCloudbreakEntity<AmbariStackDetailsJson, AmbariStackDetailsResponse, AmbariStackDetailsEntity> {

    protected AmbariStackDetailsEntity(TestContext testContext) {
        super(new AmbariStackDetailsJson(), testContext);
    }

    @Override
    public CloudbreakEntity valid() {
        return withVersion("2.6")
                .withStack("HDP")
                .withStackRepoId("HDP")
                .withRepositoryVersion("2.6.5.0-292")
                .withVersionDefinitionFileUrl("http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.5.0/HDP-2.6.5.0-292.xml")
                .withVerify(true);
    }

    public AmbariStackDetailsEntity withVersion(String version) {
        getRequest().setVersion(version);
        return this;
    }

    public AmbariStackDetailsEntity withOs(String os) {
        getRequest().setOs(os);
        return this;
    }

    public AmbariStackDetailsEntity withOsType(String osType) {
        getRequest().setOsType(osType);
        return this;
    }

    public AmbariStackDetailsEntity withStackRepoId(String stackRepoId) {
        getRequest().setStackRepoId(stackRepoId);
        return this;
    }

    public AmbariStackDetailsEntity withStackBaseURL(String stackBaseURL) {
        getRequest().setStackBaseURL(stackBaseURL);
        return this;
    }

    public AmbariStackDetailsEntity withUtilsRepoId(String utilsRepoId) {
        getRequest().setUtilsRepoId(utilsRepoId);
        return this;
    }

    public AmbariStackDetailsEntity withUtilsBaseURL(String utilsBaseURL) {
        getRequest().setUtilsBaseURL(utilsBaseURL);
        return this;
    }

    public AmbariStackDetailsEntity withEnableGplRepo(boolean enableGplRepo) {
        getRequest().setEnableGplRepo(enableGplRepo);
        return this;
    }

    public AmbariStackDetailsEntity withVerify(boolean verify) {
        getRequest().setVerify(verify);
        return this;
    }

    public AmbariStackDetailsEntity withRepositoryVersion(String repositoryVersion) {
        getRequest().setRepositoryVersion(repositoryVersion);
        return this;
    }

    public AmbariStackDetailsEntity withVersionDefinitionFileUrl(String versionDefinitionFileUrl) {
        getRequest().setVersionDefinitionFileUrl(versionDefinitionFileUrl);
        return this;
    }

    public AmbariStackDetailsEntity withMpackUrl(String mpackUrl) {
        getRequest().setMpackUrl(mpackUrl);
        return this;
    }

    public AmbariStackDetailsEntity withMpacks(String... mpacks) {
        getRequest().setMpacks(Stream.of(mpacks).map(key -> {
            ManagementPackDetailsEntity entity = getTestContext().get(key);
            return entity.getRequest();
        }).collect(Collectors.toList()));
        return this;
    }

    public AmbariStackDetailsEntity withGpgKeyUrl(String gpgKeyUrl) {
        getRequest().setGpgKeyUrl(gpgKeyUrl);
        return this;
    }

    public AmbariStackDetailsEntity withStack(String stack) {
        getRequest().setStack(stack);
        return this;
    }
}
