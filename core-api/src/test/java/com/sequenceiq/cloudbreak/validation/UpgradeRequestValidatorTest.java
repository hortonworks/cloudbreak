package com.sequenceiq.cloudbreak.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.ClouderaManagerV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.repository.ClouderaManagerRepositoryV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeRequestValidator;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;

class UpgradeRequestValidatorTest {

    @ParameterizedTest
    @MethodSource("invalidComponents")
    void rejectsIncompleteComponents(String version, String baseUrl, List<String> products) {
        assertTrue(UpgradeRequestValidator.getComponentsContentError(version, baseUrl, products).isPresent());
    }

    static Stream<Arguments> invalidComponents() {
        return Stream.of(
                Arguments.of(null, "https://repo/", List.of("CDH")),
                Arguments.of("7.13.2", null, List.of("CDH")),
                Arguments.of("7.13.2", "https://repo/", null),
                Arguments.of("7.13.2", "https://repo/", List.of()),
                Arguments.of("7.13.2", "https://repo/", Collections.singletonList(null)),
                Arguments.of("7.13.2", "https://repo/", List.of("CFM")),
                Arguments.of("7.13.2", "https://repo/", List.of("CDH", "CDH")));
    }

    @Test
    void acceptsCdhAndAdditionalParcelNames() {
        assertTrue(UpgradeRequestValidator.getComponentsContentError("7.13.2", "https://repo/", List.of("CDH", "CFM")).isEmpty());
    }

    @Test
    void rejectsNullProductBeforeTheV1ToV4Converter() {
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);
        UpgradeV4Request v4 = new UpgradeV4Request();
        v4.setComponents(new ClouderaManagerV4Request()
                .withRepository(new ClouderaManagerRepositoryV4Request().withVersion("7.13.2").withBaseUrl("https://repo/"))
                .withProducts(Collections.singletonList(null)));
        DistroXUpgradeV1Request v1 = new DistroXUpgradeV1Request();
        v1.setComponents(new ClouderaManagerV1Request()
                .withRepository(new ClouderaManagerRepositoryV1Request().withVersion("7.13.2").withBaseUrl("https://repo/"))
                .withProducts(Collections.singletonList(null)));

        assertFalse(new UpgradeRequestValidator().isValid(v4, context));
        assertFalse(new DistroXUpgradeRequestValidator().isValid(v1, context));
    }

    @Test
    void reportsMissingComponentsForBothRequestVersions() {
        assertThat(UpgradeRequestValidator.getComponentsContentError((ClouderaManagerV4Request) null))
                .contains(UpgradeRequestValidator.MISSING_REPOSITORY_ERROR);
        assertThat(UpgradeRequestValidator.getComponentsContentError((ClouderaManagerV1Request) null))
                .contains(UpgradeRequestValidator.MISSING_REPOSITORY_ERROR);
    }

    @Test
    void reportsMissingRepositoryForBothRequestVersions() {
        assertThat(UpgradeRequestValidator.getComponentsContentError(new ClouderaManagerV4Request()))
                .contains(UpgradeRequestValidator.MISSING_REPOSITORY_ERROR);
        assertThat(UpgradeRequestValidator.getComponentsContentError(new ClouderaManagerV1Request()))
                .contains(UpgradeRequestValidator.MISSING_REPOSITORY_ERROR);
    }

}
