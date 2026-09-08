package com.sequenceiq.cloudbreak.controller.validation.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

class CmRepoOsValidatorTest {

    private static final String REDHAT9 = "redhat9";

    private static final String REDHAT8 = "redhat8";

    private static final String CENTOS7 = "centos7";

    private static final String RH8_BASE_URL = "http://cloudera-build/cm7/7.13.2.10000/redhat8/yum/";

    private static final String RH9_BASE_URL = "http://cloudera-build/cm7/7.13.2.10000/redhat9/yum/";

    private static final String EL8_PARCEL = "http://cloudera-build/cdh/7.x/parcels/CDH-7.3.2-1.cdh7.3.2.p10000-el8.parcel";

    private static final String EL9_PARCEL = "http://cloudera-build/cdh/7.x/parcels/CDH-7.3.2-1.cdh7.3.2.p10000-el9.parcel";

    // Real production catalog URLs (see CB-32586): CFM parcels are published under /redhat8/ and /redhat9/ path segments,
    // with no -el8/-el9 postfix in the file name. The validator must catch these too.
    private static final String CFM_RH8_PARCEL = "https://archive.cloudera.com/p/cfm2/2.2.7.0/redhat8/yum/tars/parcel";

    private static final String CFM_RH9_PARCEL = "https://archive.cloudera.com/p/cfm2/2.2.7.0/redhat9/yum/tars/parcel";

    private final CmRepoOsValidator underTest = new CmRepoOsValidator();

    @Test
    void rhel9ImageWithRedhat8BaseUrlThrows() {
        StackV4Request request = requestWith(RH8_BASE_URL, null);
        Image image = image(REDHAT9);

        assertThatThrownBy(() -> underTest.validate(request, image))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("baseUrl OS 'redhat8'")
                .hasMessageContaining("image OS 'redhat9'")
                .hasMessageContaining("cluster.cm.repository.baseUrl");
    }

    @Test
    void rhel8ImageWithRedhat9BaseUrlThrows() {
        StackV4Request request = requestWith(RH9_BASE_URL, null);
        Image image = image(REDHAT8);

        assertThatThrownBy(() -> underTest.validate(request, image))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("baseUrl OS 'redhat9'")
                .hasMessageContaining("image OS 'redhat8'");
    }

    @ParameterizedTest
    @ValueSource(strings = {REDHAT8, REDHAT9})
    void matchingRhelBaseUrlPasses(String os) {
        String baseUrl = REDHAT8.equals(os) ? RH8_BASE_URL : RH9_BASE_URL;
        StackV4Request request = requestWith(baseUrl, null);

        assertThatCode(() -> underTest.validate(request, image(os))).doesNotThrowAnyException();
    }

    @Test
    void rhel9ImageWithEl8ParcelThrows() {
        StackV4Request request = requestWith(null, List.of(product(EL8_PARCEL)));

        assertThatThrownBy(() -> underTest.validate(request, image(REDHAT9)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("parcel URL OS 'el8'")
                .hasMessageContaining("image OS 'redhat9'")
                .hasMessageContaining("cluster.cm.products[0].parcel");
    }

    @Test
    void rhel9ImageWithEl9ParcelPasses() {
        StackV4Request request = requestWith(null, List.of(product(EL9_PARCEL)));

        assertThatCode(() -> underTest.validate(request, image(REDHAT9))).doesNotThrowAnyException();
    }

    @Test
    void rhel9ImageWithCfmRedhat8ParcelThrows() {
        StackV4Request request = requestWith(null, List.of(product(CFM_RH8_PARCEL)));

        assertThatThrownBy(() -> underTest.validate(request, image(REDHAT9)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("parcel URL OS 'redhat8'")
                .hasMessageContaining("image OS 'redhat9'")
                .hasMessageContaining("cluster.cm.products[0].parcel");
    }

    @Test
    void rhel8ImageWithCfmRedhat9ParcelThrows() {
        StackV4Request request = requestWith(null, List.of(product(CFM_RH9_PARCEL)));

        assertThatThrownBy(() -> underTest.validate(request, image(REDHAT8)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("parcel URL OS 'redhat9'")
                .hasMessageContaining("image OS 'redhat8'")
                .hasMessageContaining("cluster.cm.products[0].parcel");
    }

    @Test
    void rhel9ImageWithCfmRedhat9ParcelPasses() {
        StackV4Request request = requestWith(null, List.of(product(CFM_RH9_PARCEL)));

        assertThatCode(() -> underTest.validate(request, image(REDHAT9))).doesNotThrowAnyException();
    }

    @Test
    void rhel8ImageWithCfmRedhat8ParcelPasses() {
        StackV4Request request = requestWith(null, List.of(product(CFM_RH8_PARCEL)));

        assertThatCode(() -> underTest.validate(request, image(REDHAT8))).doesNotThrowAnyException();
    }

    @Test
    void nullBaseUrlPassesAsCatalogDefaultIsUsed() {
        StackV4Request request = requestWith(null, null);

        assertThatCode(() -> underTest.validate(request, image(REDHAT9))).doesNotThrowAnyException();
    }

    @Test
    void blankBaseUrlPasses() {
        StackV4Request request = requestWith("   ", null);

        assertThatCode(() -> underTest.validate(request, image(REDHAT9))).doesNotThrowAnyException();
    }

    @Test
    void unknownImageOsPasses() {
        StackV4Request request = requestWith(RH8_BASE_URL, List.of(product(EL8_PARCEL)));

        // centos7 image should not have RHEL8/RHEL9 constraint applied
        assertThatCode(() -> underTest.validate(request, image(CENTOS7))).doesNotThrowAnyException();
    }

    @Test
    void nullOsOnImagePasses() {
        StackV4Request request = requestWith(RH8_BASE_URL, null);

        assertThatCode(() -> underTest.validate(request, image(null))).doesNotThrowAnyException();
    }

    @Test
    void nullCmSectionPasses() {
        StackV4Request request = new StackV4Request();
        request.setCluster(new ClusterV4Request());

        assertThatCode(() -> underTest.validate(request, image(REDHAT9))).doesNotThrowAnyException();
    }

    @Test
    void nullProductsPasses() {
        StackV4Request request = requestWith(RH9_BASE_URL, null);

        assertThatCode(() -> underTest.validate(request, image(REDHAT9))).doesNotThrowAnyException();
    }

    @Test
    void nullRequestOrImagePasses() {
        assertThatCode(() -> underTest.validate(null, image(REDHAT9))).doesNotThrowAnyException();
        assertThatCode(() -> underTest.validate(requestWith(RH9_BASE_URL, null), null)).doesNotThrowAnyException();
    }

    @Test
    void parcelTokenBoundaryAvoidsSubstringFalsePositives() {
        // A version string that innocently contains "el8" as part of a longer token must not trip validation on a redhat9 image
        ClouderaManagerProductV4Request innocent = product("http://build/parcels/CDH-7.3.2-1.cdh7.3.2.p10000-el9.parcel");
        StackV4Request request = requestWith(null, List.of(innocent));

        assertThatCode(() -> underTest.validate(request, image(REDHAT9))).doesNotThrowAnyException();

        // But a URL that ends with /el8 (no trailing slash) must still be caught
        ClouderaManagerProductV4Request trailing = product("http://build/parcels/CDH/el8");
        StackV4Request trailingRequest = requestWith(null, List.of(trailing));

        assertThatThrownBy(() -> underTest.validate(trailingRequest, image(REDHAT9)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void productAtIndexOneReportedInMessage() {
        ClouderaManagerProductV4Request ok = product(EL9_PARCEL);
        ClouderaManagerProductV4Request bad = product(EL8_PARCEL);
        StackV4Request request = requestWith(null, List.of(ok, bad));

        assertThatThrownBy(() -> underTest.validate(request, image(REDHAT9)))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(ex.getMessage()).contains("cluster.cm.products[1].parcel"));
    }

    private StackV4Request requestWith(String baseUrl, List<ClouderaManagerProductV4Request> products) {
        StackV4Request request = new StackV4Request();
        ClusterV4Request cluster = new ClusterV4Request();
        ClouderaManagerV4Request cm = new ClouderaManagerV4Request();
        if (baseUrl != null) {
            ClouderaManagerRepositoryV4Request repo = new ClouderaManagerRepositoryV4Request();
            repo.setBaseUrl(baseUrl);
            cm.setRepository(repo);
        }
        if (products != null) {
            cm.setProducts(products);
        }
        cluster.setCm(cm);
        request.setCluster(cluster);
        return request;
    }

    private ClouderaManagerProductV4Request product(String parcelUrl) {
        ClouderaManagerProductV4Request product = new ClouderaManagerProductV4Request();
        product.setName("CDH");
        product.setVersion("7.3.2-1.cdh7.3.2.p10000.77529402");
        product.setParcel(parcelUrl);
        return product;
    }

    private Image image(String os) {
        return new Image(
                null, null, null, null,
                os, "test-image-uuid", null, null,
                null, null, os, null,
                null, null, null, true,
                null, null, null, null);
    }
}
