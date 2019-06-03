package com.sequenceiq.cloudbreak.cm;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.LdapView;

@Service
public class ClouderaManagerLdapService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerLdapService.class);

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    public void setupLdap(Stack stack, Cluster cluster, HttpClientConfig clientConfig, LdapView ldapView) throws ApiException {
        if (ldapView != null) {
            ApiClient client = clouderaManagerClientFactory.getClient(stack, cluster, clientConfig);

            LOGGER.debug("Setup LDAP on ClouderaManager API for stack: {}", stack.getId());
            ExternalUserMappingsResourceApi externalUserMappingsResourceApi =
                    clouderaManagerClientFactory.getExternalUserMappingsResourceApi(client);
            AuthRolesResourceApi authRolesResourceApi = clouderaManagerClientFactory.getAuthRolesResourceApi(client);
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
