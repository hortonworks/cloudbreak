package com.sequenceiq.node.health.client;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.sequenceiq.cloudbreak.client.RPCMessage;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.client.RpcListener;

public class CdpNodeStatusMonitorClient implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdpNodeStatusMonitorClient.class);

    private final Client restClient;

    private final WebTarget rpcTarget;

    private final MultivaluedMap<String, Object> headers;

    private final RpcListener listener;

    private final Optional<String> username;

    private final Optional<String> password;

    public CdpNodeStatusMonitorClient(Client restClient, URL url, Map<String, String> headers, RpcListener listener, Optional<String> username,
            Optional<String> password) {
        this.restClient = restClient;
        this.headers = new MultivaluedHashMap<>(headers);
        this.listener = listener;
        this.username = username;
        this.password = password;

        rpcTarget = restClient.target(url.toString());
    }

    @Override
    public void close() {
        restClient.close();
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> nodeMeteringReport() throws CdpNodeStatusMonitorClientException {
        return invoke("node metering check", "/api/v1/metering", nodeStatusReportBuilderFunction());
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> nodeNetworkReport() throws CdpNodeStatusMonitorClientException {
        return invoke("node network check", "/api/v1/network", nodeStatusReportBuilderFunction());
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> nodeServicesReport() throws CdpNodeStatusMonitorClientException {
        return invoke("node services check", "/api/v1/services", nodeStatusReportBuilderFunction());
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> systemMetricsReport() throws CdpNodeStatusMonitorClientException {
        return invoke("node system metrics check", "/api/v1/system/metrics", nodeStatusReportBuilderFunction());
    }

    public RPCResponse<NodeStatusProto.SaltHealthReport> saltReport() throws CdpNodeStatusMonitorClientException {
        return invoke("node salt health check", "/api/v1/salt", saltHealthBuilderFunction());
    }

    private <T> RPCResponse<T> invoke(String name, String path, Function<String, T> buildProtoFunction)
            throws CdpNodeStatusMonitorClientException {
        Builder builder = rpcTarget.path(path)
                .request()
                .headers(headers);
        if (username.isPresent() && password.isPresent()) {
            builder.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, username.get());
            builder.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, password.get());
        }
        try (Response response = builder.get()) {
            bufferResponseEntity(response);
            processRpcListener(response);
            checkResponseStatus(response);
            return toRpcResponse(name, response, buildProtoFunction);
        } catch (CdpNodeStatusMonitorClientException e) {
            throw e;
        } catch (Throwable throwable) {
            String message = String.format("Invoke FreeIPA health check failed: %s", throwable.getLocalizedMessage());
            LOGGER.warn(message);
            throw new CdpNodeStatusMonitorClientException(message, throwable);
        }
    }

    private void bufferResponseEntity(Response response) throws CdpNodeStatusMonitorClientException {
        if (!response.bufferEntity()) {
            throw new CdpNodeStatusMonitorClientException("Unable to buffer the response from FreeIPA");
        }
    }

    private void processRpcListener(Response response) throws Exception {
        if (listener != null) {
            listener.onBeforeResponseProcessed(response);
        }
    }

    private void checkResponseStatus(Response response) throws CdpNodeStatusMonitorClientException {
        if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL &&
                response.getStatus() != Status.SERVICE_UNAVAILABLE.getStatusCode()) {
            String message = String.format("Invoke FreeIPA health check failed: %d", response.getStatus());
            LOGGER.warn("{}, reason: {}", message, response.readEntity(String.class));
            throw new CdpNodeStatusMonitorClientException(message, response.getStatus());
        }
    }

    private <T> RPCResponse<T> toRpcResponse(String name, Response response, Function<String, T> builderFunc) {
        RPCResponse<T> rpcResponse = new RPCResponse<>();
        String message = response.readEntity(String.class);
        rpcResponse.setResult(builderFunc.apply(message));
        RPCMessage rpcMessage = new RPCMessage();
        rpcMessage.setName(name);
        rpcMessage.setCode(response.getStatus());
        rpcMessage.setMessage(message);
        rpcResponse.setSummary(name);
        rpcResponse.setMessages(List.of(rpcMessage));
        rpcResponse.setCount(1);
        rpcResponse.setTruncated(Boolean.FALSE);
        return rpcResponse;
    }

    private Function<String, NodeStatusProto.NodeStatusReport> nodeStatusReportBuilderFunction() {
        return message -> {
            NodeStatusProto.NodeStatusReport.Builder builder =
                    NodeStatusProto.NodeStatusReport.newBuilder();
            try {
                JsonFormat.parser()
                        .ignoringUnknownFields()
                        .merge(message, builder);
                return builder.build();
            } catch (InvalidProtocolBufferException e) {
                // skip
            }
            return null;
        };
    }

    private Function<String, NodeStatusProto.SaltHealthReport> saltHealthBuilderFunction() {
        return message -> {
            NodeStatusProto.SaltHealthReport.Builder builder =
                    NodeStatusProto.SaltHealthReport.newBuilder();
            try {
                JsonFormat.parser()
                        .ignoringUnknownFields()
                        .merge(message, builder);
                return builder.build();
            } catch (InvalidProtocolBufferException e) {
                // skip
            }
            return null;
        };
    }
}
