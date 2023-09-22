package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.StackPatchRepository;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.core.FlowLogService;

@ExtendWith(MockitoExtension.class)
class UserDataMigrationPatchServiceTest {

    public static final String CORE_USER_DATA = "CORE-USER-DATA";

    public static final String CORRECT_GATEWAY_USER_DATA = "IS_CCM_V2_JUMPGATE_ENABLED=true";

    public static final String INCORRECT_GATEWAY_USER_DATA = "IS_CCM_V2_JUMPGATE_ENABLED=false";

    @Mock
    private StackPatchRepository stackPatchRepository;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private UserDataService userDataService;

    @InjectMocks
    private UserDataMigrationPatchService underTest;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(123L);
        stack.setTunnel(Tunnel.CCMV2_JUMPGATE);
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = {"DIRECT", "CCM", "CLUSTER_PROXY", "CCMV2"})
    void nonCcmv2jgTunnelsNotAffected(Tunnel tunnel) {
        setGwUserData(INCORRECT_GATEWAY_USER_DATA);
        stack.setTunnel(tunnel);

        boolean result = underTest.isAffected(stack);

        assertThat(result).isFalse();
    }

    @Test
    void notAffectedWithCorrectUserData() {
        setGwUserData(CORRECT_GATEWAY_USER_DATA);

        boolean result = underTest.isAffected(stack);

        assertThat(result).isFalse();
    }

    @Test
    void affectedWithIncorrectUserData() {
        setGwUserData(INCORRECT_GATEWAY_USER_DATA);

        boolean result = underTest.isAffected(stack);

        assertThat(result).isTrue();
    }

    @Test
    void doApply() throws ExistingStackPatchApplyException {
        boolean result = underTest.doApply(stack);

        verify(userDataService).updateJumpgateFlagOnly(stack.getId());
        assertThat(result).isTrue();
    }

    private void setGwUserData(String gwUserData) {
        lenient().when(userDataService.getUserData(stack.getId())).thenReturn(Map.of(
                InstanceGroupType.GATEWAY, gwUserData,
                InstanceGroupType.CORE, CORE_USER_DATA
        ));
    }

}
