package com.sequenceiq.node.health.client;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import com.sequenceiq.cloudbreak.client.RPCMessage;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.client.RpcListener;
import com.sequenceiq.node.health.client.model.HealthReport;
import com.sequenceiq.node.health.client.model.SaltReport;

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

    public RPCResponse<HealthReport> nodeMeteringReport() throws CdpNodeStatusMonitorClientException {
        return invoke("node metering check", "/report/metering_report.json", HealthReport.class);
    }

    public RPCResponse<HealthReport> nodeNetworkReport() throws CdpNodeStatusMonitorClientException {
        return invoke("node network check", "/report/network_report.json", HealthReport.class);
    }

    public RPCResponse<HealthReport> nodeServicesReport() throws CdpNodeStatusMonitorClientException {
        return invoke("node services check", "/report/services_report.json", HealthReport.class);
    }

    public RPCResponse<SaltReport> saltReport() throws CdpNodeStatusMonitorClientException {
        return invoke("node services check", "/report/salt_report.json", SaltReport.class);
    }

    private <T> RPCResponse<T> invoke(String name, String path, Class<T> resultType) throws CdpNodeStatusMonitorClientException {
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
            return toRpcResponse(name, response, resultType);
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

    private <T> RPCResponse<T> toRpcResponse(String name, Response response, Class<T> resultType) {
        T resultResponse = response.readEntity(resultType);
        RPCResponse<T> rpcResponse = new RPCResponse<>();
        RPCMessage rpcMessage = new RPCMessage();
        rpcMessage.setName(name);
        rpcMessage.setCode(response.getStatus());
        rpcMessage.setMessage(response.readEntity(String.class));
        rpcResponse.setSummary(name);
        rpcResponse.setMessages(List.of(rpcMessage));
        rpcResponse.setResult(resultResponse);
        rpcResponse.setCount(1);
        rpcResponse.setTruncated(Boolean.FALSE);
        return rpcResponse;
    }
}
