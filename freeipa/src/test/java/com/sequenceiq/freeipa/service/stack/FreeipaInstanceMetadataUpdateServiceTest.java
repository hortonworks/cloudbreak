package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_OPTIONAL;
import static com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateEvent.STACK_IMDUPDATE_EVENT;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateProperties;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateTypeMetadata;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateTypeProperty;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;

@ExtendWith(MockitoExtension.class)
public class FreeipaInstanceMetadataUpdateServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private InstanceMetadataUpdateProperties instanceMetadataUpdateProperties;

    @InjectMocks
    private FreeipaInstanceMetadataUpdateService underTest;

    @BeforeEach
    void setup() {
        InstanceMetadataUpdateTypeMetadata metadataV2 = new InstanceMetadataUpdateTypeMetadata("v2");
        InstanceMetadataUpdateTypeProperty propertyV2 = new InstanceMetadataUpdateTypeProperty("AWS", Map.of(AWS, metadataV2));
        InstanceMetadataUpdateTypeMetadata metadataV1 = new InstanceMetadataUpdateTypeMetadata("v1");
        InstanceMetadataUpdateTypeProperty propertyV1 = new InstanceMetadataUpdateTypeProperty("AWS", Map.of(AWS, metadataV1));
        when(instanceMetadataUpdateProperties.getTypes()).thenReturn(Map.of(IMDS_HTTP_TOKEN_REQUIRED, propertyV2, IMDS_HTTP_TOKEN_OPTIONAL, propertyV1));
    }

    @Test
    void testTrigger() {
        Stack stack = mock(Stack.class);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stack.getSupportedImdsVersion()).thenReturn("v1");
        when(stackService.getByEnvironmentCrnAndAccountId(any(), any())).thenReturn(stack);
        when(flowManager.notify(anyString(), any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "1"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateInstanceMetadata("env", IMDS_HTTP_TOKEN_REQUIRED));

        verify(flowManager).notify(eq(STACK_IMDUPDATE_EVENT.event()), any());
    }

    @Test
    void testTriggerWhenNotNeeded() {
        Stack stack = mock(Stack.class);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stack.getSupportedImdsVersion()).thenReturn("v2");
        when(stackService.getByEnvironmentCrnAndAccountId(any(), any())).thenReturn(stack);

        assertThrows(BadRequestException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateInstanceMetadata("env", IMDS_HTTP_TOKEN_REQUIRED)));

        verify(flowManager, times(0)).notify(eq(STACK_IMDUPDATE_EVENT.event()), any());
    }
}
