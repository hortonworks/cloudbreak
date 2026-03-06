package com.sequenceiq.environment.environment.service.stack;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.exception.StackOperationFailedException;

@ExtendWith(MockitoExtension.class)
class StackServiceTest {

    private static final String USERCRN = CrnTestUtil.getUserCrnBuilder()
            .setAccountId("acc")
            .setResource("user")
            .build().toString();

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private StackService underTest;

    @Test
    void modifyUserDefinedTags() {
        String resourceCrn = "resourceCrn";
        Map<String, String> userDefinedTags = Map.of("owner", "john doe");

        ThreadBasedUserCrnProvider.doAs(USERCRN, () -> underTest.modifyUserDefinedTags(resourceCrn, userDefinedTags));

        verify(stackV4Endpoint).modifyUserDefinedTagsInternal(0L, resourceCrn, userDefinedTags);
    }

    @Test
    void modifyUserDefinedTagsFailureTest() {
        String resourceCrn = "resourceCrn";
        Map<String, String> userDefinedTags = Map.of("owner", "john doe");

        doThrow(new WebApplicationException("Error")).when(stackV4Endpoint).modifyUserDefinedTagsInternal(0L, resourceCrn, userDefinedTags);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("custom error");

        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USERCRN, () -> underTest.modifyUserDefinedTags(resourceCrn, userDefinedTags)))
                .hasMessage("custom error")
                .isExactlyInstanceOf(StackOperationFailedException.class);
    }
}