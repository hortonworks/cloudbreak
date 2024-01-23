package com.sequenceiq.cloudbreak.metrics.processor;

import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

import org.xerial.snappy.Snappy;

import com.sequenceiq.cloudbreak.streaming.model.StreamProcessingException;
import com.sequenceiq.cloudbreak.streaming.processor.RecordWorker;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MetricsRecordWorker extends RecordWorker<MetricsRecordProcessor, MetricsProcessorConfiguration, MetricsRecordRequest> {

    private static final Integer STATUS_OK = 200;

    private static final Integer LAST_REDIRECT_CODE = 399;

    private final OkHttpClient client;

    private final String remoteWriteUrlUnformatted;

    private final String remoteWritePaasUrlUnformatted;

    public MetricsRecordWorker(String name, String serviceName, MetricsRecordProcessor recordProcessor, BlockingDeque<MetricsRecordRequest> processingQueue,
            MetricsProcessorConfiguration configuration) {
        super(name, serviceName, recordProcessor, processingQueue, configuration);
        client = createClient();
        remoteWriteUrlUnformatted = configuration.getRemoteWriteUrl().replace("$accountid", "%s");
        remoteWritePaasUrlUnformatted = configuration.getRemotePaasWriteUrl().replace("$accountid", "%s");
    }

    @Override
    public void processRecordInput(MetricsRecordRequest input) throws StreamProcessingException {
        try {
            byte[] compressed = Snappy.compress(input.getWriteRequest().toByteArray());
            MediaType mediaType = MediaType.parse("application/x-protobuf");
            RequestBody body = RequestBody.create(mediaType, compressed);
            String unformattedEndpoint = input.isSaas() ? remoteWriteUrlUnformatted : remoteWritePaasUrlUnformatted;
            Request request = new Request.Builder()
                    .url(String.format(unformattedEndpoint, input.getAccountId()))
                    .addHeader("Content-Encoding", "snappy")
                    .addHeader("User-Agent", "cb-prometheus-java-client")
                    .addHeader("X-Prometheus-Remote-Write-Version", "0.1.0")
                    .addHeader("THANOS-TENANT", input.getAccountId())
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            if (response.code() < STATUS_OK || response.code() > LAST_REDIRECT_CODE) {
                throw new StreamProcessingException(String.format("Response code is not valid. (status code: %s)", response.code()));
            }
            if (response.body() != null) {
                response.body().close();
            }
        } catch (IOException e) {
            throw new StreamProcessingException(e);
        }
    }

    @Override
    public void onInterrupt() {
    }

    private OkHttpClient createClient() {
        Integer timeout = getConfiguration().getHttpTimeout();
        return new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }
}
