package com.sequenceiq.cloudbreak.service.workspace;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceRepository;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceServiceTest {

    private static final String WORKSPACE_NAME = "test-workspace";

    private static final String TENANT_NAME = "test-tenant";

    private static final String USER_ID_2 = "user2";

    private static final String USER_ID_3 = "user3";

    @Spy
    private final TransactionService transactionService = new TransactionService();

    @Mock
    private WorkspaceRepository workspaceRepository;

    @InjectMocks
    private final WorkspaceService underTest = new WorkspaceService();

    private final Tenant testTenant = new Tenant();

    private final Workspace testWorkspace = new Workspace();

    private final User initiator = new User();

    @Before
    public void setup() throws TransactionExecutionException {
        initiator.setId(1L);
        initiator.setUserId("initiator");
        initiator.setTenant(testTenant);
        initiator.setUserCrn("crn:cdp:iam:us-west-1:1:user:1");
        testTenant.setId(1L);
        testTenant.setName(TENANT_NAME);
        testWorkspace.setName(WORKSPACE_NAME);
        testWorkspace.setId(1L);
        testWorkspace.setTenant(testTenant);
        testWorkspace.setResourceCrn("crn:cdp:iam:us-west-1:1:workspace:1");
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any());
    }

    @Test
    public void testWorkspaceCreation() {
        when(workspaceRepository.save(any())).thenReturn(testWorkspace);

        underTest.create(testWorkspace);
    }
}
