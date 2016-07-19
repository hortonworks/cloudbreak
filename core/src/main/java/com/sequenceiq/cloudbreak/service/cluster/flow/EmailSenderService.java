package com.sequenceiq.cloudbreak.service.cluster.flow;

import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

import freemarker.template.Configuration;

@Service
public class EmailSenderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSenderService.class);
    private static final String CLUSTER_READY_SUBJECT = "Your cluster is ready";

    @Value("${cb.smtp.sender.from:}")
    private String msgFrom;

    @Value("${cb.success.cluster.installer.mail.template.path:}")
    private String successClusterMailTemplatePath;

    @Value("${cb.failed.cluster.installer.mail.template.path:}")
    private String failedClusterMailTemplatePath;

    @Value("${cb.telemetry.mail.address:aws-marketplace@hortonworks.com}")
    private String telemetryMailAddress;

    @Value("${cb.smartsense.configure:false}")
    private boolean configureSmartSense;

    @Value("${hwx.cloud.template.version:}")
    private String templateVersion;

    @Value("${hwx.cloud.address:}")
    private String cloudAddress;

    @Value("${aws.instance.id:}")
    private String awsInstanceId;

    @Value("${aws.account.id:}")
    private String accountId;

    @Inject
    private EmailMimeMessagePreparator emailMimeMessagePreparator;

    @Inject
    private JavaMailSender mailSender;

    @Inject
    private ImageService imageService;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private UserDetailsService userDetailsService;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private enum State {
        PROVISIONING_SUCCESS("SUCCESS", "Cluster Install Success",
                "Your cluster '%s' is ready to use. You can log into the Ambari UI %s:8080 using the configured username/password."),
        PROVISIONING_FAILURE("FAILED", "Cluster Install Failed",
                "Something went terribly wrong - we are happy to help, please let us know your cluster details, "
                        + "time, etc - and we will check the logs and get a fix for you."),
        START_SUCCESS("SUCCESS", "Cluster Start Success",
                "Your cluster '%s' is ready to use after the start. You can log into the Ambari UI %s:8080 using the configured username/password."),
        START_FAILURE("FAILED", "Cluster Start Failed",
                "Failed to start your cluster: %s - we are happy to help, please let us know your cluster details, time, etc - and we will check the"
                        + " logs and get a fix for you."),
        STOP_SUCCESS("SUCCESS", "Cluster Stop Success",
                "Your cluster '%s' was successfully stopped. If you want to use again just restart."),
        STOP_FAILURE("FAILED", "Cluster Stop Failed",
                "Failed to stop your cluster: %s - we are happy to help, please let us know your cluster details, time, etc - and we will check the "
                        + "logs and get a fix for you."),
        UPSCALE_SUCCESS("SUCCESS", "Cluster Upscale Success",
                "Your cluster '%s' is ready to use after the upscale. You can log into the Ambari UI %s:8080 using the configured username/password."),
        DOWN_SCALE_SUCCESS("SUCCESS", "Cluster Downscale Success",
                "Your cluster '%s' is ready to use after the downscale. You can log into the Ambari UI %s:8080 using the configured username/password."),
        TERMINATION_SUCCESS("SUCCESS", "Cluster Termination Success",
                "Your cluster '%s' was successfully terminated."),
        TERMINATION_FAILURE("FAILED", "Cluster Termination Failed",
                "Failed to terminate your cluster: '%s'. Please try again... - we are happy to help, please let us know your cluster details, time, "
                        + "etc - and we will check the logs and get a fix for you.");

        private final String status;
        private final String title;
        private final String text;

        State(String status, String title, String text) {
            this.status = status;
            this.title = title;
            this.text = text;
        }
    }

    private boolean isHwxCloud() {
        return !Strings.isNullOrEmpty(templateVersion);
    }

    @Async
    public void sendProvisioningSuccessEmail(String owner, String email, String ambariServer, String clusterName) {
        CbUser user = userDetailsService.getDetails(owner, UserFilterField.USERID);
        sendEmail(user, email, successClusterMailTemplatePath, CLUSTER_READY_SUBJECT, getEmailModel(user.getGivenName(),
                ambariServer, State.PROVISIONING_SUCCESS, clusterName));
    }

    @Async
    public void sendProvisioningFailureEmail(String owner, String email, String clusterName) {
        CbUser user = userDetailsService.getDetails(owner, UserFilterField.USERID);
        sendEmail(user, email, failedClusterMailTemplatePath, "Cluster install failed", getEmailModel(user.getGivenName(),
                null, State.PROVISIONING_FAILURE, clusterName));
    }

    @Async
    public void sendStartSuccessEmail(String owner, String email, String ambariServer, String clusterName) {
        CbUser user = userDetailsService.getDetails(owner, UserFilterField.USERID);
        sendEmail(user, email, successClusterMailTemplatePath, CLUSTER_READY_SUBJECT, getEmailModel(user.getGivenName(),
                ambariServer, State.START_SUCCESS, clusterName));
    }

    @Async
    public void sendStartFailureEmail(String owner, String email, String ambariServer, String clusterName) {
        CbUser user = userDetailsService.getDetails(owner, UserFilterField.USERID);
        sendEmail(user, email, failedClusterMailTemplatePath, "Cluster start failed", getEmailModel(user.getGivenName(),
                ambariServer, State.START_FAILURE, clusterName));
    }

    @Async
    public void sendStopSuccessEmail(String owner, String email, String ambariServer, String clusterName) {
        CbUser user = userDetailsService.getDetails(owner, UserFilterField.USERID);
        sendEmail(user, email, successClusterMailTemplatePath, "Your cluster has been stopped", getEmailModel(user.getGivenName(),
                ambariServer, State.STOP_SUCCESS, clusterName));
    }

    @Async
    public void sendStopFailureEmail(String owner, String email, String ambariServer, String clusterName) {
        CbUser user = userDetailsService.getDetails(owner, UserFilterField.USERID);
        sendEmail(user, email, failedClusterMailTemplatePath, "Cluster stop failed", getEmailModel(user.getGivenName(),
                ambariServer, State.STOP_FAILURE, clusterName));
    }

    public void sendUpscaleSuccessEmail(String owner, String email, String ambariServer, String clusterName) {
        CbUser user = userDetailsService.getDetails(owner, UserFilterField.USERID);
        sendEmail(user, email, successClusterMailTemplatePath, CLUSTER_READY_SUBJECT, getEmailModel(user.getGivenName(),
                ambariServer, State.UPSCALE_SUCCESS, clusterName));
    }

    @Async
    public void sendDownScaleSuccessEmail(String owner, String email, String ambariServer, String clusterName) {
        CbUser user = userDetailsService.getDetails(owner, UserFilterField.USERID);
        sendEmail(user, email, successClusterMailTemplatePath, CLUSTER_READY_SUBJECT, getEmailModel(user.getGivenName(),
                ambariServer, State.DOWN_SCALE_SUCCESS, clusterName));
    }

    @Async
    public void sendTerminationSuccessEmail(String owner, String email, String ambariServer, String clusterName) {
        CbUser user = userDetailsService.getDetails(owner, UserFilterField.USERID);
        sendEmail(user, email, successClusterMailTemplatePath, "Your cluster has been terminated", getEmailModel(user.getGivenName(),
                ambariServer, State.TERMINATION_SUCCESS, clusterName));
    }

    @Async
    public void sendTerminationFailureEmail(String owner, String email, String ambariServer, String clusterName) {
        CbUser user = userDetailsService.getDetails(owner, UserFilterField.USERID);
        sendEmail(user, email, failedClusterMailTemplatePath, "Cluster termination failed", getEmailModel(user.getGivenName(),
                ambariServer, State.TERMINATION_FAILURE, clusterName));
    }

    @Async
    public void sendTelemetryMailIfNeeded(Stack stack, Status status) {
        Cluster cluster = stack.getCluster();
        String smartSenseId = (String) stack.getCredential().getAttributes().getMap().get(CloudCredential.SMART_SENSE_ID);
        Map<String, Object> telemetryMailMap = new LinkedHashMap<>();
        if (configureSmartSense && !StringUtils.isEmpty(smartSenseId)) {
            telemetryMailMap.put("Date", dateFormat.format(new Date()));
            if (Strings.isNullOrEmpty(accountId)) {
                telemetryMailMap.put("Account ID", accountId);
            }
            telemetryMailMap.put("Smartsense ID", smartSenseId);
            telemetryMailMap.put("Cluster ID", cluster.getId());
            telemetryMailMap.put("Cluster type", getClusterType(stack, cluster));
            telemetryMailMap.put("Node count", stack.getFullNodeCount());
            telemetryMailMap.put("Instance type(s)", getInstanceTypes(stack));
            telemetryMailMap.put("Running time", getRunningTime(cluster));
            telemetryMailMap.put("Status", status.normalizedStatusName());
            if (Strings.isNullOrEmpty(templateVersion)) {
                telemetryMailMap.put("Version", templateVersion);
            }
            if (Strings.isNullOrEmpty(awsInstanceId)) {
                telemetryMailMap.put("Controller Instance ID", awsInstanceId);
            }
            telemetryMailMap.put("Master Instance ID", getMasterInstanceId(stack));

            StringBuilder telemetryMailBodyBuilder = new StringBuilder();
            for (String key : telemetryMailMap.keySet()) {
                telemetryMailBodyBuilder.append(key).append(": ").append(telemetryMailMap.get(key)).append("<br />");
            }
            mailSender.send(emailMimeMessagePreparator.prepareMessage(telemetryMailAddress, "Telemetry", telemetryMailBodyBuilder.toString()));
        }
    }

    private String getClusterType(Stack stack, Cluster cluster) {
        String hdpVersion = null;
        try {
            hdpVersion = imageService.getImage(stack.getId()).getHdpVersion();
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.info("cant find hdp version");
        }
        if (hdpVersion == null) {
            hdpVersion = "HDP_VERSION_NOT_FOUND";
        }
        return hdpVersion + " : " + cluster.getBlueprint().getBlueprintName();
    }

    private static String getRunningTime(Cluster cluster) {
        long upTime = cluster.getUpSince() == null || !cluster.isAvailable() ? 0L : new Date().getTime() - cluster.getUpSince();
        return uptimeToFormattedString(upTime);
    }

    private static String uptimeToFormattedString(long upTime) {
        return DurationFormatUtils.formatDuration(upTime, "HH:mm:ss");
    }

    private String getInstanceTypes(Stack stack) {
        StringBuilder instanceTypesStringBuilder = new StringBuilder();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            String instanceType = instanceGroup.getTemplate().getInstanceType();
            instanceTypesStringBuilder.append("<br />").append(" - ");
            instanceTypesStringBuilder.append(instanceGroup.getGroupName()).append(" (").append(instanceGroup.getNodeCount()).append(") : ");
            instanceTypesStringBuilder.append(instanceType);
        }
        return instanceTypesStringBuilder.toString();
    }

    private String getMasterInstanceId(Stack stack) {
        return stack.getGatewayInstanceGroup().getAllInstanceMetaData().iterator().next().getInstanceId();
    }

    private void sendEmail(CbUser user, String mail, String template, String subject, Map<String, Object> model) {
        try {
            String emailBody = processTemplateIntoString(freemarkerConfiguration.getTemplate(template, "UTF-8"), model);
            LOGGER.debug("Sending email. Content: {}", emailBody);
            mailSender.send(emailMimeMessagePreparator.prepareMessage(Strings.isNullOrEmpty(mail) ? user.getUsername() : mail, subject, emailBody));
        } catch (Exception e) {
            LOGGER.error("Could not send email. User: {}", user.getUserId());
            throw new CloudbreakServiceException(e);
        }
    }

    private Map<String, Object> getEmailModel(String name, String server, State state, String clusterName) {
        Map<String, Object> model = new HashMap<>();
        model.put("status", state.status);
        model.put("name", name);
        model.put("text", String.format(state.text, clusterName, server));
        model.put("title", state.title);
        model.put("state", state);
        model.put("clusterName", clusterName);
        model.put("hwx_cloud", isHwxCloud());
        model.put("server", isHwxCloud() ? cloudAddress : server);
        return model;
    }
}
