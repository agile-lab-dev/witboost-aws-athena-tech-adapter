package com.witboost.provisioning.athena.config;

import com.witboost.provisioning.athena.awsClient.AthenaManager;
import com.witboost.provisioning.athena.service.provision.OutputPortProvisionService;
import com.witboost.provisioning.athena.service.validation.OutputPortValidationService;
import com.witboost.provisioning.framework.service.ProvisionConfiguration;
import com.witboost.provisioning.framework.service.validation.ValidationConfiguration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;

@Configuration
public class ConfigurationBean {

    @Autowired
    private AthenaManager athenaManager;

    private final Map<Region, AthenaClient> athenaClientCache = new ConcurrentHashMap<>();

    @Bean
    public OutputPortValidationService outputPortValidationService() {
        return new OutputPortValidationService(this::getAthenaClient, athenaManager);
    }

    @Bean
    public ValidationConfiguration validationConfiguration(OutputPortValidationService outputPortValidationService) {
        return ValidationConfiguration.builder()
                .outputPortValidationService(outputPortValidationService)
                .build();
    }

    @Bean
    public OutputPortProvisionService outputPortProvisionService(
            OutputPortValidationService outputPortValidationService, AthenaManager athenaManager) {
        return new OutputPortProvisionService(outputPortValidationService, this::getAthenaClient, athenaManager);
    }

    @Bean
    ProvisionConfiguration provisionConfiguration(OutputPortProvisionService outputPortProvisionService) {
        return ProvisionConfiguration.builder()
                .outputPortProvisionService(outputPortProvisionService)
                .build();
    }

    public AthenaClient getAthenaClient(Region region) {
        return athenaClientCache.computeIfAbsent(
                region, r -> AthenaClient.builder().region(r).build());
    }
}
