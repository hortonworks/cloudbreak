package com.sequenceiq.cloudbreak.converter;

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.kerberos.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.model.kerberos.AmbariKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.model.kerberos.FreeIPAKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosTypeBase;
import com.sequenceiq.cloudbreak.api.model.kerberos.MITKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosRequest;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.v2.KerberosRequestToKerberosConfigConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosTypeResolver;

@RunWith(Parameterized.class)
public class KerberosRequestToKerberosConfigConverterTest extends AbstractConverterTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private KerberosTypeResolver kerberosTypeResolver;

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private KerberosRequestToKerberosConfigConverter underTest;

    private KerberosData testData;

    public KerberosRequestToKerberosConfigConverterTest(KerberosData testData) {
        this.testData = testData;
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Parameterized.Parameters()
    public static Object[] data() {
        return new Object[]{
                KerberosData.FREEIPA,
                KerberosData.CUSTOM,
                KerberosData.ACTIVE_DIRECTORY,
                KerberosData.MIT,
        };
    }

    @Test
    public void testConverterWhenKerberosTypeResolverReturnsASpecificKerberosTypeThenThatShouldBeConvertIntoAKerberosConfig() {
        KerberosRequest request = testData.getRequest();
        KerberosTypeBase actualType = testData.getActualType();
        KerberosConfig expected = testData.getExpected();
        when(kerberosTypeResolver.propagateKerberosConfiguration(request)).thenReturn(actualType);
        when(conversionService.convert(actualType, KerberosConfig.class)).thenReturn(expected);

        KerberosConfig result = underTest.convert(request);

        Assert.assertNotNull(result);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testConvertWhenConversionComponentDoesNotWorkProperlyAndReturnsNullThenBadRequestExceptionComes() {
        KerberosRequest request = testData.getRequest();
        KerberosTypeBase actualType = testData.getActualType();
        when(kerberosTypeResolver.propagateKerberosConfiguration(testData.getRequest())).thenReturn(actualType);
        when(conversionService.convert(actualType, KerberosConfig.class)).thenReturn(null);

        thrown.expectMessage("Obtaining KerberosConfig from KerberosTypeBase was unsuccessful, it has returned null. "
                + "Further operations are impossible");
        thrown.expect(BadRequestException.class);

        underTest.convert(request);
    }

    private enum KerberosData {

        FREEIPA {
            @Override
            public KerberosRequest getRequest() {
                KerberosRequest request = new KerberosRequest();
                request.setFreeIpa(new FreeIPAKerberosDescriptor());
                return request;
            }

            @Override
            public KerberosConfig getExpected() {
                return new KerberosConfig();
            }

            @Override
            public KerberosTypeBase getActualType() {
                return new FreeIPAKerberosDescriptor();
            }
        },

        MIT {
            @Override
            public KerberosRequest getRequest() {
                KerberosRequest request = new KerberosRequest();
                request.setMit(new MITKerberosDescriptor());
                return request;
            }

            @Override
            public KerberosConfig getExpected() {
                return new KerberosConfig();
            }

            @Override
            public KerberosTypeBase getActualType() {
                return new MITKerberosDescriptor();
            }
        },

        ACTIVE_DIRECTORY {
            @Override
            public KerberosRequest getRequest() {
                KerberosRequest request = new KerberosRequest();
                request.setActiveDirectory(new ActiveDirectoryKerberosDescriptor());
                return request;
            }

            @Override
            public KerberosConfig getExpected() {
                return new KerberosConfig();
            }

            @Override
            public KerberosTypeBase getActualType() {
                return new ActiveDirectoryKerberosDescriptor();
            }
        },

        CUSTOM {
            @Override
            public KerberosRequest getRequest() {
                KerberosRequest request = new KerberosRequest();
                request.setAmbariKerberosDescriptor(new AmbariKerberosDescriptor());
                return request;
            }

            @Override
            public KerberosConfig getExpected() {
                return new KerberosConfig();
            }

            @Override
            public KerberosTypeBase getActualType() {
                return new AmbariKerberosDescriptor();
            }
        };

        public abstract KerberosRequest getRequest();

        public abstract KerberosConfig getExpected();

        public abstract KerberosTypeBase getActualType();

    }

}