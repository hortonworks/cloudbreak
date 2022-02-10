package com.sequenceiq.cloudbreak.cloudwatch.processor;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingDeque;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import com.amazonaws.services.logs.model.CreateLogStreamResult;
import com.amazonaws.services.logs.model.DescribeLogStreamsRequest;
import com.amazonaws.services.logs.model.DescribeLogStreamsResult;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.LogStream;
import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.cloudwatch.config.CloudwatchConfiguration;
import com.sequenceiq.cloudbreak.cloudwatch.model.CloudwatchRecordRequest;

import io.opentracing.Tracer;

@ExtendWith(MockitoExtension.class)
public class CloudwatchRecordWorkerTest {

    @Mock
    private AbstractCloudwatchRecordProcessor recordProcessor;

    @Mock
    private BlockingDeque<CloudwatchRecordRequest> blockingDeque;

    @Mock
    private Tracer tracer;

    @Mock
    private AWSLogsClient awsLogsClient;

    private CloudwatchRecordWorker underTest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        CloudwatchConfiguration configuration = new CloudwatchConfiguration(
                true, 1, 1, "TestGroup", "TestStream", "TestRegion", 1);
        underTest = new CloudwatchRecordWorker("Test", "TestService", recordProcessor, blockingDeque, configuration, tracer);
        Whitebox.setInternalState(underTest, "awsLogsClient", awsLogsClient);
    }

    @Test
    public void testDescribeLogStreamsWhenLogStreamExists() {
        DescribeLogStreamsRequest describeLogStreamsRequest = new DescribeLogStreamsRequest();
        describeLogStreamsRequest.setLogGroupName("TestGroup");
        describeLogStreamsRequest.setLogStreamNamePrefix(underTest.initLogStream("TestStream"));
        DescribeLogStreamsResult describeLogStreamsResult = new DescribeLogStreamsResult();
        Set<LogStream> logStreams = new HashSet<>();
        LogStream logStream = new LogStream();
        logStreams.add(logStream);
        describeLogStreamsResult.setLogStreams(logStreams);
        when(awsLogsClient.describeLogStreams(describeLogStreamsRequest)).thenReturn(describeLogStreamsResult);

        underTest.describeLogStreams();

        verify(awsLogsClient, times(1)).describeLogStreams(describeLogStreamsRequest);
    }

    @Test
    public void testDescribeLogStreamsWhenLogStreamDoesNotExist() {
        DescribeLogStreamsRequest describeLogStreamsRequest = new DescribeLogStreamsRequest();
        describeLogStreamsRequest.setLogGroupName("TestGroup");
        describeLogStreamsRequest.setLogStreamNamePrefix(underTest.initLogStream("TestStream"));
        DescribeLogStreamsResult describeLogStreamsResult = new DescribeLogStreamsResult();
        Set<LogStream> logStreams = new HashSet<>();
        describeLogStreamsResult.setLogStreams(logStreams);
        when(awsLogsClient.describeLogStreams(describeLogStreamsRequest)).thenReturn(describeLogStreamsResult);
        when(awsLogsClient.describeLogStreams(describeLogStreamsRequest)).thenReturn(describeLogStreamsResult);

        CreateLogStreamRequest createLogStreamRequest = new CreateLogStreamRequest();
        createLogStreamRequest.setLogGroupName("TestGroup");
        createLogStreamRequest.setLogStreamName(underTest.initLogStream("TestStream"));
        when(awsLogsClient.createLogStream(createLogStreamRequest)).thenReturn(new CreateLogStreamResult());

        underTest.describeLogStreams();

        verify(awsLogsClient, times(2)).describeLogStreams(describeLogStreamsRequest);
        verify(awsLogsClient, times(1)).createLogStream(createLogStreamRequest);
    }

    @Test
    void testCreateLogUsesEventsMessageBodyIfItsPresentAndRawIsNotForced() {
        CloudwatchRecordRequest input = CloudwatchRecordRequest.Builder.newBuilder()
                .withRawBody("TestRawData")
                .withMessageBody(UsageProto.Event.newBuilder().setId("TestMessageBodyId").build())
                .withTimestamp(1L)
                .withForceRawOutput(false)
                .build();

        Collection<InputLogEvent> inputLogEvents = underTest.createLogEvents(input);

        Assertions.assertEquals("{\"id\":\"TestMessageBodyId\"}", inputLogEvents.stream().findFirst().get().getMessage());
    }

    @Test
    void testCreateLogEventsForcedToUseRawDataEvenIfMessageBodyIsPresent() {
        CloudwatchRecordRequest input = CloudwatchRecordRequest.Builder.newBuilder()
                .withRawBody("TestRawData")
                .withMessageBody(UsageProto.Event.newBuilder().setId("TestMessageBodyId").build())
                .withTimestamp(1L)
                .withForceRawOutput(true)
                .build();

        Collection<InputLogEvent> inputLogEvents = underTest.createLogEvents(input);

        Assertions.assertEquals("TestRawData", inputLogEvents.stream().findFirst().get().getMessage());
    }
}
