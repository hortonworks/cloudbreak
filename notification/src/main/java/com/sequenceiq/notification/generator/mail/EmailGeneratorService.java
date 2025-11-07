package com.sequenceiq.notification.generator.mail;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.generator.NotificationGeneratorService;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Service
public class EmailGeneratorService implements NotificationGeneratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailGeneratorService.class);

    private final Configuration freemarkerConfiguration;

    private final FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    public EmailGeneratorService(Configuration freemarkerConfiguration, FreeMarkerTemplateUtils freeMarkerTemplateUtils) {
        this.freemarkerConfiguration = freemarkerConfiguration;
        this.freeMarkerTemplateUtils = freeMarkerTemplateUtils;
    }

    @Override
    public Optional<String> generate(NotificationGeneratorDto dto, NotificationType notificationType) {
        try {
            return Optional.ofNullable(freeMarkerTemplateUtils.processTemplateIntoString(getEmailTemplate(notificationType), dto));
        } catch (TemplateException | IOException e) {
            LOGGER.warn("Failed to generate email notification: {}", e.getMessage(), e);
            // We don't want to fail the email sending process in case of template processing failure.
            // `null` will be filtered out by the caller.
            return Optional.empty();
        }
    }

    private Template getEmailTemplate(NotificationType notificationType) throws IOException {
        return freeMarkerTemplateUtils.template(freemarkerConfiguration, notificationType.getTemplate(), notificationType.name());
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.EMAIL;
    }
}
