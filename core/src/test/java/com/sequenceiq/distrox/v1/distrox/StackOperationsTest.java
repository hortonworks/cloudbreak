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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.ResourceAccessDto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.ResourceAccessDto.ResourceAccessDtoBuilder;
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
        underTest.delete(ResourceAccessDtoBuilder.aResourceAccessDtoBuilder().withName(stack.getName()).build(), stack.getWorkspace().getId(), true);

        verify(stackCommonService, times(1)).deleteWithKerberosByNameInWorkspace(anyString(), anyLong(), anyBoolean());
        verify(stackCommonService, times(1)).deleteWithKerberosByNameInWorkspace(stack.getName(), stack.getWorkspace().getId(), true);
        verify(stackCommonService, times(0)).deleteWithKerberosByCrnInWorkspace(anyString(), anyLong(), anyBoolean());
    }

    @Test
    public void testDeleteWhenDtoNameFilledAndForcedFalseThenDeleteCalled() {
        underTest.delete(ResourceAccessDtoBuilder.aResourceAccessDtoBuilder().withName(stack.getName()).build(), stack.getWorkspace().getId(), false);

        verify(stackCommonService, times(1)).deleteWithKerberosByNameInWorkspace(anyString(), anyLong(), anyBoolean());
        verify(stackCommonService, times(1)).deleteWithKerberosByNameInWorkspace(stack.getName(), stack.getWorkspace().getId(), false);
        verify(stackCommonService, times(0)).deleteWithKerberosByCrnInWorkspace(anyString(), anyLong(), anyBoolean());
    }

    @Test
    public void testDeleteWhenDtoCrnFilledAndForcedTrueThenDeleteCalled() {
        underTest.delete(ResourceAccessDtoBuilder.aResourceAccessDtoBuilder().withCrn(stack.getResourceCrn()).build(), stack.getWorkspace().getId(), true);

        verify(stackCommonService, times(1)).deleteWithKerberosByCrnInWorkspace(anyString(), anyLong(), anyBoolean());
        verify(stackCommonService, times(1)).deleteWithKerberosByCrnInWorkspace(stack.getResourceCrn(), stack.getWorkspace().getId(), true);
        verify(stackCommonService, times(0)).deleteWithKerberosByNameInWorkspace(anyString(), anyLong(), anyBoolean());
    }

    @Test
    public void testDeleteWhenDtoCrnFilledAndForcedFalseThenDeleteCalled() {
        underTest.delete(ResourceAccessDtoBuilder.aResourceAccessDtoBuilder().withCrn(stack.getResourceCrn()).build(), stack.getWorkspace().getId(), false);

        verify(stackCommonService, times(1)).deleteWithKerberosByCrnInWorkspace(anyString(), anyLong(), anyBoolean());
        verify(stackCommonService, times(1)).deleteWithKerberosByCrnInWorkspace(stack.getResourceCrn(), stack.getWorkspace().getId(), false);
        verify(stackCommonService, times(0)).deleteWithKerberosByNameInWorkspace(anyString(), anyLong(), anyBoolean());
    }

    @Test
    public void testDeleteWhenNeitherCrnOrNameProvidedThenBadRequestExceptionComes() {
        exceptionRule.expect(com.sequenceiq.cloudbreak.exception.BadRequestException.class);
        exceptionRule.expectMessage(ResourceAccessDto.INVALID_RESOURCE_ACCESS_DTO_EXCEPTION_MESSAGE);

        underTest.delete(ResourceAccessDtoBuilder.aResourceAccessDtoBuilder().build(), stack.getWorkspace().getId(), true);

        verify(stackCommonService, times(0)).findStackByNameAndWorkspaceId(anyString(), anyLong(), anySet(), any());
        verify(stackCommonService, times(0)).findStackByCrnAndWorkspaceId(anyString(), anyLong(), anySet(), any());
    }

    @Test
    public void testDeleteIfDtoIsNullThenIllegalArgumentExceptionComes() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(ResourceAccessDto.NULL_DTO_EXCEPTION_MESSAGE);

        underTest.delete(null, stack.getWorkspace().getId(), true);

        verify(stackCommonService, times(0)).findStackByNameAndWorkspaceId(anyString(), anyLong(), anySet(), any());
        verify(stackCommonService, times(0)).findStackByCrnAndWorkspaceId(anyString(), anyLong(), anySet(), any());
    }

    @Test
    public void testGetWhenDtoNameFilledThenProperGetCalled() {
        StackV4Response expected = stackResponse();
        when(stackCommonService.findStackByNameAndWorkspaceId(stack.getName(), stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE))
                .thenReturn(expected);

        StackV4Response result = underTest.get(ResourceAccessDtoBuilder.aResourceAccessDtoBuilder().withName(stack.getName()).build(),
                stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE);

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

        StackV4Response result = underTest.get(ResourceAccessDtoBuilder.aResourceAccessDtoBuilder().withCrn(stack.getResourceCrn()).build(),
                stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE);

        assertEquals(expected, result);
        verify(stackCommonService, times(1)).findStackByCrnAndWorkspaceId(anyString(), anyLong(), anySet(), any());
        verify(stackCommonService, times(1)).findStackByCrnAndWorkspaceId(stack.getResourceCrn(), stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE);
        verify(stackCommonService, times(0)).findStackByNameAndWorkspaceId(anyString(), anyLong(), anySet(), any());
    }

    @Test
    public void testGethenNeitherCrnOrNameProvidedThenBadRequestExceptionComes() {
        exceptionRule.expect(com.sequenceiq.cloudbreak.exception.BadRequestException.class);
        exceptionRule.expectMessage(ResourceAccessDto.INVALID_RESOURCE_ACCESS_DTO_EXCEPTION_MESSAGE);

        underTest.get(ResourceAccessDtoBuilder.aResourceAccessDtoBuilder().build(), stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE);

        verify(stackCommonService, times(0)).findStackByNameAndWorkspaceId(anyString(), anyLong(), anySet(), any());
        verify(stackCommonService, times(0)).findStackByCrnAndWorkspaceId(anyString(), anyLong(), anySet(), any());
    }

    @Test
    public void testGetByWorkspaceIfDtoIsNullThenIllegalArgumentExceptionComes() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(ResourceAccessDto.NULL_DTO_EXCEPTION_MESSAGE);

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

        StackViewV4Response response = underTest.getForInternalCrn(ResourceAccessDtoBuilder.aResourceAccessDtoBuilder().withCrn("myCrn").build(), STACK_TYPE);

        assertNotNull(response);
        verify(stackApiViewService, times(1)).retrieveStackByCrnAndType(anyString(), any(StackType.class));
        verify(converterUtil, times(1)).convert(any(StackApiView.class), any());
        verify(environmentServiceDecorator, times(1)).prepareEnvironment(any(StackViewV4Response.class));
    }

    private StackV4Response stackResponse() {
        return new StackV4Response();
    }

}
