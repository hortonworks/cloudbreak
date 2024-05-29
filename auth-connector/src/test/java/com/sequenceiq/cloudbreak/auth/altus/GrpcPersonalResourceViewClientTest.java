package com.sequenceiq.cloudbreak.auth.altus;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.exception.UnauthorizedException;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@ExtendWith(MockitoExtension.class)
public class GrpcPersonalResourceViewClientTest {

    private static final String USER_CRN = CrnTestUtil.getUserCrnBuilder()
            .setResource("user")
            .setAccountId("acc")
            .build().toString();

    private static final String RESOURCE_CRN = CrnTestUtil.getEnvironmentCrnBuilder()
            .setResource(UUID.randomUUID().toString())
            .setAccountId("account")
            .build().toString();

    @Mock
    private PersonalResourceViewClient personalResourceViewClient;

    @InjectMocks
    private GrpcPersonalResourceViewClient underTest;

    @BeforeEach
    void setUp() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "personalResourceViewClient", personalResourceViewClient, true);
    }

    @Test
    void testNotFound() {
        when(personalResourceViewClient.hasResourcesByRight(any(), any(), any())).thenThrow(new StatusRuntimeException(Status.NOT_FOUND));
        assertThrows(UnauthorizedException.class, () -> underTest.hasRightOnResources(USER_CRN, "any", List.of(RESOURCE_CRN)));
    }
}
