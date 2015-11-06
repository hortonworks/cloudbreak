package com.sequenceiq.periscope.service;

import static org.apache.commons.lang3.StringUtils.join;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.Notification;
import com.sequenceiq.periscope.log.MDCBuilder;

import freemarker.template.TemplateException;

@Service
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);
    private static final String ALARM_TEMPLATE = "templates/alarm-notification.ftl";
    private static final String SUBJECT = "Alarm notification";

    @Autowired
    private EmailService emailService;

    public void sendNotification(Cluster cluster, BaseAlert alert, Notification notification) {
        MDCBuilder.buildMdcContext(cluster);
        switch (notification.getType()) {
            case EMAIL:
                sendEmailNotification(alert, notification);
                break;
            default:
                LOGGER.info("Only email notification is supported, yet");
        }
    }

    private void sendEmailNotification(BaseAlert alert, Notification notification) {
        String[] recipients = notification.getTarget();
        LOGGER.info("Sending e-mail notification to: {}", join(recipients, ","));
        Map<String, String> model = new HashMap<>();
        model.put("alarmName", alert.getName());
        model.put("description", alert.getDescription());
        try {
            emailService.sendMail(recipients, SUBJECT, ALARM_TEMPLATE, model);
        } catch (IOException | TemplateException e) {
            LOGGER.warn("Cannot send e-mail notification to {}", recipients);
        }
    }
}
