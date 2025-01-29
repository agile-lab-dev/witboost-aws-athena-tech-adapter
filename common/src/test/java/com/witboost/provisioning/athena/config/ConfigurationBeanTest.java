package com.witboost.provisioning.athena.config;

import static org.junit.jupiter.api.Assertions.*;

import com.witboost.provisioning.athena.service.provision.OutputPortProvisionService;
import com.witboost.provisioning.athena.service.validation.OutputPortValidationService;
import org.junit.jupiter.api.Test;

class ConfigurationBeanTest {

    @Test
    void provisionBeanCreation() {
        var outputPort = new OutputPortProvisionService();
        var bean = new ConfigurationBean().provisionConfiguration(outputPort);

        assertEquals(outputPort, bean.getOutputPortProvisionService());
    }

    @Test
    void validationBeanCreation() {
        var outputPort = new OutputPortValidationService();
        var bean = new ConfigurationBean().validationConfiguration(outputPort);

        assertEquals(outputPort, bean.getOutputPortValidationService());
    }
}
