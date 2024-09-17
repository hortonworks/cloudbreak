package com.sequenceiq.cloudbreak.controller.v4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.ServiceTypeV4Response;
import com.sequenceiq.cloudbreak.validation.AllServiceTypes;

@ExtendWith(MockitoExtension.class)
class CustomConfigurationsV4ControllerTest {

    private CustomConfigurationsV4Controller underTest;

    @BeforeEach
    void setUp() {
        underTest = new CustomConfigurationsV4Controller();
    }

    @Test
    void getServiceTypes() {
        ServiceTypeV4Response serviceTypeV4Response = underTest.getServiceTypes();
        List<String> orderedServiceTypes = Arrays.stream(AllServiceTypes.values())
                .map(Enum::toString)
                .sorted()
                .collect(Collectors.toList());
        assertEquals(serviceTypeV4Response.getServiceTypes(), orderedServiceTypes);
    }
}