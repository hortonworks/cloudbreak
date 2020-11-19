package com.sequenceiq.mock.clouderamanager;

import java.util.List;

import com.sequenceiq.mock.swagger.model.ApiProductVersion;

public class ClouderaManagerDto {

    private String mockUuid;

    private List<ApiProductVersion> product;

    public ClouderaManagerDto(String mockUuid) {
        this.mockUuid = mockUuid;
    }

    public String getMockUuid() {
        return mockUuid;
    }

    public List<ApiProductVersion> getProduct() {
        return product;
    }

    public void setProduct(List<ApiProductVersion> product) {
        this.product = product;
    }
}
