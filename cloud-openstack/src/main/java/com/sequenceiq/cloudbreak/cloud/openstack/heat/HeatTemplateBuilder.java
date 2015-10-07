package com.sequenceiq.cloudbreak.cloud.openstack.heat;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_MAX_OPENSTACK_RESOURCE_NAME_LENGTH;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_OPENSTACK_HEAT_TEMPLATE_PATH;
import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.cloudbreak.cloud.openstack.view.NeutronNetworkView;
import com.sequenceiq.cloudbreak.cloud.openstack.view.NovaInstanceView;
import com.sequenceiq.cloudbreak.cloud.openstack.view.OpenStackGroupView;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Service
public class HeatTemplateBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeatTemplateBuilder.class);

    @Value("${cb.openstack.heat.template.path:" + CB_OPENSTACK_HEAT_TEMPLATE_PATH + "}")
    private String openStackHeatTemplatePath;
    @Value("${cb.max.openstack.resource.name.length:" + CB_MAX_OPENSTACK_RESOURCE_NAME_LENGTH + "}")
    private int maxResourceNameLength;


    @Inject
    private OpenStackUtils openStackUtil;
    @Inject
    private Configuration freemarkerConfiguration;

    public String build(String stackName, List<Group> groups, Security security, Image instanceUserData) {
        try {
            List<NovaInstanceView> novaInstances = new OpenStackGroupView(groups).getFlatNovaView();
            Map<String, Object> model = new HashMap<>();
            model.put("cb_stack_name", new String(Splitter.fixedLength(maxResourceNameLength).splitToList(stackName).get(0)));
            model.put("agents", novaInstances);
            model.put("core_user_data", formatUserData(instanceUserData.getUserData(InstanceGroupType.CORE)));
            model.put("gateway_user_data", formatUserData(instanceUserData.getUserData(InstanceGroupType.GATEWAY)));
            model.put("rules", security.getRules());
            String generatedTemplate = processTemplateIntoString(freemarkerConfiguration.getTemplate(openStackHeatTemplatePath, "UTF-8"), model);
            LOGGER.debug("Generated Heat template: {}", generatedTemplate);
            return generatedTemplate;
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the OpenStack HeatTemplateBuilder", e);
        }
    }

    public Map<String, String> buildParameters(AuthenticatedContext auth, Network network, Image image) {
        KeystoneCredentialView osCredential = new KeystoneCredentialView(auth);
        NeutronNetworkView neutronView = new NeutronNetworkView(network);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("public_net_id", neutronView.getPublicNetId());
        parameters.put("image_id", image.getImageName());
        parameters.put("key_name", osCredential.getKeyPairName());
        parameters.put("app_net_cidr", neutronView.getSubnetCIDR());
        return parameters;
    }

    private String formatUserData(String userData) {
        String[] lines = userData.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            // be aware of the OpenStack Heat template formatting
            sb.append("            " + lines[i] + "\n");
        }
        return sb.toString();
    }

}