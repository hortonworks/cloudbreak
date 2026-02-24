package com.sequenceiq.notification.generator.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;
import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDtos;
import com.sequenceiq.notification.scheduled.register.dto.clusterhealth.ClusterHealthNotificationAdditionalDataDto;
import com.sequenceiq.notification.scheduled.register.dto.clusterhealth.InstanceStatusDto;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
        EmailGeneratorService.class,
        FreeMarkerTemplateUtils.class,
        TestFreemarkerConfiguration.class
})
class EmailGeneratorServiceIntegrationTest {

    @Inject
    private EmailGeneratorService underTest;

    @Test
    void testGenerateEmailWithValidTemplateAndModel() throws IOException {
                NotificationGeneratorDto dto = NotificationGeneratorDto.builder()
                        .additionalData(
                                BaseNotificationRegisterAdditionalDataDtos.builder()
                                .results(
                                        List.of(
                                            ClusterHealthNotificationAdditionalDataDto.builder()
                                                    .detailedStatus("STACK_SCALE_FAILED")
                                                    .name("test-cluster")
                                                    .statusReason("Quota issue")
                                                    .status("SCLAE_FAILED")
                                                    .crn("crn:cdp:datahub:us-west-1:test-account:datahub:test-crn")
                                                    .dateTimeString("2026.03.01 13:69:11")
                                                    .stackType("Data Hub")
                                                    .controlPlaneUrl("localhost")
                                                    .addInstance(
                                                            InstanceStatusDto.builder()
                                                                    .name("i-12345")
                                                                    .instanceType("m3.xlarge")
                                                                    .status("Failed")
                                                                    .groupName("master")
                                                                    .url("localhost")
                                                                    .build()
                                                    )
                                                    .addInstance(
                                                            InstanceStatusDto.builder()
                                                                    .name("i-12346")
                                                                    .instanceType("m3.xlarge")
                                                                    .status("Zombie")
                                                                    .groupName("master")
                                                                    .url("localhost")
                                                                    .build()
                                                    )
                                                    .build()
                                        )
                                )
                                .build()
                        )
                        .accountId("2345")
                        .name("appletree")
                        .resourceCrn("crn:cdp:datahub:us-west-1:test-account:datahub:test-crn")
                        .resourceName("appletree")
                        .build();
        Optional<String> result = underTest.generate(dto, NotificationType.STACK_HEALTH);
        assertTrue(result.isPresent());

        Document.OutputSettings settings = new Document.OutputSettings()
                .prettyPrint(true)
                .indentAmount(0)
                .outline(true);


        Document actual = Jsoup.parse(result.get(), "UTF-8");
        Document expected = Jsoup.parse(FileReaderUtils.readFileFromClasspath("integrationtest/resize.html"), "UTF-8");

        actual.outputSettings(settings);
        expected.outputSettings(settings);

        assertEquals(actual.html().trim(), expected.html().trim());
    }
}
