package com.sequenceiq.cloudbreak.service.sharedservice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.projection.StackStatusView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith({MockitoExtension.class})
public class DatalakeServiceTest {

    @Mock
    private StackService stackService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private DatalakeService underTest;

    @BeforeEach
    public void setup() {
        Stack resultStack = new Stack();
        resultStack.setName("teststack");
        lenient().when(stackService.getByCrn(anyString())).thenReturn(resultStack);
    }

    @Test
    public void testPrepareDatalakeRequestWhenDatalakeCrnIsNotNull() {
        Stack source = new Stack();
        source.setDatalakeCrn("crn");
        StackV4Request x = new StackV4Request();
        underTest.prepareDatalakeRequest(source, x);
        verify(stackService, times(1)).getByCrn("crn");
    }

    @Test
    public void testPrepareDatalakeRequestWhenDatalakeCrnIsNull() {
        Stack source = new Stack();
        source.setDatalakeCrn(null);
        StackV4Request x = new StackV4Request();
        underTest.prepareDatalakeRequest(source, x);
        verify(stackService, never()).getByCrn("crn");
    }

    @Test
    public void testAddSharedServiceResponseWhenDatalakeCrnIsNotNull() {
        Stack source = new Stack();
        source.setDatalakeCrn("crn");
        StackV4Response x = new StackV4Response();
        underTest.addSharedServiceResponse(source, x);
        verify(stackService, times(1)).getByCrn("crn");
    }

    @Test
    public void testAddSharedServiceResponseWhenDatalakeCrnIsNull() {
        Stack source = new Stack();
        source.setDatalakeCrn(null);
        StackV4Response x = new StackV4Response();
        underTest.addSharedServiceResponse(source, x);
        verify(stackService, never()).getByCrn("crn");
    }

    @Test
    public void testGetDatalakeStackByDatahubStackWhereDatalakeCrnIsNull() {
        Stack stack = new Stack();
        stack.setDatalakeCrn(null);
        underTest.getDatalakeStackByDatahubStack(stack);
        verify(stackService, never()).getByCrn("crn");
    }

    @Test
    public void testGetDatalakeStackByDatahubStackWhereDatalakeCrnIsNotNull() {
        Stack stack = new Stack();
        stack.setDatalakeCrn("crn");
        underTest.getDatalakeStackByDatahubStack(stack);
        verify(stackService, times(1)).getByCrn("crn");
    }

    @Test
    public void testGetDatalakeStackByStackEnvironmentCrnWhenParamStackIsNotDatahub() {
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        Optional<Stack> res = underTest.getDatalakeStackByStackEnvironmentCrn(stack);
        Assertions.assertTrue(res.isEmpty());
    }

    @Test
    public void testGetDatalakeStackByStackEnvironmentCrnResultsAreEmpty() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        Optional<Stack> res = underTest.getDatalakeStackByStackEnvironmentCrn(stack);
        Assertions.assertTrue(res.isEmpty());
    }

    @Test
    public void testGetDatalakeStackByStackEnvironmentCrnWithResult() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        when(stackService.getByEnvironmentCrnAndStackType(any(), any())).thenReturn(List.of(new StackStatusView() {
            @Override
            public Long getId() {
                return 1L;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public StackStatus getStatus() {
                return null;
            }
        }));
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(new Stack());

        Optional<Stack> res = underTest.getDatalakeStackByStackEnvironmentCrn(stack);
        Assertions.assertTrue(!res.isEmpty());
    }

    @Test
    public void testCreateSharedServiceConfigsViewByCrn() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        stack.setDatalakeCrn("crn");
        stack.setType(StackType.WORKLOAD);

        SharedServiceConfigsView res = underTest.createSharedServiceConfigsView(stack);

        verify(stackService, times(1)).getByCrn("crn");
        Assertions.assertFalse(res.isDatalakeCluster());

    }

    @Test
    public void testCreateSharedServiceConfigsViewFromBlueprintUtilsWhenDatalake() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        stack.setType(StackType.DATALAKE);

        SharedServiceConfigsView res = underTest.createSharedServiceConfigsView(stack);

        verify(stackService, times(0)).getByCrn("crn");
        Assertions.assertTrue(res.isDatalakeCluster());
    }

    @Test
    public void testCreateSharedServiceConfigsViewWhenDatahubButDatalakeCrnIsMissing() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        stack.setType(StackType.WORKLOAD);

        SharedServiceConfigsView res = underTest.createSharedServiceConfigsView(stack);

        verify(stackService, times(0)).getByCrn("crn");
        Assertions.assertFalse(res.isDatalakeCluster());
    }

    @Test
    public void testGetDatalakeCrnHasResult() {
        StackV4Request source = new StackV4Request();
        Workspace workspace = new Workspace();
        source.setSharedService(new SharedServiceV4Request());
        source.getSharedService().setDatalakeName("name");
        Stack output = new Stack();
        output.setResourceCrn("resultCrn");
        when(stackService.findStackByNameOrCrnAndWorkspaceId(any(), any())).thenReturn(Optional.of(output));
        String res = underTest.getDatalakeCrn(source, workspace);
        Assertions.assertTrue("resultCrn".equals(res));
    }
}
