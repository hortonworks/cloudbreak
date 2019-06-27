package com.sequenceiq.environment.environment.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.environment.dto.SecurityAccessDto.Builder;
import com.sequenceiq.environment.testing.BuilderFieldValidator;

class SecurityAccessDtoTest {

    public static final String CIDR = "cidr";

    public static final String SECURITY_GROUP_KNOX = "sg-knox";

    public static final String SECURITY_GROUP_OTHER = "sg-other";

    private final BuilderFieldValidator builderFieldValidator = new BuilderFieldValidator();

    @Test
    void builderCreatesFilledPojo() {
        builderFieldValidator.assertBuilderFields(SecurityAccessDto.class, Builder.class);

        SecurityAccessDto result = SecurityAccessDto.builder()
                .withCidr(CIDR)
                .withSecurityGroupIdForKnox(SECURITY_GROUP_KNOX)
                .withDefaultSecurityGroupId(SECURITY_GROUP_OTHER)
                .build();

        assertThat(result)
                .matches(m -> Objects.equals(m.getCidr(), CIDR))
                .matches(m -> Objects.equals(m.getSecurityGroupIdForKnox(), SECURITY_GROUP_KNOX))
                .matches(m -> Objects.equals(m.getDefaultSecurityGroupId(), SECURITY_GROUP_OTHER));
    }
}
