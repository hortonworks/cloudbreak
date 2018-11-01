package com.sequenceiq.cloudbreak.converter.v2.cli;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;

@Component
public class ClusterToAmbariV2RequestRequestConverter extends AbstractConversionServiceAwareConverter<Cluster, AmbariV2Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterToAmbariV2RequestRequestConverter.class);

    @Inject
    private ClusterComponentConfigProvider componentConfigProvider;

    @Override
    public AmbariV2Request convert(Cluster source) {
        AmbariV2Request ambariV2Request = new AmbariV2Request();
        ambariV2Request.setBlueprintName(source.getBlueprint().getName());
        prepareAmbariRepo(source, ambariV2Request);
        prepareStackRepoDetails(source, ambariV2Request);
        ambariV2Request.setConfigStrategy(null);
        ambariV2Request.setConnectedCluster(null);
        ambariV2Request.setEnableSecurity(source.isSecure());
        ambariV2Request.setGateway(null);
        ambariV2Request.setPassword("");
        ambariV2Request.setUserName("");
        ambariV2Request.setValidateBlueprint(null);
        if (source.isSecure() && source.getKerberosConfig() != null) {
            ambariV2Request.setKerberos(getConversionService().convert(source.getKerberosConfig(), KerberosRequest.class));
        }
        if (source.getGateway() != null) {
            ambariV2Request.setGateway(getConversionService().convert(source.getGateway(), GatewayJson.class));
        }
        return ambariV2Request;
    }

    private void prepareStackRepoDetails(Cluster source, AmbariV2Request ambariV2Request) {
        StackRepoDetails stackRepoDetails = componentConfigProvider.getHDPRepo(source.getId());
        if (stackRepoDetails != null) {
            ambariV2Request.setAmbariStackDetails(getConversionService().convert(stackRepoDetails, AmbariStackDetailsJson.class));
        }
    }

    private void prepareAmbariRepo(Cluster source, AmbariV2Request ambariV2Request) {
        AmbariRepo ambariRepo = componentConfigProvider.getAmbariRepo(source.getId());
        if (ambariRepo != null) {
            ambariV2Request.setAmbariRepoDetailsJson(getConversionService().convert(ambariRepo, AmbariRepoDetailsJson.class));
        }
    }
}
