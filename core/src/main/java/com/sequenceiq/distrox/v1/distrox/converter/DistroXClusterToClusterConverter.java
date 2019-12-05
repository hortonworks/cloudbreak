package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXClusterV1Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class DistroXClusterToClusterConverter {

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private ClouderaManagerV1ToClouderaManagerV4Converter cmConverter;

    @Inject
    private CloudStorageDecorator cloudStorageDecorator;

    @Inject
    private GatewayV1ToGatewayV4Converter gatewayConverter;

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

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
        response.setUserName(source.getUserName());
        response.setPassword(source.getPassword());
        response.setProxyConfigCrn(getIfNotNull(source.getProxy(), this::getProxyCrnByName));
        response.setCm(getIfNotNull(source.getCm(), cmConverter::convert));
        response.setCloudStorage(
                cloudStorageDecorator.decorate(
                        source.getBlueprintName(),
                        request.getName(),
                        source.getCloudStorage(),
                        environment));
        response.setValidateBlueprint(source.getValidateBlueprint());
        response.setExecutorType(ExecutorType.DEFAULT);
        response.setCustomContainer(null);
        response.setCustomQueue(null);
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
        return request;
    }

    private String getProxyCrnByName(String proxyName) {
        return proxyConfigDtoService.getByName(proxyName).getCrn();
    }

    private String getProxyNameByCrn(String proxyCrn) {
        return proxyConfigDtoService.getByCrn(proxyCrn).getName();
    }
}
