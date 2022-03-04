package com.sequenceiq.cloudbreak.cloud.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNoSqlTablesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNoSqlTablesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTable;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class CloudParameterServiceTest {

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @InjectMocks
    private CloudParameterService underTest;

    @Test
    void getNoSqlTables() {
        CloudNoSqlTables expected = new CloudNoSqlTables(List.of(new CloudNoSqlTable("a"), new CloudNoSqlTable("b")));
        GetPlatformNoSqlTablesResult response = new GetPlatformNoSqlTablesResult(1L, expected);
        doAnswer(invocation -> {
            Event<GetPlatformNoSqlTablesRequest> ev = invocation.getArgument(1);
            ev.getData().getResult().onNext(response);
            return null;
        }).when(eventBus).notify(anyString(), any(Event.class));
        CloudNoSqlTables noSqlTables = underTest.getNoSqlTables(
                new ExtendedCloudCredential(
                        new CloudCredential("id", "name", "acc"), "aws", "desc", "crn",
                        "account", new ArrayList<>()),
                "region",
                "aws",
                null);
        assertEquals(expected, noSqlTables);
    }
}
