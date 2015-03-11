package com.sequenceiq.periscope.service;

import static org.apache.commons.lang3.StringUtils.join;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.Notification;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;

import freemarker.template.TemplateException;

@Service
public class NotificationService {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(NotificationService.class);
    private static final String ALARM_TEMPLATE = "templates/alarm-notification.ftl";
    private static final String SUBJECT = "Alarm notification";

    @Autowired
    private EmailService emailService;

    public void sendNotification(Cluster cluster, BaseAlert alert, Notification notification) {
        long clusterId = cluster.getId();
        switch (notification.getType()) {
            case EMAIL:
                sendEmailNotification(clusterId, alert, notification);
                break;
            default:
                LOGGER.info(clusterId, "Only email notification is supported, yet");
        }
    }

    private void sendEmailNotification(long clusterId, BaseAlert alert, Notification notification) {
        String[] recipients = notification.getTarget();
        LOGGER.info(clusterId, "Sending e-mail notification to: {}", join(recipients, ","));
        Map<String, String> model = new HashMap<>();
        model.put("alarmName", alert.getName());
        model.put("description", alert.getDescription());
        try {
            emailService.sendMail(recipients, SUBJECT, ALARM_TEMPLATE, model);
        } catch (IOException | TemplateException e) {
            LOGGER.warn(clusterId, "Cannot send e-mail notification to {}", recipients);
        }
    }
}
