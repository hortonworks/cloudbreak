package com.sequenceiq.datalake.flow.create.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
class RdsCreationHandlerSupportTest {

    private static final Long SDX_ID = 12L;

    private static final String USER_ID = "userId";

    private static final int DURATION_IN_MINUTES = 80;

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private RdsCreationHandlerSupport underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "durationInMinutes", DURATION_IN_MINUTES);
    }

    @Test
    void validForDatabaseCreationFailsOnAwsWithSingleSubnet() {
        DetailedEnvironmentResponse environment = environment("AWS", subnets("a"));

        assertThatThrownBy(() -> underTest.validForDatabaseCreation(SDX_ID, environment))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot create external database for Data Lake: 12, not enough subnets in the vpc");
    }

    @Test
    void validForDatabaseCreationFailsOnAwsWhenSubnetsShareOneAvailabilityZone() {
        DetailedEnvironmentResponse environment = environment("AWS", subnets("a", "a"));

        assertThatThrownBy(() -> underTest.validForDatabaseCreation(SDX_ID, environment))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot create external database for Data Lake: 12, vpc subnets must cover at least two different availability zones");
    }

    @Test
    void validForDatabaseCreationPassesOnAwsWithTwoAvailabilityZones() {
        DetailedEnvironmentResponse environment = environment("AWS", subnets("a", "b"));

        assertThatCode(() -> underTest.validForDatabaseCreation(SDX_ID, environment)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"aws", "Aws", "AWS"})
    void validForDatabaseCreationMatchesAwsCaseInsensitively(String cloudPlatform) {
        DetailedEnvironmentResponse environment = environment(cloudPlatform, subnets("a"));

        assertThatThrownBy(() -> underTest.validForDatabaseCreation(SDX_ID, environment))
                .isInstanceOf(BadRequestException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AZURE", "GCP", "MOCK"})
    void validForDatabaseCreationSkipsSubnetChecksOnNonAwsPlatforms(String cloudPlatform) {
        DetailedEnvironmentResponse environment = environment(cloudPlatform, subnets("a"));

        assertThatCode(() -> underTest.validForDatabaseCreation(SDX_ID, environment)).doesNotThrowAnyException();
    }

    @Test
    void setRdsCreatedStatusMarksExternalDatabaseCreated() {
        SdxCluster cluster = new SdxCluster();

        underTest.setRdsCreatedStatus(cluster);

        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.EXTERNAL_DATABASE_CREATED, "Data Lake external database created", cluster);
    }

    @Test
    void toFailedEventKeepsUserBreakExceptionAsIs() {
        UserBreakException exception = new UserBreakException("user break");

        Selectable result = underTest.toFailedEvent(SDX_ID, USER_ID, exception);

        assertFailedEvent(result, exception);
    }

    @Test
    void toFailedEventReplacesPollerStoppedExceptionWithTimeoutMessage() {
        PollerStoppedException exception = new PollerStoppedException("stopped");

        Selectable result = underTest.toFailedEvent(SDX_ID, USER_ID, exception);

        assertThat(result).isInstanceOf(SdxCreateFailedEvent.class);
        assertThat(result.getException())
                .isNotSameAs(exception)
                .isInstanceOf(PollerStoppedException.class)
                .hasMessage("Database creation timed out after 80 minutes");
        assertIdentity((SdxCreateFailedEvent) result);
    }

    @Test
    void toFailedEventKeepsPollerExceptionAsIs() {
        PollerException exception = new PollerException("poller failed");

        Selectable result = underTest.toFailedEvent(SDX_ID, USER_ID, exception);

        assertFailedEvent(result, exception);
    }

    @Test
    void toFailedEventKeepsAnyOtherExceptionAsIs() {
        IllegalStateException exception = new IllegalStateException("unexpected");

        Selectable result = underTest.toFailedEvent(SDX_ID, USER_ID, exception);

        assertFailedEvent(result, exception);
    }

    @Test
    void toFailedEventKeepsBadRequestFromValidation() {
        BadRequestException exception = new BadRequestException("not enough subnets");

        Selectable result = underTest.toFailedEvent(SDX_ID, USER_ID, exception);

        assertFailedEvent(result, exception);
    }

    private void assertFailedEvent(Selectable result, Exception expectedException) {
        assertThat(result).isInstanceOf(SdxCreateFailedEvent.class);
        assertThat(result.getException()).isSameAs(expectedException);
        assertIdentity((SdxCreateFailedEvent) result);
    }

    private void assertIdentity(SdxCreateFailedEvent failed) {
        assertThat(failed.getResourceId()).isEqualTo(SDX_ID);
        assertThat(failed.getUserId()).isEqualTo(USER_ID);
    }

    private DetailedEnvironmentResponse environment(String cloudPlatform, Map<String, CloudSubnet> subnetMetas) {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setSubnetMetas(subnetMetas);
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform(cloudPlatform);
        environment.setNetwork(network);
        return environment;
    }

    private Map<String, CloudSubnet> subnets(String... availabilityZones) {
        Map<String, CloudSubnet> subnets = new LinkedHashMap<>();
        for (int i = 0; i < availabilityZones.length; i++) {
            CloudSubnet subnet = new CloudSubnet();
            subnet.setId("subnet-" + i);
            subnet.setAvailabilityZone(availabilityZones[i]);
            subnets.put("subnet-" + i, subnet);
        }
        return subnets;
    }
}
