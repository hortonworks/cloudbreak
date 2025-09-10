package com.sequenceiq.datalake.controller.sdx;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.ProxyConfigService;
import com.sequenceiq.datalake.service.sdx.SdxService;

@ExtendWith(MockitoExtension.class)
class SdxInternalControllerTest {

    private static final String SDX_CRN = "sdx-crn";

    @InjectMocks
    private SdxInternalController underTest;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxCluster sdxCluster;

    @Mock
    private ProxyConfigService proxyConfigService;

    @BeforeEach
    void setUp() {
        when(sdxService.getByCrn(SDX_CRN)).thenReturn(sdxCluster);
    }

    @Test
    void modifyProxy() {
        String previousProxyCrn = "prev-proxy-crn";
        String initiatorUserCrn = "user-crn";

        underTest.modifyProxy(SDX_CRN, previousProxyCrn, initiatorUserCrn);

        verify(proxyConfigService).modifyProxyConfig(sdxCluster, previousProxyCrn);
    }

}
