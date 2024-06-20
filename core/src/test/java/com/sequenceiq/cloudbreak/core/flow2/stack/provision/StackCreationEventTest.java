package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.ATTACHED_VOLUME_CONSUMPTION_COLLECTION_SCHEDULING_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.ATTACHED_VOLUME_CONSUMPTION_COLLECTION_SCHEDULING_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.COLLECT_LOADBALANCER_METADATA_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.COLLECT_LOADBALANCER_METADATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.COLLECT_METADATA_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.COLLECT_METADATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.CREATE_CREDENTIAL_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.CREATE_CREDENTIAL_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.CREATE_USER_DATA_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.CREATE_USER_DATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.GENERATE_ENCRYPTION_KEYS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.GENERATE_ENCRYPTION_KEYS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.GET_TLS_INFO_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.GET_TLS_INFO_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_COPY_CHECK_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_COPY_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_COPY_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_FALLBACK_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_FALLBACK_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_FALLBACK_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_PREPARATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_PREPARATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.LAUNCH_LOAD_BALANCER_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.LAUNCH_LOAD_BALANCER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.LAUNCH_STACK_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.LAUNCH_STACK_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.SETUP_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.SETUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.SSHFINGERPRINTS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.SSHFINGERPRINTS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.STACKCREATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.STACK_CREATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.STACK_CREATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.START_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.TLS_SETUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.UPDATE_USERDATA_SECRETS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.UPDATE_USERDATA_SECRETS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.VALIDATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.VALIDATION_FINISHED_EVENT;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class StackCreationEventTest {

    private static final Map<StackCreationEvent, String> ENUM_TO_EVENT_MAP = new EnumMap<>(Map.ofEntries(
            entry(START_CREATION_EVENT, "STACK_PROVISION_TRIGGER_EVENT"), entry(VALIDATION_FINISHED_EVENT, "VALIDATIONRESULT"),
            entry(VALIDATION_FAILED_EVENT, "VALIDATIONRESULT_ERROR"), entry(CREATE_USER_DATA_FINISHED_EVENT, "CREATEUSERDATASUCCESS"),
            entry(GENERATE_ENCRYPTION_KEYS_FINISHED_EVENT, "GENERATEENCRYPTIONKEYSSUCCESS"),
            entry(GENERATE_ENCRYPTION_KEYS_FAILED_EVENT, "GENERATEENCRYPTIONKEYSFAILED"),
            entry(CREATE_USER_DATA_FAILED_EVENT, "CREATEUSERDATAFAILED"), entry(SETUP_FINISHED_EVENT, "SETUPRESULT"),
            entry(SETUP_FAILED_EVENT, "SETUPRESULT_ERROR"), entry(IMAGE_PREPARATION_FINISHED_EVENT, "PREPAREIMAGERESULT"),
            entry(IMAGE_PREPARATION_FAILED_EVENT, "PREPAREIMAGERESULT_ERROR"), entry(IMAGE_COPY_CHECK_EVENT, "IMAGECOPYCHECK"),
            entry(IMAGE_COPY_FINISHED_EVENT, "IMAGECOPYFINISHED"), entry(IMAGE_COPY_FAILED_EVENT, "IMAGECOPYFAILED"),
            entry(CREATE_CREDENTIAL_FINISHED_EVENT, "CREATECREDENTIALRESULT"), entry(CREATE_CREDENTIAL_FAILED_EVENT, "CREATECREDENTIALRESULT_ERROR"),
            entry(LAUNCH_STACK_FINISHED_EVENT, "LAUNCHSTACKRESULT"), entry(LAUNCH_STACK_FAILED_EVENT, "LAUNCHSTACKRESULT_ERROR"),
            entry(LAUNCH_LOAD_BALANCER_FINISHED_EVENT, "LAUNCHLOADBALANCERRESULT"),
            entry(LAUNCH_LOAD_BALANCER_FAILED_EVENT, "LAUNCHLOADBALANCERRESULT_ERROR"), entry(COLLECT_METADATA_FINISHED_EVENT, "COLLECTMETADATARESULT"),
            entry(COLLECT_METADATA_FAILED_EVENT, "COLLECTMETADATARESULT_ERROR"),
            entry(COLLECT_LOADBALANCER_METADATA_FINISHED_EVENT, "COLLECTLOADBALANCERMETADATARESULT"),
            entry(COLLECT_LOADBALANCER_METADATA_FAILED_EVENT, "COLLECTLOADBALANCERMETADATARESULT_ERROR"),
            entry(UPDATE_USERDATA_SECRETS_FINISHED_EVENT, "UPDATEUSERDATASECRETSSUCCESS"),
            entry(UPDATE_USERDATA_SECRETS_FAILED_EVENT, "UPDATEUSERDATASECRETSFAILED"),
            entry(SSHFINGERPRINTS_EVENT, "GETSSHFINGERPRINTSRESULT"), entry(SSHFINGERPRINTS_FAILED_EVENT, "GETSSHFINGERPRINTSRESULT_ERROR"),
            entry(GET_TLS_INFO_FINISHED_EVENT, "GETTLSINFORESULT"),
            entry(GET_TLS_INFO_FAILED_EVENT, "GETTLSINFORESULT_ERROR"), entry(TLS_SETUP_FINISHED_EVENT, "TLS_SETUP_FINISHED_EVENT"),
            entry(ATTACHED_VOLUME_CONSUMPTION_COLLECTION_SCHEDULING_FINISHED_EVENT, "ATTACHEDVOLUMECONSUMPTIONCOLLECTIONSCHEDULINGSUCCESS"),
            entry(ATTACHED_VOLUME_CONSUMPTION_COLLECTION_SCHEDULING_FAILED_EVENT, "ATTACHEDVOLUMECONSUMPTIONCOLLECTIONSCHEDULINGFAILED"),
            entry(STACK_CREATION_FAILED_EVENT, "STACK_CREATION_FAILED"), entry(STACK_CREATION_FINISHED_EVENT, "STACK_CREATION_FINISHED"),
            entry(STACKCREATION_FAILURE_HANDLED_EVENT, "STACK_CREATION_FAILHANDLED"),
            entry(IMAGE_FALLBACK_EVENT, "IMAGEFALLBACK"),
            entry(IMAGE_FALLBACK_FINISHED_EVENT, "IMAGEFALLBACKSUCCESS"),
            entry(IMAGE_FALLBACK_FAILED_EVENT, "IMAGEFALLBACKFAILED")
    ));

    @ParameterizedTest(name = "underTest={0}")
    @EnumSource(StackCreationEvent.class)
    void eventTest(StackCreationEvent underTest) {
        assertThat(underTest.event()).isEqualTo(ENUM_TO_EVENT_MAP.get(underTest));
    }

}