package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.ifNotNullF;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXClusterV1Request;

@Component
public class DistroXClusterToClusterConverter {

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private ClouderaManagerV1ToClouderaManagerV4Converter cmConverter;

    @Inject
    private CloudStorageV1ToCloudStorageV4Converter cloudStorageConverter;

    @Inject
    private GatewayV1ToGatewayV4Converter gatewayConverter;

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    public ClusterV4Request convert(DistroXClusterV1Request source) {

        Workspace workspace = workspaceService.getForCurrentUser();

        ClusterV4Request response = new ClusterV4Request();
        response.setKerberosName(null);
        response.setGateway(ifNotNullF(source.getGateway(), gatewayConverter::convert));
        response.setAmbari(null);
        response.setName(null);
        response.setDatabases(source.getDatabases());
        response.setLdapName(ldapConfigService.findAllInWorkspace(workspace.getId()).stream().findFirst().map(LdapConfig::getName).orElse(null));
        response.setBlueprintName(source.getBlueprintName());
        response.setUserName(source.getUserName());
        response.setPassword(source.getPassword());
        response.setProxyConfigCrn(ifNotNullF(source.getProxy(), this::getProxyCrnByName));
        response.setCm(ifNotNullF(source.getCm(), cmConverter::convert));
        response.setCloudStorage(ifNotNullF(source.getCloudStorage(), cloudStorageConverter::convert));
        response.setValidateBlueprint(false);
        response.setExecutorType(ExecutorType.DEFAULT);
        response.setCustomContainer(null);
        response.setCustomQueue(null);
        return response;
    }

    public DistroXClusterV1Request convert(ClusterV4Request source) {
        DistroXClusterV1Request response = new DistroXClusterV1Request();
        response.setGateway(ifNotNullF(source.getGateway(), gatewayConverter::convert));
        response.setDatabases(source.getDatabases());
        response.setBlueprintName(source.getBlueprintName());
        response.setUserName(source.getUserName());
        response.setPassword(source.getPassword());
        response.setCm(ifNotNullF(source.getCm(), cmConverter::convert));
        response.setCloudStorage(ifNotNullF(source.getCloudStorage(), cloudStorageConverter::convert));
        response.setProxy(source.getProxyConfigCrn());
        return response;
    }

    private String getProxyCrnByName(String proxyName) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        return proxyConfigDtoService.get(proxyName, accountId, userCrn).getName();
    }

    private String getProxyNameByCrn(String proxyCrn) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        //TODO use the dedicated endpoint when name and crn will be break down on the API level
        return proxyConfigDtoService.get(proxyCrn, accountId, userCrn).getCrn();
    }
}
