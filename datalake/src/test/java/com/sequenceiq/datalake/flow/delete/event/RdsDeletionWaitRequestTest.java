package com.sequenceiq.datalake.flow.delete.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.datalake.flow.SdxContext;

class RdsDeletionWaitRequestTest {

    private static final Long SDX_ID = 123L;

    private static final String USER_ID = "userId";

    private static final boolean FORCED = true;

    @Test
    void constructorTest() {
        RdsDeletionWaitRequest result = new RdsDeletionWaitRequest(SDX_ID, USER_ID, FORCED);

        verifyProperties(result);
    }

    private void verifyProperties(RdsDeletionWaitRequest result) {
        assertThat(result.getResourceId()).isEqualTo(SDX_ID);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getSdxName()).isNull();
        assertThat(result.isForced()).isEqualTo(FORCED);
    }

    @Test
    void selectorTest() {
        RdsDeletionWaitRequest result = new RdsDeletionWaitRequest(SDX_ID, USER_ID, FORCED);

        assertThat(result.getSelector()).isEqualTo("RdsDeletionWaitRequest");
    }

    @Test
    void fromTest() {
        SdxContext context = new SdxContext(null, SDX_ID, USER_ID);
        StorageConsumptionCollectionUnschedulingSuccessEvent payload = new StorageConsumptionCollectionUnschedulingSuccessEvent(SDX_ID, USER_ID, FORCED);

        RdsDeletionWaitRequest result = RdsDeletionWaitRequest.from(context, payload);

        assertThat(result).isNotNull();
        verifyProperties(result);
    }

}