package com.witboost.provisioning.athena.config;

import com.witboost.provisioning.athena.awsClient.AthenaManager;
import com.witboost.provisioning.athena.awsClient.BucketManager;
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
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class ConfigurationBean {

    @Autowired
    private AthenaManager athenaManager;

    @Autowired
    private BucketManager bucketManager;

    private final Map<Region, S3Client> s3ClientCache = new ConcurrentHashMap<>();
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
            OutputPortValidationService outputPortValidationService,
            AthenaManager athenaManager,
            BucketManager bucketManager) {
        return new OutputPortProvisionService(
                outputPortValidationService, this::getS3Client, this::getAthenaClient, athenaManager, bucketManager);
    }

    @Bean
    ProvisionConfiguration provisionConfiguration(OutputPortProvisionService outputPortProvisionService) {
        return ProvisionConfiguration.builder()
                .outputPortProvisionService(outputPortProvisionService)
                .build();
    }

    protected S3Client getS3Client(Region region) {
        return s3ClientCache.computeIfAbsent(
                region, r -> S3Client.builder().region(r).build());
    }

    public AthenaClient getAthenaClient(Region region) {
        return athenaClientCache.computeIfAbsent(
                region, r -> AthenaClient.builder().region(r).build());
    }
}
