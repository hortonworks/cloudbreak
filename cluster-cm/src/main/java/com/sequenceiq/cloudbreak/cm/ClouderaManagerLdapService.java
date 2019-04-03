package com.sequenceiq.cloudbreak.cm;

import java.util.Optional;

import javax.inject.Inject;

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
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiExternalUserMapping;
import com.cloudera.api.swagger.model.ApiExternalUserMappingList;
import com.cloudera.api.swagger.model.ApiExternalUserMappingType;
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

    public void setupLdap(Stack stack, Cluster cluster, HttpClientConfig clientConfig) throws ApiException {
        LdapConfig ldapConfig = cluster.getLdapConfig();
        ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerClientFactory.getClouderaManagerResourceApi(stack, cluster, clientConfig);
        clouderaManagerResourceApi.beginTrial();
        if (ldapConfig != null) {
            LOGGER.debug("Setup LDAP on ClouderaManager API for stack: {}", stack.getId());
            LdapView ldapView = new LdapView(ldapConfig, ldapConfig.getBindDn(), ldapConfig.getBindPassword());
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
