package com.sequenceiq.datalake.flow.delete.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.datalake.flow.SdxContext;

class StorageConsumptionCollectionUnschedulingRequestTest {

    private static final Long SDX_ID = 123L;

    private static final String USER_ID = "userId";

    private static final boolean FORCED = true;

    @Test
    void constructorTest() {
        StorageConsumptionCollectionUnschedulingRequest result = new StorageConsumptionCollectionUnschedulingRequest(SDX_ID, USER_ID, FORCED);

        verifyProperties(result);
    }

    private void verifyProperties(StorageConsumptionCollectionUnschedulingRequest result) {
        assertThat(result.getResourceId()).isEqualTo(SDX_ID);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getSdxName()).isNull();
        assertThat(result.isForced()).isEqualTo(FORCED);
    }

    @Test
    void selectorTest() {
        StorageConsumptionCollectionUnschedulingRequest result = new StorageConsumptionCollectionUnschedulingRequest(SDX_ID, USER_ID, FORCED);

        assertThat(result.getSelector()).isEqualTo("STORAGECONSUMPTIONCOLLECTIONUNSCHEDULINGREQUEST");
    }

    @Test
    void fromTest() {
        SdxContext context = new SdxContext(null, SDX_ID, USER_ID);
        StackDeletionSuccessEvent payload = new StackDeletionSuccessEvent(SDX_ID, USER_ID, FORCED);

        StorageConsumptionCollectionUnschedulingRequest result = StorageConsumptionCollectionUnschedulingRequest.from(context, payload);

        assertThat(result).isNotNull();
        verifyProperties(result);
    }

}