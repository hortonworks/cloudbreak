package com.sequenceiq.cloudbreak.cm;

import java.util.Optional;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.AuthRolesResourceApi;
import com.cloudera.api.swagger.ExternalUserMappingsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiAuthRoleMetadata;
import com.cloudera.api.swagger.model.ApiAuthRoleMetadataList;
import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiExternalUserMapping;
import com.cloudera.api.swagger.model.ApiExternalUserMappingList;
import com.cloudera.api.swagger.model.ApiExternalUserMappingType;
import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class ClouderaManagerLdapService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerLdapService.class);

    @Value("${cb.cm.admin.role}")
    private String adminRole;

    @Value("${cb.cm.limited.admin.role}")
    private String limitedAdminRole;

    @Value("${cb.cm.dashboard.user.role}")
    private String dashboardUserRole;

    @Value("${cb.cm.user.role}")
    private String userRole;

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private VirtualGroupService virtualGroupService;

    public void setupLdap(StackView stack, ClusterView cluster, HttpClientConfig clientConfig, LdapView ldapView, VirtualGroupRequest virtualGroupRequest)
            throws ApiException, ClouderaManagerClientInitException {
        if (ldapView != null) {
            String user = cluster.getCloudbreakAmbariUser();
            String password = cluster.getCloudbreakAmbariPassword();
            ApiClient client = clouderaManagerApiClientProvider.getV31Client(stack.getGatewayPort(), user, password, clientConfig);

            LOGGER.debug("Setup LDAP on ClouderaManager API for stack: {}", stack.getId());
            ExternalUserMappingsResourceApi externalUserMappingsResourceApi =
                    clouderaManagerApiFactory.getExternalUserMappingsResourceApi(client);
            AuthRolesResourceApi authRolesResourceApi = clouderaManagerApiFactory.getAuthRolesResourceApi(client);
            ApiAuthRoleMetadataList roleMetadataList = authRolesResourceApi.readAuthRolesMetadata(null);
            if (roleMetadataList.getItems() != null) {
                Optional<ApiAuthRoleMetadata> dashboardUserRoleOpt = findRole(roleMetadataList, dashboardUserRole);
                Optional<ApiAuthRoleMetadata> limitedAdminRoleOpt = findRole(roleMetadataList, limitedAdminRole);
                Optional<ApiAuthRoleMetadata> role = limitedAdminRoleOpt.isPresent() ? limitedAdminRoleOpt : findRole(roleMetadataList, adminRole);
                if (role.isPresent()) {
                    String virtualGroup = virtualGroupService
                            .createOrGetVirtualGroup(virtualGroupRequest, UmsVirtualGroupRight.CLOUDER_MANAGER_ADMIN);
                    addGroupMapping(externalUserMappingsResourceApi, role.get(), dashboardUserRoleOpt, virtualGroup);
                } else {
                    LOGGER.info("Cannot setup admin group mapping. Admin roles ({}, {}) are not found", adminRole, limitedAdminRole);
                }
                Optional<ApiAuthRoleMetadata> userMetadata = roleMetadataList.getItems().stream().filter(toRole(userRole)).findFirst();
                if (userMetadata.isPresent() && StringUtils.isNotBlank(ldapView.getUserGroup())) {
                    addGroupMapping(externalUserMappingsResourceApi, userMetadata.get(), dashboardUserRoleOpt, ldapView.getUserGroup());
                } else {
                    LOGGER.info("Cannot setup user group mapping. User metadata present: [{}] User group: [{}]",
                            userMetadata.isPresent(), ldapView.getUserGroup());
                }
            }
        }
    }

    private Optional<ApiAuthRoleMetadata> findRole(ApiAuthRoleMetadataList apiAuthRoleMetadataList, String role) {
        return apiAuthRoleMetadataList.getItems().stream().filter(toRole(role)).findFirst();
    }

    private Predicate<ApiAuthRoleMetadata> toRole(String role) {
        return rm -> role.equals(rm.getRole());
    }

    private void addGroupMapping(ExternalUserMappingsResourceApi cmApi, ApiAuthRoleMetadata adminRole,
            Optional<ApiAuthRoleMetadata> dashboardUserRoleOpt, String ldapGroup) throws ApiException {
        LOGGER.info("Associating virtual group '{}' to CM role '{}'", ldapGroup, adminRole.getDisplayName());
        ApiExternalUserMappingList apiExternalUserMappingList = new ApiExternalUserMappingList()
                .addItemsItem(new ApiExternalUserMapping()
                        .name(ldapGroup)
                        .type(ApiExternalUserMappingType.LDAP)
                        .addAuthRolesItem(new ApiAuthRoleRef().displayName(adminRole.getDisplayName()).uuid(adminRole.getUuid()))
                );
        if (dashboardUserRoleOpt.isPresent()) {
            ApiAuthRoleMetadata dashboardUserRole = dashboardUserRoleOpt.get();
            LOGGER.info("Associating virtual group '{}' to CM role '{}'", ldapGroup, dashboardUserRole.getDisplayName());
            apiExternalUserMappingList.getItems()
                    .get(0).addAuthRolesItem(new ApiAuthRoleRef().displayName(dashboardUserRole.getDisplayName()).uuid(dashboardUserRole.getUuid()));
        }
        cmApi.createExternalUserMappings(apiExternalUserMappingList);
    }
}
