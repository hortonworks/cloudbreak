package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.Tunnel;

@ExtendWith(MockitoExtension.class)
class UserDataCcmV2SetupPatchServiceTest {

    @Mock
    private UserDataService userDataService;

    @InjectMocks
    private UserDataCcmV2SetupPatchService underTest;

    static Stream<Arguments> isAffectedParametersSource() {
        return Stream.of(
                Arguments.arguments(Tunnel.CCMV2, "", true),
                Arguments.arguments(Tunnel.CCMV2, "export CDP_API_ENDPOINT_URL=", false),
                Arguments.arguments(Tunnel.CCMV2_JUMPGATE, "", false),
                Arguments.arguments(Tunnel.CLUSTER_PROXY, "", false),
                Arguments.arguments(Tunnel.DIRECT, "", false),
                Arguments.arguments(Tunnel.CCM, "", false)
        );
    }

    @ParameterizedTest
    @MethodSource("isAffectedParametersSource")
    void testIsAffected(Tunnel tunnel, String gatewayUserData, boolean expectedResult) {
        Stack stack = TestUtil.stack();
        stack.setTunnel(tunnel);
        Map<InstanceGroupType, String> userDataMap = Map.of(InstanceGroupType.GATEWAY, gatewayUserData);
        lenient().when(userDataService.getUserData(stack.getId())).thenReturn(userDataMap);

        boolean actual = underTest.isAffected(stack);

        assertEquals(expectedResult, actual);
    }

    @Test
    void testDoApplyWhenGatewayUserDataIsEmpty() throws ExistingStackPatchApplyException {
        Stack stack = TestUtil.stack();
        String coreUserData = "untouched";
        Map<InstanceGroupType, String> userDataMap = new HashMap<>();
        userDataMap.put(InstanceGroupType.GATEWAY, "");
        userDataMap.put(InstanceGroupType.CORE, coreUserData);
        lenient().when(userDataService.getUserData(stack.getId())).thenReturn(userDataMap);

        boolean actual = underTest.doApply(stack);

        ArgumentCaptor<Map<InstanceGroupType, String>> userDataMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(userDataService, times(1)).createOrUpdateUserData(eq(stack.getId()), userDataMapCaptor.capture());
        Map<InstanceGroupType, String> capturedUserDataMap = userDataMapCaptor.getValue();
        assertEquals(coreUserData, capturedUserDataMap.get(InstanceGroupType.CORE));
        assertEquals("export CDP_API_ENDPOINT_URL=\"\"\n", capturedUserDataMap.get(InstanceGroupType.GATEWAY));
        assertTrue(actual);
    }

    @Test
    void testDoApplyShouldAddAdditionalExportToScriptWhenGatewayUserDataIsABashScript() throws ExistingStackPatchApplyException, IOException {
        Stack stack = TestUtil.stack();
        String coreUserData = "untouched";
        Map<InstanceGroupType, String> userDataMap = new HashMap<>();
        userDataMap.put(InstanceGroupType.GATEWAY, FileReaderUtils.readFileFromClasspath("/input/userdata/gateway-user-data.sh"));
        userDataMap.put(InstanceGroupType.CORE, coreUserData);
        lenient().when(userDataService.getUserData(stack.getId())).thenReturn(userDataMap);

        boolean actual = underTest.doApply(stack);

        ArgumentCaptor<Map<InstanceGroupType, String>> userDataMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(userDataService, times(1)).createOrUpdateUserData(eq(stack.getId()), userDataMapCaptor.capture());
        Map<InstanceGroupType, String> capturedUserDataMap = userDataMapCaptor.getValue();
        assertEquals(coreUserData, capturedUserDataMap.get(InstanceGroupType.CORE));
        String expectedUserDataScript = FileReaderUtils.readFileFromClasspath("/input/userdata/expected-gateway-user-data.sh");
        assertEquals(expectedUserDataScript, capturedUserDataMap.get(InstanceGroupType.GATEWAY));
        assertTrue(actual);
    }
}