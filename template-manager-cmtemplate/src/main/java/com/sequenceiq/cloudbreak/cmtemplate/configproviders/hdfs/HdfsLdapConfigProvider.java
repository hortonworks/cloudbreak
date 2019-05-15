package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.LdapView;

@Component
public class HdfsLdapConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String HDFS_HADOOP_GROUP_MAPPING_LDAP_URL = "hdfs-hadoop_group_mapping_ldap_url";

    private static final String HDFS_HADOOP_GROUP_MAPPING_LDAP_BIND_USER = "hdfs-hadoop_group_mapping_ldap_bind_user";

    private static final String HDFS_HADOOP_GROUP_MAPPING_LDAP_BIND_PASSWD = "hdfs-hadoop_group_mapping_ldap_bind_passwd";

    private static final String HDFS_HADOOP_GROUP_MAPPING_LDAP_USER_FILTER = "hdfs-hadoop_group_mapping_ldap_user_filter";

    private static final String HDFS_HADOOP_GROUP_MAPPING_LDAP_GROUP_FILTER = "hdfs-hadoop_group_mapping_ldap_group_filter";

    private static final String HDFS_HADOOP_GROUP_MAPPING_LDAP_GROUP_NAME_ATTR = "hdfs-hadoop_group_mapping_ldap_group_name_attr";

    private static final String HDFS_HADOOP_GROUP_MAPPING_LDAP_MEMBER_ATTR = "hdfs-hadoop_group_mapping_ldap_member_attr";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        result.add(new ApiClusterTemplateConfig().name("hadoop_security_group_mapping").value("org.apache.hadoop.security.LdapGroupsMapping"));
        result.add(new ApiClusterTemplateConfig().name("hadoop_group_mapping_ldap_url").variable(HDFS_HADOOP_GROUP_MAPPING_LDAP_URL));
        result.add(new ApiClusterTemplateConfig().name("hadoop_group_mapping_ldap_bind_user").variable(HDFS_HADOOP_GROUP_MAPPING_LDAP_BIND_USER));
        result.add(new ApiClusterTemplateConfig().name("hadoop_group_mapping_ldap_bind_passwd").variable(HDFS_HADOOP_GROUP_MAPPING_LDAP_BIND_PASSWD));
        // TODO Add core-site / hadoop.security.group.mapping.ldap.userbase cfg
        result.add(new ApiClusterTemplateConfig().name("hadoop_group_mapping_ldap_user_filter").variable(HDFS_HADOOP_GROUP_MAPPING_LDAP_USER_FILTER));
        // TODO Add core-site / hadoop.security.group.mapping.ldap.groupbase cfg
        result.add(new ApiClusterTemplateConfig().name("hadoop_group_mapping_ldap_group_filter").variable(HDFS_HADOOP_GROUP_MAPPING_LDAP_GROUP_FILTER));
        result.add(new ApiClusterTemplateConfig().name("hadoop_group_mapping_ldap_group_name_attr").variable(HDFS_HADOOP_GROUP_MAPPING_LDAP_GROUP_NAME_ATTR));
        result.add(new ApiClusterTemplateConfig().name("hadoop_group_mapping_ldap_member_attr").variable(HDFS_HADOOP_GROUP_MAPPING_LDAP_MEMBER_ATTR));
        return result;
    }

    @Override
    public List<ApiClusterTemplateVariable> getServiceConfigVariables(TemplatePreparationObject source) {
        List<ApiClusterTemplateVariable> result = new ArrayList<>();
        LdapView ldapView = source.getLdapConfig().get();
        result.add(new ApiClusterTemplateVariable().name(HDFS_HADOOP_GROUP_MAPPING_LDAP_URL).value(ldapView.getConnectionURL()));
        result.add(new ApiClusterTemplateVariable().name(HDFS_HADOOP_GROUP_MAPPING_LDAP_BIND_USER).value(ldapView.getBindDn()));
        result.add(new ApiClusterTemplateVariable().name(HDFS_HADOOP_GROUP_MAPPING_LDAP_BIND_PASSWD).value(ldapView.getBindPassword()));
        // TODO Add core-site / hadoop.security.group.mapping.ldap.userbase var: ldap.userSearchBase
        result.add(new ApiClusterTemplateVariable().name(HDFS_HADOOP_GROUP_MAPPING_LDAP_USER_FILTER).value(
                String.format("(&(objectClass=%s)(%s={0}))", ldapView.getUserObjectClass(), ldapView.getUserNameAttribute())));
        // TODO Add core-site / hadoop.security.group.mapping.ldap.groupbase var: ldap.groupSearchBase
        result.add(new ApiClusterTemplateVariable().name(HDFS_HADOOP_GROUP_MAPPING_LDAP_GROUP_FILTER).value(
                String.format("(objectClass=%s)", ldapView.getGroupObjectClass())));
        result.add(new ApiClusterTemplateVariable().name(HDFS_HADOOP_GROUP_MAPPING_LDAP_GROUP_NAME_ATTR).value(ldapView.getGroupNameAttribute()));
        result.add(new ApiClusterTemplateVariable().name(HDFS_HADOOP_GROUP_MAPPING_LDAP_MEMBER_ATTR).value(ldapView.getGroupMemberAttribute()));
        return result;
    }

    @Override
    public String getServiceType() {
        return "HDFS";
    }

    @Override
    public List<String> getRoleTypes() {
        return Arrays.asList("NAMENODE", "DATANODE", "SECONDARYNAMENODE");
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return isNotHdfStack(cmTemplateProcessor) && source.getKerberosConfig().isEmpty() && source.getLdapConfig().isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    private boolean isNotHdfStack(CmTemplateProcessor cmTemplateProcessor) {
        return cmTemplateProcessor.getTemplate().getProducts().stream().filter(apv -> StackType.HDF.name().equals(apv.getProduct())).findFirst().isEmpty();
    }

}
