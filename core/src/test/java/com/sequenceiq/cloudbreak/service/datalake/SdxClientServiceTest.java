package com.sequenceiq.cloudbreak.service.datalake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@ExtendWith(MockitoExtension.class)
public class SdxClientServiceTest {

    public static final String CRN = "dlcrn";

    @Mock
    private SdxEndpoint sdxEndpoint;

    @Mock
    private SdxInternalEndpoint internalEndpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @InjectMocks
    private SdxClientService sdxClientService;

    @BeforeEach
    public void setUp() {
        RegionAwareInternalCrnGenerator crnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        lenient().when(crnGenerator.getInternalCrnForServiceAsString()).thenReturn("internalCrn");
        lenient().when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(crnGenerator);
    }

    @Test
    public void testGetByEnvironmentCrn() {
        String environmentCrn = "testEnvironmentCrn";
        List<SdxClusterResponse> clusters = new ArrayList<>();
        when(sdxEndpoint.getByEnvCrn(environmentCrn)).thenReturn(clusters);

        List<SdxClusterResponse> result = sdxClientService.getByEnvironmentCrn(environmentCrn);

        assertEquals(clusters, result);
        verify(sdxEndpoint, times(1)).getByEnvCrn(environmentCrn);
    }

    @Test
    public void testGetByEnvironmentCrnInternal() {
        String environmentCrn = "testEnvironmentCrn";
        List<SdxClusterResponse> clusters = new ArrayList<>();

        when(sdxEndpoint.getByEnvCrn(any())).thenReturn(clusters);

        List<SdxClusterResponse> result = sdxClientService.getByEnvironmentCrnInernal(environmentCrn);

        assertEquals(clusters, result);
        verify(sdxEndpoint, times(1)).getByEnvCrn(environmentCrn);
        verify(regionAwareInternalCrnGeneratorFactory.iam(), times(1)).getInternalCrnForServiceAsString();
    }

    @Test
    public void testGetByCrn() {
        SdxClusterResponse cluster = new SdxClusterResponse();

        when(sdxEndpoint.getByCrn(CRN)).thenReturn(cluster);

        SdxClusterResponse result = sdxClientService.getByCrn(CRN);

        assertEquals(cluster, result);
        verify(sdxEndpoint, times(1)).getByCrn(CRN);
    }

    @Test
    public void testGetByCrnInternal() {
        SdxClusterResponse cluster = new SdxClusterResponse();

        when(sdxEndpoint.getByCrn(any())).thenReturn(cluster);

        SdxClusterResponse result = sdxClientService.getByCrnInternal(CRN);

        assertEquals(cluster, result);
        verify(sdxEndpoint, times(1)).getByCrn(CRN);
        verify(regionAwareInternalCrnGeneratorFactory.iam(), times(1)).getInternalCrnForServiceAsString();
    }

    @Test
    public void testList() {
        List<SdxClusterResponse> clusters = new ArrayList<>();
        when(sdxEndpoint.list(null, false)).thenReturn(clusters);

        List<SdxClusterResponse> result = sdxClientService.list();

        assertEquals(clusters, result);
        verify(sdxEndpoint, times(1)).list(null, false);
    }

    @Test
    public void testUpdateDatabaseEngineVersion() {
        String databaseEngineVersion = "newVersion";

        sdxClientService.updateDatabaseEngineVersion(CRN, databaseEngineVersion);

        verify(internalEndpoint, times(1)).updateDbEngineVersion(CRN, databaseEngineVersion);
        verify(regionAwareInternalCrnGeneratorFactory.iam(), times(1)).getInternalCrnForServiceAsString();
    }
}
