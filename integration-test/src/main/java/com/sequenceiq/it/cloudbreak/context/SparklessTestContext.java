package com.sequenceiq.it.cloudbreak.context;

import javax.inject.Inject;

import org.springframework.context.annotation.Primary;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestMethodNameMissingException;
import com.sequenceiq.it.util.TagsUtil;

@Prototype
@Primary
public class SparklessTestContext extends TestContext {

    @Inject
    private TagsUtil tagsUtil;

    @Override
    public <O extends CloudbreakTestDto> O init(Class<O> clss, CloudPlatform cloudPlatform) {
        O bean = super.init(clss, cloudPlatform);
        tagsUtil.addTestNameTag(bean, getTestMethodName().orElseThrow(TestMethodNameMissingException::new));
        return bean;
    }

    @Override
    public void cleanupTestContext() {
        super.cleanupTestContext();
        getResources().values().forEach(value -> tagsUtil.verifyTags(value, this));
    }
}
