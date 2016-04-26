package com.sequenceiq.cloudbreak.orchestrator.onhost.client;

import static java.util.Collections.EMPTY_SET;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.onhost.domain.CbBootResponse;
import com.sequenceiq.cloudbreak.orchestrator.onhost.domain.CbBootResponses;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import com.sequenceiq.cloudbreak.util.KeyStoreUtil;


public class OnHostClient {

    private enum OnHostClientEndpoint {
        INFO("health"),
        SALT_PILLAR_SAVE("salt/server/pillar"),
        SALT_RUN_DISTRIBUTE("salt/run/distribute");

        private String url;

        OnHostClientEndpoint(String url) {
            this.url = url;
        }

        public String url() {
            return url;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(OnHostClient.class);

    private RestTemplate restTemplate;

    private GatewayConfig gatewayConfig;
    private Set<String> targets = new HashSet<>();
    private String port;

    public OnHostClient(GatewayConfig gatewayConfig, String port) {
        this(gatewayConfig, EMPTY_SET, port);
    }

    public OnHostClient(GatewayConfig gatewayConfig, Set<String> targets, String port) {
        this.gatewayConfig = gatewayConfig;
        this.targets = targets;
        this.port = port;
        try {
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(KeyStoreUtil.createTrustStore(gatewayConfig.getServerCert()), null)
                    .loadKeyMaterial(KeyStoreUtil.createKeyStore(gatewayConfig.getClientCert(), gatewayConfig.getClientKey()), "consul".toCharArray())
                    .build();

            RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
            registryBuilder.register("http", PlainConnectionSocketFactory.getSocketFactory());
            if (sslContext != null) {
                registryBuilder.register("https", new SSLConnectionSocketFactory(sslContext));
            }
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registryBuilder.build());
            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
            CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).setConnectionManager(connectionManager).build();
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
            this.restTemplate = new RestTemplate(requestFactory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create rest client with 2-way-ssl config", e);
        }
    }

    public Set<String> getTargets() {
        return targets;
    }

    private String getGatewayPrivateIp() {
        return gatewayConfig.getPrivateAddress();
    }

    public String getGatewayPublicIp() {
        return gatewayConfig.getPublicAddress();
    }

    private HttpHeaders httpHeaders() {
        String plainCreds = "cbadmin:cbadmin";
        byte[] plainCredsBytes = plainCreds.getBytes();
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64Creds);
        return headers;
    }

    private HttpEntity httpEntity(Map<String, Object> config) throws JsonProcessingException {
        return new HttpEntity<>(JsonUtil.writeValueAsString(config), httpHeaders());
    }

    private <T extends Class> ResponseEntity exchange(Map<String, Object> entity, HttpMethod method, String port,
            OnHostClientEndpoint onHostClientEndpoint, T clazz) throws Exception {
        String endpoint = String.format("https://%s:%s/cbboot/%s", gatewayConfig.getPublicAddress(), port, onHostClientEndpoint.url());
        return restTemplate.exchange(endpoint, method, httpEntity(entity), clazz);
    }

    private <T extends Class> ResponseEntity exchange(HttpMethod method, String port, OnHostClientEndpoint onHostClientEndpoint, T clazz) throws Exception {
        return exchange(new HashMap<>(), method, port, onHostClientEndpoint, clazz);
    }


    public boolean info() {
        try {
            ResponseEntity<String> response = exchange(HttpMethod.GET, port, OnHostClientEndpoint.INFO, String.class);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public boolean copySaltPillar(String path, Map<String, Object> consulConfig) throws Exception {
        LOGGER.info("Sending request to save pillar config");
        Map<String, Object> map = new HashMap<>();
        map.put("path", path);
        map.put("json", consulConfig);
        ResponseEntity<CbBootResponse> exchange = exchange(map, HttpMethod.POST, port, OnHostClientEndpoint.SALT_PILLAR_SAVE, CbBootResponse.class);
        CbBootResponse exchangeBody = exchange.getBody();
        LOGGER.info("Pillar save response: {}", exchangeBody);
        return 200 == exchangeBody.getStatusCode();
    }

    public Set<String> startSaltServiceOnTargetMachines(Set<String> targetIps, Set<String> consulServers) throws CloudbreakOrchestratorException {
        Set<String> missingTargets = new HashSet<>();
        Map<String, Object> map = new HashMap<>();
        Set<String> minionsTargets = new HashSet<>(targetIps);
        ArrayList<Map<String, Object>> minions = new ArrayList<>();
        if (minionsTargets.contains(getGatewayPrivateIp())) {
            map.put("server", getGatewayPrivateIp());
            String[] roles = {"consul_server", "ambari_server", "ambari_agent"};
            minions.add(minionConfig(getGatewayPrivateIp(), roles));
        }
        for (String minionIp : targetIps) {
            if (!minionIp.equals(getGatewayPrivateIp())) {
                Set<String> roles = new HashSet<>();
                roles.add("ambari_agent");
                if (consulServers.contains(minionIp)) {
                    roles.add("consul_server");
                } else {
                    roles.add("consul_agent");
                }
                minions.add(minionConfig(minionIp, roles.toArray(new String[roles.size()])));
            }
        }
        map.put("minions", minions);

        try {
            ResponseEntity<CbBootResponses> response = exchange(map, HttpMethod.POST, port, OnHostClientEndpoint.SALT_RUN_DISTRIBUTE, CbBootResponses.class);
            CbBootResponses responseBody = response.getBody();
            LOGGER.info("Salt run response: {}", responseBody);
            for (CbBootResponse cbBootResponse : responseBody.getResponses()) {
                if (cbBootResponse.getStatusCode() != HttpStatus.OK.value()) {
                    LOGGER.info("Successfully distributed salt run to: " + cbBootResponse.getAddress());
                    missingTargets.add(cbBootResponse.getAddress().split(":")[0]);
                }
            }
            if (!missingTargets.isEmpty()) {
                LOGGER.info("Missing nodes to run salt: %s", missingTargets);
            }
        } catch (Exception e) {
            LOGGER.info("Error occured when ran salt on hosts: ", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
        return missingTargets;
    }

    private Map<String, Object> minionConfig(String address, String[] roles) {
        Map<String, Object> minion = new HashMap<>();
        minion.put("address", address);
        minion.put("roles", roles);
        return minion;
    }


}
