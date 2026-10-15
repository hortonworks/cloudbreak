package com.sequenceiq.cloudbreak.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeComponents;

class UpgradeComponentsConverterTest {

    @Test
    void copiesTheRequestInsteadOfKeepingMutableRequestObjects() {
        ClouderaManagerV4Request request = request();

        UpgradeComponents components = UpgradeComponentsConverter.fromRequest(request);
        request.getRepository().setBaseUrl("https://changed/cm/");
        request.getProducts().getFirst().setVersion("changed");
        request.getProducts().getFirst().getCsd().clear();

        assertThat(components.repository().getBaseUrl()).isEqualTo("https://build/cm/");
        assertThat(components.products().getFirst().getVersion()).isEqualTo("7.3.2-1.cdh7.3.2.p20000.83281140");
        assertThat(components.products().getFirst().getCsd()).containsExactly("https://build/csd.jar");
    }

    @Test
    void restoresFullCmVersionAndParcelsForReinitiation() {
        UpgradeComponents components = UpgradeComponentsConverter.fromRequest(request());
        components.repository().setVersion("7.13.2.20000");
        components.repository().setBuildNumber("83309838");

        ClouderaManagerV4Request request = UpgradeComponentsConverter.toRequest(components);

        assertThat(request.getRepository().getVersion()).isEqualTo("7.13.2.20000-83309838");
        assertThat(request.getRepository().getBaseUrl()).isEqualTo("https://build/cm/");
        assertThat(request.getProducts().getFirst().getParcel()).isEqualTo("https://build/cdh/");
        assertThat(request.getProducts().getFirst().getCsd()).containsExactly("https://build/csd.jar");
    }

    @Test
    void preservesNullProductForRequestValidation() {
        ClouderaManagerV4Request request = UpgradeComponentsConverter.toRequest(new UpgradeComponents(null, Collections.singletonList(null)));

        assertThat(request.getRepository()).isNull();
        assertThat(request.getProducts()).containsExactly((ClouderaManagerProductV4Request) null);
    }

    private ClouderaManagerV4Request request() {
        return new ClouderaManagerV4Request().withRepository(new ClouderaManagerRepositoryV4Request()
                .withVersion("7.13.2.20000-83309838").withBaseUrl("https://build/cm/"))
                .withProducts(List.of(new ClouderaManagerProductV4Request().withName("CDH")
                        .withVersion("7.3.2-1.cdh7.3.2.p20000.83281140").withParcel("https://build/cdh/")
                        .withCsd(new ArrayList<>(List.of("https://build/csd.jar")))));
    }

    @Test
    void copiesMutableFieldsWhenRestoringRequest() {
        UpgradeComponents components = UpgradeComponentsConverter.fromRequest(request());
        ClouderaManagerV4Request restored = UpgradeComponentsConverter.toRequest(components);
        restored.getRepository().setBaseUrl("https://changed/cm/");
        restored.getProducts().getFirst().setVersion("changed");
        restored.getProducts().getFirst().getCsd().clear();

        assertThat(components.repository().getBaseUrl()).isEqualTo("https://build/cm/");
        assertThat(components.products().getFirst().getVersion()).isEqualTo("7.3.2-1.cdh7.3.2.p20000.83281140");
        assertThat(components.products().getFirst().getCsd()).containsExactly("https://build/csd.jar");
    }

    @Test
    void preservesMissingRepositoryAndProductsInBothDirections() {
        UpgradeComponents components = UpgradeComponentsConverter.fromRequest(new ClouderaManagerV4Request());
        ClouderaManagerV4Request restored = UpgradeComponentsConverter.toRequest(components);

        assertThat(components.repository()).isNull();
        assertThat(components.products()).isNull();
        assertThat(restored.getRepository()).isNull();
        assertThat(restored.getProducts()).isNull();
    }

}
