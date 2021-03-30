package com.sequenceiq.cloudbreak.cmtemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.BlueprintUpdater;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplateProcessor;

@Component
public class CentralCmTemplateUpdater implements BlueprintUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralCmTemplateUpdater.class);

    @Inject
    private TemplateProcessor templateProcessor;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private CmTemplateComponentConfigProviderProcessor cmTemplateComponentConfigProviderProcessor;

    @Inject
    private CmTemplateConfigInjectorProcessor cmTemplateConfigInjectorProcessor;

    @Inject
    private CustomConfigurationsInjectorProcessor customConfigurationsInjectorProcessor;

    @Inject
    private CmHostGroupRoleConfigProviderProcessor cmHostGroupRoleConfigProviderProcessor;

    public ApiClusterTemplate getCmTemplate(TemplatePreparationObject source, Map<String, List<Map<String, String>>> hostGroupMappings,
            ClouderaManagerRepo clouderaManagerRepoDetails, List<ClouderaManagerProduct> clouderaManagerProductDetails, String sdxContextName) {
        try {
            CmTemplateProcessor processor = getCmTemplateProcessor(source);
            processor.removeDanglingVariableReferences();
            updateCmTemplateRepoDetails(processor, clouderaManagerRepoDetails, clouderaManagerProductDetails);
            updateCmTemplateConfiguration(processor, clouderaManagerRepoDetails, source, hostGroupMappings, sdxContextName);
            return processor.getTemplate();
        } catch (IOException e) {
            String message = String.format("Unable to update cmTemplate with default properties which was: %s",
                    source.getBlueprintView().getBlueprintText());
            LOGGER.warn(message);
            throw new BlueprintProcessingException(message, e);
        }
    }

    public CmTemplateProcessor getCmTemplateProcessor(TemplatePreparationObject source) throws IOException {
        String cmTemplate = source.getBlueprintView().getBlueprintText();
        cmTemplate = templateProcessor.process(cmTemplate, source, Maps.newHashMap());
        return cmTemplateProcessorFactory.get(cmTemplate);
    }

    @Override
    public String getBlueprintText(TemplatePreparationObject source) {
        ApiClusterTemplate template = getCmTemplate(source, Map.of(), null, null, null);
        return JsonUtil.writeValueAsStringSilent(template);
    }

    @Override
    public String getVariant() {
        return ClusterApi.CLOUDERA_MANAGER;
    }

    private void updateCmTemplateConfiguration(CmTemplateProcessor processor, ClouderaManagerRepo clouderaManagerRepoDetails,
            TemplatePreparationObject source, Map<String, List<Map<String, String>>> hostGroupMappings, String sdxContextName) {
        processor.addInstantiator(clouderaManagerRepoDetails, source, sdxContextName);
        processor.addHosts(hostGroupMappings);
        processor.addDiagnosticTags(source, clouderaManagerRepoDetails);
        cmTemplateComponentConfigProviderProcessor.process(processor, source);
        cmTemplateConfigInjectorProcessor.process(processor, source);
        cmHostGroupRoleConfigProviderProcessor.process(processor, source);
        customConfigurationsInjectorProcessor.process(processor, source);
        processor.setDisplayName(source.getGeneralClusterConfigs().getClusterName());
    }

    private void updateCmTemplateRepoDetails(CmTemplateProcessor cmTemplateProcessor, ClouderaManagerRepo clouderaManagerRepoDetails,
            List<ClouderaManagerProduct> clouderaManagerProductDetails) {
        if (Objects.nonNull(clouderaManagerRepoDetails)) {
            cmTemplateProcessor.setCmVersion(clouderaManagerRepoDetails.getVersion());
        }
        if (Objects.nonNull(clouderaManagerProductDetails) && !clouderaManagerProductDetails.isEmpty()) {
            cmTemplateProcessor.resetProducts();
            cmTemplateProcessor.resetRepositories();
            clouderaManagerProductDetails.forEach(product -> {
                String version = product.getVersion();
                String name = product.getName();
                String parcel = product.getParcel();
                cmTemplateProcessor.addProduct(name, version);
                cmTemplateProcessor.addRepositoryItem(parcel);
                if (Objects.nonNull(name) && StackType.CDH.name().equals(name)) {
                    cmTemplateProcessor.setCdhVersion(parseDistroVersion(version));
                }
            });
        }
    }

    // longVersion example: 6.1.0-1.cdh6.1.0.p0.770702
    // return value: 6.1.0
    private String parseDistroVersion(String longVersion) {
        return longVersion.split("-")[0];
    }
}
