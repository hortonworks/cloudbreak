package com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles.IDBROKER;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.AccountMappingView;
import com.sequenceiq.common.model.FileSystemType;

@Component
public class KnoxIdBrokerConfigProvider extends AbstractRoleConfigProvider implements BaseKnoxConfigProvider {

    private static final Map<String, CloudPlatform> FILE_SYSTEM_TYPE_TO_CLOUD_PLATFORM_MAP = Map.ofEntries(
            Map.entry(FileSystemType.S3.name(), CloudPlatform.AWS),
            Map.entry(FileSystemType.ADLS.name(), CloudPlatform.AZURE),
            Map.entry(FileSystemType.ADLS_GEN_2.name(), CloudPlatform.AZURE),
            Map.entry(FileSystemType.WASB.name(), CloudPlatform.AZURE),
            Map.entry(FileSystemType.WASB_INTEGRATED.name(), CloudPlatform.AZURE),
            Map.entry(FileSystemType.GCS.name(), CloudPlatform.GCP)
    );

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        switch (roleType) {
            case IDBROKER:
                List<ApiClusterTemplateConfig> config;
                AccountMappingView accountMappingView = source.getAccountMappingView() == null
                        ? AccountMappingView.EMPTY_MAPPING : source.getAccountMappingView();
                String userMappings = getAccountMappingSetting(accountMappingView.getUserMappings());
                String groupMappings = getAccountMappingSetting(accountMappingView.getGroupMappings());
                switch (getCloudPlatform(source)) {
                    case AWS:
                        config = List.of(config("idbroker_aws_user_mapping", userMappings),
                                config("idbroker_aws_group_mapping", groupMappings));
                        break;
                    case AZURE:
                        config = new ArrayList<>(List.of(config("idbroker_azure_user_mapping", userMappings),
                                        config("idbroker_azure_group_mapping", groupMappings)));
                        ClouderaManagerRepo cmRepo = source.getProductDetailsView().getCm();
                        if (CMRepositoryVersionUtil.isIdBrokerManagedIdentitySupported(cmRepo)) {
                            String idBrokerManagedIdentity = getIdBrokerManagedIdentity(source.getFileSystemConfigurationView());
                            if (Objects.nonNull(idBrokerManagedIdentity)) {
                                config.add(config("idbroker_azure_vm_assumer_identity", idBrokerManagedIdentity));
                            }
                        }
                        break;
                    case GCP:
                        config = List.of(config("idbroker_gcp_user_mapping", userMappings),
                                config("idbroker_gcp_group_mapping", groupMappings));
                        break;
                    default:
                        config = List.of();
                        break;
                }
                return config;
            default:
                return List.of();
        }
    }

    private String getAccountMappingSetting(Map<String, String> accountMappings) {
        return accountMappings.entrySet().stream()
                .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                .collect(Collectors.joining(";"));
    }

    private CloudPlatform getCloudPlatform(TemplatePreparationObject source) {
        CloudPlatform cloudPlatform;
        if (source.getFileSystemConfigurationView().isPresent()) {
            String fileSystemType = source.getFileSystemConfigurationView().get().getType();
            cloudPlatform = FILE_SYSTEM_TYPE_TO_CLOUD_PLATFORM_MAP.get(fileSystemType);
            if (cloudPlatform == null) {
                throw new IllegalStateException("Unknown file system type: " + fileSystemType);
            }
        } else {
            cloudPlatform = source.getCloudPlatform();
        }
        return cloudPlatform;
    }

    private String getIdBrokerManagedIdentity(Optional<BaseFileSystemConfigurationsView> fileSystemConfigurationsView) {
        return fileSystemConfigurationsView.map(BaseFileSystemConfigurationsView::getIdBrokerIdentityId).orElse(null);
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(IDBROKER);
    }

}
