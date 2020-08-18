package com.sequenceiq.cloudbreak.cloud.openstack.heat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils;
import com.sequenceiq.cloudbreak.tag.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;

@Service
public class HeatNetworkTemplateBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeatNetworkTemplateBuilder.class);

    @Value("${cb.openstack.network.template.path:}")
    private String openStackHeatTemplatePath;

    @Inject
    private OpenStackUtils openStackUtil;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private DefaultCostTaggingService defaultCostTaggingService;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    public String build(ModelContext modelContext) {
//        try {
//            List<NovaInstanceView> novaInstances = new OpenStackGroupView(modelContext.stackName, modelContext.groups, modelContext.tags).getFlatNovaView();
//            Map<String, Object> model = new HashMap<>();
//            model.put("cb_stack_name", openStackUtil.adjustStackNameLength(modelContext.stackName));
//            model.put("network", modelContext.neutronNetworkView);
//            model.putAll(defaultCostTaggingService.prepareAllTagsForTemplate());
//            AvailabilityZone az = modelContext.location.getAvailabilityZone();
//            if (az != null && az.value() != null) {
//                model.put("availability_zone", az.value());
//            }
//            Template template = new Template(openStackHeatTemplatePath, modelContext.templateString, freemarkerConfiguration);
//            String generatedTemplate = freeMarkerTemplateUtils.processTemplateIntoString(template, model);
//            LOGGER.debug("Generated Heat template: {}", generatedTemplate);
//            return generatedTemplate;
//        } catch (IOException | TemplateException e) {
//            throw new CloudConnectorException("Failed to process the OpenStack HeatTemplateBuilder", e);
//        }
        return null;
    }

    public Map<String, String> buildParameters(AuthenticatedContext auth) {
        //Map<String, String> parameters = new HashMap<>();

//        KeystoneCredentialView osCredential = new KeystoneCredentialView(auth);
//        NeutronNetworkView neutronView = new NeutronNetworkView(cloudStack.getNetwork());
//        Map<String, String> parameters = new HashMap<>();
//        if (neutronView.isAssignFloatingIp()) {
//            parameters.put("public_net_id", neutronView.getPublicNetId());
//        }
//        parameters.put("image_id", cloudStack.getImage().getImageName());
//        if (cloudStack.getInstanceAuthentication().getPublicKeyId() != null) {
//            parameters.put("key_name", cloudStack.getInstanceAuthentication().getPublicKeyId());
//        } else {
//            parameters.put("key_name", osCredential.getKeyPairName());
//        }
//        if (existingNetwork) {
//            parameters.put("app_net_id", neutronView.getCustomNetworkId());
//            if (isNotEmpty(existingSubnetCidr)) {
//                parameters.put("subnet_id", neutronView.getCustomSubnetId());
//            } else {
//                parameters.put("router_id", neutronView.getCustomRouterId());
//            }
//        }
//        parameters.put("app_net_cidr", isBlank(existingSubnetCidr) ? neutronView.getSubnetCIDR() : existingSubnetCidr);
        return new HashMap<>();
    }

//    private String formatUserData(String userData) {
//        String[] lines = userData.split("\n");
//        StringBuilder sb = new StringBuilder();
//        for (String line : lines) {
//            // be aware of the OpenStack Heat template formatting
//            sb.append("            ").append(line).append('\n');
//        }
//        return sb.toString();
//    }

    public String getTemplate() {
        try {
            return freemarkerConfiguration.getTemplate(openStackHeatTemplatePath, "UTF-8").toString();
        } catch (IOException e) {
            throw new CloudConnectorException("can't get openstack heat template", e);
        }
    }

    public static class ModelContext {
    }

}
