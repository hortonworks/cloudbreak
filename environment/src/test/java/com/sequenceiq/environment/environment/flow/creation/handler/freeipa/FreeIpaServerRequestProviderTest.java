package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.dns.EnvironmentBasedDomainNameProvider;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;

@ExtendWith(MockitoExtension.class)
class FreeIpaServerRequestProviderTest {

    private static final String ENV_NAME = "envName";

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private GrpcUmsClient grpcUmsClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EnvironmentBasedDomainNameProvider environmentBasedDomainNameProvider;

    @InjectMocks
    private FreeIpaServerRequestProvider underTest;

    @Test
    void testCreateWithLegacyDomain() {
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder().build();
        when(grpcUmsClient.getAccountDetails(ACCOUNT_ID, Optional.empty())).thenReturn(account);
        when(environmentBasedDomainNameProvider.getDomainName(ENV_NAME, "internal")).thenReturn("mydomain");

        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setName(ENV_NAME);
        FreeIpaServerRequest freeIpaServerRequest = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.create(environmentDto));

        assertEquals("mydomain", freeIpaServerRequest.getDomain());
    }

    @Test
    void testCreateWithDomainReturnedFromUms() {
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder().setWorkloadSubdomain("checkme").build();
        when(grpcUmsClient.getAccountDetails(ACCOUNT_ID, Optional.empty())).thenReturn(account);
        when(environmentBasedDomainNameProvider.getDomainName(ENV_NAME, "checkme")).thenReturn("checkme.mydomain");

        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setName(ENV_NAME);
        FreeIpaServerRequest freeIpaServerRequest = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.create(environmentDto));

        assertEquals("checkme.mydomain", freeIpaServerRequest.getDomain());
    }
}
