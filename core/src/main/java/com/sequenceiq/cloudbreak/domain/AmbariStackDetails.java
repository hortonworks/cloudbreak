package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public class AmbariStackDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "amb_stack_generator")
    @SequenceGenerator(name = "amb_stack_generator", sequenceName = "amb_stack_table")
    private Long id;
    private String stack;
    private String version;
    private String os;
    private String repoId;
    private String baseURL;
    private boolean verify = true;

    public String getStack() {
        return stack;
    }

    public void setStack(String stack) {
        this.stack = stack;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public boolean isVerify() {
        return verify;
    }

    public void setVerify(boolean verify) {
        this.verify = verify;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AmbariStackDetails{");
        sb.append("stack='").append(stack).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", os='").append(os).append('\'');
        sb.append(", repoId='").append(repoId).append('\'');
        sb.append(", baseURL='").append(baseURL).append('\'');
        sb.append(", verify=").append(verify);
        sb.append('}');
        return sb.toString();
    }
}
