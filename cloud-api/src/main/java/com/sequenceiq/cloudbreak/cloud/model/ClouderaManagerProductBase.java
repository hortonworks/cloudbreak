package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

public interface ClouderaManagerProductBase {

    String getName();

    String getVersion();

    String getParcel();

    List<String> getCsd();

}
