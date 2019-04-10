package com.sequenceiq.cloudbreak.cm;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.AuthRolesResourceApi;
import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ExternalUserMappingsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiAuthRoleMetadata;
import com.cloudera.api.swagger.model.ApiAuthRoleMetadataList;
import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiExternalUserMapping;
import com.cloudera.api.swagger.model.ApiExternalUserMappingList;
import com.cloudera.api.swagger.model.ApiExternalUserMappingType;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.template.views.LdapView;

@Service
public class ClouderaManagerLdapService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerLdapService.class);

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Inject
    private GrpcUmsClient umsClient;

    public void setupLdap(Stack stack, Cluster cluster, HttpClientConfig clientConfig) throws ApiException {
        LdapConfig ldapConfig = cluster.getLdapConfig();
        ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerClientFactory.getClouderaManagerResourceApi(stack, cluster, clientConfig);

        // Begin the Cloudera Manager trial only if UMS is not enabled. Otherwise, we'll be using a
        // license from UMS.
        String userCrn = stack.getCreator().getUserCrn();
        if (!umsClient.isUmsUsable(userCrn) || StringUtils.isEmpty(
                umsClient.getAccountDetails(userCrn, userCrn, Optional.empty()).getClouderaManagerLicenseKey())) {
            LOGGER.info("Enabling trial license.");
            clouderaManagerResourceApi.beginTrial();
        } else {
            LOGGER.info("UMS detected and license key available, skipping trial license.");
        }

        if (ldapConfig != null) {
            LOGGER.debug("Setup LDAP on ClouderaManager API for stack: {}", stack.getId());
            LdapView ldapView = new LdapView(ldapConfig, ldapConfig.getBindDn(), ldapConfig.getBindPassword());
            ExternalUserMappingsResourceApi externalUserMappingsResourceApi =
                    clouderaManagerClientFactory.getExternalUserMappingsResourceApi(stack, cluster, clientConfig);
            AuthRolesResourceApi authRolesResourceApi = clouderaManagerClientFactory.getAuthRolesResourceApi(stack, cluster, clientConfig);
            ApiAuthRoleMetadataList roleMetadataList = authRolesResourceApi.readAuthRolesMetadata(null);
            if (roleMetadataList.getItems() != null) {
                Optional<ApiAuthRoleMetadata> roleMetadata = roleMetadataList.getItems().stream().filter(rm -> rm.getRole().equals("ROLE_ADMIN")).findFirst();
                if (roleMetadata.isPresent()) {
                    ApiAuthRoleMetadata role = roleMetadata.get();
                    ApiExternalUserMappingList apiExternalUserMappingList = new ApiExternalUserMappingList()
                            .addItemsItem(new ApiExternalUserMapping()
                                    .name(ldapView.getAdminGroup())
                                    .type(ApiExternalUserMappingType.LDAP)
                                    .addAuthRolesItem(new ApiAuthRoleRef()
                                            .displayName(role.getDisplayName()).uuid(role.getUuid())));
                    externalUserMappingsResourceApi.createExternalUserMappings(apiExternalUserMappingList);
                }
            }
        }
    }
}
