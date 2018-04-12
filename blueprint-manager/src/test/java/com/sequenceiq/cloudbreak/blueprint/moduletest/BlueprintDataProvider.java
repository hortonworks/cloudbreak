package com.sequenceiq.cloudbreak.blueprint.moduletest;

import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.blueprint.testrepeater.TestFile;
import com.sequenceiq.cloudbreak.blueprint.testrepeater.TripleTestData;

class BlueprintDataProvider extends TripleTestData<TestFile, TestFile, TemplatePreparationObject> {

    BlueprintDataProvider(TestFile input, TestFile output, TemplatePreparationObject model) {
        super(input, output, model);
    }

    TestFile getInput() {
        return getData1();
    }

    TestFile getOutput() {
        return getData2();
    }

    TemplatePreparationObject getModel() {
        return getData3();
    }

}
