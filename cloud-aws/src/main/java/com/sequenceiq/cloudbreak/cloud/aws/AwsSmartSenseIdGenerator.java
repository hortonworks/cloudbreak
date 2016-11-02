package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;

@Component
public class AwsSmartSenseIdGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSmartSenseIdGenerator.class);

    private static final int FIRST_PART_LENGTH = 4;

    @Value("${cb.smartsense.configure:false}")
    private boolean configureSmartSense;

    @Value("${cb.smartsense.id.pattern:}")
    private String smartSenseIdPattern;

    public String getSmartSenseId(AwsCredentialView credentialView) {
        String result = "";
        if (configureSmartSense) {
            result = getSmartSenseId(credentialView.getRoleArn(), credentialView.getAccessKey(), credentialView.getSecretKey());
        }
        return result;
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
            String firstPart = accountId.substring(0, FIRST_PART_LENGTH);
            String secondPart = accountId.substring(FIRST_PART_LENGTH);
            smartSenseId = String.format(smartSenseIdPattern, firstPart, secondPart);
        }
        return smartSenseId;
    }
}
