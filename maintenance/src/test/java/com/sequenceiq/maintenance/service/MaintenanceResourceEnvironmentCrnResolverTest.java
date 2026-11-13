package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXInternalV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaInternalV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaEnvironmentCrnResponse;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@ExtendWith(MockitoExtension.class)
class MaintenanceResourceEnvironmentCrnResolverTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc-1:datalake:dl-1";

    private static final String SDX_CLUSTER_CRN = "crn:cdp:datalake:us-west-1:acc-1:sdxcluster:sdx-1";

    private static final String FREEIPA_CRN = "crn:cdp:freeipa:us-west-1:acc-1:freeipa:fi-1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-1:environment:env-1";

    @Mock
    private DistroXInternalV1Endpoint distroXInternalV1Endpoint;

    @Mock
    private SdxEndpoint sdxEndpoint;

    @Mock
    private FreeIpaInternalV1Endpoint freeIpaInternalV1Endpoint;

    private MaintenanceResourceEnvironmentCrnResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new MaintenanceResourceEnvironmentCrnResolver(
                distroXInternalV1Endpoint, sdxEndpoint, freeIpaInternalV1Endpoint);
    }

    @Test
    void resolveDataHubEnvironmentCrn() {
        StackViewV4Response stack = new StackViewV4Response();
        stack.setEnvironmentCrn(ENV_CRN);
        when(distroXInternalV1Endpoint.getByCrn(DATAHUB_CRN)).thenReturn(stack);

        assertThat(resolver.resolveOptional(ACCOUNT_ID, DATAHUB_CRN, MaintenanceScopeType.DATAHUB)).contains(ENV_CRN);
    }

    @Test
    void resolveDatalakeEnvironmentCrn() {
        SdxClusterResponse cluster = new SdxClusterResponse();
        cluster.setEnvironmentCrn(ENV_CRN);
        when(sdxEndpoint.getByCrn(DATALAKE_CRN)).thenReturn(cluster);

        assertThat(resolver.resolveOptional(ACCOUNT_ID, DATALAKE_CRN, MaintenanceScopeType.DATALAKE)).contains(ENV_CRN);
        verify(sdxEndpoint, never()).getDetailByCrn(DATALAKE_CRN, Set.of());
    }

    @Test
    void resolveLegacySdxClusterCrnViaDetailLookup() {
        SdxClusterDetailResponse cluster = new SdxClusterDetailResponse();
        cluster.setEnvironmentCrn(ENV_CRN);
        when(sdxEndpoint.getDetailByCrn(SDX_CLUSTER_CRN, Set.of())).thenReturn(cluster);

        assertThat(resolver.resolveOptional(ACCOUNT_ID, SDX_CLUSTER_CRN, MaintenanceScopeType.DATALAKE)).contains(ENV_CRN);
        verify(sdxEndpoint, never()).getByCrn(SDX_CLUSTER_CRN);
    }

    @Test
    void resolveLegacySdxClusterReturnsEmptyWhenDetailLookupNotFound() {
        when(sdxEndpoint.getDetailByCrn(SDX_CLUSTER_CRN, Set.of()))
                .thenThrow(new WebApplicationException(Response.status(404).build()));

        assertThat(resolver.resolveOptional(ACCOUNT_ID, SDX_CLUSTER_CRN, MaintenanceScopeType.DATALAKE)).isEmpty();
    }

    @Test
    void resolveFreeIpaEnvironmentCrn() {
        when(freeIpaInternalV1Endpoint.getEnvironmentCrnByResourceCrn(FREEIPA_CRN))
                .thenReturn(new FreeIpaEnvironmentCrnResponse(ENV_CRN));

        assertThat(resolver.resolveOptional(ACCOUNT_ID, FREEIPA_CRN, MaintenanceScopeType.FREEIPA)).contains(ENV_CRN);
    }

    @Test
    void resolveOptionalReturnsEmptyWhenResourceMissing() {
        when(distroXInternalV1Endpoint.getByCrn(DATAHUB_CRN))
                .thenThrow(new WebApplicationException(Response.status(404).build()));

        assertThat(resolver.resolveOptional(ACCOUNT_ID, DATAHUB_CRN, MaintenanceScopeType.DATAHUB)).isEmpty();
    }

    @Test
    void resolveOptionalReturnsEmptyWhenOwnerReturnsBlankEnvironmentCrn() {
        StackViewV4Response stack = new StackViewV4Response();
        stack.setEnvironmentCrn("  ");
        when(distroXInternalV1Endpoint.getByCrn(DATAHUB_CRN)).thenReturn(stack);

        assertThat(resolver.resolveOptional(ACCOUNT_ID, DATAHUB_CRN, MaintenanceScopeType.DATAHUB)).isEmpty();
    }

    @Test
    void resolveOptionalPropagatesNonNotFoundFailures() {
        when(distroXInternalV1Endpoint.getByCrn(DATAHUB_CRN))
                .thenThrow(new WebApplicationException(Response.status(503).build()));

        assertThrows(WebApplicationException.class,
                () -> resolver.resolveOptional(ACCOUNT_ID, DATAHUB_CRN, MaintenanceScopeType.DATAHUB));
    }

    @Test
    void resolveRethrowsWhenWebApplicationExceptionHasNullResponse() {
        WebApplicationException failure = new WebApplicationException((Response) null);
        when(distroXInternalV1Endpoint.getByCrn(DATAHUB_CRN)).thenThrow(failure);

        assertThrows(WebApplicationException.class,
                () -> resolver.resolveOptional(ACCOUNT_ID, DATAHUB_CRN, MaintenanceScopeType.DATAHUB));
    }
}
