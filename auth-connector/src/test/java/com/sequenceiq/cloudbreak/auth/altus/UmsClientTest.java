package com.sequenceiq.cloudbreak.auth.altus;

import static com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator.regionalAwareInternalCrnGenerator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.paging.PagingProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementGrpc;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;

@ExtendWith(MockitoExtension.class)
class UmsClientTest {

    private static final String ACCOUNT_ID = "account";

    private static final String RESOURCE_CRN = CrnTestUtil.getEnvironmentCrnBuilder()
            .setResource(UUID.randomUUID().toString())
            .setAccountId(ACCOUNT_ID)
            .build().toString();

    private static final PagingProto.PageToken SECOND_PAGE_TOKEN = PagingProto.PageToken.newBuilder()
            .putExclusiveStartKeyStringAttrs("assigneeCrn", "assignee-page-1")
            .build();

    @Mock
    private UmsClientConfig umsClientConfig;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    private Server server;

    private ManagedChannel channel;

    private PagingUserManagementService userManagementService;

    private UmsClient underTest;

    @BeforeEach
    void setUp() throws IOException {
        lenient().when(umsClientConfig.getGrpcTimeoutSec()).thenReturn(60L);
        lenient().when(umsClientConfig.getCallingServiceName()).thenReturn("cloudbreak");
        lenient().when(regionAwareInternalCrnGeneratorFactory.iam())
                .thenReturn(regionalAwareInternalCrnGenerator(Crn.Service.IAM, "cdp", "us-west-1"));

        userManagementService = new PagingUserManagementService();
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(userManagementService)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        underTest = new UmsClient(channel, umsClientConfig, regionAwareInternalCrnGeneratorFactory);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        channel.shutdownNow();
        server.shutdownNow();
        channel.awaitTermination(5, TimeUnit.SECONDS);
        server.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void listResourceAssigneesFollowsThePageTokenAndTerminates() {
        List<UserManagementProto.ResourceAssignee> result = underTest.listResourceAssignees(RESOURCE_CRN);

        assertEquals(List.of("assignee-page-1", "assignee-page-2"),
                result.stream().map(UserManagementProto.ResourceAssignee::getAssigneeCrn).toList());
        assertEquals(2, userManagementService.requests.size());
        assertFalse(userManagementService.requests.get(0).hasPageToken());
        assertTrue(userManagementService.requests.get(1).hasPageToken());
        assertEquals(SECOND_PAGE_TOKEN, userManagementService.requests.get(1).getPageToken());
        assertEquals(ACCOUNT_ID, userManagementService.requests.get(0).getAccountId());
        assertEquals(RESOURCE_CRN, userManagementService.requests.get(0).getResourceCrn());
    }

    /**
     * Serves two pages: the first carries a next page token, the second does not. A client that does not forward the
     * token keeps asking for the first page forever, so the second such request is answered with an error to fail the
     * test immediately instead of looping until the timeout.
     */
    private static final class PagingUserManagementService extends UserManagementGrpc.UserManagementImplBase {

        private final List<UserManagementProto.ListResourceAssigneesRequest> requests = new ArrayList<>();

        private boolean firstPageServed;

        @Override
        public void listResourceAssignees(UserManagementProto.ListResourceAssigneesRequest request,
                StreamObserver<UserManagementProto.ListResourceAssigneesResponse> responseObserver) {
            requests.add(request);
            UserManagementProto.ListResourceAssigneesResponse.Builder response =
                    UserManagementProto.ListResourceAssigneesResponse.newBuilder();
            if (!request.hasPageToken()) {
                if (firstPageServed) {
                    responseObserver.onError(Status.FAILED_PRECONDITION
                            .withDescription("The first page was requested twice, so the next page token was not forwarded.")
                            .asRuntimeException());
                    return;
                }
                firstPageServed = true;
                response.addResourceAssignee(assignee("assignee-page-1")).setNextPageToken(SECOND_PAGE_TOKEN);
            } else {
                response.addResourceAssignee(assignee("assignee-page-2"));
            }
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }

        private UserManagementProto.ResourceAssignee assignee(String assigneeCrn) {
            return UserManagementProto.ResourceAssignee.newBuilder().setAssigneeCrn(assigneeCrn).build();
        }
    }
}
