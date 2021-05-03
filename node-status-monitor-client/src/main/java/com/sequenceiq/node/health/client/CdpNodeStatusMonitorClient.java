package com.sequenceiq.node.health.client;

import java.net.URL;
import java.util.Base64;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.sequenceiq.cloudbreak.client.RPCMessage;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.client.RpcListener;
import com.sequenceiq.node.health.client.model.CdpNodeStatusRequest;
import com.sequenceiq.node.health.client.model.CdpNodeStatuses;

public class CdpNodeStatusMonitorClient implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdpNodeStatusMonitorClient.class);

    private final Client restClient;

    private final WebTarget rpcTarget;

    private final MultivaluedMap<String, Object> headers;

    private final RpcListener listener;

    public CdpNodeStatusMonitorClient(Client restClient, URL url, Map<String, String> headers, RpcListener listener, Optional<String> username,
            Optional<String> password) {
        this.restClient = restClient;
        this.headers = new MultivaluedHashMap<>(headers);
        this.listener = listener;
        addBasicAuthHeader(username, password);
        rpcTarget = restClient.target(url.toString());
    }

    private void addBasicAuthHeader(Optional<String> username, Optional<String> password) {
        if (username.isPresent() && password.isPresent()) {
            String base64AuthStr = Base64.getEncoder().encodeToString(String.format("%s:%s", username.get(), password.get()).getBytes());
            this.headers.add("Authorization", "Basic " + base64AuthStr);
        }
    }

    @Override
    public void close() {
        restClient.close();
    }

    public CdpNodeStatuses nodeStatusReport(CdpNodeStatusRequest request) throws CdpNodeStatusMonitorClientException {
        CdpNodeStatuses.Builder responseBuilder = CdpNodeStatuses.Builder.builder()
                .withNetworkReport(nodeNetworkReport(true))
                .withServicesReport(nodeServicesReport(true))
                .withSystemMetricsReport(systemMetricsReport(true));
        if (request.isMetering()) {
            responseBuilder.withMeteringReport(nodeMeteringReport(true));
        }
        if (request.isCmMonitoring()) {
            responseBuilder.withCmMetricsReport(cmMetricsReport(true));
        }
        return responseBuilder.build();
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> nodeMeteringReport() throws CdpNodeStatusMonitorClientException {
        return nodeMeteringReport(false);
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> nodeMeteringReport(boolean acceptNotFound) throws CdpNodeStatusMonitorClientException {
        return invoke("node metering check", "/api/v1/metering", nodeStatusReportBuilderFunction(), acceptNotFound);
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> nodeNetworkReport() throws CdpNodeStatusMonitorClientException {
        return nodeNetworkReport(false);
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> nodeNetworkReport(boolean acceptNotFound) throws CdpNodeStatusMonitorClientException {
        return invoke("node network check", "/api/v1/network", nodeStatusReportBuilderFunction(), acceptNotFound);
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> nodeServicesReport() throws CdpNodeStatusMonitorClientException {
        return nodeServicesReport(false);
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> nodeServicesReport(boolean acceptNotFound) throws CdpNodeStatusMonitorClientException {
        return invoke("node services check", "/api/v1/services", nodeStatusReportBuilderFunction(), acceptNotFound);
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> systemMetricsReport() throws CdpNodeStatusMonitorClientException {
        return systemMetricsReport(false);
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> systemMetricsReport(boolean acceptNotFound) throws CdpNodeStatusMonitorClientException {
        return invoke("node system metrics check", "/api/v1/system/metrics", nodeStatusReportBuilderFunction(), acceptNotFound);
    }

    public RPCResponse<NodeStatusProto.SaltHealthReport> saltReport() throws CdpNodeStatusMonitorClientException {
        return saltReport(false);
    }

    public RPCResponse<NodeStatusProto.SaltHealthReport> saltReport(boolean acceptNotFound) throws CdpNodeStatusMonitorClientException {
        return invoke("node salt health check", "/api/v1/salt", saltHealthBuilderFunction(), acceptNotFound);
    }

    public RPCResponse<NodeStatusProto.SaltHealthReport> saltPing(boolean acceptNotFound) throws CdpNodeStatusMonitorClientException {
        return invoke("node salt ping", "/api/v1/salt/ping", saltHealthBuilderFunction(), acceptNotFound);
    }

    public RPCResponse<NodeStatusProto.CmMetricsReport> cmMetricsReport() throws CdpNodeStatusMonitorClientException {
        return cmMetricsReport(false);
    }

    public RPCResponse<NodeStatusProto.CmMetricsReport> cmMetricsReport(boolean acceptNotFound) throws CdpNodeStatusMonitorClientException {
        return invoke("node cm metrics check", "/api/v1/cm_services/metrics", cmMetricsReportBuilderFunction(), acceptNotFound);
    }

    private <T> RPCResponse<T> invoke(String name, String path, Function<String, T> buildProtoFunction, boolean acceptNotFound)
            throws CdpNodeStatusMonitorClientException {
        Builder builder = rpcTarget.path(path)
                .request()
                .headers(headers);
        try (Response response = builder.get()) {
            bufferResponseEntity(response);
            processRpcListener(response);
            checkResponseStatus(response);
            return toRpcResponse(name, response, buildProtoFunction);
        } catch (CdpNodeStatusMonitorClientException e) {
            if (acceptNotFound) {
                LOGGER.debug("Get 404 from node status response for {}, but it is accepted as an empty response.", path);
                return null;
            }
            throw e;
        } catch (Throwable throwable) {
            String message = String.format("Invoke node status check failed: %s", throwable.getLocalizedMessage());
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
            String message = String.format("Invoke node status check check failed: %d", response.getStatus());
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

    private  Function<String, NodeStatusProto.CmMetricsReport> cmMetricsReportBuilderFunction() {
        return message -> {
            NodeStatusProto.CmMetricsReport.Builder builder =
                    NodeStatusProto.CmMetricsReport.newBuilder();
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
