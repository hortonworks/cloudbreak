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
    private String stackRepoId;
    private String stackBaseURL;
    private String utilsRepoId;
    private String utilsBaseURL;
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

    public String getStackRepoId() {
        return stackRepoId;
    }

    public void setStackRepoId(String stackRepoId) {
        this.stackRepoId = stackRepoId;
    }

    public String getStackBaseURL() {
        return stackBaseURL;
    }

    public void setStackBaseURL(String stackBaseURL) {
        this.stackBaseURL = stackBaseURL;
    }

    public boolean isVerify() {
        return verify;
    }

    public void setVerify(boolean verify) {
        this.verify = verify;
    }

    public String getUtilsRepoId() {
        return utilsRepoId;
    }

    public void setUtilsRepoId(String utilsRepoId) {
        this.utilsRepoId = utilsRepoId;
    }

    public String getUtilsBaseURL() {
        return utilsBaseURL;
    }

    public void setUtilsBaseURL(String utilsBaseURL) {
        this.utilsBaseURL = utilsBaseURL;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AmbariStackDetails{");
        sb.append("id=").append(id);
        sb.append(", stack='").append(stack).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", os='").append(os).append('\'');
        sb.append(", stackRepoId='").append(stackRepoId).append('\'');
        sb.append(", utilsRepoId='").append(utilsRepoId).append('\'');
        sb.append(", stackBaseURL='").append(stackBaseURL).append('\'');
        sb.append(", utilsBaseURL='").append(utilsBaseURL).append('\'');
        sb.append(", verify=").append(verify);
        sb.append('}');
        return sb.toString();
    }
}
