package com.sequenceiq.cloudbreak.cloud.openstack.heat;

import static com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils.processTemplateIntoString;
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

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.cloudbreak.cloud.openstack.view.NeutronNetworkView;
import com.sequenceiq.cloudbreak.cloud.openstack.view.NovaInstanceView;
import com.sequenceiq.cloudbreak.cloud.openstack.view.OpenStackGroupView;

import freemarker.template.Configuration;
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

    public String build(Location location, String stackName, List<Group> groups, Image instanceUserData, boolean existingNetwork,
            boolean existingSubnet, NeutronNetworkView neutronNetworkView) {
        try {
            List<NovaInstanceView> novaInstances = new OpenStackGroupView(stackName, groups).getFlatNovaView();
            Map<String, Object> model = new HashMap<>();
            model.put("cb_stack_name", openStackUtil.adjustStackNameLength(stackName));
            model.put("agents", novaInstances);
            model.put("core_user_data", formatUserData(instanceUserData.getUserData(InstanceGroupType.CORE)));
            model.put("gateway_user_data", formatUserData(instanceUserData.getUserData(InstanceGroupType.GATEWAY)));
            model.put("groups", groups);
            model.put("existingNetwork", existingNetwork);
            model.put("existingSubnet", existingSubnet);
            model.put("network", neutronNetworkView);
            AvailabilityZone az = location.getAvailabilityZone();
            if (az != null && az.value() != null) {
                model.put("availability_zone", az.value());
            }
            String generatedTemplate = processTemplateIntoString(freemarkerConfiguration.getTemplate(openStackHeatTemplatePath, "UTF-8"), model);
            LOGGER.debug("Generated Heat template: {}", generatedTemplate);
            return generatedTemplate;
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the OpenStack HeatTemplateBuilder", e);
        }
    }

    public Map<String, String> buildParameters(AuthenticatedContext auth, Network network, Image image, boolean existingNetwork, String existingSubnetCidr) {
        KeystoneCredentialView osCredential = new KeystoneCredentialView(auth);
        NeutronNetworkView neutronView = new NeutronNetworkView(network);
        Map<String, String> parameters = new HashMap<>();
        if (neutronView.isAssignFloatingIp()) {
            parameters.put("public_net_id", neutronView.getPublicNetId());
        }
        parameters.put("image_id", image.getImageName());
        parameters.put("key_name", osCredential.getKeyPairName());
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
            sb.append("            ").append(line).append("\n");
        }
        return sb.toString();
    }

}