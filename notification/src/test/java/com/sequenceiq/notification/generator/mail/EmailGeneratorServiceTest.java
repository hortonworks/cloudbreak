package com.sequenceiq.notification.generator.mail;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

class EmailGeneratorServiceTest {

    private Configuration freemarkerConfiguration;

    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    private EmailGeneratorService emailGeneratorService;

    @BeforeEach
    void setUp() {
        freemarkerConfiguration = mock(Configuration.class);
        freeMarkerTemplateUtils = mock(FreeMarkerTemplateUtils.class);
        emailGeneratorService = new EmailGeneratorService(freemarkerConfiguration, freeMarkerTemplateUtils);
    }

    @Test
    void generateReturnsProcessedTemplateString() throws IOException, TemplateException {
        NotificationGeneratorDto dto = new NotificationGeneratorDto();
        NotificationType notificationType = NotificationType.AZURE_DEFAULT_OUTBOUND;
        Template template = mock(Template.class);

        when(freeMarkerTemplateUtils.template(eq(freemarkerConfiguration), eq(notificationType.getTemplate()), eq(notificationType.name())))
                .thenReturn(template);
        when(freeMarkerTemplateUtils.processTemplateIntoString(template, dto)).thenReturn("Processed Template");

        Optional<String> result = emailGeneratorService.generate(dto, notificationType);

        assertThat(result.get()).isEqualTo("Processed Template");
    }

    @Test
    void generateReturnsNullWhenTemplateProcessingFails() throws IOException, TemplateException {
        NotificationGeneratorDto dto = new NotificationGeneratorDto();
        NotificationType notificationType = NotificationType.AZURE_DEFAULT_OUTBOUND;

        when(freeMarkerTemplateUtils.template(eq(freemarkerConfiguration), eq(notificationType.getTemplate()), eq(notificationType.name())))
                .thenThrow(new IOException("Template not found"));

        Optional<String> result = emailGeneratorService.generate(dto, notificationType);

        assertTrue(result.isEmpty());
    }

    @Test
    void channelTypeReturnsEmailChannelType() {
        ChannelType result = emailGeneratorService.channelType();

        assertThat(result).isEqualTo(ChannelType.EMAIL);
    }
}