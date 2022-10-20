package com.sequenceiq.environment.api.v1.proxy.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.proxy.ProxyConfigDescription;
import com.sequenceiq.environment.api.v1.proxy.model.ProxyBase;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ProxyConfigDescription.DESCRIPTION)
@JsonInclude(Include.NON_NULL)
public class ProxyResponse extends ProxyBase {

    @ApiModelProperty(ProxyConfigDescription.PROXY_CONFIG_ID)
    private String crn;

    @ApiModelProperty(ProxyConfigDescription.USERNAME)
    private SecretResponse userName;

    @ApiModelProperty(ProxyConfigDescription.PASSWORD)
    private SecretResponse password;

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    @ApiModelProperty(ModelDescriptions.CREATOR)
    private String creator;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public SecretResponse getUserName() {
        return userName;
    }

    public void setUserName(SecretResponse userName) {
        this.userName = userName;
    }

    public SecretResponse getPassword() {
        return password;
    }

    public void setPassword(SecretResponse password) {
        this.password = password;
    }

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    public String getCreator() {
        return creator;
    }

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    public void setCreator(String creator) {
        this.creator = creator;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "ProxyResponse{" +
                "crn='" + crn + '\'' +
                ", creator='" + creator + '\'' +
                '}';
    }
}
