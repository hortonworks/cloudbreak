package com.sequenceiq.cloudbreak.sdx.paas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.paas.service.PaasSdxDescribeService;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
public class PaasSdxDescribeServiceTest {

    private static final String PAAS_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:crn1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:crn1";

    private static final String INTERNAL_ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    @Mock
    private SdxEndpoint sdxEndpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @InjectMocks
    private PaasSdxDescribeService underTest;

    @BeforeEach
    void setup() {
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        lenient().when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        lenient().when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_ACTOR);
    }

    @Test
    public void testLocalListCrn() throws IllegalAccessException {
        LocalPaasSdxService mockLocalSdxService = mock(LocalPaasSdxService.class);
        FieldUtils.writeField(underTest, "localPaasSdxService", Optional.of(mockLocalSdxService), true);
        when(mockLocalSdxService.listSdxCrns(anyString())).thenReturn(Set.of(PAAS_CRN));
        Set<String> sdxCrns = underTest.listSdxCrns(ENV_CRN);
        assertTrue(sdxCrns.contains(PAAS_CRN));
        verifyNoInteractions(sdxEndpoint);
        verify(mockLocalSdxService).listSdxCrns(anyString());
    }

    @Test
    public void testDlServiceListCrn() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "localPaasSdxService", Optional.empty(), true);
        when(sdxEndpoint.getByEnvCrn(any(), eq(false))).thenReturn(List.of(getSdxClusterResponse(false)));
        Set<String> sdxCrns = underTest.listSdxCrns(ENV_CRN);
        assertTrue(sdxCrns.contains(PAAS_CRN));
        verify(sdxEndpoint).getByEnvCrn(eq(ENV_CRN), eq(false));
    }

    @Test
    void testGetPaasSdxLocally() throws IllegalAccessException {
        LocalPaasSdxService mockLocalSdxService = mock(LocalPaasSdxService.class);
        FieldUtils.writeField(underTest, "localPaasSdxService", Optional.of(mockLocalSdxService), true);
        when(mockLocalSdxService.getSdxBasicView(anyString())).thenReturn(Optional.of(SdxBasicView.builder().withCrn("crn").build()));

        underTest.getSdxByEnvironmentCrn("envCrn");

        verifyNoInteractions(sdxEndpoint);
        verify(mockLocalSdxService).getSdxBasicView(anyString());
    }

    @Test
    void testGetPaasSdxUsingDlService() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "localPaasSdxService", Optional.empty(), true);
        when(sdxEndpoint.getByEnvCrn(anyString(), eq(false))).thenReturn(List.of(getSdxClusterResponse(false)));

        underTest.getSdxByEnvironmentCrn("envCrn");

        verify(sdxEndpoint).getByEnvCrn(anyString(), eq(false));
    }

    @ParameterizedTest
    @MethodSource("detachedDatalakeSource")
    void testGetPaasSdxUsingDlServiceUsingFilterOutDetached(boolean detachedDatalake, boolean datalakeFound) throws IllegalAccessException {
        FieldUtils.writeField(underTest, "localPaasSdxService", Optional.empty(), true);
        when(sdxEndpoint.getByEnvCrn(anyString(), eq(false))).thenReturn(List.of(getSdxClusterResponse(detachedDatalake)));

        Optional<SdxBasicView> response = underTest.getSdxByEnvironmentCrn("envCrn");

        verify(sdxEndpoint).getByEnvCrn(anyString(), eq(false));
        assertEquals(response.isPresent(), datalakeFound);
    }

    private static Stream<Arguments> detachedDatalakeSource() {
        return Stream.of(
                Arguments.of(false, true),
                Arguments.of(true, false)
        );
    }

    private SdxClusterResponse getSdxClusterResponse(boolean detached) {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setCrn(PAAS_CRN);
        sdxClusterResponse.setEnvironmentCrn(ENV_CRN);
        sdxClusterResponse.setStatus(SdxClusterStatusResponse.RUNNING);
        sdxClusterResponse.setDetached(detached);
        return sdxClusterResponse;
    }
}
