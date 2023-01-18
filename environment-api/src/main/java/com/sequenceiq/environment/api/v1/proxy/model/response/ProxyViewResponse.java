package com.sequenceiq.environment.api.v1.proxy.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.proxy.ProxyConfigDescription;
import com.sequenceiq.environment.api.v1.proxy.model.ProxyBase;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ProxyConfigDescription.DESCRIPTION)
@JsonInclude(Include.NON_NULL)
public class ProxyViewResponse extends ProxyBase {

    @Schema(description = ProxyConfigDescription.PROXY_CONFIG_ID)
    private String crn;

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    @Schema(description = ModelDescriptions.CREATOR)
    private String creator;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
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
        return super.toString() + ", " + "ProxyViewResponse{" +
                "crn='" + crn + '\'' +
                ", creator='" + creator + '\'' +
                '}';
    }
}
