package com.sequenceiq.mock.aspect;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.sequenceiq.mock.service.ResponseModifierService;

@ExtendWith(MockitoExtension.class)
class ResponseModifierAspectTest {

    private static final String MOCK_UUID = "mock-uuid-1";

    @Mock
    private ResponseModifierService responseModifierService;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private ResponseModifierAspect underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "environment", new StandardEnvironment());
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalThrows")
    void testPlaceholderInClassLevelRequestMappingIsResolvedToItsDefault() throws Throwable {
        when(proceedingJoinPoint.getTarget()).thenReturn(new TestHostsController());
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(TestHostsController.class.getMethod("readHosts", String.class));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{MOCK_UUID});

        underTest.proceedOnRepositorySave(proceedingJoinPoint);

        String expectedPath = "get_/" + MOCK_UUID + "/api/v45/hosts";
        verify(responseModifierService).handleProfiles(MOCK_UUID, expectedPath);
        verify(responseModifierService).evaluateResponse(eq(expectedPath), eq(ResponseEntity.class), any(), anyList());
    }

    @RequestMapping("${openapi.clouderaManager.base-path:/{mockUuid}/api/v45}")
    interface TestHostsApi {

        @RequestMapping(method = RequestMethod.GET, value = "/hosts")
        ResponseEntity<String> readHosts(@PathVariable("mockUuid") String mockUuid);
    }

    static class TestHostsController implements TestHostsApi {

        @Override
        public ResponseEntity<String> readHosts(String mockUuid) {
            return ResponseEntity.ok("");
        }
    }
}
