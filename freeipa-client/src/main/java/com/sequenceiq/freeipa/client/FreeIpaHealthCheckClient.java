package com.sequenceiq.freeipa.client;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.healthcheckmodel.CheckResult;
import com.sequenceiq.freeipa.client.healthcheckmodel.ClusterCheckResult;
import com.sequenceiq.freeipa.client.model.RPCMessage;
import com.sequenceiq.freeipa.client.model.RPCResponse;

public class FreeIpaHealthCheckClient implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaHealthCheckClient.class);

    private final Client restClient;

    private final WebTarget rpcTarget;

    private final MultivaluedMap<String, Object> headers;

    private final FreeIpaHealthCheckRpcListener listener;

    public FreeIpaHealthCheckClient(Client restClient, URL url, Map<String, String> headers, FreeIpaHealthCheckRpcListener listener) {
        this.restClient = restClient;
        this.headers = new MultivaluedHashMap<>(headers);
        this.listener = listener;

        rpcTarget = restClient.target(url.toString());
    }

    @Override
    public void close() throws Exception {
        restClient.close();
    }

    public RPCResponse<CheckResult> nodeHealth() throws FreeIpaClientException {
        return invoke("node health check", "", CheckResult.class);
    }

    public RPCResponse<ClusterCheckResult> clusterHealth() throws FreeIpaClientException {
        return invoke("cluster health from node", "/cluster", ClusterCheckResult.class);
    }

    private <T> RPCResponse<T> invoke(String name, String path, Class<T> resultType) throws FreeIpaClientException {
        Invocation.Builder builder = rpcTarget.path(path)
                .request()
                .headers(headers);
        try (Response response = builder.get()) {
            bufferResponseEntity(response);
            processRpcListener(response);
            checkResponseStatus(response);
            return toRpcResponse(name, response, resultType);
        } catch (FreeIpaClientException e) {
            throw e;
        } catch (Throwable throwable) {
            String message = String.format("Invoke FreeIPA health check failed: %s", throwable.getLocalizedMessage());
            LOGGER.warn(message);
            throw new FreeIpaClientException(message, throwable);
        }
    }

    private void bufferResponseEntity(Response response) throws FreeIpaClientException {
        if (!response.bufferEntity()) {
            throw new FreeIpaClientException("Unable to buffer the response from FreeIPA");
        }
    }

    private void processRpcListener(Response response) throws Exception {
        if (listener != null) {
            listener.onBeforeResponseProcessed(response);
        }
    }

    private void checkResponseStatus(Response response) throws FreeIpaClientException {
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL &&
                response.getStatus() != Response.Status.SERVICE_UNAVAILABLE.getStatusCode()) {
            String message = String.format("Invoke FreeIPA health check failed: %d", response.getStatus());
            LOGGER.warn("{}, reason: {}", message, response.readEntity(String.class));
            throw new FreeIpaClientException(message, response.getStatus());
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
