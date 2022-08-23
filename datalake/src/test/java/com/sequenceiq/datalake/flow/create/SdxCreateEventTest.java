package com.sequenceiq.datalake.flow.create;

import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.ENV_WAIT_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.RDS_WAIT_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_CREATE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_CREATE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_CREATE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_STACK_CREATION_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_STACK_CREATION_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_VALIDATION_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_VALIDATION_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.STORAGE_CONSUMPTION_COLLECTION_SCHEDULING_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.STORAGE_VALIDATION_SUCCESS_EVENT;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SdxCreateEventTest {

    private static final Map<SdxCreateEvent, String> ENUM_TO_EVENT_MAP = new EnumMap<>(Map.ofEntries(
            entry(SDX_VALIDATION_EVENT, "SDX_VALIDATION_EVENT"), entry(SDX_VALIDATION_SUCCESS_EVENT, "SdxValidationSuccessEvent"),
            entry(STORAGE_VALIDATION_SUCCESS_EVENT, "StorageValidationSuccessEvent"), entry(ENV_WAIT_SUCCESS_EVENT, "EnvWaitSuccessEvent"),
            entry(RDS_WAIT_SUCCESS_EVENT, "RdsWaitSuccessEvent"),
            entry(STORAGE_CONSUMPTION_COLLECTION_SCHEDULING_SUCCESS_EVENT, "STORAGECONSUMPTIONCOLLECTIONSCHEDULINGSUCCESSEVENT"),
            entry(SDX_STACK_CREATION_IN_PROGRESS_EVENT, "SDX_STACK_CREATION_IN_PROGRESS_EVENT"),
            entry(SDX_STACK_CREATION_SUCCESS_EVENT, "StackCreationSuccessEvent"), entry(SDX_CREATE_FAILED_EVENT, "SdxCreateFailedEvent"),
            entry(SDX_CREATE_FAILED_HANDLED_EVENT, "SDX_CREATE_FAILED_HANDLED_EVENT"), entry(SDX_CREATE_FINALIZED_EVENT, "SDX_CREATE_FINALIZED_EVENT")
    ));

    @ParameterizedTest(name = "underTest={0}")
    @EnumSource(SdxCreateEvent.class)
    void eventTest(SdxCreateEvent underTest) {
        assertThat(underTest.event()).isEqualTo(ENUM_TO_EVENT_MAP.get(underTest));
    }

}