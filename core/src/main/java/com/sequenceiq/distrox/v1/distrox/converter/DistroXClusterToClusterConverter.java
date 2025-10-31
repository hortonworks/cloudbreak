package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXClusterV1Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;

@Component
public class DistroXClusterToClusterConverter {

    private final ClouderaManagerV1ToClouderaManagerV4Converter cmConverter;

    private final GatewayV1ToGatewayV4Converter gatewayConverter;

    private final CloudStorageDecorator cloudStorageDecorator;

    private final ProxyEndpoint proxyEndpoint;

    public DistroXClusterToClusterConverter(ClouderaManagerV1ToClouderaManagerV4Converter cmConverter, CloudStorageDecorator cloudStorageDecorator,
            GatewayV1ToGatewayV4Converter gatewayConverter, ProxyEndpoint proxyEndpoint) {
        this.cmConverter = cmConverter;
        this.cloudStorageDecorator = cloudStorageDecorator;
        this.gatewayConverter = gatewayConverter;
        this.proxyEndpoint = proxyEndpoint;
    }

    public ClusterV4Request convert(DistroXV1Request request) {
        return convert(request, null);
    }

    public ClusterV4Request convert(DistroXV1Request request, DetailedEnvironmentResponse environment) {
        DistroXClusterV1Request source = request.getCluster();
        ClusterV4Request response = new ClusterV4Request();
        if (isEmpty(source.getExposedServices())) {
            source.setExposedServices(List.of("ALL"));
        }
        response.setGateway(gatewayConverter.convert(source.getExposedServices()));
        response.setName(null);
        response.setDatabases(source.getDatabases());
        response.setBlueprintName(source.getBlueprintName());
        response.setCustomConfigurationsName(source.getCustomConfigurationsName());
        response.setUserName(source.getUserName());
        response.setPassword(source.getPassword());
        response.setProxyConfigCrn(getIfNotNull(source.getProxy(), proxy -> getProxyCrnByName(ThreadBasedUserCrnProvider.getAccountId(), proxy)));
        response.setCm(getIfNotNull(source.getCm(), cmConverter::convert));
        response.setCloudStorage(
                cloudStorageDecorator.decorate(
                        source.getBlueprintName(),
                        request.getName(),
                        source.getCloudStorage(),
                        environment));
        response.setValidateBlueprint(source.getValidateBlueprint());
        response.setCustomContainer(null);
        response.setCustomQueue(null);
        response.setEncryptionProfileName(source.getEncryptionProfileName());

        return response;
    }

    public DistroXClusterV1Request convert(ClusterV4Request source) {
        DistroXClusterV1Request request = new DistroXClusterV1Request();
        request.setExposedServices(getIfNotNull(source.getGateway(), gatewayConverter::exposedService));
        request.setDatabases(source.getDatabases());
        request.setBlueprintName(source.getBlueprintName());
        request.setUserName(null);
        request.setPassword(null);
        request.setCm(getIfNotNull(source.getCm(), cmConverter::convert));
        request.setCloudStorage(source.getCloudStorage());
        request.setProxy(source.getProxyConfigCrn());
        request.setEncryptionProfileName(source.getEncryptionProfileName());
        return request;
    }

    private String getProxyCrnByName(String accountId, String proxyName) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> proxyEndpoint.getCrnByAccountIdAndName(accountId, proxyName));
    }

}
