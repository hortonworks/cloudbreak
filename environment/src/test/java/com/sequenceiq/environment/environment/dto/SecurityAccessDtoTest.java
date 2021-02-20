package com.sequenceiq.environment.environment.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    void testSecurityAccessCidrWide() {
        SecurityAccessDto result = SecurityAccessDto.builder()
                .withCidr("0.0.0.0/0")
                .build();

        assertEquals("CIDR_WIDE_OPEN", result.getSecurityAccessType());
    }

    @Test
    void testSecurityAccessCidrPrivate() {
        SecurityAccessDto result = SecurityAccessDto.builder()
                .withCidr("10.0.0.0/16")
                .build();

        assertEquals("CIDR_PRIVATE", result.getSecurityAccessType());

        result = SecurityAccessDto.builder()
                .withCidr("192.168.0.0/16")
                .build();

        assertEquals("CIDR_PRIVATE", result.getSecurityAccessType());

        result = SecurityAccessDto.builder()
                .withCidr("172.17.15.0/24")
                .build();

        assertEquals("CIDR_PRIVATE", result.getSecurityAccessType());
    }

    @Test
    void testSecurityAccessCidrPublic() {
        SecurityAccessDto result = SecurityAccessDto.builder()
                .withCidr("155.14.15.16/32")
                .build();

        assertEquals("CIDR_PUBLIC", result.getSecurityAccessType());
    }

    @Test
    void testSecurityAccessSecGroup() {
        SecurityAccessDto result = SecurityAccessDto.builder()
                .withSecurityGroupIdForKnox(SECURITY_GROUP_KNOX)
                .withDefaultSecurityGroupId(SECURITY_GROUP_OTHER)
                .build();

        assertEquals("EXISTING_SECURITY_GROUP", result.getSecurityAccessType());
    }

    @Test
    void testSecurityAccessEmpty() {
        SecurityAccessDto result = SecurityAccessDto.builder()
                .build();

        assertNull(result.getSecurityAccessType());
    }
}
