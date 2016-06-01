package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;

@Component
public class SmartSenseConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmartSenseConfigProvider.class);
    private static final String SMART_SENSE_SERVER_CONFIG_FILE = "hst-server-conf";
    private static final String HST_SERVER_COMPONENT = "HST_SERVER";
    private static final int FIRST_PART_LENGTH = 4;

    @Value("${cb.smartsense.configure:false}")
    private boolean configureSmartSense;

    @Value("${cb.smartsense.id.pattern:}")
    private String smartSenseIdPattern;

    @Inject
    private BlueprintProcessor blueprintProcessor;

    public boolean smartSenseIsConfigurable(String blueprint) {
        return configureSmartSense && blueprintProcessor.componentExistsInBlueprint(HST_SERVER_COMPONENT, blueprint);
    }

    public List<BlueprintConfigurationEntry> getConfigs(Stack stack, String blueprintText) {
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        if (smartSenseIsConfigurable(blueprintText)) {
            Credential credential = stack.getCredential();
            configs.addAll(getSmartSenseConfigForAws(credential));
            configs.addAll(getSmartSenseGatewayConfigs(stack));
        }
        return configs;
    }

    private List<BlueprintConfigurationEntry> getSmartSenseConfigForAws(Credential credential) {
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        Map<String, Object> params = credential.getAttributes().getMap();
        String roleArn = String.valueOf(params.get("roleArn"));
        String accessKey = String.valueOf(params.get("accessKey"));
        String secretKey = String.valueOf(params.get("secretKey"));
        String smartSenseId = getSmartSenseId(roleArn, accessKey, secretKey);
        if (StringUtils.isNoneEmpty(smartSenseId)) {
            configs.add(new BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "customer.smartsense.id", smartSenseId));
        }
        return configs;
    }

    private String getSmartSenseId(String roleArn, String accessKey, String secretKey) {
        String smartSenseId = "";
        try {
            if (StringUtils.isNoneEmpty(roleArn)) {
                smartSenseId = getSmartSenseIdFromArn(roleArn);
            } else if (StringUtils.isNoneEmpty(accessKey) && StringUtils.isNoneEmpty(secretKey)) {
                try {
                    AmazonIdentityManagementClient iamClient = new AmazonIdentityManagementClient(new BasicAWSCredentials(accessKey, secretKey));
                    String arn = iamClient.getUser().getUser().getArn();
                    smartSenseId = getSmartSenseIdFromArn(arn);
                } catch (Exception e) {
                    LOGGER.error("Could not get ARN of IAM user from AWS.", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not get SmartSense Id from AWS credential.", e);
        }
        return smartSenseId;
    }

    private String getSmartSenseIdFromArn(String roleArn) {
        String smartSenseId = "";
        Matcher m = Pattern.compile("arn:aws:iam::(?<accountId>[0-9]{12}):.*").matcher(roleArn);
        if (m.matches()) {
            String accountId = m.group("accountId");
            Integer firstPart = Integer.valueOf(accountId.substring(0, FIRST_PART_LENGTH));
            Integer secondPart = Integer.valueOf(accountId.substring(FIRST_PART_LENGTH));
            smartSenseId = String.format(smartSenseIdPattern, firstPart, secondPart);
        }
        return smartSenseId;
    }

    private Collection<? extends BlueprintConfigurationEntry> getSmartSenseGatewayConfigs(Stack stack) {
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        String privateIp = stack.getGatewayInstanceGroup().getInstanceMetaData().stream().findFirst().get().getPrivateIp();
        configs.add(new BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "gateway.host", privateIp));
        configs.add(new BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "gateway.enabled", "true"));
        return configs;
    }
}
