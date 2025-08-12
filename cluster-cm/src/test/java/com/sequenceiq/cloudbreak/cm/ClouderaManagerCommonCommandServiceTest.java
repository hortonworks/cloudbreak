package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.util.CheckedFunction;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerCommonCommandServiceTest {

    private static final String COMMAND_NAME = "commandName";

    private static final String CLUSTER_NAME = "clusterName";

    private static final String STACK_CRN = "crn:cdp:datalake:us-west-1:acc1:datalake:cluster";

    @Mock
    private CheckedFunction<String, ApiCommand, ApiException> checkedFunction;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    private ClouderaManagerCommonCommandService underTest;

    private Stack stack;

    private List<ApiCommand> commands;

    @BeforeEach
    public void setUp() {
        underTest = new ClouderaManagerCommonCommandService();
        stack = new Stack();
        stack.setName(CLUSTER_NAME);
        stack.setResourceCrn(STACK_CRN);
        commands = new ArrayList<>();
    }

    @Test
    public void testGetApiCommand() throws ApiException {
        // GIVEN
        given(checkedFunction.apply(CLUSTER_NAME)).willReturn(new ApiCommand().name(COMMAND_NAME).id(BigDecimal.ONE));
        // WHEN
        underTest.getApiCommand(new ArrayList<>(), COMMAND_NAME, CLUSTER_NAME, checkedFunction);
        // THEN
        verify(checkedFunction, times(1)).apply(CLUSTER_NAME);
    }

    @Test
    public void testGetApiCommandAlreadyRunning() throws ApiException {
        // GIVEN
        List<ApiCommand> commands = new ApiCommandList().addItemsItem(new ApiCommand().id(BigDecimal.ONE).name(COMMAND_NAME)).getItems();
        // WHEN
        ApiCommand result = underTest.getApiCommand(commands, COMMAND_NAME, CLUSTER_NAME, checkedFunction);
        // THEN
        assertEquals(COMMAND_NAME, result.getName());
    }
}
