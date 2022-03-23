package com.sequenceiq.cloudbreak.sigmadbus;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sequenceiq.cloudbreak.sigmadbus.converter.DatabusRequestConverter.convert;
import static com.sequenceiq.cloudbreak.sigmadbus.converter.DatabusRequestConverter.getPayload;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.sigma.service.dbus.DbusProto;
import com.cloudera.sigma.service.dbus.SigmaDbusGrpc;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.cloudbreak.sigmadbus.config.SigmaDatabusConfig;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRecordProcessingException;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequest;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequestContext;
import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentracing.Tracer;

public class SigmaDatabusClient<D extends AbstractDatabusStreamConfiguration> implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SigmaDatabusClient.class);

    private final Tracer tracer;

    private final SigmaDatabusConfig sigmaDatabusConfig;

    private final D databusStreamConfiguration;

    private ManagedChannelWrapper managedChannelWrapper;

    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public SigmaDatabusClient(Tracer tracer,
            SigmaDatabusConfig sigmaDatabusConfig,
            D databusStreamConfiguration,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.tracer = tracer;
        this.sigmaDatabusConfig = sigmaDatabusConfig;
        this.databusStreamConfiguration = databusStreamConfiguration;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    /**
     * Upload data into databus. If the payload is larger than 1 MB, the data will be uploaded to cloudera S3.
     * @param request databus record payload input
     * @throws DatabusRecordProcessingException error during databus record processing
     */
    public void putRecord(DatabusRequest request) throws DatabusRecordProcessingException {
        ManagedChannelWrapper channelWrapper = getMessageWrapper();
        DbusProto.PutRecordRequest recordRequest = convert(request, databusStreamConfiguration);
        String requestId = MDCBuilder.getOrGenerateRequestId();
        LOGGER.debug("Creating databus request with request id: {}", requestId);
        buildMdcContext(request, requestId);
        DbusProto.PutRecordResponse recordResponse = newStub(channelWrapper.getChannel(),
                requestId, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString())
                .putRecord(recordRequest);
        DbusProto.Record.Reply.Status status = recordResponse.getRecord().getStatus();
        LOGGER.debug("Returned dbus record status is {}", status);
        if (DbusProto.Record.Reply.Status.SENT.equals(status)) {
            String recordId = recordResponse.getRecord().getRecordId();
            LOGGER.debug("Dbus record sucessfully processed with record id: {}", recordId);
        } else if (DbusProto.Record.Reply.Status.PENDING.equals(status)) {
            String recordId = recordResponse.getRecord().getRecordId();
            String s3BucketUrl = recordResponse.getRecord().getUploadUrl();
            LOGGER.debug("Dbus record can be uploaded to s3 [record id: {}], [s3 url: {}]", recordId, s3BucketUrl);
            uploadRecordToS3(s3BucketUrl, request, recordId);
        } else {
            throw new DatabusRecordProcessingException("Cannot process record to Sigma Databus.");
        }
    }

    private void uploadRecordToS3(String s3Url, DatabusRequest request, String recordId) throws DatabusRecordProcessingException {
            String payload = getPayload(request);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(new URI(s3Url))
                    .PUT(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            Response.Status.Family statusFamily = Response.Status.Family.familyOf(response.statusCode());
            LOGGER.debug("Databus (upload to S3) response status code: {}", response.statusCode());
            if (Response.Status.Family.SUCCESSFUL.equals(statusFamily) || Response.Status.Family.REDIRECTION.equals(statusFamily)) {
                LOGGER.debug("Databus record with id {} successfully uploaded to s3.", recordId);
            } else {
                throw new DatabusRecordProcessingException(String.format("S3 upload failed for databus record with id %s", recordId));
            }
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new DatabusRecordProcessingException(String.format("Error during uploading record with id %s to s3", recordId), e);
        }
    }

    /**
     * Creates Managed Channel wrapper from endpoint address
     *
     * @return the wrapper object
     */
    private ManagedChannelWrapper makeWrapper() {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(sigmaDatabusConfig.getHost(), sigmaDatabusConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build());
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param channel   channel
     * @param requestId the request ID
     * @param actorCrn  actor
     * @return the stub
     */
    private SigmaDbusGrpc.SigmaDbusBlockingStub newStub(ManagedChannel channel, String requestId, String actorCrn) {
        checkNotNull(requestId, "requestId should not be null.");
        return SigmaDbusGrpc.newBlockingStub(channel)
                .withInterceptors(GrpcUtil.getTimeoutInterceptor(sigmaDatabusConfig.getGrpcTimeoutSec().longValue()))
                .withInterceptors(GrpcUtil.getTracingInterceptor(tracer),
                        new AltusMetadataInterceptor(requestId, actorCrn));
    }

    @Override
    public void close() {
        if (managedChannelWrapper != null) {
            managedChannelWrapper.close();
        }
    }

    private ManagedChannelWrapper getMessageWrapper() {
        if (managedChannelWrapper == null) {
            managedChannelWrapper = makeWrapper();
        }
        return managedChannelWrapper;
    }

    private void buildMdcContext(DatabusRequest request, String requestId) {
        if (request.getContext().isPresent()) {
            DatabusRequestContext context = request.getContext().get();
            MdcContext.builder()
                    .requestId(requestId)
                    .tenant(context.getAccountId())
                    .environmentCrn(context.getEnvironmentCrn())
                    .resourceCrn(context.getResourceCrn())
                    .resourceName(context.getResourceName())
                    .buildMdc();
        }
    }
}
