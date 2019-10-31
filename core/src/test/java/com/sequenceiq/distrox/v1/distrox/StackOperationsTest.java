package com.sequenceiq.distrox.v1.distrox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.dto.StackAccessDto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.DefaultClouderaManagerRepoService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.stack.StackApiViewService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.distrox.v1.distrox.service.EnvironmentServiceDecorator;
import com.sequenceiq.distrox.v1.distrox.service.SdxServiceDecorator;

public class StackOperationsTest {

    private static final String INVALID_DTO_MESSAGE = "A stack name or crn must be provided. One and only one of them.";

    private static final String NULL_DTO_EXCEPTION_MESSAGE = "StackAccessDto should not be null";

    private static final StackType STACK_TYPE = StackType.WORKLOAD;

    private static final Set<String> STACK_ENTRIES = Collections.emptySet();

    @Rule
    public final ExpectedException exceptionRule = ExpectedException.none();

    @InjectMocks
    private StackOperations underTest;

    @Mock
    private StackCommonService stackCommonService;

    @Mock
    private UserService userService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterCommonService clusterCommonService;

    @Mock
    private ConverterUtil converterUtil;

    @Mock
    private StackApiViewService stackApiViewService;

    @Mock
    private DefaultClouderaManagerRepoService defaultClouderaManagerRepoService;

    @Mock
    private EnvironmentServiceDecorator environmentServiceDecorator;

    @Mock
    private CloudbreakUser cloudbreakUser;

    @Mock
    private SdxServiceDecorator sdxServiceDecorator;

    private Stack stack;

    private User user;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        user = TestUtil.user(1L, "someUserId");
        stack = TestUtil.stack();
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(userService.getOrCreate(cloudbreakUser)).thenReturn(user);
    }

    @Test
    public void testDeleteWhenDtoNameFilledAndForcedTrueThenDeleteCalled() {
        underTest.delete(StackAccessDto.builder().withName(stack.getName()).build(), stack.getWorkspace().getId(), true);

        verify(userService, times(1)).getOrCreate(any());
        verify(userService, times(1)).getOrCreate(cloudbreakUser);
        verify(stackCommonService, times(1)).deleteByNameInWorkspace(anyString(), anyLong(), anyBoolean(), any());
        verify(stackCommonService, times(1)).deleteByNameInWorkspace(stack.getName(), stack.getWorkspace().getId(), true, user);
        verify(stackCommonService, times(0)).deleteByCrnInWorkspace(anyString(), anyLong(), anyBoolean(), any());
    }

    @Test
    public void testDeleteWhenDtoNameFilledAndForcedFalseThenDeleteCalled() {
        underTest.delete(StackAccessDto.builder().withName(stack.getName()).build(), stack.getWorkspace().getId(), false);

        verify(userService, times(1)).getOrCreate(any());
        verify(userService, times(1)).getOrCreate(cloudbreakUser);
        verify(stackCommonService, times(1)).deleteByNameInWorkspace(anyString(), anyLong(), anyBoolean(), any());
        verify(stackCommonService, times(1)).deleteByNameInWorkspace(stack.getName(), stack.getWorkspace().getId(), false, user);
        verify(stackCommonService, times(0)).deleteByCrnInWorkspace(anyString(), anyLong(), anyBoolean(), any());
    }

    @Test
    public void testDeleteWhenDtoCrnFilledAndForcedTrueThenDeleteCalled() {
        underTest.delete(StackAccessDto.builder().withCrn(stack.getResourceCrn()).build(), stack.getWorkspace().getId(), true);

        verify(userService, times(1)).getOrCreate(any());
        verify(userService, times(1)).getOrCreate(cloudbreakUser);
        verify(stackCommonService, times(1)).deleteByCrnInWorkspace(anyString(), anyLong(), anyBoolean(), any());
        verify(stackCommonService, times(1)).deleteByCrnInWorkspace(stack.getResourceCrn(), stack.getWorkspace().getId(), true, user);
        verify(stackCommonService, times(0)).deleteByNameInWorkspace(anyString(), anyLong(), anyBoolean(), any());
    }

    @Test
    public void testDeleteWhenDtoCrnFilledAndForcedFalseThenDeleteCalled() {
        underTest.delete(StackAccessDto.builder().withCrn(stack.getResourceCrn()).build(), stack.getWorkspace().getId(), false);

        verify(userService, times(1)).getOrCreate(any());
        verify(userService, times(1)).getOrCreate(cloudbreakUser);
        verify(stackCommonService, times(1)).deleteByCrnInWorkspace(anyString(), anyLong(), anyBoolean(), any());
        verify(stackCommonService, times(1)).deleteByCrnInWorkspace(stack.getResourceCrn(), stack.getWorkspace().getId(), false, user);
        verify(stackCommonService, times(0)).deleteByNameInWorkspace(anyString(), anyLong(), anyBoolean(), any());
    }

    @Test
    public void testDeleteWhenNeitherCrnOrNameProvidedThenBadRequestExceptionComes() {
        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage(INVALID_DTO_MESSAGE);

        underTest.delete(StackAccessDto.builder().build(), stack.getWorkspace().getId(), true);

        verify(stackCommonService, times(0)).findStackByNameAndWorkspaceId(anyString(), anyLong(), anySet(), any());
        verify(stackCommonService, times(0)).findStackByCrnAndWorkspaceId(anyString(), anyLong(), anySet(), any());
    }

    @Test
    public void testDeleteIfDtoIsNullThenIllegalArgumentExceptionComes() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(NULL_DTO_EXCEPTION_MESSAGE);

        underTest.delete(null, stack.getWorkspace().getId(), true);

        verify(stackCommonService, times(0)).findStackByNameAndWorkspaceId(anyString(), anyLong(), anySet(), any());
        verify(stackCommonService, times(0)).findStackByCrnAndWorkspaceId(anyString(), anyLong(), anySet(), any());
    }

    @Test
    public void testGetWhenDtoNameFilledThenProperGetCalled() {
        StackV4Response expected = stackResponse();
        when(stackCommonService.findStackByNameAndWorkspaceId(stack.getName(), stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE))
                .thenReturn(expected);

        StackV4Response result = underTest.get(StackAccessDto.builder().withName(stack.getName()).build(), stack.getWorkspace().getId(),
                STACK_ENTRIES, STACK_TYPE);

        assertEquals(expected, result);
        verify(stackCommonService, times(1)).findStackByNameAndWorkspaceId(anyString(), anyLong(), anySet(), any());
        verify(stackCommonService, times(1)).findStackByNameAndWorkspaceId(stack.getName(), stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE);
        verify(stackCommonService, times(0)).findStackByCrnAndWorkspaceId(anyString(), anyLong(), anySet(), any());
    }

    @Test
    public void testGetWhenDtoCrnFilledThenProperGetCalled() {
        StackV4Response expected = stackResponse();
        when(stackCommonService.findStackByCrnAndWorkspaceId(stack.getResourceCrn(), stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE))
                .thenReturn(expected);

        StackV4Response result = underTest.get(StackAccessDto.builder().withCrn(stack.getResourceCrn()).build(), stack.getWorkspace().getId(),
                STACK_ENTRIES, STACK_TYPE);

        assertEquals(expected, result);
        verify(stackCommonService, times(1)).findStackByCrnAndWorkspaceId(anyString(), anyLong(), anySet(), any());
        verify(stackCommonService, times(1)).findStackByCrnAndWorkspaceId(stack.getResourceCrn(), stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE);
        verify(stackCommonService, times(0)).findStackByNameAndWorkspaceId(anyString(), anyLong(), anySet(), any());
    }

    @Test
    public void testGethenNeitherCrnOrNameProvidedThenBadRequestExceptionComes() {
        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage(INVALID_DTO_MESSAGE);

        underTest.get(StackAccessDto.builder().build(), stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE);

        verify(stackCommonService, times(0)).findStackByNameAndWorkspaceId(anyString(), anyLong(), anySet(), any());
        verify(stackCommonService, times(0)).findStackByCrnAndWorkspaceId(anyString(), anyLong(), anySet(), any());
    }

    @Test
    public void testGetByWorkspaceIfDtoIsNullThenIllegalArgumentExceptionComes() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(NULL_DTO_EXCEPTION_MESSAGE);

        underTest.get(null, stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE);

        verify(stackCommonService, times(0)).findStackByNameAndWorkspaceId(anyString(), anyLong(), anySet(), any());
        verify(stackCommonService, times(0)).findStackByCrnAndWorkspaceId(anyString(), anyLong(), anySet(), any());
    }

    @Test
    public void testGetForInternalCrn() {
        when(cloudbreakUser.getUserCrn()).thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(stackApiViewService.retrieveStackByCrnAndType(anyString(), any(StackType.class))).thenReturn(new StackApiView());
        when(converterUtil.convert(any(StackApiView.class), any())).thenReturn(new StackViewV4Response());
        doNothing().when(environmentServiceDecorator).prepareEnvironment(any(StackViewV4Response.class));

        StackViewV4Response response = underTest.getForInternalCrn(StackAccessDto.builder().withCrn("myCrn").build(), STACK_TYPE);

        assertNotNull(response);
        verify(stackApiViewService, times(1)).retrieveStackByCrnAndType(anyString(), any(StackType.class));
        verify(converterUtil, times(1)).convert(any(StackApiView.class), any());
        verify(environmentServiceDecorator, times(1)).prepareEnvironment(any(StackViewV4Response.class));
    }

    private StackV4Response stackResponse() {
        return new StackV4Response();
    }

}
