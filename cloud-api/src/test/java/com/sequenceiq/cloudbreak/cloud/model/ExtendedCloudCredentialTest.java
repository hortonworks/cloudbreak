package com.sequenceiq.cloudbreak.cloud.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtendedCloudCredentialTest {

    private static final String CONFIDENTIAL = "confidential";

    private static final String SECRET = "mySecret";

    @Mock
    private CloudCredential cloudCredential;

    @Test
    void toStringTestShouldNotIncludeSecrets() {
        Map<String, Object> parameters = Map.of(CONFIDENTIAL, SECRET);
        when(cloudCredential.getParameters()).thenReturn(parameters);
        when(cloudCredential.getId()).thenReturn("id");
        when(cloudCredential.getName()).thenReturn("name");
        when(cloudCredential.isVerifyPermissions()).thenReturn(true);

        ExtendedCloudCredential underTest = new ExtendedCloudCredential(cloudCredential, "AWS",
                "myCred", "myUser", "accountId", new ArrayList<>());
        String result = underTest.toString();

        assertThat(underTest.getStringParameter(CONFIDENTIAL)).isEqualTo(SECRET);
        assertThat(result).doesNotContain("DynamicModel");
        assertThat(result).doesNotContain(CONFIDENTIAL);
        assertThat(result).doesNotContain(SECRET);
    }

}