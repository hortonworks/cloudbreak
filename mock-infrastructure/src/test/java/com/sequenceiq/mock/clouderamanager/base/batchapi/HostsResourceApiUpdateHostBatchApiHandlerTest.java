package com.sequenceiq.mock.clouderamanager.base.batchapi;

import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.mock.clouderamanager.DataProviderService;
import com.sequenceiq.mock.swagger.model.ApiBatchRequestElement;
import com.sequenceiq.mock.swagger.model.ApiBatchResponseElement;
import com.sequenceiq.mock.swagger.model.ApiHost;
import com.sequenceiq.mock.swagger.model.HTTPMethod;

@ExtendWith(MockitoExtension.class)
public class HostsResourceApiUpdateHostBatchApiHandlerTest {

    public static final String MOCK_UUID = "mockUuid";

    @InjectMocks
    private HostsResourceApiUpdateHostBatchApiHandler underTest;

    @Mock
    private DataProviderService dataProviderService;

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] canProcess() {
        return new Object[][] {
                // method         matches   url
                {HTTPMethod.PUT,    true,     "/api/v31/hosts/method"},
                {HTTPMethod.PUT,    true,     "/api/v49/hosts/method"},
                {HTTPMethod.GET,    false,    "/api/v49/hosts/method"},
                {HTTPMethod.POST,   false,    "/api/v49/hosts/method"},
                {HTTPMethod.DELETE, false,    "/api/v49/hosts/method"},
                {HTTPMethod.PUT,    false,    "/api/v31/hosts/"},
                {HTTPMethod.PUT,    false,    "/api/v31/hosts"},
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "#{index} :\n "
            + "Method: {0} \n"
            + "should matches {1} \n"
            + "with url: {2}")
    @MethodSource("canProcess")
    public void testCanProcessWhenMatchesAndPut(HTTPMethod method, boolean matches, String url) {
        ApiBatchRequestElement apiBatchRequestElement = new ApiBatchRequestElement().method(method).url(url);
        boolean actual = underTest.canProcess(apiBatchRequestElement);
        Assertions.assertThat(actual).isEqualTo(matches);
    }

    @Test
    public void testProcess() {
        ApiBatchRequestElement apiBatchRequestElement = new ApiBatchRequestElement()
                .method(HTTPMethod.PUT)
                .url("/api/v31/hosts/method")
                .body(new ApiHost().rackId("rackId"));
        ApiHost returnedHost = new ApiHost();
        when(dataProviderService.getApiHost(MOCK_UUID, "method")).thenReturn(returnedHost);
        ApiBatchResponseElement actual = underTest.process(MOCK_UUID, apiBatchRequestElement);
        Assertions.assertThat(actual.getStatusCode().intValue()).isEqualTo(200);
        Assertions.assertThat(returnedHost.getRackId()).isEqualTo("rackId");
    }
}
