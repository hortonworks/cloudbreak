package com.sequenceiq.flow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;

@ExtendWith(MockitoExtension.class)
class EventParameterFactoryTest {

    private static final long RESOURCE_ID = 1L;

    private static String userCrnByResourceId;

    @InjectMocks
    private FakeEventParameterFactory underTest;

    @Mock
    private CrnUserDetailsService crnUserDetailsService;

    @BeforeEach
    void setUp() {
        userCrnByResourceId = null;
    }

    @Test
    void threadBasedUser() {
        Map<String, Object> result = ThreadBasedUserCrnProvider.doAs("user", () -> underTest.createEventParameters(RESOURCE_ID));

        assertEquals("user", result.get(FlowConstants.FLOW_TRIGGER_USERCRN));
    }

    @Test
    void getUserCrnByResourceIdWhenNoThreadBasedUser() {
        userCrnByResourceId = "user2";
        Map<String, Object> result = underTest.createEventParameters(RESOURCE_ID);

        assertEquals(userCrnByResourceId, result.get(FlowConstants.FLOW_TRIGGER_USERCRN));
    }

    @Test
    void emptyWhenNoThreadBasedUserAndNoGetUserCrnByResourceId() {
        Map<String, Object> result = underTest.createEventParameters(RESOURCE_ID);

        assertEquals(Map.of(), result);
    }

    @Test
    void emptyWhenNoThreadBasedUserAndGetUserCrnByResourceIdDoesNotExistInUms() {
        userCrnByResourceId = "user2";
        when(crnUserDetailsService.getUmsUser(userCrnByResourceId)).thenThrow(RuntimeException.class);

        Map<String, Object> result = underTest.createEventParameters(RESOURCE_ID);

        assertEquals(Map.of(), result);
    }

    private static class FakeEventParameterFactory extends EventParameterFactory {
        FakeEventParameterFactory(CrnUserDetailsService crnUserDetailsService) {
            super(crnUserDetailsService);
        }

        @Override
        public Optional<String> getUserCrnByResourceId(Long resourceId) {
            return Optional.ofNullable(userCrnByResourceId);
        }
    }

}
