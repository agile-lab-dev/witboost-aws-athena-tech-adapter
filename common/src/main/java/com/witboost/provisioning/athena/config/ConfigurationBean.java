package com.witboost.provisioning.athena.config;

import com.witboost.provisioning.athena.service.provision.OutputPortProvisionService;
import com.witboost.provisioning.athena.service.validation.OutputPortValidationService;
import com.witboost.provisioning.framework.service.ProvisionConfiguration;
import com.witboost.provisioning.framework.service.validation.ValidationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigurationBean {

    @Bean
    ValidationConfiguration validationConfiguration(OutputPortValidationService outputPortValidationService) {
        return ValidationConfiguration.builder()
                .outputPortValidationService(outputPortValidationService)
                .build();
    }

    @Bean
    ProvisionConfiguration provisionConfiguration(OutputPortProvisionService outputPortProvisionService) {
        return ProvisionConfiguration.builder()
                .outputPortProvisionService(outputPortProvisionService)
                .build();
    }
}
