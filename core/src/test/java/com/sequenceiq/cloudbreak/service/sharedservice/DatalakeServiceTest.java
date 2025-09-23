package com.sequenceiq.cloudbreak.service.sharedservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.paas.service.PaasSdxDescribeService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;

@ExtendWith({MockitoExtension.class})
public class DatalakeServiceTest {

    @Mock
    private StackService stackService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Mock
    private PaasSdxDescribeService paasSdxDescribeService;

    @InjectMocks
    private DatalakeService underTest;

    @Test
    public void testPrepareDatalakeRequestWhenDatalakeIsNotNull() {
        Stack source = new Stack();
        source.setEnvironmentCrn("envCrn");
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(anyString())).thenReturn(
                Optional.of(SdxBasicView.builder().withCrn("crn").withName("name").build()));
        StackV4Request stackRequest = new StackV4Request();
        underTest.prepareDatalakeRequest(source, stackRequest);
        verify(platformAwareSdxConnector, times(1)).getSdxBasicViewByEnvironmentCrn(eq("envCrn"));
        assertEquals("name", stackRequest.getSharedService().getDatalakeName());
    }

    @Test
    public void testPrepareDatalakeRequestWhenDatalakeIsMissing() {
        Stack source = new Stack();
        source.setEnvironmentCrn("envCrn");
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(anyString())).thenReturn(Optional.empty());
        StackV4Request stackRequest = new StackV4Request();
        underTest.prepareDatalakeRequest(source, stackRequest);
        assertNull(stackRequest.getSharedService().getDatalakeName());
    }

    @Test
    public void testAddSharedServiceResponse() {
        when(paasSdxDescribeService.getSdxByEnvironmentCrn(anyString())).thenReturn(
                Optional.of(SdxBasicView.builder().withCrn("crn").withName("name").build()));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setEnvironmentCrn("envCrn");
        underTest.addSharedServiceResponse(stackV4Response);
        verify(paasSdxDescribeService, times(1)).getSdxByEnvironmentCrn(eq("envCrn"));
        assertEquals("name", stackV4Response.getSharedService().getSharedClusterName());
        assertEquals("name", stackV4Response.getSharedService().getSdxName());
        assertEquals("crn", stackV4Response.getSharedService().getSdxCrn());
    }

    @Test
    public void testAddSharedServiceResponseWhenDatalakeIsMissing() {
        when(paasSdxDescribeService.getSdxByEnvironmentCrn(anyString())).thenReturn(Optional.empty());
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setEnvironmentCrn("envCrn");
        underTest.addSharedServiceResponse(stackV4Response);
        verify(paasSdxDescribeService, times(1)).getSdxByEnvironmentCrn(eq("envCrn"));
        assertNull(stackV4Response.getSharedService().getSharedClusterName());
        assertNull(stackV4Response.getSharedService().getSdxCrn());
        assertNull(stackV4Response.getSharedService().getSdxName());
    }

    @Test
    public void testAddSharedServiceResponseWhenEnvCrnIsNull() {
        StackV4Response stackV4Response = new StackV4Response();
        underTest.addSharedServiceResponse(stackV4Response);
        verify(platformAwareSdxConnector, never()).getSdxBasicViewByEnvironmentCrn(anyString());
    }

    @Test
    public void testCreateSharedServiceConfigsViewByCrn() {
        SharedServiceConfigsView res = underTest.createSharedServiceConfigsView("pwd", StackType.WORKLOAD, "envcrn");

        verify(platformAwareSdxConnector, times(1)).getSdxAccessViewByEnvironmentCrn(anyString());
        Assertions.assertFalse(res.isDatalakeCluster());

    }

    @Test
    public void testCreateSharedServiceConfigsViewFromBlueprintUtilsWhenDatalake() {
        SharedServiceConfigsView res = underTest.createSharedServiceConfigsView("pwd", StackType.DATALAKE, null);

        verify(stackService, times(0)).getByCrnOrElseNull("crn");
        Assertions.assertTrue(res.isDatalakeCluster());
    }

    @Test
    public void testCreateSharedServiceConfigsViewWhenDatahubButDatalakeCrnIsMissing() {
        SharedServiceConfigsView res = underTest.createSharedServiceConfigsView("pwd", StackType.WORKLOAD, null);

        verify(stackService, times(0)).getByCrnOrElseNull("crn");
        Assertions.assertFalse(res.isDatalakeCluster());
    }

    @Test
    void testDecorateWithDataLakeResponseAnyPlatform() {
        StackV4Response stackV4Response = new StackV4Response();
        String envCrn = "envCrn";
        stackV4Response.setEnvironmentCrn(envCrn);
        SdxBasicView sdxBasicView = new SdxBasicView("dlaname", "dlcrn", "7.3.1", false,
                0L, null, null, TargetPlatform.PAAS);
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(envCrn)).thenReturn(Optional.of(sdxBasicView));

        underTest.decorateWithDataLakeResponseAnyPlatform(StackType.WORKLOAD, stackV4Response);

        assertEquals(sdxBasicView.name(), stackV4Response.getDataLakeResponse().name());
        assertEquals(sdxBasicView.crn(), stackV4Response.getDataLakeResponse().crn());
        assertEquals(sdxBasicView.platform().name(), stackV4Response.getDataLakeResponse().platform());
    }

    @Test
    void testDecorateWithDataLakeResponseAnyPlatformNoDatalake() {
        StackV4Response stackV4Response = new StackV4Response();
        String envCrn = "envCrn";
        stackV4Response.setEnvironmentCrn(envCrn);
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(envCrn)).thenReturn(Optional.empty());

        underTest.decorateWithDataLakeResponseAnyPlatform(StackType.WORKLOAD, stackV4Response);

        assertNull(stackV4Response.getDataLakeResponse());
    }

    @Test
    void testDecorateWithDataLakeResponseAnyPlatformIsDatalake() {
        StackV4Response stackV4Response = new StackV4Response();
        underTest.decorateWithDataLakeResponseAnyPlatform(StackType.DATALAKE, stackV4Response);

        assertNull(stackV4Response.getDataLakeResponse());

        verifyNoInteractions(platformAwareSdxConnector);
    }
}
