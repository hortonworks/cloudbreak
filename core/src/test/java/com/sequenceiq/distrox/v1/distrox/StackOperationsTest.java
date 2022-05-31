package com.sequenceiq.distrox.v1.distrox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackClusterStatusViewToStatusConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.UserNamePasswordV4RequestToUpdateClusterV4RequestConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.view.StackApiViewToStackViewV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.projection.StackCrnView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.image.GenerateImageCatalogService;
import com.sequenceiq.cloudbreak.service.stack.StackApiViewService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.distrox.v1.distrox.service.EnvironmentServiceDecorator;
import com.sequenceiq.distrox.v1.distrox.service.SdxServiceDecorator;
import com.sequenceiq.flow.core.FlowLogService;

public class StackOperationsTest {

    private static final StackType STACK_TYPE = StackType.WORKLOAD;

    private static final Set<String> STACK_ENTRIES = Collections.emptySet();

    private static final String IMAGE_CATALOG = "image catalog";

    @Rule
    public final ExpectedException exceptionRule = ExpectedException.none();

    @InjectMocks
    private StackOperations underTest;

    @Mock
    private StackCommonService stackCommonService;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterCommonService clusterCommonService;

    @Mock
    private StackApiViewService stackApiViewService;

    @Mock
    private EnvironmentServiceDecorator environmentServiceDecorator;

    @Mock
    private CloudbreakUser cloudbreakUser;

    @Mock
    private SdxServiceDecorator sdxServiceDecorator;

    @Mock
    private StackApiViewToStackViewV4ResponseConverter stackApiViewToStackViewV4ResponseConverter;

    @Mock
    private StackClusterStatusViewToStatusConverter stackClusterStatusViewToStatusConverter;

    @Mock
    private UserNamePasswordV4RequestToUpdateClusterV4RequestConverter userNamePasswordV4RequestToUpdateClusterV4RequestConverter;

    @Mock
    private StackImageService stackImageService;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private GenerateImageCatalogService generateImageCatalogService;

    @Mock
    private EntitlementService entitlementService;

    private Stack stack;

    private User user;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        user = TestUtil.user(1L, "someUserId");
        stack = TestUtil.stack();
        when(userService.getOrCreate(cloudbreakUser)).thenReturn(user);
    }

    @Test
    public void testDeleteWhenForcedTrueThenDeleteCalled() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());

        underTest.delete(nameOrCrn, stack.getWorkspace().getId(), true);

        verify(stackCommonService, times(1)).deleteWithKerberosInWorkspace(nameOrCrn, stack.getWorkspace().getId(), true);
    }

    @Test
    public void testDeleteWhenForcedFalseThenDeleteCalled() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());

        underTest.delete(nameOrCrn, stack.getWorkspace().getId(), false);

        verify(stackCommonService, times(1)).deleteWithKerberosInWorkspace(nameOrCrn, stack.getWorkspace().getId(), false);
    }

    @Test
    public void testGetWhenNameOrCrnNameFilledThenProperGetCalled() {
        StackV4Response expected = stackResponse();
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());
        when(stackCommonService.findStackByNameOrCrnAndWorkspaceId(nameOrCrn, stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE))
                .thenReturn(expected);

        StackV4Response result = underTest.get(nameOrCrn, stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE);

        assertEquals(expected, result);
        verify(stackCommonService, times(1)).findStackByNameOrCrnAndWorkspaceId(nameOrCrn, stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE);
    }

    @Test
    public void testGetWhenNameOrCrnCrnFilledThenProperGetCalled() {
        StackV4Response expected = stackResponse();
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(stack.getResourceCrn());
        when(stackCommonService.findStackByNameOrCrnAndWorkspaceId(nameOrCrn, stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE))
                .thenReturn(expected);

        StackV4Response result = underTest.get(nameOrCrn, stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE);

        assertEquals(expected, result);
        verify(stackCommonService, times(1)).findStackByNameOrCrnAndWorkspaceId(
                nameOrCrn, stack.getWorkspace().getId(), STACK_ENTRIES, STACK_TYPE);
    }

    @Test
    public void testGetForInternalCrn() {
        when(cloudbreakUser.getUserCrn()).thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(stackApiViewService.retrieveStackByCrnAndType(anyString(), any(StackType.class))).thenReturn(new StackApiView());
        when(stackApiViewToStackViewV4ResponseConverter.convert(any(StackApiView.class))).thenReturn(new StackViewV4Response());
        doNothing().when(environmentServiceDecorator).prepareEnvironment(any(StackViewV4Response.class));

        StackViewV4Response response = underTest.getForInternalCrn(NameOrCrn.ofCrn("myCrn"), STACK_TYPE);

        assertNotNull(response);
        verify(stackApiViewService, times(1)).retrieveStackByCrnAndType(anyString(), any(StackType.class));
        verify(stackApiViewToStackViewV4ResponseConverter, times(1)).convert(any(StackApiView.class));
        verify(environmentServiceDecorator, times(1)).prepareEnvironment(any(StackViewV4Response.class));
    }

    @Test
    public void testGetWithEnvironmentCrnsByResourceCrns() {
        StackCrnView stack1 = mock(StackCrnView.class);
        when(stack1.getResourceCrn()).thenReturn("crn1");
        when(stack1.getEnvironmentCrn()).thenReturn("envcrn1");
        StackCrnView stack2 = mock(StackCrnView.class);
        when(stack2.getResourceCrn()).thenReturn("crn2");
        when(stack2.getEnvironmentCrn()).thenReturn("envcrn2");
        StackCrnView stackWithoutEnv = mock(StackCrnView.class);
        when(stackWithoutEnv.getResourceCrn()).thenReturn("crn3");
        when(stackService.findAllByCrn(anySet())).thenReturn(List.of(stack1, stack2, stackWithoutEnv));

        Map<String, Optional<String>> result = ThreadBasedUserCrnProvider.doAs("crn:altus:iam:us-west-1:123:user:456",
                () -> underTest.getEnvironmentCrnsByResourceCrns(List.of("crn1", "crn2", "crn3")));

        Map<String, Optional<String>> expected = new LinkedHashMap<>();
        expected.put("crn1", Optional.of("envcrn1"));
        expected.put("crn2", Optional.of("envcrn2"));
        expected.put("crn3", Optional.empty());
        assertEquals(expected, result);
    }

    @Test
    public void testChangeImageCatalogFlowNotInProgress() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());
        when(stackService.getByNameOrCrnInWorkspace(nameOrCrn, stack.getWorkspace().getId())).thenReturn(stack);
        when(flowLogService.isOtherFlowRunning(stack.getId())).thenReturn(false);

        underTest.changeImageCatalog(nameOrCrn, stack.getWorkspace().getId(), IMAGE_CATALOG);

        verify(stackImageService).changeImageCatalog(stack, IMAGE_CATALOG);
    }

    @Test
    public void testChangeImageCatalogThrowsExceptionWhenFlowInProgress() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());
        when(stackService.getByNameOrCrnInWorkspace(nameOrCrn, stack.getWorkspace().getId())).thenReturn(stack);
        when(flowLogService.isOtherFlowRunning(stack.getId())).thenReturn(true);

        assertThrows(CloudbreakServiceException.class, () -> underTest.changeImageCatalog(nameOrCrn, stack.getWorkspace().getId(), IMAGE_CATALOG));
    }

    @Test
    public void testGenerateImageCatalog() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());
        CloudbreakImageCatalogV3 imageCatalog = mock(CloudbreakImageCatalogV3.class);
        when(stackService.getByNameOrCrnInWorkspace(nameOrCrn, stack.getWorkspace().getId())).thenReturn(stack);
        when(generateImageCatalogService.generateImageCatalogForStack(stack)).thenReturn(imageCatalog);

        CloudbreakImageCatalogV3 actual = underTest.generateImageCatalog(nameOrCrn, stack.getWorkspace().getId());

        assertEquals(imageCatalog, actual);
    }

    @Test
    public void rotateSaltPassword() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());
        long workspaceId = 0L;

        underTest.rotateSaltPassword(nameOrCrn, workspaceId);

        verify(stackCommonService).rotateSaltPassword(nameOrCrn, workspaceId);
    }

    private StackV4Response stackResponse() {
        return new StackV4Response();
    }

}
