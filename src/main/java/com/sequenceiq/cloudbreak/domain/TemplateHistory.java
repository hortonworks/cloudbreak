package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "templatehistory")
public class TemplateHistory extends AbstractHistory {
    private String dType;
    private String amiid;
    private String instancetype;
    private String region;
    private String sshLocation;
    private String imageName;
    private String location;
    private String vmType;
}
