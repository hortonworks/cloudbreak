package com.sequenceiq.cloudbreak.service.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

class UpgradeComponentsTest {

    @ParameterizedTest
    @MethodSource("invalidComponents")
    void rejectsIncompleteComponentsWithClearMessage(UpgradeComponents components, String message) {
        assertThatThrownBy(components::validate).isInstanceOf(BadRequestException.class).hasMessageContaining(message);
    }

    static Stream<Arguments> invalidComponents() {
        return Stream.of(
                Arguments.of(new UpgradeComponents(null, List.of(cdh())), "repository.version"),
                Arguments.of(new UpgradeComponents(repository().withVersion(null), List.of(cdh())), "repository.version"),
                Arguments.of(new UpgradeComponents(repository().withBaseUrl("bad url"), List.of(cdh())), "repository.baseUrl"),
                Arguments.of(new UpgradeComponents(repository().withGpgKeyUrl("file:///key"), List.of(cdh())), "repository.gpgKeyUrl"),
                Arguments.of(new UpgradeComponents(repository(), null), "products"),
                Arguments.of(new UpgradeComponents(repository(), List.of()), "products"),
                Arguments.of(new UpgradeComponents(repository(), Collections.singletonList(null)), "non-empty product names"),
                Arguments.of(new UpgradeComponents(repository(), List.of(cdh().withName("CFM"))), "exactly one CDH"),
                Arguments.of(new UpgradeComponents(repository(), List.of(cdh(), cdh())), "exactly one CDH"),
                Arguments.of(new UpgradeComponents(repository(), List.of(cdh().withVersion("invalid"))), "Invalid CDH parcel version"),
                Arguments.of(new UpgradeComponents(repository(), List.of(cdh().withParcel(null))), "products.parcel"),
                Arguments.of(new UpgradeComponents(repository(), List.of(cdh().withCsd(Collections.singletonList(null)))), "products.csd"));
    }

    @Test
    void acceptsExplicitComponentsWithoutLookingUpArtifacts() {
        UpgradeComponents components = new UpgradeComponents(repository(), List.of(cdh()));

        components.validate();

        assertThat(components.getRuntimeVersion()).isEqualTo("7.3.2");
    }

    @Test
    void rejectsMissingCdhWhenReadingRuntime() {
        assertThatThrownBy(() -> new UpgradeComponents(repository(), new ArrayList<>()).getRuntimeVersion())
                .isInstanceOf(BadRequestException.class).hasMessageContaining("CDH parcel");
    }

    private static ClouderaManagerRepo repository() {
        return new ClouderaManagerRepo().withVersion("7.13.2.20000-83309838").withBaseUrl("https://build/cm/");
    }

    private static ClouderaManagerProduct cdh() {
        return new ClouderaManagerProduct().withName("CDH").withVersion("7.3.2-1.cdh7.3.2.p20000.83281140").withParcel("https://build/cdh/");
    }
}
