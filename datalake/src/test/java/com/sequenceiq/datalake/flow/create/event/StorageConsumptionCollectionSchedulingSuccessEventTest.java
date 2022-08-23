package com.sequenceiq.datalake.flow.create.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

class StorageConsumptionCollectionSchedulingSuccessEventTest {

    private static final Long SDX_ID = 123L;

    private static final String USER_ID = "userId";

    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @BeforeEach
    void setUp() {
        detailedEnvironmentResponse = DetailedEnvironmentResponse.builder().build();
    }

    @Test
    void constructorTest() {
        StorageConsumptionCollectionSchedulingSuccessEvent result =
                new StorageConsumptionCollectionSchedulingSuccessEvent(SDX_ID, USER_ID, detailedEnvironmentResponse);

        verifyProperties(result);
    }

    private void verifyProperties(StorageConsumptionCollectionSchedulingSuccessEvent result) {
        assertThat(result.getResourceId()).isEqualTo(SDX_ID);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getSdxName()).isNull();
        assertThat(result.getDetailedEnvironmentResponse()).isSameAs(detailedEnvironmentResponse);
    }

    @Test
    void selectorTest() {
        StorageConsumptionCollectionSchedulingSuccessEvent result =
                new StorageConsumptionCollectionSchedulingSuccessEvent(SDX_ID, USER_ID, detailedEnvironmentResponse);

        assertThat(result.getSelector()).isEqualTo("STORAGECONSUMPTIONCOLLECTIONSCHEDULINGSUCCESSEVENT");
    }

}