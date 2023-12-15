package com.sequenceiq.common;

import com.sequenceiq.common.model.JsonEntity;

public interface RootVolumeRequest extends JsonEntity {

    Integer getSize();

    void setSize(Integer size);

}
