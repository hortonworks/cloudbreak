package com.sequenceiq.cloudbreak.sdx.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.cache.RdcCache;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.common.service.RdcViewFactory;
import com.sequenceiq.cloudbreak.sdx.paas.service.PaasSdxDescribeService;
import com.sequenceiq.cloudbreak.template.TemplateEndpoint;

@ExtendWith(MockitoExtension.class)
class PlatformAwareSdxConnectorRdcCacheTest {

    private static final String SDX_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:crn1";

    private static final String RDC_JSON = "{\"endPoints\":{}}";

    @Mock
    private PaasSdxDescribeService paasSdxDescribeService;

    @Mock
    private RdcViewFactory rdcViewFactory;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private PlatformAwareSdxConnector underTest;

    private ConcurrentMapCache cache;

    @BeforeEach
    void setUp() throws IllegalAccessException {
        Map<TargetPlatform, PlatformAwareSdxDescribeService> describe = Maps.newHashMap();
        describe.put(TargetPlatform.PAAS, paasSdxDescribeService);
        FieldUtils.writeField(underTest, "platformDependentSdxDescribeServices", describe, true);

        cache = new ConcurrentMapCache(RdcCache.RDC_CACHE);
        when(cacheManager.getCache(RdcCache.RDC_CACHE)).thenReturn(cache);
    }

    @Test
    void liveSuccessReturnsFreshRdcViewAndPopulatesCache() {
        RdcView expected = new RdcView(SDX_CRN, RDC_JSON, null, null, null);
        when(paasSdxDescribeService.getRemoteDataContext(SDX_CRN)).thenReturn(Optional.of(RDC_JSON));
        when(rdcViewFactory.create(SDX_CRN, Optional.of(RDC_JSON))).thenReturn(expected);
        when(paasSdxDescribeService.extendRdcView(expected)).thenReturn(expected);

        RdcView result = underTest.getRdcView(SDX_CRN);

        assertEquals(RDC_JSON, result.getRemoteDataContext().orElseThrow());
        RdcView cached = cache.get(SDX_CRN, RdcView.class);
        assertEquals(RDC_JSON, cached.getRemoteDataContext().orElseThrow());
        assertNotSame(result, cached);
    }

    @Test
    void mutatingFreshRdcViewDoesNotAffectCache() {
        RdcView fresh = new RdcView(SDX_CRN, RDC_JSON, null, null, null);
        when(paasSdxDescribeService.getRemoteDataContext(SDX_CRN)).thenReturn(Optional.of(RDC_JSON));
        when(rdcViewFactory.create(SDX_CRN, Optional.of(RDC_JSON))).thenReturn(fresh);
        when(paasSdxDescribeService.extendRdcView(fresh)).thenReturn(fresh);

        RdcView result = underTest.getRdcView(SDX_CRN);
        result.extendEndpoints(Set.of(new TemplateEndpoint("svc", "type", "endpoint")));

        assertTrue(cache.get(SDX_CRN, RdcView.class).getEndpoints("svc", "type").isEmpty());
    }

    @Test
    void mutatingStaleRdcViewDoesNotAffectCache() {
        cache.put(SDX_CRN, new RdcView(SDX_CRN, RDC_JSON, null, null, null));
        when(paasSdxDescribeService.getRemoteDataContext(SDX_CRN)).thenThrow(new RuntimeException("Gateway Timeout"));

        RdcView result = underTest.getRdcView(SDX_CRN);
        result.extendEndpoints(Set.of(new TemplateEndpoint("svc", "type", "endpoint")));

        assertTrue(cache.get(SDX_CRN, RdcView.class).getEndpoints("svc", "type").isEmpty());
    }

    @Test
    void liveFailureWithCachedEntryReturnsStaleCachedRdcView() {
        RdcView cachedView = new RdcView(SDX_CRN, RDC_JSON, null, null, null);
        cache.put(SDX_CRN, cachedView);

        when(paasSdxDescribeService.getRemoteDataContext(SDX_CRN))
                .thenThrow(new RuntimeException("Gateway Timeout"));

        RdcView result = underTest.getRdcView(SDX_CRN);

        assertNotSame(cachedView, result);
        assertEquals(RDC_JSON, result.getRemoteDataContext().orElseThrow());
    }

    @Test
    void liveFailureWithoutCacheRethrows() {
        when(paasSdxDescribeService.getRemoteDataContext(SDX_CRN))
                .thenThrow(new RuntimeException("Gateway Timeout"));

        assertThrows(RuntimeException.class, () -> underTest.getRdcView(SDX_CRN));
    }

    @Test
    void failureAfterRemoteFetchDoesNotFallBackToCache() {
        cache.put(SDX_CRN, new RdcView(SDX_CRN, RDC_JSON, null, null, null));
        when(paasSdxDescribeService.getRemoteDataContext(SDX_CRN)).thenReturn(Optional.of(RDC_JSON));
        when(rdcViewFactory.create(SDX_CRN, Optional.of(RDC_JSON))).thenThrow(new IllegalStateException("bad json"));

        assertThrows(IllegalStateException.class, () -> underTest.getRdcView(SDX_CRN));
    }

    @Test
    void hmsServiceConfigDoesNotFallBackToStaleCache() {
        cache.put(SDX_CRN, new RdcView(SDX_CRN, RDC_JSON, null, null, null));
        when(paasSdxDescribeService.getRemoteDataContext(SDX_CRN))
                .thenThrow(new RuntimeException("Gateway Timeout"));

        assertThrows(RuntimeException.class, () -> underTest.getHmsServiceConfig(SDX_CRN));
    }
}
