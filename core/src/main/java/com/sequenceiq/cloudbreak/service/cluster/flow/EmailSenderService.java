package com.sequenceiq.cloudbreak.service.cluster.flow;

import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.HDPRepo;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

import freemarker.template.Configuration;

@Service
public class EmailSenderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSenderService.class);
    private static final String CLUSTER_READY_SUBJECT = "Your cluster '%s' is ready";

    @Value("${cb.smtp.sender.from:}")
    private String msgFrom;

    @Value("${cb.success.cluster.installer.mail.template.path:}")
    private String successClusterMailTemplatePath;

    @Value("${cb.failed.cluster.installer.mail.template.path:}")
    private String failedClusterMailTemplatePath;

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

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private enum State {
        PROVISIONING_SUCCESS("SUCCESS", "Cluster Install Success",
                "Your cluster '%s' is ready"),
        PROVISIONING_FAILURE("FAILED", "Cluster Install Failed",
                "Something went terribly wrong - we are happy to help, please let us know your cluster details, "
                        + "time, etc - and we will check the logs and get a fix for you."),
        START_SUCCESS("SUCCESS", "Cluster Start Success",
                "Your cluster '%s' is ready"),
        START_FAILURE("FAILED", "Cluster Start Failed",
                "Failed to start your cluster: %s - we are happy to help, please let us know your cluster details, time, etc - and we will check the"
                        + " logs and get a fix for you."),
        STOP_SUCCESS("SUCCESS", "Cluster Stop Success",
                "Your cluster '%s' was successfully stopped. If you want to use again just restart."),
        STOP_FAILURE("FAILED", "Cluster Stop Failed",
                "Failed to stop your cluster: %s - we are happy to help, please let us know your cluster details, time, etc - and we will check the "
                        + "logs and get a fix for you."),
        UPSCALE_SUCCESS("SUCCESS", "Cluster Upscale Success",
                "Your cluster '%s' is ready"),
        DOWN_SCALE_SUCCESS("SUCCESS", "Cluster Downscale Success",
                "Your cluster '%s' is ready"),
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

    private static String getRunningTime(Cluster cluster) {
        long upTime = cluster.getUpSince() == null || !cluster.isAvailable() ? 0L : new Date().getTime() - cluster.getUpSince();
        return uptimeToFormattedString(upTime);
    }

    private static String uptimeToFormattedString(long upTime) {
        return DurationFormatUtils.formatDuration(upTime, "HH:mm:ss");
    }

    private boolean isHwxCloud() {
        return !Strings.isNullOrEmpty(templateVersion);
    }

    @Async
    public void sendProvisioningSuccessEmail(String owner, String email, String ambariServer, String clusterName) {
        CbUser user = userDetailsService.getDetails(owner, UserFilterField.USERID);
        sendEmail(user, email, successClusterMailTemplatePath, String.format(CLUSTER_READY_SUBJECT, clusterName), getEmailModel(user.getGivenName(),
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
        sendEmail(user, email, successClusterMailTemplatePath, String.format(CLUSTER_READY_SUBJECT, clusterName), getEmailModel(user.getGivenName(),
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
        sendEmail(user, email, successClusterMailTemplatePath, String.format(CLUSTER_READY_SUBJECT, clusterName), getEmailModel(user.getGivenName(),
                ambariServer, State.UPSCALE_SUCCESS, clusterName));
    }

    @Async
    public void sendDownScaleSuccessEmail(String owner, String email, String ambariServer, String clusterName) {
        CbUser user = userDetailsService.getDetails(owner, UserFilterField.USERID);
        sendEmail(user, email, successClusterMailTemplatePath, String.format(CLUSTER_READY_SUBJECT, clusterName), getEmailModel(user.getGivenName(),
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

    private String getClusterType(Stack stack, Cluster cluster) {
        String hdpVersion = null;
        try {
            HDPRepo repo = componentConfigProvider.getHDPRepo(stack.getId());
            if (repo != null) {
                hdpVersion = repo.getHdpVersion();
            }
        } catch (CloudbreakServiceException e) {
            LOGGER.info("cant find hdp version");
        }
        if (hdpVersion == null) {
            hdpVersion = "HDP_VERSION_NOT_FOUND";
        }
        return hdpVersion + " : " + cluster.getBlueprint().getBlueprintName();
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
