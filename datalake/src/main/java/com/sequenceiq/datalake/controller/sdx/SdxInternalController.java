package com.sequenceiq.datalake.controller.sdx;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.ProxyConfigService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.cert.CertRenewalService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxInternalClusterRequest;

@Controller
public class SdxInternalController implements SdxInternalEndpoint {

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxClusterConverter sdxClusterConverter;

    @Inject
    private SdxMetricService metricService;

    @Inject
    private CertRenewalService certRenewalService;

    @Inject
    private ProxyConfigService proxyConfigService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATALAKE)
    public SdxClusterResponse create(String name, SdxInternalClusterRequest createSdxClusterRequest) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        Pair<SdxCluster, FlowIdentifier> result = sdxService.createSdx(userCrn, name, createSdxClusterRequest, createSdxClusterRequest.getStackV4Request());
        SdxCluster sdxCluster = result.getLeft();
        metricService.incrementMetricCounter(MetricType.INTERNAL_SDX_REQUESTED, sdxCluster);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        sdxClusterResponse.setName(sdxCluster.getClusterName());
        sdxClusterResponse.setFlowIdentifier(result.getRight());
        return sdxClusterResponse;
    }

    @Override
    @InternalOnly
    public FlowIdentifier renewCertificate(@ResourceCrn String crn) {
        SdxCluster sdxCluster = sdxService.getByCrn(crn);
        return certRenewalService.triggerInternalRenewCertificate(sdxCluster);
    }

    @Override
    @InternalOnly
    public void updateDbEngineVersion(@ResourceCrn String crn, String dbEngineVersion) {
        MDCBuilder.addResourceCrn(crn);
        sdxService.updateDatabaseEngineVersion(crn, dbEngineVersion);
    }

    @Override
    @InternalOnly
    public FlowIdentifier modifyProxy(@ResourceCrn String crn, String previousProxyCrn, @InitiatorUserCrn String initiatorUserCrn) {
        SdxCluster sdxCluster = sdxService.getByCrn(crn);
        return proxyConfigService.modifyProxyConfig(sdxCluster, previousProxyCrn);
    }
}
