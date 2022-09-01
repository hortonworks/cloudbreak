package com.sequenceiq.consumption.endpoint.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ConsumptionType;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ResourceType;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionRequest;
import com.sequenceiq.consumption.dto.ConsumptionCreationDto;

@ExtendWith(MockitoExtension.class)
public class ConsumptionApiConverterTest {

    private static final String USER_CRN = "crn:altus:iam:us-west-1:test-aws:user:cloudbreak@hortonworks.com";

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @InjectMocks
    private ConsumptionApiConverter underTest;

    @BeforeEach
    void init() {
        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
    }

    @Test
    void testInitCreationDtoForStorage() {
        StorageConsumptionRequest request = new StorageConsumptionRequest();
        request.setEnvironmentCrn("env-crn");
        request.setMonitoredResourceType(ResourceType.DATALAKE);
        request.setMonitoredResourceName("name");
        request.setMonitoredResourceCrn("dl-crn");
        request.setStorageLocation("location");

        ConsumptionCreationDto result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.initCreationDtoForStorage(request, ConsumptionType.STORAGE));

        Assertions.assertEquals("name_STORAGE", result.getName());
        Assertions.assertNull(result.getDescription());
        Assertions.assertEquals("test-aws", result.getAccountId());
        Assertions.assertTrue(result.getResourceCrn().startsWith("crn:cdp:consumption:us-west-1:test-aws:consumption:"));
        Assertions.assertEquals("env-crn", result.getEnvironmentCrn());
        Assertions.assertEquals(ResourceType.DATALAKE, result.getMonitoredResourceType());
        Assertions.assertEquals("dl-crn", result.getMonitoredResourceCrn());
        Assertions.assertEquals(ConsumptionType.STORAGE, result.getConsumptionType());
        Assertions.assertEquals("location", result.getStorageLocation());
    }
}
