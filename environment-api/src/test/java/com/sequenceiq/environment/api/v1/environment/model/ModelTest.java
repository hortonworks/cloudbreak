package com.sequenceiq.environment.api.v1.environment.model;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openpojo.reflection.PojoClass;
import com.openpojo.reflection.PojoClassFilter;
import com.openpojo.reflection.impl.PojoClassFactory;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.rule.impl.GetterMustExistRule;
import com.openpojo.validation.rule.impl.NoFieldShadowingRule;
import com.openpojo.validation.rule.impl.NoNestedClassRule;
import com.openpojo.validation.rule.impl.NoPublicFieldsExceptStaticFinalRule;
import com.openpojo.validation.rule.impl.NoStaticExceptFinalRule;
import com.openpojo.validation.rule.impl.SetterMustExistRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;

class ModelTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelTest.class);

    private static final String MODEL_PACKAGE = "com.sequenceiq.environment.api.v1.environment.model";

    private static final Pattern TESTCLASS_NAME_PATTERN = Pattern.compile("^\\S+?Test(?:\\$\\S+)?$");

    private static final Pattern BUILDERCLASS_NAME_PATTERN = Pattern.compile("^\\S+?Builder$");

    private static final PojoClassFilter BASE_CLASS_FILTER = pojoClass -> pojoClass.isConcrete()
            && !TESTCLASS_NAME_PATTERN.matcher(pojoClass.getName()).matches()
            && !BUILDERCLASS_NAME_PATTERN.matcher(pojoClass.getName()).matches();

    @Test
    public void testPojoStructureAndBehavior() {
        List<PojoClass> pojoClasses = PojoClassFactory.getPojoClassesRecursively(MODEL_PACKAGE, BASE_CLASS_FILTER);

        Validator validator = ValidatorBuilder.create()
                .with(new SetterMustExistRule(),
                        new GetterMustExistRule())
                .with(new GetterTester())
                .with(new NoPublicFieldsExceptStaticFinalRule())
                .with(new NoStaticExceptFinalRule())
                .with(new NoNestedClassRule())
                .with(new NoFieldShadowingRule())
                .build();
        for (PojoClass pojoClass : pojoClasses) {
            try {
                validator.validate(pojoClass);
            } catch (Exception e) {
                LOGGER.error(String.format("Exception occurred because: %s in %s class", e, pojoClass.getName()));
            }
        }

        validator = ValidatorBuilder.create()
                .with(new SetterTester())
                .build();
        for (PojoClass pojoClass : pojoClasses) {
            try {
                validator.validate(pojoClass);
            } catch (Exception e) {
                LOGGER.error(String.format("Exception occurred because: %s in %s class", e, pojoClass.getName()));
            }
        }
        // openpojo will do for now (seems buggy) but later would worth experimenting with pojo-tester (https://www.pojo.pl/)
    }
}
