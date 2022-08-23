package com.sequenceiq.datalake.flow.create.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

class StorageConsumptionCollectionSchedulingRequestTest {

    private static final Long SDX_ID = 123L;

    private static final String USER_ID = "userId";

    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @BeforeEach
    void setUp() {
        detailedEnvironmentResponse = DetailedEnvironmentResponse.builder().build();
    }

    @Test
    void constructorTest() {
        StorageConsumptionCollectionSchedulingRequest result = new StorageConsumptionCollectionSchedulingRequest(SDX_ID, USER_ID, detailedEnvironmentResponse);

        verifyProperties(result);
    }

    private void verifyProperties(StorageConsumptionCollectionSchedulingRequest result) {
        assertThat(result.getResourceId()).isEqualTo(SDX_ID);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getSdxName()).isNull();
        assertThat(result.getDetailedEnvironmentResponse()).isSameAs(detailedEnvironmentResponse);
    }

    @Test
    void selectorTest() {
        StorageConsumptionCollectionSchedulingRequest result = new StorageConsumptionCollectionSchedulingRequest(SDX_ID, USER_ID, detailedEnvironmentResponse);

        assertThat(result.getSelector()).isEqualTo("STORAGECONSUMPTIONCOLLECTIONSCHEDULINGREQUEST");
    }

    @Test
    void fromTest() {
        SdxContext context = new SdxContext(null, SDX_ID, USER_ID);

        StorageConsumptionCollectionSchedulingRequest result = StorageConsumptionCollectionSchedulingRequest.from(context, detailedEnvironmentResponse);

        assertThat(result).isNotNull();
        verifyProperties(result);
    }

}