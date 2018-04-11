package com.sequenceiq.cloudbreak.blueprint.moduletest;

import com.sequenceiq.cloudbreak.templateprocessor.processor.PreparationObject;
import com.sequenceiq.cloudbreak.blueprint.testrepeater.TestFile;
import com.sequenceiq.cloudbreak.blueprint.testrepeater.TripleTestData;

class BlueprintDataProvider extends TripleTestData<TestFile, TestFile, PreparationObject> {

    BlueprintDataProvider(TestFile input, TestFile output, PreparationObject model) {
        super(input, output, model);
    }

    TestFile getInput() {
        return getData1();
    }

    TestFile getOutput() {
        return getData2();
    }

    PreparationObject getModel() {
        return getData3();
    }

}
