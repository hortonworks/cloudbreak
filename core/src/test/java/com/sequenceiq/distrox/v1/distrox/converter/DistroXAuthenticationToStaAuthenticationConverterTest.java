package com.sequenceiq.distrox.v1.distrox.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;

class DistroXAuthenticationToStaAuthenticationConverterTest {

    private DistroXAuthenticationToStaAuthenticationConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new DistroXAuthenticationToStaAuthenticationConverter();
    }

    @Test
    void testConvertEnvironmentAuthenticationResponseToStackAuthenticationV4Request() {
        EnvironmentAuthenticationResponse input = new EnvironmentAuthenticationResponse();
        input.setPublicKey("somePublicKey");
        input.setPublicKeyId("somePublicKeyId");
        input.setLoginUserName("someLoginUserName");

        StackAuthenticationV4Request result = underTest.convert(input);

        assertNotNull(result);
        assertNull(result.getLoginUserName());
        assertEquals(input.getPublicKey(), result.getPublicKey());
        assertEquals(input.getPublicKeyId(), result.getPublicKeyId());
    }

}