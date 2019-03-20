package com.sequenceiq.cloudbreak.cm;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.AuthRolesResourceApi;
import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ExternalUserMappingsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiAuthRoleMetadata;
import com.cloudera.api.swagger.model.ApiAuthRoleMetadataList;
import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiExternalUserMapping;
import com.cloudera.api.swagger.model.ApiExternalUserMappingList;
import com.cloudera.api.swagger.model.ApiExternalUserMappingType;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.template.views.LdapView;

@Service
public class ClouderaManagerLdapService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerLdapService.class);

    public void setupLdap(ApiClient client, Stack stack, Cluster cluster)  throws ApiException {
        LdapConfig ldapConfig = cluster.getLdapConfig();
        if (ldapConfig != null) {
            LOGGER.debug("Setup LDAP on ClouderaManager API for stack: {}", stack.getId());
            LdapView ldapView = new LdapView(ldapConfig, ldapConfig.getBindDn(), ldapConfig.getBindPassword());
            ClouderaManagerResourceApi clouderaManagerResourceApi = getClouderaManagerResourceApi(client);
            clouderaManagerResourceApi.beginTrial();
            ApiConfigList apiConfigList = new ApiConfigList()
                    .addItemsItem(new ApiConfig().name("auth_backend_order").value("DB_THEN_LDAP"))
                    .addItemsItem(new ApiConfig().name("ldap_url").value(ldapView.getConnectionURL()))
                    .addItemsItem(new ApiConfig().name("ldap_bind_dn").value(ldapView.getBindDn()))
                    .addItemsItem(new ApiConfig().name("ldap_bind_pw").value(ldapView.getBindPassword()))
                    .addItemsItem(new ApiConfig().name("nt_domain").value(ldapView.getDomain()))
                    .addItemsItem(new ApiConfig().name("ldap_user_search_filter").value(ldapView.getUserNameAttribute() + "={0}"))
                    .addItemsItem(new ApiConfig().name("ldap_user_search_base").value(ldapView.getUserSearchBase()))
                    .addItemsItem(new ApiConfig().name("ldap_group_search_filter").value(ldapView.getGroupMemberAttribute() + "={0}"))
                    .addItemsItem(new ApiConfig().name("ldap_group_search_base").value(ldapView.getGroupSearchBase()))
                    .addItemsItem(new ApiConfig().name("ldap_dn_pattern").value(ldapView.getUserDnPattern()));
            if (ldapView.isLdap()) {
                apiConfigList.addItemsItem(new ApiConfig().name("ldap_type").value("LDAP"));
            }
            clouderaManagerResourceApi.updateConfig("Add LDAP configuration", apiConfigList);
            ExternalUserMappingsResourceApi externalUserMappingsResourceApi = getExternalUserMappingsResourceApi(client);
            AuthRolesResourceApi authRolesResourceApi = getAuthRolesResourceApi(client);
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

    ClouderaManagerResourceApi getClouderaManagerResourceApi(ApiClient client) {
        return new ClouderaManagerResourceApi(client);
    }

    ExternalUserMappingsResourceApi getExternalUserMappingsResourceApi(ApiClient client) {
        return new ExternalUserMappingsResourceApi(client);
    }

    AuthRolesResourceApi getAuthRolesResourceApi(ApiClient client) {
        return new AuthRolesResourceApi(client);
    }
}
