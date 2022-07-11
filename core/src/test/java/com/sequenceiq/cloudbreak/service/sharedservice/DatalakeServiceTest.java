package com.sequenceiq.cloudbreak.service.sharedservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
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
import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackIdViewImpl;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class DatalakeServiceTest {

    @Mock
    private StackService stackService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private StackViewService stackViewService;

    @InjectMocks
    private DatalakeService underTest;

    @BeforeEach
    void setup() {
        Stack resultStack = new Stack();
        resultStack.setName("teststack");
        lenient().when(stackService.getByCrn(anyString())).thenReturn(resultStack);
        lenient().when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(1L);
    }

    @Test
    void testPrepareDatalakeRequestWhenDatalakeCrnIsNotNull() {
        Stack source = new Stack();
        source.setDatalakeCrn("crn");
        StackV4Request x = new StackV4Request();
        lenient().when(stackService.getResourceBasicViewByResourceCrn(anyString())).thenReturn(Optional.empty());
        underTest.prepareDatalakeRequest(source, x);
        verify(stackService, times(1)).getResourceBasicViewByResourceCrn("crn");
    }

    @Test
    void testPrepareDatalakeRequestWhenDatalakeCrnIsNull() {
        Stack source = new Stack();
        source.setDatalakeCrn(null);
        StackV4Request x = new StackV4Request();
        underTest.prepareDatalakeRequest(source, x);
        verify(stackService, never()).getByCrn("crn");
    }

    @Test
    void testAddSharedServiceResponseWhenDatalakeCrnIsNotNull() {
        ResourceBasicView resourceBasicView = mock(ResourceBasicView.class);
        when(resourceBasicView.getId()).thenReturn(1L);
        when(resourceBasicView.getName()).thenReturn("teststack");
        lenient().when(stackService.getResourceBasicViewByResourceCrn(anyString())).thenReturn(Optional.of(resourceBasicView));
        Stack source = new Stack();
        source.setDatalakeCrn("crn");
        StackV4Response x = new StackV4Response();
        underTest.addSharedServiceResponse(source, x);
        verify(stackService, times(1)).getResourceBasicViewByResourceCrn("crn");
        assertEquals(1L, x.getSharedService().getSharedClusterId());
        assertEquals("teststack", x.getSharedService().getSharedClusterName());
    }

    @Test
    void testAddSharedServiceResponseWhenDatalakeCrnIsNotNullAndDatalakeIsMissing() {
        lenient().when(stackService.getResourceBasicViewByResourceCrn(anyString())).thenReturn(Optional.empty());
        Stack source = new Stack();
        source.setDatalakeCrn("crn");
        StackV4Response x = new StackV4Response();
        underTest.addSharedServiceResponse(source, x);
        verify(stackService, times(1)).getResourceBasicViewByResourceCrn("crn");
        assertNull(x.getSharedService().getSharedClusterId());
        assertNull(x.getSharedService().getSharedClusterName());
    }

    @Test
    void testAddSharedServiceResponseWhenDatalakeCrnIsNull() {
        Stack source = new Stack();
        source.setDatalakeCrn(null);
        StackV4Response x = new StackV4Response();
        underTest.addSharedServiceResponse(source, x);
        verify(stackService, never()).getByCrnOrElseNull("crn");
    }

    @Test
    void testGetDatalakeStackByDatahubStackWhereDatalakeCrnIsNull() {
        Stack stack = new Stack();
        stack.setDatalakeCrn(null);
        underTest.getDatalakeStackByDatahubStack(stack);
        verify(stackService, never()).getByCrn("crn");
    }

    @Test
    void testGetDatalakeStackByDatahubStackWhereDatalakeCrnIsNotNull() {
        Stack resultStack = new Stack();
        resultStack.setName("teststack");
        lenient().when(stackService.getByCrnOrElseNull(anyString())).thenReturn(resultStack);
        Stack stack = new Stack();
        stack.setDatalakeCrn("crn");
        Optional<Stack> datalake = underTest.getDatalakeStackByDatahubStack(stack);
        verify(stackService, times(1)).getByCrnOrElseNull("crn");
        assertTrue(datalake.isPresent());
    }

    @Test
    void testGetDatalakeStackByDatahubStackWhereDatalakeCrnIsNotNullAndDatalakeIsMissing() {
        Stack stack = new Stack();
        stack.setDatalakeCrn("crn");
        Optional<Stack> datalake = underTest.getDatalakeStackByDatahubStack(stack);
        verify(stackService, times(1)).getByCrnOrElseNull("crn");
        assertTrue(datalake.isEmpty());
    }

    @Test
    void testGetDatalakeStackByStackEnvironmentCrnWhenParamStackIsNotDatahub() {
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        Optional<Stack> res = underTest.getDatalakeStackByStackEnvironmentCrn(stack);
        assertTrue(res.isEmpty());
    }

    @Test
    void testGetDatalakeStackByStackEnvironmentCrnResultsAreEmpty() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        Optional<Stack> res = underTest.getDatalakeStackByStackEnvironmentCrn(stack);
        assertTrue(res.isEmpty());
    }

    @Test
    void testGetDatalakeStackByStackEnvironmentCrnWithResult() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        when(stackService.getByEnvironmentCrnAndStackType(any(), any())).thenReturn(List.of(new StackIdViewImpl(1L, "no", "no")));

        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(new Stack());

        Optional<Stack> res = underTest.getDatalakeStackByStackEnvironmentCrn(stack);
        assertFalse(res.isEmpty());
    }

    @Test
    void testCreateSharedServiceConfigsViewByCrn() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        stack.setDatalakeCrn("crn");
        stack.setType(StackType.WORKLOAD);

        SharedServiceConfigsView res = underTest.createSharedServiceConfigsView(stack);

        verify(stackService, times(1)).getByCrnOrElseNull("crn");
        assertFalse(res.isDatalakeCluster());

    }

    @Test
    void testCreateSharedServiceConfigsViewFromBlueprintUtilsWhenDatalake() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        stack.setType(StackType.DATALAKE);

        SharedServiceConfigsView res = underTest.createSharedServiceConfigsView(stack);

        verify(stackService, times(0)).getByCrnOrElseNull("crn");
        assertTrue(res.isDatalakeCluster());
    }

    @Test
    void testCreateSharedServiceConfigsViewWhenDatahubButDatalakeCrnIsMissing() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        stack.setType(StackType.WORKLOAD);

        SharedServiceConfigsView res = underTest.createSharedServiceConfigsView(stack);

        verify(stackService, times(0)).getByCrnOrElseNull("crn");
        assertFalse(res.isDatalakeCluster());
    }

    @Test
    void testGetDatalakeCrnHasResult() {
        StackV4Request source = new StackV4Request();
        Workspace workspace = new Workspace();
        source.setSharedService(new SharedServiceV4Request());
        source.getSharedService().setDatalakeName("name");
        Stack output = new Stack();
        output.setResourceCrn("resultCrn");
        when(stackService.findStackByNameOrCrnAndWorkspaceId(any(), any())).thenReturn(Optional.of(output));
        String res = underTest.getDatalakeCrn(source, workspace);
        assertEquals("resultCrn", res);
    }

    @Test
    void testGetResourceCrnByResourceName() throws IllegalAccessException {
        when(stackViewService.findNotTerminatedByName(any(), any())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> underTest.getResourceCrnByResourceName("name"), "name stack not found");

        when(stackViewService.findNotTerminatedByName(any(), any())).thenReturn(Optional.of(stackView(StackType.WORKLOAD, null, null)));
        assertThrows(BadRequestException.class, () -> underTest.getResourceCrnByResourceName("name"), "name stack is not a Data Lake");

        when(stackViewService.findNotTerminatedByName(any(), any())).thenReturn(Optional.of(stackView(StackType.DATALAKE, "crn", null)));
        assertEquals("crn", underTest.getResourceCrnByResourceName("name"));
    }

    @Test
    void testGetEnviromentCrnByResourceCrn() throws IllegalAccessException {
        when(stackViewService.findNotTerminatedByCrn(any(), any())).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), underTest.getEnvironmentCrnByResourceCrn("crn"));

        when(stackViewService.findNotTerminatedByCrn(any(), any())).thenReturn(Optional.of(stackView(StackType.WORKLOAD, null, null)));
        assertThrows(BadRequestException.class, () -> underTest.getEnvironmentCrnByResourceCrn("crn"), "Stack with CRN crn is not a Data Lake");

        when(stackViewService.findNotTerminatedByCrn(any(), any())).thenReturn(Optional.of(stackView(StackType.DATALAKE, null, "envCrn")));
        assertEquals(Optional.of("envCrn"), underTest.getEnvironmentCrnByResourceCrn("crn"));
    }

    @Test
    void testGetResourceCrnListByResourceNameList() throws IllegalAccessException {
        when(stackViewService.findNotTerminatedByNames(any(), any())).thenReturn(Set.of(
                stackView(StackType.WORKLOAD, "crn1", null),
                stackView(StackType.DATALAKE, "crn2", null)));
        List<String> crnList = underTest.getResourceCrnListByResourceNameList(List.of("name1", "name2"));
        assertFalse(crnList.isEmpty());
        assertEquals(1, crnList.size());
        assertTrue(crnList.contains("crn2"));
    }

    @Test
    void testGetEnvironmentCrnListByResourceCrnList() throws IllegalAccessException {
        when(stackViewService.findNotTerminatedByCrns(any(), any())).thenReturn(Set.of(
                stackView(StackType.WORKLOAD, "crn1", "envCrn1"),
                stackView(StackType.DATALAKE, "crn2", "envCrn2")));
        Map<String, Optional<String>> crnMap = underTest.getEnvironmentCrnsByResourceCrns(List.of("crn1", "crn2"));
        assertFalse(crnMap.isEmpty());
        assertEquals(1, crnMap.entrySet().size());
        assertTrue(crnMap.containsKey("crn2"));
        assertTrue(crnMap.containsValue(Optional.of("envCrn2")));
    }

    private StackView stackView(StackType type, String resourceCrn, String envCrn) throws IllegalAccessException {
        StackView view = new StackView();
        view.setType(type);
        view.setResourceCrn(resourceCrn);
        FieldUtils.writeField(view, "environmentCrn", envCrn, true);
        return view;
    }
}
