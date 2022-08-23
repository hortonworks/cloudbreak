package com.sequenceiq.datalake.flow.delete.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StorageConsumptionCollectionUnschedulingSuccessEventTest {

    private static final Long SDX_ID = 123L;

    private static final String USER_ID = "userId";

    private static final boolean FORCED = true;

    @Test
    void constructorTest() {
        StorageConsumptionCollectionUnschedulingSuccessEvent result = new StorageConsumptionCollectionUnschedulingSuccessEvent(SDX_ID, USER_ID, FORCED);

        verifyProperties(result);
    }

    private void verifyProperties(StorageConsumptionCollectionUnschedulingSuccessEvent result) {
        assertThat(result.getResourceId()).isEqualTo(SDX_ID);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getSdxName()).isNull();
        assertThat(result.isForced()).isEqualTo(FORCED);
    }

    @Test
    void selectorTest() {
        StorageConsumptionCollectionUnschedulingSuccessEvent result = new StorageConsumptionCollectionUnschedulingSuccessEvent(SDX_ID, USER_ID, FORCED);

        assertThat(result.getSelector()).isEqualTo("STORAGECONSUMPTIONCOLLECTIONUNSCHEDULINGSUCCESSEVENT");
    }

}