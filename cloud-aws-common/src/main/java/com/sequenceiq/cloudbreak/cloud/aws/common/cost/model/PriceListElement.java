package com.sequenceiq.cloudbreak.cloud.aws.common.cost.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceListElement {

    private Product product;

    private String serviceCode;

    private Terms terms;

    private String version;

    private Date publicationDate;

    public Product getProduct() {
        return product;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public Terms getTerms() {
        return terms;
    }

    public String getVersion() {
        return version;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }
}
