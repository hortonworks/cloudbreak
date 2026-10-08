package com.sequenceiq.notification.generator.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.sequenceiq.notification.scheduled.register.dto.clusterhealth.ActionResult;
import com.sequenceiq.notification.scheduled.register.dto.clusterhealth.ClusterHealthNotificationAdditionalDataDto;
import com.sequenceiq.notification.scheduled.register.dto.clusterhealth.InstanceStatusDto;
import com.sequenceiq.notification.scheduled.register.dto.clusterhealth.OperationType;

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
        Optional<String> result = underTest.generate(failedDto(), NotificationType.STACK_HEALTH);
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

    @Test
    void testGenerateEmailForAlertedBranchRendersNonEmptyAdvisory() {
        Optional<String> result = underTest.generate(alertedDto(), NotificationType.STACK_HEALTH);

        assertTrue(result.isPresent());
        String html = result.get();
        assertFalse(html.isBlank());

        String text = Jsoup.parse(html, "UTF-8").text();
        // alerted branch intro paragraph: "Your ${stackType} ${name} ${operationMessage}."
        assertTrue(text.contains("Your Data Lake appletree is running low on storage"),
                () -> "alerted intro paragraph missing from rendered email: " + text);
        assertTrue(text.contains("Your environment remains fully operational"),
                () -> "alerted reassurance paragraph missing from rendered email: " + text);
        // alerted advisory status label
        assertTrue(text.contains("Warning (Non-Critical)"),
                () -> "alerted advisory status missing from rendered email: " + text);
        // the failed-only sentence must not leak into the alerted email
        assertFalse(text.contains("on the following Data Lake alerted"),
                () -> "failed-branch sentence leaked into alerted email: " + text);
    }

    private NotificationGeneratorDto failedDto() {
        return NotificationGeneratorDto.builder()
                .additionalData(
                        BaseNotificationRegisterAdditionalDataDtos.builder()
                        .results(
                                List.of(
                                    ClusterHealthNotificationAdditionalDataDto.builder()
                                            .detailedStatus("STACK_SCALE_FAILED")
                                            .name("test-cluster")
                                            .statusReason("Quota issue")
                                            .status("SCALE_FAILED")
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
    }

    /**
     * Mirrors the DTO shape produced by redbeams' {@code DbStackEmailNotificationDataPreparationService} for the external-database
     * low-storage email: {@link ActionResult#ALERTED}, {@link OperationType#CHECK}, an {@code operationMessage}, empty instances and a
     * populated {@code statusReason}. Guards the {@code alerted} template branch (and the silent-drop hazard on the unconditional fields)
     * that no other render test exercises.
     */
    private NotificationGeneratorDto alertedDto() {
        return NotificationGeneratorDto.builder()
                .additionalData(
                        BaseNotificationRegisterAdditionalDataDtos.builder()
                        .results(
                                List.of(
                                    ClusterHealthNotificationAdditionalDataDto.builder()
                                            .name("appletree")
                                            .crn("crn:cdp:datalake:us-west-1:test-account:datalake:test-crn")
                                            .status("STORAGE_LOW")
                                            .stackType("Data Lake")
                                            .controlPlaneUrl("localhost")
                                            .dateTimeString("2026.03.01 13:69:11")
                                            .statusReason("Only 5.0% storage free")
                                            .operationMessage("is running low on storage")
                                            .operationType(OperationType.CHECK)
                                            .actionResult(ActionResult.ALERTED)
                                            .mainTitle("Storage Alert")
                                            .build()
                                )
                        )
                        .build()
                )
                .accountId("2345")
                .name("appletree")
                .resourceCrn("crn:cdp:datalake:us-west-1:test-account:datalake:test-crn")
                .resourceName("appletree")
                .build();
    }
}
