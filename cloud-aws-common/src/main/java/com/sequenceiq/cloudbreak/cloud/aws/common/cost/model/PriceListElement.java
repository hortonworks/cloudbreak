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

    public PriceListElement() {
    }

    public PriceListElement(Product product, String serviceCode, Terms terms, String version, Date publicationDate) {
        this.product = product;
        this.serviceCode = serviceCode;
        this.terms = terms;
        this.version = version;
        this.publicationDate = publicationDate;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public Terms getTerms() {
        return terms;
    }

    public void setTerms(Terms terms) {
        this.terms = terms;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(Date publicationDate) {
        this.publicationDate = publicationDate;
    }
}
