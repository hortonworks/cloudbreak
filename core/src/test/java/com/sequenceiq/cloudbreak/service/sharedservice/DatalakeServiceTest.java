package com.sequenceiq.cloudbreak.service.sharedservice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.List;
import java.util.Map;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.projection.StackStatusView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.view.ClusterApiView;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith({MockitoExtension.class})
public class DatalakeServiceTest {

    @Mock
    private DatalakeResourcesService datalakeResourcesService;

    @Mock
    private StackService stackService;

    @Mock
    private BlueprintUtils blueprintUtils;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private DatalakeService underTest;

    @BeforeEach
    public void setup() {
        Stack resultStack = new Stack();
        resultStack.setName("teststack");
        lenient().when(stackService.getByCrn(anyString())).thenReturn(resultStack);

        DatalakeResources resultDatalakeResource = new DatalakeResources();
        resultDatalakeResource.setName("testdl");
        resultDatalakeResource.setServiceDescriptorMap(Map.of());
        lenient().when(datalakeResourcesService.findById(any())).thenReturn(Optional.of(resultDatalakeResource));
    }

    @Test
    public void testPrepareDatalakeRequestWhenDatalakeCrnIsNotNull() {
        Stack source = new Stack();
        source.setDatalakeCrn("crn");
        source.setDatalakeResourceId(1L);
        StackV4Request x = new StackV4Request();
        underTest.prepareDatalakeRequest(source, x);
        verify(stackService, times(1)).getByCrn("crn");
        verify(datalakeResourcesService, never()).findById(1L);
    }

    @Test
    public void testPrepareDatalakeRequestWhenDatalakeCrnIsNull() {
        Stack source = new Stack();
        source.setDatalakeCrn(null);
        source.setDatalakeResourceId(1L);
        StackV4Request x = new StackV4Request();
        underTest.prepareDatalakeRequest(source, x);
        verify(stackService, never()).getByCrn("crn");
        verify(datalakeResourcesService, times(1)).findById(1L);
    }

    @Test
    public void testAddSharedServiceResponseWhenDatalakeCrnIsNotNull() {
        Stack source = new Stack();
        source.setDatalakeCrn("crn");
        source.setDatalakeResourceId(1L);
        StackV4Response x = new StackV4Response();
        underTest.addSharedServiceResponse(source, x);
        verify(stackService, times(1)).getByCrn("crn");
        verify(datalakeResourcesService, never()).findById(1L);
    }

    @Test
    public void testAddSharedServiceResponseWhenDatalakeCrnIsNull() {
        Stack source = new Stack();
        source.setDatalakeCrn(null);
        source.setDatalakeResourceId(1L);
        StackV4Response x = new StackV4Response();
        underTest.addSharedServiceResponse(source, x);
        verify(stackService, never()).getByCrn("crn");
        verify(datalakeResourcesService, times(1)).findById(1L);
    }

    @Test
    public void testGetDatalakeStackByDatahubStackWhereDatalakeCrnIsNull() {
        Stack stack = new Stack();
        stack.setDatalakeCrn(null);
        stack.setDatalakeResourceId(1L);
        underTest.getDatalakeStackByDatahubStack(stack);
        verify(stackService, never()).getByCrn("crn");
        verify(datalakeResourcesService, times(1)).findById(1L);
    }

    @Test
    public void testGetDatalakeStackByDatahubStackWhereDatalakeCrnIsNotNull() {
        Stack stack = new Stack();
        stack.setDatalakeCrn("crn");
        stack.setDatalakeResourceId(1L);
        underTest.getDatalakeStackByDatahubStack(stack);
        verify(stackService, times(1)).getByCrn("crn");
        verify(datalakeResourcesService, never()).findById(1L);
    }

    @Test
    public void testAddSharedServiceResponseByViewWhenDatalakeCrnIsNotNull() {
        ClusterApiView clusterApiView = new ClusterApiView();
        ClusterViewV4Response clusterViewV4Response = new ClusterViewV4Response();
        StackApiView stackApiView = new StackApiView();
        stackApiView.setDatalakeCrn("crn");
        stackApiView.setDatalakeId(1L);
        clusterApiView.setStack(stackApiView);
        underTest.addSharedServiceResponse(clusterApiView, clusterViewV4Response);
        verify(stackService, times(1)).getByCrn("crn");
        verify(datalakeResourcesService, never()).findById(1L);
    }

    @Test
    public void testAddSharedServiceResponseByViewWhenDatalakeCrnIsNull() {
        ClusterApiView clusterApiView = new ClusterApiView();
        ClusterViewV4Response clusterViewV4Response = new ClusterViewV4Response();
        StackApiView stackApiView = new StackApiView();
        stackApiView.setDatalakeCrn(null);
        stackApiView.setDatalakeId(1L);
        clusterApiView.setStack(stackApiView);
        underTest.addSharedServiceResponse(clusterApiView, clusterViewV4Response);
        verify(stackService, never()).getByCrn("crn");
        verify(datalakeResourcesService, times(1)).findById(1L);
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
    public void testGetDatalakeResourceIdHasNoResult() {
        Optional<Long> res = underTest.getDatalakeResourceId(1L);
        Assertions.assertTrue(res.isEmpty());
    }

    @Test
    public void testGetDatalakeResourceIdHasResult() {
        DatalakeResources datalakeResources = new DatalakeResources();
        datalakeResources.setId(2L);
        when(datalakeResourcesService.findByDatalakeStackId(any())).thenReturn(Optional.of(datalakeResources));
        Optional<Long> res = underTest.getDatalakeResourceId(1L);
        Assertions.assertTrue(res.get().equals(2L));
    }

    @Test
    public void testCreateSharedServiceConfigsViewByCrn() {
        Stack stack = new Stack();
        stack.setDatalakeCrn("crn");
        stack.setDatalakeResourceId(1L);
        underTest.createSharedServiceConfigsView(stack);
        verify(stackService, times(1)).getByCrn("crn");
        verify(datalakeResourcesService, never()).findById(1L);
    }

    @Test
    public void testCreateSharedServiceConfigsViewByDatalakeResourceCrn() {
        Stack stack = new Stack();
        stack.setDatalakeCrn(null);
        stack.setDatalakeResourceId(1L);
        stack.setCluster(new Cluster());
        underTest.createSharedServiceConfigsView(stack);
        verify(stackService, never()).getByCrn("crn");
        verify(datalakeResourcesService, times(1)).findById(1L);
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

    @Test
    public void testSetDatalakeIdOnStackNoResult() {
        Stack inputStack = new Stack();
        StackV4Request stackV4Request = new StackV4Request();
        Workspace workspace = new Workspace();
        underTest.setDatalakeIdOnStack(inputStack, stackV4Request, workspace);
        Assertions.assertTrue(inputStack.getDatalakeResourceId() == null);
    }

    @Test
    public void testSetDatalakeIdOnStackHasResult() {
        Stack inputStack = new Stack();
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setSharedService(new SharedServiceV4Request());
        stackV4Request.getSharedService().setDatalakeName("name");
        DatalakeResources outputDatalakeResource = new DatalakeResources();
        when(datalakeResourcesService.getByNameForWorkspace(any(), any())).thenReturn(outputDatalakeResource);
        Stack outputStack = new Stack();
        outputStack.setResourceCrn("crn");
        outputDatalakeResource.setId(1L);
        outputDatalakeResource.setDatalakeStackId(1L);
        when(stackService.get(any())).thenReturn(outputStack);
        Workspace workspace = new Workspace();
        underTest.setDatalakeIdOnStack(inputStack, stackV4Request, workspace);
        Assertions.assertTrue(inputStack.getDatalakeResourceId() != null);
        Assertions.assertTrue(inputStack.getDatalakeCrn() != null);
    }
}
