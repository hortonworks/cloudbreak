package com.sequenceiq.cloudbreak.cloudwatch.processor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import com.amazonaws.services.logs.model.DescribeLogStreamsRequest;
import com.amazonaws.services.logs.model.DescribeLogStreamsResult;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.ResourceNotFoundException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.sequenceiq.cloudbreak.cloudwatch.config.CloudwatchConfiguration;
import com.sequenceiq.cloudbreak.cloudwatch.model.CloudwatchRecordRequest;
import com.sequenceiq.cloudbreak.streaming.model.StreamProcessingException;
import com.sequenceiq.cloudbreak.streaming.processor.RecordWorker;

import io.opentracing.Tracer;

public class CloudwatchRecordWorker extends RecordWorker<AbstractCloudwatchRecordProcessor, CloudwatchConfiguration, CloudwatchRecordRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudwatchRecordWorker.class);

    private AWSLogs awsLogsClient;

    private final Tracer tracer;

    private final String logGroup;

    private final String logStream;

    private final int maxRetry;

    public CloudwatchRecordWorker(String name, String serviceName, AbstractCloudwatchRecordProcessor recordProcessor,
            BlockingDeque<CloudwatchRecordRequest> processingQueue, CloudwatchConfiguration configuration, Tracer tracer) {
        super(name, serviceName, recordProcessor, processingQueue, configuration);
        this.tracer = tracer;
        this.logGroup = configuration.getLogGroup();
        this.logStream = initLogStream(configuration.getLogStream());
        this.maxRetry = configuration.getMaxRetry();
    }

    @Override
    public void processRecordInput(CloudwatchRecordRequest input) throws StreamProcessingException {
        DescribeLogStreamsResult logStreamsResult = describeLogStreams();
        String sequenceToken = logStreamsResult.getLogStreams().get(0).getUploadSequenceToken();
        PutLogEventsRequest putLogEvents = new PutLogEventsRequest();
        putLogEvents.setSequenceToken(sequenceToken);
        putLogEvents.setLogGroupName(logGroup);
        putLogEvents.setLogStreamName(logStream);
        putLogEvents.setLogEvents(createLogEvents(input));
        getAwsLogsClient().putLogEvents(putLogEvents);
    }

    @Override
    public void onInterrupt() {
        if (awsLogsClient != null) {
            try {
                awsLogsClient.shutdown();
            } catch (Exception e) {
                LOGGER.error("Error during closing AWS cloudwatch client", e);
            }
        }
    }

    public AWSLogs getAwsLogsClient() {
        if (awsLogsClient == null) {
            ClientConfiguration clientConfig = new ClientConfiguration();
            clientConfig.setUseThrottleRetries(true);
            clientConfig.setMaxErrorRetry(maxRetry);
            awsLogsClient = AWSLogsClient.builder()
                    .withClientConfiguration(clientConfig)
                    .build();
        }
        return awsLogsClient;
    }

    private DescribeLogStreamsResult describeLogStreams() {
        DescribeLogStreamsRequest describeLogStreams = new DescribeLogStreamsRequest();
        describeLogStreams.setLogGroupName(logGroup);
        describeLogStreams.setLogStreamNamePrefix(logStream);
        try {
            return getAwsLogsClient().describeLogStreams(describeLogStreams);
        } catch (ResourceNotFoundException re) {
            CreateLogStreamRequest createLogStreamRequest = new CreateLogStreamRequest();
            createLogStreamRequest.setLogGroupName(logGroup);
            createLogStreamRequest.setLogStreamName(logStream);
            getAwsLogsClient().createLogStream(createLogStreamRequest);
            return getAwsLogsClient().describeLogStreams(describeLogStreams);
        }
    }

    private Collection<InputLogEvent> createLogEvents(CloudwatchRecordRequest input) {
        Collection<InputLogEvent> inputLogEvents = new ArrayList<>();
        String eventJson = null;
        if (input.getMessageBody().isPresent()) {
            try {
                eventJson = JsonFormat.printer()
                        .omittingInsignificantWhitespace().print(input.getMessageBody().get());
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalArgumentException("Error during transforming grpc record to json string", e);
            }
        } else if (input.getRawBody().isPresent()) {
            eventJson = input.getRawBody().get();
        }
        inputLogEvents.add(new InputLogEvent().withMessage(eventJson).withTimestamp(input.getTimestamp()));
        return inputLogEvents;
    }

    private String initLogStream(String logStream) {
        String fullLogStream = logStream;
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            fullLogStream = String.format("%s_%s", logStream, hostname);
        } catch (UnknownHostException e) {
            LOGGER.error("Error during log stream suffix initialization");
        }
        return fullLogStream;
    }
}
