package com.sequenceiq.it.cloudbreak.newway.dto;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.repository.RepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.stackrepository.StackRepositoryV4Response;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class StackRepositoryTestDto extends AbstractCloudbreakTestDto<StackRepositoryV4Request, StackRepositoryV4Response, StackRepositoryTestDto> {

    protected StackRepositoryTestDto(TestContext testContext) {
        super(new StackRepositoryV4Request(), testContext);
    }

    @Override
    public CloudbreakTestDto valid() {
        return withVersion("2.7")
                .withStack("HDP")
                .withRepoId("HDP")
                .withRepositoryVersion("2.7.5.0-292")
                .withVersionDefinitionFileUrl("http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.7.5.0/HDP-2.7.5.0-292.xml")
                .withVerify(true);
    }

    public StackRepositoryTestDto withVersion(String version) {
        getRequest().setVersion(version);
        return this;
    }

    public StackRepositoryTestDto withOs(String os) {
        getRequest().setOs(os);
        return this;
    }

    public StackRepositoryTestDto withOsType(String osType) {
        getRequest().setOsType(osType);
        return this;
    }

    public StackRepositoryTestDto withRepoId(String stackRepoId) {
        getRequest().setRepoId(stackRepoId);
        return this;
    }

    public StackRepositoryTestDto withStackBaseURL(String stackBaseURL) {
        if (getRequest().getRepository() == null) {
            getRequest().setRepository(new RepositoryV4Request());
        }
        getRequest().getRepository().setBaseUrl(stackBaseURL);
        return this;
    }

    public StackRepositoryTestDto withUtilsRepoId(String utilsRepoId) {
        getRequest().setUtilsRepoId(utilsRepoId);
        return this;
    }

    public StackRepositoryTestDto withUtilsBaseURL(String utilsBaseURL) {
        getRequest().setUtilsBaseURL(utilsBaseURL);
        return this;
    }

    public StackRepositoryTestDto withEnableGplRepo(boolean enableGplRepo) {
        getRequest().setEnableGplRepo(enableGplRepo);
        return this;
    }

    public StackRepositoryTestDto withVerify(boolean verify) {
        getRequest().setVerify(verify);
        return this;
    }

    public StackRepositoryTestDto withRepositoryVersion(String repositoryVersion) {
        if (getRequest().getRepository() == null) {
            getRequest().setRepository(new RepositoryV4Request());
        }
        getRequest().getRepository().setVersion(repositoryVersion);
        return this;
    }

    public StackRepositoryTestDto withVersionDefinitionFileUrl(String versionDefinitionFileUrl) {
        getRequest().setVersionDefinitionFileUrl(versionDefinitionFileUrl);
        return this;
    }

    public StackRepositoryTestDto withMpackUrl(String mpackUrl) {
        getRequest().setMpackUrl(mpackUrl);
        return this;
    }

    public StackRepositoryTestDto withMpacks(String... mpacks) {
        getRequest().setMpacks(Stream.of(mpacks).map(key -> {
            ManagementPackDetailsTestDto entity = getTestContext().get(key);
            return entity.getRequest();
        }).collect(Collectors.toList()));
        return this;
    }

    public StackRepositoryTestDto withGpgKeyUrl(String gpgKeyUrl) {
        if (getRequest().getRepository() == null) {
            getRequest().setRepository(new RepositoryV4Request());
        }
        getRequest().getRepository().setGpgKeyUrl(gpgKeyUrl);
        return this;
    }

    public StackRepositoryTestDto withStack(String stack) {
        getRequest().setStack(stack);
        return this;
    }
}
