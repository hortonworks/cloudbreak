package com.sequenceiq.it.cloudbreak.dto;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerV4Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;

@Prototype
public class StackRepositoryTestDto extends AbstractCloudbreakTestDto<ClouderaManagerV4Request, ClouderaManagerV4Response, StackRepositoryTestDto> {

    protected StackRepositoryTestDto(TestContext testContext) {
        super(new ClouderaManagerV4Request(), testContext);
    }

    @Override
    public CloudbreakTestDto valid() {
        return withVersion("2.7")
                .withProductName("CDH")
                .withVersion("2.7.5.0-292")
                .withParcel("http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.7.5.0/HDP-2.7.5.0-292.xml")
                .withBaseURL("http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.7.5.0/HDP-2.7.5.0-292.xml")
                .withRepositoryVersion("2.7.5.0-292")
                .withGpgKeyUrl("http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.7.5.0/HDP-2.7.5.0-292.xml");
    }

    public StackRepositoryTestDto withVersion(String version) {
        if (getRequest().getProducts() == null) {
            getRequest().setProducts(List.of(new ClouderaManagerProductV4Request()));
        }
        getRequest().getProducts().get(0).setVersion(version);
        return this;
    }

    public StackRepositoryTestDto withProductName(String name) {
        if (getRequest().getProducts() == null) {
            getRequest().setProducts(List.of(new ClouderaManagerProductV4Request()));
        }
        getRequest().getProducts().get(0).setName(name);
        return this;
    }

    public StackRepositoryTestDto withParcel(String parcel) {
        if (getRequest().getProducts() == null) {
            getRequest().setProducts(List.of(new ClouderaManagerProductV4Request()));
        }
        getRequest().getProducts().get(0).setParcel(parcel);
        return this;
    }

    public StackRepositoryTestDto withCsd(List<String> csd) {
        if (getRequest().getProducts() == null) {
            getRequest().setProducts(List.of(new ClouderaManagerProductV4Request()));
        }
        getRequest().getProducts().get(0).setCsd(csd);
        return this;
    }

    public StackRepositoryTestDto withBaseURL(String stackBaseURL) {
        if (getRequest().getRepository() == null) {
            getRequest().setRepository(new ClouderaManagerRepositoryV4Request());
        }
        getRequest().getRepository().setBaseUrl(stackBaseURL);
        return this;
    }

    public StackRepositoryTestDto withRepositoryVersion(String version) {
        if (getRequest().getRepository() == null) {
            getRequest().setRepository(new ClouderaManagerRepositoryV4Request());
        }
        getRequest().getRepository().setVersion(version);
        return this;
    }

    public StackRepositoryTestDto withGpgKeyUrl(String gpgKeyUrl) {
        if (getRequest().getRepository() == null) {
            getRequest().setRepository(new ClouderaManagerRepositoryV4Request());
        }
        getRequest().getRepository().setGpgKeyUrl(gpgKeyUrl);
        return this;
    }
}
