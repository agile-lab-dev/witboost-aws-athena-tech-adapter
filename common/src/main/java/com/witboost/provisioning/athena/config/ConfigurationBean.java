package com.witboost.provisioning.athena.config;

import com.witboost.provisioning.athena.awsClient.AthenaManager;
import com.witboost.provisioning.athena.awsClient.LakeFormationManager;
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
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.lakeformation.LakeFormationClient;
import software.amazon.awssdk.services.sts.StsClient;

@Configuration
public class ConfigurationBean {

    @Autowired
    private AthenaManager athenaManager;

    @Autowired
    private LakeFormationManager lakeFormationManager;

    private final Map<Region, AthenaClient> athenaClientCache = new ConcurrentHashMap<>();
    private final Map<Region, LakeFormationClient> lakeFormationClientCache = new ConcurrentHashMap<>();
    private final Map<Region, GlueClient> glueClientCache = new ConcurrentHashMap<>();

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
            OutputPortValidationService outputPortValidationService, AthenaManager athenaManager, StsClient stsClient) {
        return new OutputPortProvisionService(
                outputPortValidationService,
                this::getAthenaClient,
                this::getLakeFormationClient,
                this::getGlueClient,
                stsClient,
                athenaManager,
                lakeFormationManager);
    }

    @Bean
    ProvisionConfiguration provisionConfiguration(OutputPortProvisionService outputPortProvisionService) {
        return ProvisionConfiguration.builder()
                .outputPortProvisionService(outputPortProvisionService)
                .build();
    }

    @Bean
    public StsClient stsClient() {
        return StsClient.create();
    }

    public AthenaClient getAthenaClient(Region region) {
        return athenaClientCache.computeIfAbsent(
                region, r -> AthenaClient.builder().region(r).build());
    }

    public LakeFormationClient getLakeFormationClient(Region region) {
        return lakeFormationClientCache.computeIfAbsent(
                region, r -> LakeFormationClient.builder().region(r).build());
    }

    public GlueClient getGlueClient(Region region) {
        return glueClientCache.computeIfAbsent(
                region, r -> GlueClient.builder().region(r).build());
    }
}
