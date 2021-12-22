package com.sequenceiq.cloudbreak.reactor.handler.cluster.dr;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;

@Service
public class RangerVirtualGroupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RangerVirtualGroupService.class);

    @Inject
    private VirtualGroupService virtualGroupService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private EnvironmentConfigProvider environmentConfigProvider;

    public String getRangerVirtualGroup(Stack stack) {
        if (CloudPlatform.MOCK.equalsIgnoreCase(stack.getCloudPlatform())) {
            return "mockGroup";
        }
        Optional<LdapView> ldapView = ldapConfigService.get(stack.getEnvironmentCrn(), stack.getName());
        String virtualGroupsEnvironmentCrn = environmentConfigProvider.getParentEnvironmentCrn(stack.getEnvironmentCrn());
        String adminGroup = ldapView.orElseThrow(() -> new CloudbreakServiceException("Ranger admin group not found."))
                .getAdminGroup();

        LOGGER.debug("Admin Group:", adminGroup);
        VirtualGroupRequest virtualGroupRequest = new VirtualGroupRequest(virtualGroupsEnvironmentCrn, adminGroup);
        return virtualGroupService.getVirtualGroup(virtualGroupRequest, UmsRight.RANGER_ADMIN.getRight());
    }
}
