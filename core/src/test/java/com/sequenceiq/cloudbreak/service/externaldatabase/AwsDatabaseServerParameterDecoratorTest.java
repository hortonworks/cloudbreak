package com.sequenceiq.cloudbreak.service.externaldatabase;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;

class AwsDatabaseServerParameterDecoratorTest {

    private AwsDatabaseServerParameterDecorator underTest = new AwsDatabaseServerParameterDecorator();

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void setParameters(boolean needsHA) {
        DatabaseServerParameter serverParameter = DatabaseServerParameter.builder().withHighlyAvailable(needsHA).build();
        DatabaseServerV4StackRequest request = new DatabaseServerV4StackRequest();
        underTest.setParameters(request, serverParameter);
        assertThat(request.getAws().getMultiAZ()).isEqualTo(Boolean.toString(needsHA));
    }

    @Test
    void getCloudPlatform() {
        assertThat(underTest.getCloudPlatform()).isEqualTo(CloudPlatform.AWS);
    }
}
