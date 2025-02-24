package com.witboost.provisioning.athena.config;

import static org.junit.jupiter.api.Assertions.*;

import com.witboost.provisioning.athena.awsClient.AthenaManager;
import com.witboost.provisioning.athena.awsClient.BucketManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ConfigurationBeanTest {

    @InjectMocks
    private ConfigurationBean configurationBean;

    @Mock
    private AthenaManager athenaManager;

    @Mock
    private BucketManager bucketManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void provisionBeanCreation() {
        var outputPort = configurationBean.outputPortProvisionService(
                configurationBean.outputPortValidationService(), athenaManager, bucketManager);
        var bean = new ConfigurationBean().provisionConfiguration(outputPort);

        assertEquals(outputPort, bean.getOutputPortProvisionService());
    }

    @Test
    void validationBeanCreation() {
        var outputPort = configurationBean.outputPortValidationService();
        var bean = new ConfigurationBean().validationConfiguration(outputPort);

        assertEquals(outputPort, bean.getOutputPortValidationService());
    }
}
