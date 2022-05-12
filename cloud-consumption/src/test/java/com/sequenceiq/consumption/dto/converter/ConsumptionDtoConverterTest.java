package com.sequenceiq.consumption.dto.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.consumption.api.v1.consumption.model.common.ConsumptionType;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ResourceType;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.ConsumptionCreationDto;

public class ConsumptionDtoConverterTest {

    public static final String NAME = "consumption-name";

    public static final String DESCRIPTION = "description";

    public static final String ENVIRONMENT_CRN = "env-crn";

    public static final String ACCOUNT_ID = "account-id";

    public static final String CONSUMPTION_CRN = "consumption-crn";

    public static final ResourceType MONITORED_TYPE = ResourceType.ENVIRONMENT;

    public static final String MONITORED_CRN = "monitored-crn";

    public static final ConsumptionType TYPE = ConsumptionType.STORAGE;

    public static final String STORAGE = "storage-location";

    public static final long ID = 1234L;

    private ConsumptionDtoConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new ConsumptionDtoConverter();
    }

    @Test
    void testConsumptionCreationDtoToConsumption() {
        ConsumptionCreationDto creationDto = ConsumptionCreationDto.builder()
                .withName(NAME)
                .withDescription(DESCRIPTION)
                .withEnvironmentCrn(ENVIRONMENT_CRN)
                .withAccountId(ACCOUNT_ID)
                .withResourceCrn(CONSUMPTION_CRN)
                .withMonitoredResourceType(MONITORED_TYPE)
                .withMonitoredResourceCrn(MONITORED_CRN)
                .withConsumptionType(TYPE)
                .withStorageLocation(STORAGE)
                .build();

        Consumption result = underTest.creationDtoToConsumption(creationDto);

        assertEquals(NAME, result.getName());
        assertEquals(DESCRIPTION, result.getDescription());
        assertEquals(ENVIRONMENT_CRN, result.getEnvironmentCrn());
        assertEquals(ACCOUNT_ID, result.getAccountId());
        assertEquals(CONSUMPTION_CRN, result.getResourceCrn());
        assertEquals(MONITORED_TYPE, result.getMonitoredResourceType());
        assertEquals(MONITORED_CRN, result.getMonitoredResourceCrn());
        assertEquals(TYPE, result.getConsumptionType());
        assertEquals(STORAGE, result.getStorageLocation());
    }
}
