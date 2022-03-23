package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

@ExtendWith(MockitoExtension.class)
class RedbeamsClientServiceTest {

    @Mock
    private DatabaseServerV4Endpoint redbeamsServerEndpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private RedbeamsClientService underTest;

    static Stream<Arguments> emptyStringValues() {
        return Stream.of(
                Arguments.of("Null string", null),
                Arguments.of("Empty string", ""),
                Arguments.of("Blank string", "  ")
        );
    }

    @Test
    void deleteByCrnNotFoundIsRethrownAsIs() {
        when(redbeamsServerEndpoint.deleteByCrn(any(), anyBoolean())).thenThrow(new NotFoundException("not found"));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        assertThatThrownBy(() -> underTest.deleteByCrn("crn", true)).isExactlyInstanceOf(NotFoundException.class);
    }

    @Test
    void getByClusterCrnNotFoundIsRethrownAsIs() {
        when(redbeamsServerEndpoint.getByClusterCrn(anyString(), anyString())).thenThrow(new NotFoundException("not found"));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        assertThatThrownBy(() -> underTest.getByClusterCrn("crn", "crn2")).isExactlyInstanceOf(NotFoundException.class);
    }

    @MethodSource("emptyStringValues")
    @ParameterizedTest(name = "{0}")
    void getByClusterCrnThrowsIfNullEnvCrn(String testName, String value) {
        assertThatThrownBy(() -> underTest.getByClusterCrn(value, "crn2")).isExactlyInstanceOf(CloudbreakServiceException.class);
    }

    @MethodSource("emptyStringValues")
    @ParameterizedTest(name = "{0}")
    void getByClusterCrnThrowsIfNullClusterCrn(String testName, String value) {
        assertThatThrownBy(() -> underTest.getByClusterCrn("crn", value)).isExactlyInstanceOf(CloudbreakServiceException.class);
    }
}
