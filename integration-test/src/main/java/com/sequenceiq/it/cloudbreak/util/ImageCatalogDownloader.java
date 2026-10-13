package com.sequenceiq.it.cloudbreak.util;

import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Component
public class ImageCatalogDownloader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T> T download(String catalogUrl, Class<T> catalogType) {
        if (StringUtils.isBlank(catalogUrl)) {
            throw new TestFailException("Image catalog URL is not configured");
        }
        try (Response response = RestClientUtil.get().target(catalogUrl).request().get()) {
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new TestFailException(String.format("Failed to download image catalog %s: HTTP %d", catalogUrl, response.getStatus()));
            }
            return objectMapper.readValue(response.readEntity(String.class), catalogType);
        } catch (JsonProcessingException e) {
            throw new TestFailException(String.format("Failed to parse image catalog %s", catalogUrl), e);
        }
    }
}
