package com.sequenceiq.cloudbreak.service.externaldatabase;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;

class AwsDatabaseServerParameterDecoratorTest {

    private AwsDatabaseServerParameterDecorator underTest = new AwsDatabaseServerParameterDecorator();

    @ParameterizedTest
    @MethodSource("availabilityTypes")
    void setParameters(DatabaseAvailabilityType availabilityType, boolean needsHA) {
        DatabaseServerParameter serverParameter = DatabaseServerParameter.builder().withAvailabilityType(availabilityType).build();
        DatabaseServerV4StackRequest request = new DatabaseServerV4StackRequest();
        underTest.setParameters(request, serverParameter);
        assertThat(request.getAws().getMultiAZ()).isEqualTo(Boolean.toString(needsHA));
    }

    @Test
    void getCloudPlatform() {
        assertThat(underTest.getCloudPlatform()).isEqualTo(CloudPlatform.AWS);
    }

    public static Stream<Arguments> availabilityTypes() {
        return Stream.of(
                Arguments.of(DatabaseAvailabilityType.NONE, false),
                Arguments.of(DatabaseAvailabilityType.ON_ROOT_VOLUME, false),
                Arguments.of(DatabaseAvailabilityType.NON_HA, false),
                Arguments.of(DatabaseAvailabilityType.HA, true)
        );
    }
}
