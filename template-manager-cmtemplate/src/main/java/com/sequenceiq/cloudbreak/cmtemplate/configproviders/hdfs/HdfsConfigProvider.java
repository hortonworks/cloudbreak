package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreCloudStorageServiceConfigProvider.HMS_METASTORE_DIR;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.common.model.FileSystemType;

@Component
public class HdfsConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String CORE_SITE_SAFETY_VALVE = "core_site_safety_valve";

    private static final String HADOOP_HTTP_FILTER_INITIALIZERS_PARAM = "hadoop.http.filter.initializers";

    private static final String HADOOP_HTTP_FILTER_INITIALIZERS_VALUE =
            "org.apache.hadoop.security.HttpCrossOriginFilterInitializer,"
                    + "org.apache.hadoop.security.authentication.server.ProxyUserAuthenticationFilterInitializer";

    private static final String S3GUARD_METADATASTORE_IMPL_PARAM = "fs.s3a.metadatastore.impl";

    private static final String S3GUARD_METADATASTORE_IMPL_VALUE = "org.apache.hadoop.fs.s3a.s3guard.DynamoDBMetadataStore";

    private static final String S3GUARD_TABLE_CREATE_PARAM = "fs.s3a.s3guard.ddb.table.create";

    private static final String S3GUARD_TABLE_CREATE_VALUE = "true";

    private static final String S3GUARD_AUTHORITATIVE_PATH_PARAM = "fs.s3a.authoritative.path";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        StringBuilder hdfsCoreSiteSafetyValveValue = new StringBuilder();

        if (isHadoopHttpFilterInitializationNeeded(templateProcessor, templatePreparationObject)) {
            hdfsCoreSiteSafetyValveValue.append(
                    ConfigUtils.getSafetyValveProperty(HADOOP_HTTP_FILTER_INITIALIZERS_PARAM,
                            HADOOP_HTTP_FILTER_INITIALIZERS_VALUE));
        }

        if (isS3GuardInitializationNeeded(templateProcessor, templatePreparationObject)) {
            hdfsCoreSiteSafetyValveValue.append(ConfigUtils
                    .getSafetyValveProperty(S3GUARD_METADATASTORE_IMPL_PARAM, S3GUARD_METADATASTORE_IMPL_VALUE));
            hdfsCoreSiteSafetyValveValue.append(ConfigUtils
                    .getSafetyValveProperty(S3GUARD_TABLE_CREATE_PARAM, S3GUARD_TABLE_CREATE_VALUE));

            ConfigUtils.getStorageLocationForServiceProperty(templatePreparationObject, HMS_METASTORE_DIR)
                    .ifPresent(location -> hdfsCoreSiteSafetyValveValue.append(
                            ConfigUtils.getSafetyValveProperty(S3GUARD_AUTHORITATIVE_PATH_PARAM, location.getValue())));
        }
        return hdfsCoreSiteSafetyValveValue.toString().isEmpty() ? List.of()
                : List.of(config(CORE_SITE_SAFETY_VALVE, hdfsCoreSiteSafetyValveValue.toString()));
    }

    @Override
    public String getServiceType() {
        return HdfsRoles.HDFS;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HdfsRoles.NAMENODE, HdfsRoles.DATANODE, HdfsRoles.SECONDARYNAMENODE, HdfsRoles.JOURNALNODE);
    }

    protected boolean isHadoopHttpFilterInitializationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices())
                && source.getGatewayView().getExposedServices().getValue().contains(ExposedService.NAMENODE.getKnoxService());
    }

    protected boolean isS3GuardInitializationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().isPresent()
                && source.getFileSystemConfigurationView().get().getType().equals(FileSystemType.S3.name());
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}
