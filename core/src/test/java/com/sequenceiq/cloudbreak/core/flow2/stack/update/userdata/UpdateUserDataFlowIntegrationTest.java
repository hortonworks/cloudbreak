package com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata;

import static com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata.UpdateUserDataEvents.UPDATE_USERDATA_TRIGGER_EVENT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.reactor.handler.userdata.UpdateUserDataHandler;
import com.sequenceiq.cloudbreak.reactor.handler.userdata.UpdateUserDataOnProviderHandler;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateRequest;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowLogRepository;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class UpdateUserDataFlowIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final String DATAHUB_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":cluster:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    private static final int ALL_CALLED_ONCE = 2;

    private static final int CALLED_ONCE_TILL_GENERATE_USERDATA = 1;

    private static final String USER_DATA = "IS_CCM_V2_JUMPGATE_ENABLED=false";

    @Inject
    private FlowRegister flowRegister;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private ReactorNotifier reactorNotifier;

    @MockBean(reset = MockReset.NONE)
    private StackService stackService;

    @MockBean
    private UserDataService userDataService;

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private CloudPlatformConnectors cloudPlatformConnectors;

    @MockBean
    private StackToCloudStackConverter cloudStackConverter;

    @MockBean
    private CredentialToCloudCredentialConverter credentialConverter;

    @MockBean
    private ImageService imageService;

    @MockBean
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @MockBean
    private StackUtil stackUtil;

    @Mock
    private ResourceConnector<Object> resourcesApi;

    private StackView mockStackView() {
        StackView stackView = mock(StackView.class);
        ClusterView clusterView = mock(ClusterView.class);

        when(stackView.getClusterView()).thenReturn(clusterView);
        when(stackView.getId()).thenReturn(1L);
        when(stackView.isStartInProgress()).thenReturn(true);

        return stackView;
    }

    private Stack mockStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName("stackname");
        StackStatus stackStatus = new StackStatus(stack, Status.AVAILABLE, "no reason at all", DetailedStackStatus.AVAILABLE);
        stack.setStackStatus(stackStatus);
        stack.setCluster(new Cluster());
        User user = new User();
        user.setUserId("alma");
        stack.setCreator(user);
        stack.setResourceCrn(DATAHUB_CRN);
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        stack.setWorkspace(workspace);
        workspace.setTenant(new Tenant());

        return stack;
    }

    private void mockStackService(Stack mockStack) {
        StackView stackView = mockStackView();
        when(stackService.getByIdWithTransaction(STACK_ID)).thenReturn(mockStack);
        when(stackService.getViewByIdWithoutAuth(STACK_ID)).thenReturn(stackView);
    }

    @BeforeEach
    public void setup() throws CloudbreakImageNotFoundException {
        Stack mockStack = mockStack();
        mockStackService(mockStack);
        Collection<Resource> resources = Set.of();
        when(resourceService.getAllByStackId(STACK_ID)).thenReturn(resources);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(mockStack);
        Image image = new Image("alma", Map.of(InstanceGroupType.GATEWAY, ""), "", "", "", "", "", null);
        when(imageService.getImage(STACK_ID)).thenReturn(image);

        CloudConnector<Object> connector = mock(CloudConnector.class);
        AuthenticatedContext context = mock(AuthenticatedContext.class);
        Authenticator authApi = mock(Authenticator.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authApi);
        when(connector.resources()).thenReturn(resourcesApi);
        when(authApi.authenticate(any(), any())).thenReturn(context);
    }

    @Test
    public void testUserDataUpdateAllGood() throws Exception {
        testFlow(ALL_CALLED_ONCE, true);
    }

    @Test
    public void testUserDataUpdateWhenNewUserDataFails() throws Exception {
        doThrow(new BadRequestException("")).when(userDataService).updateJumpgateFlagOnly(STACK_ID);
        testFlow(CALLED_ONCE_TILL_GENERATE_USERDATA, false);
    }

    private void testFlow(int calledOnceCount, boolean success) throws Exception {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        flowFinishedSuccessfully();
        verifyServiceCalls(calledOnceCount);
        verifyFinishingStatCalls(success);
    }

    private void verifyFinishingStatCalls(boolean success) throws Exception {
        verify(resourcesApi, times(success ? 1 : 0)).updateUserData(any(), any(), any(), any());
    }

    private void verifyServiceCalls(int calledOnceCount) throws Exception {
        final int[] expected = new int[ALL_CALLED_ONCE];
        Arrays.fill(expected, 0, calledOnceCount, 1);
        verify(userDataService, times(expected[0])).updateJumpgateFlagOnly(STACK_ID);
        verify(resourcesApi, times(expected[1])).updateUserData(any(), any(), any(), any());
    }

    private void flowFinishedSuccessfully() {
        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        assertTrue(flowLog.getAllValues().stream().anyMatch(f -> f.getFinalized()), "flow has not finalized");
    }

    private FlowIdentifier triggerFlow() {
        String selector = UPDATE_USERDATA_TRIGGER_EVENT.event();
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> reactorNotifier.notify(STACK_ID, selector,
                        new UserDataUpdateRequest(selector.toString(), STACK_ID, Tunnel.CCM)));
    }

    private void letItFlow(FlowIdentifier flowIdentifier) {
        int i = 0;
        do {
            i++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        } while (flowRegister.get(flowIdentifier.getPollableId()) != null && i < 10);
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import({
            UpdateUserDataFlowConfig.class,
            UserDataUpdateActions.class,
            UpdateUserDataHandler.class,
            UpdateUserDataOnProviderHandler.class,
            FlowIntegrationTestConfig.class
    })
    static class Config {
    }
}
