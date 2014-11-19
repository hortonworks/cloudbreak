package com.sequenceiq.cloudbreak.service.stack.resource.azure.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.DescribeContextObject;

public class AzureDescribeContextObject extends DescribeContextObject {

    private AzureClient azureClient;
    private String commonName;

    public AzureDescribeContextObject(Long stackId, String commonName, AzureClient azureClient) {
        super(stackId);
        this.azureClient = azureClient;
        this.commonName = commonName;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public void setAzureClient(AzureClient azureClient) {
        this.azureClient = azureClient;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public synchronized AzureClient getNewAzureClient(AzureCredential credential) {
        return AzureStackUtil.createAzureClient(credential);
    }
}
