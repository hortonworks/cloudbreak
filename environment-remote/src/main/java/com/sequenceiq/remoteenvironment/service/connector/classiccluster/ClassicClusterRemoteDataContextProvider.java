package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.cdp.servicediscovery.model.ApiRemoteDataContext;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.remoteenvironment.exception.OnPremCMApiException;

@Component
class ClassicClusterRemoteDataContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassicClusterRemoteDataContextProvider.class);

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().
            defaultSetterInfo(JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY)).build();

    @Inject
    private ClassicClusterClouderaManagerApiClientProvider apiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    public DescribeDatalakeAsApiRemoteDataContextResponse getRemoteDataContext(OnPremisesApiProto.Cluster cluster) {
        try {
            ApiClient apiClient = apiClientProvider.getClouderaManagerRootClient(cluster);
            com.cloudera.api.swagger.model.ApiRemoteDataContext remoteDataContext =
                    clouderaManagerApiFactory.getCdpResourceApi(apiClient).getRemoteContextByCluster(cluster.getName());

            DescribeDatalakeAsApiRemoteDataContextResponse response = new DescribeDatalakeAsApiRemoteDataContextResponse();
            response.setDatalake(cluster.getClusterCrn());
            response.setContext(convert(remoteDataContext));
            return response;
        } catch (ApiException e) {
            String message = "Failed to get remote data context from Cloudera Manager";
            LOGGER.error(message, e);
            throw new OnPremCMApiException(message, e);
        } catch (JsonProcessingException e) {
            LOGGER.error("Json processing failed, thus we cannot query remote data context", e);
            throw new OnPremCMApiException("Failed to process remote data context. Please contact Cloudera support to get this resolved.");
        }
    }

    protected ApiRemoteDataContext convert(com.cloudera.api.swagger.model.ApiRemoteDataContext remoteContext) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(JsonUtil.writeValueAsString(remoteContext), ApiRemoteDataContext.class);
    }
}
