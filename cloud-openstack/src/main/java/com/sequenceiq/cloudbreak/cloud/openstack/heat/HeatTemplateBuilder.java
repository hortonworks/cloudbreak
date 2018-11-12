package com.sequenceiq.cloudbreak.cloud.openstack.heat;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.cloudbreak.cloud.openstack.view.NeutronNetworkView;
import com.sequenceiq.cloudbreak.cloud.openstack.view.NovaInstanceView;
import com.sequenceiq.cloudbreak.cloud.openstack.view.OpenStackGroupView;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Service
public class HeatTemplateBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeatTemplateBuilder.class);

    @Value("${cb.openstack.heat.template.path:}")
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
        try {
            List<NovaInstanceView> novaInstances = new OpenStackGroupView(modelContext.stackName, modelContext.groups, modelContext.tags).getFlatNovaView();
            Map<String, Object> model = new HashMap<>();
            model.put("cb_stack_name", openStackUtil.adjustStackNameLength(modelContext.stackName));
            model.put("agents", novaInstances);
            model.put("core_user_data", formatUserData(modelContext.instanceUserData.getUserDataByType(InstanceGroupType.CORE)));
            model.put("gateway_user_data", formatUserData(modelContext.instanceUserData.getUserDataByType(InstanceGroupType.GATEWAY)));
            model.put("groups", modelContext.groups);
            model.put("existingNetwork", modelContext.existingNetwork);
            model.put("existingSubnet", modelContext.existingSubnet);
            model.put("network", modelContext.neutronNetworkView);
            model.putAll(defaultCostTaggingService.prepareAllTagsForTemplate());
            AvailabilityZone az = modelContext.location.getAvailabilityZone();
            if (az != null && az.value() != null) {
                model.put("availability_zone", az.value());
            }
            Template template = new Template(openStackHeatTemplatePath, modelContext.templateString, freemarkerConfiguration);
            String generatedTemplate = freeMarkerTemplateUtils.processTemplateIntoString(template, model);
            LOGGER.debug("Generated Heat template: {}", generatedTemplate);
            return generatedTemplate;
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the OpenStack HeatTemplateBuilder", e);
        }
    }

    public Map<String, String> buildParameters(AuthenticatedContext auth, CloudStack cloudStack, boolean existingNetwork, String existingSubnetCidr) {
        KeystoneCredentialView osCredential = new KeystoneCredentialView(auth);
        NeutronNetworkView neutronView = new NeutronNetworkView(cloudStack.getNetwork());
        Map<String, String> parameters = new HashMap<>();
        if (neutronView.isAssignFloatingIp()) {
            parameters.put("public_net_id", neutronView.getPublicNetId());
        }
        parameters.put("image_id", cloudStack.getImage().getImageName());
        if (cloudStack.getInstanceAuthentication().getPublicKeyId() != null) {
            parameters.put("key_name", cloudStack.getInstanceAuthentication().getPublicKeyId());
        } else {
            parameters.put("key_name", osCredential.getKeyPairName());
        }
        if (existingNetwork) {
            parameters.put("app_net_id", neutronView.getCustomNetworkId());
            if (isNoneEmpty(existingSubnetCidr)) {
                parameters.put("subnet_id", neutronView.getCustomSubnetId());
            } else {
                parameters.put("router_id", neutronView.getCustomRouterId());
            }
        }
        parameters.put("app_net_cidr", isBlank(existingSubnetCidr) ? neutronView.getSubnetCIDR() : existingSubnetCidr);
        return parameters;
    }

    private String formatUserData(String userData) {
        String[] lines = userData.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            // be aware of the OpenStack Heat template formatting
            sb.append("            ").append(line).append('\n');
        }
        return sb.toString();
    }

    public String getTemplate() {
        try {
            return freemarkerConfiguration.getTemplate(openStackHeatTemplatePath, "UTF-8").toString();
        } catch (IOException e) {
            throw new CloudConnectorException("can't get openstack heat template", e);
        }
    }

    public static class ModelContext {

        private Location location;

        private String stackName;

        private List<Group> groups;

        private Image instanceUserData;

        private boolean existingNetwork;

        private boolean existingSubnet;

        private NeutronNetworkView neutronNetworkView;

        private String templateString;

        private Map<String, String> tags = new HashMap<>();

        public ModelContext withLocation(Location location) {
            this.location = location;
            return this;
        }

        public ModelContext withStackName(String stackName) {
            this.stackName = stackName;
            return this;
        }

        public ModelContext withGroups(List<Group> groups) {
            this.groups = groups;
            return this;
        }

        public ModelContext withInstanceUserData(Image instanceUserData) {
            this.instanceUserData = instanceUserData;
            return this;
        }

        public ModelContext withExistingNetwork(boolean existingNetwork) {
            this.existingNetwork = existingNetwork;
            return this;
        }

        public ModelContext withExistingSubnet(boolean existingSubnet) {
            this.existingSubnet = existingSubnet;
            return this;
        }

        public ModelContext withNeutronNetworkView(NeutronNetworkView neutronNetworkView) {
            this.neutronNetworkView = neutronNetworkView;
            return this;
        }

        public ModelContext withTemplateString(String templateString) {
            this.templateString = templateString;
            return this;
        }

        public ModelContext withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }
    }

}