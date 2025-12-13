package com.sequenceiq.cloudbreak.utils;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.openpojo.reflection.PojoClass;
import com.openpojo.reflection.PojoClassFilter;
import com.openpojo.reflection.filters.FilterChain;
import com.openpojo.reflection.impl.PojoClassFactory;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.rule.impl.GetterMustExistRule;
import com.openpojo.validation.rule.impl.NoNestedClassRule;
import com.openpojo.validation.rule.impl.NoStaticExceptFinalRule;
import com.openpojo.validation.rule.impl.SetterMustExistRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;
import com.sequenceiq.common.model.annotations.IgnorePojoValidation;
import com.sequenceiq.common.model.annotations.Immutable;
import com.sequenceiq.common.model.annotations.TransformGetterType;
import com.sequenceiq.common.model.annotations.TransformSetterType;

class ModelTest {

    private static final String DOMAIN_PACKAGE = "com.sequenceiq.cloudbreak.api.model";

    private static final Pattern TESTCLASS_NAME_PATTERN = Pattern.compile("^\\S+?Test(?:\\$\\S+)?$");

    private static final PojoClassFilter BASE_CLASS_FILTER = pojoClass -> !pojoClass.isEnum() && !pojoClass.isAbstract()
            && pojoClass.getAnnotation(IgnorePojoValidation.class) == null
            && !TESTCLASS_NAME_PATTERN.matcher(pojoClass.getName()).matches();

    private static final PojoClassFilter POJO_CLASS_FILTER = new FilterChain(BASE_CLASS_FILTER,
            pojoClass -> pojoClass.getAnnotation(Immutable.class) == null
                    && pojoClass.getPojoFieldsAnnotatedWith(TransformGetterType.class).isEmpty()
                    && pojoClass.getPojoFieldsAnnotatedWith(TransformSetterType.class).isEmpty());

    private static final PojoClassFilter IMMUTABLE_CLASS_FILTER = new FilterChain(BASE_CLASS_FILTER,
            pojoClass -> pojoClass.getAnnotation(Immutable.class) != null
                    && pojoClass.getPojoFieldsAnnotatedWith(TransformGetterType.class).isEmpty());

    private static final PojoClassFilter GETTER_TYPE_CHANGER_CLASS_FILTER = new FilterChain(BASE_CLASS_FILTER,
            pojoClass -> !pojoClass.getPojoFieldsAnnotatedWith(TransformGetterType.class).isEmpty()
                    && pojoClass.getPojoFieldsAnnotatedWith(TransformSetterType.class).isEmpty()
                    && pojoClass.getAnnotation(Immutable.class) == null);

    private static final PojoClassFilter SETTER_TYPE_CHANGER_CLASS_FILTER = new FilterChain(BASE_CLASS_FILTER,
            pojoClass -> !pojoClass.getPojoFieldsAnnotatedWith(TransformSetterType.class).isEmpty()
                    && pojoClass.getPojoFieldsAnnotatedWith(TransformGetterType.class).isEmpty()
                    && pojoClass.getAnnotation(Immutable.class) == null);

    @Test
    void testPojoStructureAndBehavior() {
        List<PojoClass> pojoClasses = PojoClassFactory.getPojoClassesRecursively(DOMAIN_PACKAGE, POJO_CLASS_FILTER);

        Validator validator = ValidatorBuilder.create()
                .with(new SetterMustExistRule(),
                        new GetterMustExistRule())
                .with(new SetterTester(),
                        new GetterTester())
                .with(new NoStaticExceptFinalRule())
                .with(new NoNestedClassRule())
                .build();
        validator.validate(pojoClasses);
    }

    @Test
    void testImmutableStructureAndBehavior() {
        List<PojoClass> pojoClasses = PojoClassFactory.getPojoClassesRecursively(DOMAIN_PACKAGE, IMMUTABLE_CLASS_FILTER);

        Validator validator = ValidatorBuilder.create()
                .with(new GetterMustExistRule())
                .with(new GetterTester())
                .with(new NoStaticExceptFinalRule())
                .with(new NoNestedClassRule())
                .build();
        validator.validate(pojoClasses);
    }

    @Test
    void testGetterTypeChangerPojoStructureAndBehavior() {
        List<PojoClass> pojoClasses = PojoClassFactory.getPojoClassesRecursively(DOMAIN_PACKAGE, GETTER_TYPE_CHANGER_CLASS_FILTER);

        Validator validator = ValidatorBuilder.create()
                .with(new SetterMustExistRule())
                .with(new SetterTester(),
                        new GetterTester())
                .with(new NoStaticExceptFinalRule())
                .with(new NoNestedClassRule())
                .build();
        validator.validate(pojoClasses);
    }

    @Test
    void testSetterTypeChangerPojoStructureAndBehavior() {
        List<PojoClass> pojoClasses = PojoClassFactory.getPojoClassesRecursively(DOMAIN_PACKAGE, SETTER_TYPE_CHANGER_CLASS_FILTER);

        Validator validator = ValidatorBuilder.create()
                .with(new GetterMustExistRule())
                .with(new SetterTester(),
                        new GetterTester())
                .with(new NoStaticExceptFinalRule())
                .with(new NoNestedClassRule())
                .build();
        validator.validate(pojoClasses);
    }
}
