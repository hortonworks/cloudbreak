package com.sequenceiq.datalake.flow.delete;

import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.RDS_WAIT_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_STACK_DELETION_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_STACK_DELETION_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.STORAGE_CONSUMPTION_COLLECTION_UNSCHEDULING_SUCCESS_EVENT;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SdxDeleteEventTest {

    private static final Map<SdxDeleteEvent, String> ENUM_TO_EVENT_MAP = new EnumMap<>(Map.ofEntries(
            entry(SDX_DELETE_EVENT, "SDX_DELETE_EVENT"), entry(SDX_STACK_DELETION_IN_PROGRESS_EVENT, "SDX_STACK_DELETION_IN_PROGRESS_EVENT"),
            entry(SDX_STACK_DELETION_SUCCESS_EVENT, "StackDeletionSuccessEvent"),
            entry(STORAGE_CONSUMPTION_COLLECTION_UNSCHEDULING_SUCCESS_EVENT, "STORAGECONSUMPTIONCOLLECTIONUNSCHEDULINGSUCCESSEVENT"),
            entry(RDS_WAIT_SUCCESS_EVENT, "RdsDeletionSuccessEvent"), entry(SDX_DELETE_FAILED_EVENT, "SdxDeletionFailedEvent"),
            entry(SDX_DELETE_FAILED_HANDLED_EVENT, "SDX_DELETE_FAILED_HANDLED_EVENT"), entry(SDX_DELETE_FINALIZED_EVENT, "SDX_DELETE_FINALIZED_EVENT")
    ));

    @ParameterizedTest(name = "underTest={0}")
    @EnumSource(SdxDeleteEvent.class)
    void eventTest(SdxDeleteEvent underTest) {
        assertThat(underTest.event()).isEqualTo(ENUM_TO_EVENT_MAP.get(underTest));
    }

}