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
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.LdapView;

@Service
public class ClouderaManagerLdapService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerLdapService.class);

    @Value("${cb.cm.admin.role}")
    private String adminRole;

    @Value("${cb.cm.user.role}")
    private String userRole;

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    public void setupLdap(Stack stack, Cluster cluster, HttpClientConfig clientConfig, LdapView ldapView)
            throws ApiException, ClouderaManagerClientInitException {
        if (ldapView != null) {
            String user = cluster.getCloudbreakAmbariUser();
            String password = cluster.getCloudbreakAmbariPassword();
            ApiClient client = clouderaManagerClientFactory.getClient(stack.getGatewayPort(), user, password, clientConfig);

            LOGGER.debug("Setup LDAP on ClouderaManager API for stack: {}", stack.getId());
            ExternalUserMappingsResourceApi externalUserMappingsResourceApi =
                    clouderaManagerClientFactory.getExternalUserMappingsResourceApi(client);
            AuthRolesResourceApi authRolesResourceApi = clouderaManagerClientFactory.getAuthRolesResourceApi(client);
            ApiAuthRoleMetadataList roleMetadataList = authRolesResourceApi.readAuthRolesMetadata(null);
            if (roleMetadataList.getItems() != null) {
                Optional<ApiAuthRoleMetadata> adminMetadata = roleMetadataList.getItems().stream().filter(toRole(adminRole)).findFirst();
                if (adminMetadata.isPresent() && StringUtils.isNotBlank(ldapView.getAdminGroup())) {
                    addGroupMapping(ldapView, externalUserMappingsResourceApi, adminMetadata.get(), ldapView.getAdminGroup());
                } else {
                    LOGGER.info("Cannot setup admin group mapping. Admin metadata present: [{}] Admin group: [{}]",
                            adminMetadata.isPresent(), ldapView.getAdminGroup());
                }
                Optional<ApiAuthRoleMetadata> userMetadata = roleMetadataList.getItems().stream().filter(toRole(userRole)).findFirst();
                if (userMetadata.isPresent() && StringUtils.isNotBlank(ldapView.getUserGroup())) {
                    addGroupMapping(ldapView, externalUserMappingsResourceApi, userMetadata.get(), ldapView.getUserGroup());
                } else {
                    LOGGER.info("Cannot setup user group mapping. User metadata present: [{}] User group: [{}]",
                            userMetadata.isPresent(), ldapView.getUserGroup());
                }
            }
        }
    }

    private Predicate<ApiAuthRoleMetadata> toRole(String role) {
        return rm -> role.equals(rm.getRole());
    }

    private void addGroupMapping(LdapView ldapView, ExternalUserMappingsResourceApi cmApi, ApiAuthRoleMetadata role, String ldapGroup) throws ApiException {
        ApiExternalUserMappingList apiExternalUserMappingList = new ApiExternalUserMappingList()
                .addItemsItem(new ApiExternalUserMapping()
                        .name(ldapGroup)
                        .type(ApiExternalUserMappingType.LDAP)
                        .addAuthRolesItem(new ApiAuthRoleRef()
                                .displayName(role.getDisplayName()).uuid(role.getUuid())));
        cmApi.createExternalUserMappings(apiExternalUserMappingList);
    }
}
