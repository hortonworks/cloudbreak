package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.EnvironmentNames;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.KerberosConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosViewV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(KerberosConfig.class)
public class KerberosConfigV4Controller extends NotificationController implements KerberosConfigV4Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosConfigV4Controller.class);

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public KerberosViewV4Responses list(Long workspaceId, String environment, Boolean attachGlobal) {
//        Set<KerberosConfig> allInWorkspaceAndEnvironment = kerberosConfigService.findAllByWorkspaceId(workspaceId);
//        return new KerberosViewV4Responses(converterUtil.convertAllAsSet(allInWorkspaceAndEnvironment, KerberosViewV4Response.class));
        return new KerberosViewV4Responses();
    }

    @Override
    public KerberosV4Response get(Long workspaceId, String name) {
//        KerberosConfig kerberosConfig = kerberosConfigService.getByNameForWorkspaceId(name, workspaceId);
//        return converterUtil.convert(kerberosConfig, KerberosV4Response.class);
        return null;
    }

    @Override
    public KerberosV4Response create(Long workspaceId, @Valid KerberosV4Request request) {
//        KerberosConfig newKerberosConfig = converterUtil.convert(request, KerberosConfig.class);
//        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
//        KerberosConfig createdKerberosConfig = kerberosConfigService.create(newKerberosConfig, workspaceId, user);
//        return converterUtil.convert(createdKerberosConfig, KerberosV4Response.class);
        return null;
    }

    @Override
    public KerberosV4Response delete(Long workspaceId, String name) {
//        KerberosConfig deleted = kerberosConfigService.deleteByNameFromWorkspace(name, workspaceId);
//        return converterUtil.convert(deleted, KerberosV4Response.class);
        return null;
    }

    @Override
    public KerberosV4Responses deleteMultiple(Long workspaceId, Set<String> names) {
//        Set<KerberosConfig> deleted = kerberosConfigService.deleteMultipleByNameFromWorkspace(names, workspaceId);
//        return new KerberosV4Responses(converterUtil.convertAllAsSet(deleted, KerberosV4Response.class));
        return null;
    }

    @Override
    public KerberosV4Response attach(Long workspaceId, String name, EnvironmentNames environmentNames) {
        throw new UnsupportedOperationException("Attaching Kerberos config to environment is not supported anymore!");
    }

    @Override
    public KerberosV4Response detach(Long workspaceId, String name, EnvironmentNames environmentNames) {
        throw new UnsupportedOperationException("Detaching Kerberos config from environment is not supported anymore!");
    }

    @Override
    public KerberosV4Request getRequest(Long workspaceId, String name) {
//        KerberosConfig kerberosConfig = kerberosConfigService.getByNameForWorkspaceId(name, workspaceId);
//        return converterUtil.convert(kerberosConfig, KerberosV4Request.class);
        return null;
    }

}
