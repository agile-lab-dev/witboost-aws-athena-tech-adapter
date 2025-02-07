package com.witboost.provisioning.athena.service.provision;

import com.witboost.provisioning.athena.awsClient.AthenaManager;
import com.witboost.provisioning.athena.awsClient.BucketManager;
import com.witboost.provisioning.framework.service.ProvisionService;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import com.witboost.provisioning.model.request.ProvisionOperationRequest;
import com.witboost.provisioning.model.status.ProvisionInfo;
import io.vavr.control.Either;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.s3.S3Client;

@Component
public class OutputPortProvisionService implements ProvisionService {

    private final Logger logger = LoggerFactory.getLogger(OutputPortProvisionService.class);

    private final Function<Region, S3Client> s3ClientProvider;
    private final Function<Region, AthenaClient> athenaClientProvider;
    private final AthenaManager athenaManager;
    private final BucketManager bucketManager;

    public OutputPortProvisionService(
            Function<Region, S3Client> s3ClientProvider,
            Function<Region, AthenaClient> athenaClientProvider,
            AthenaManager athenaManager,
            BucketManager bucketManager) {
        this.s3ClientProvider = s3ClientProvider;
        this.athenaClientProvider = athenaClientProvider;
        this.athenaManager = athenaManager;
        this.bucketManager = bucketManager;
    }

    @Override
    public Either<FailedOperation, ProvisionInfo> provision(
            ProvisionOperationRequest<?, ? extends Specific> operationRequest) {

        return Either.left(
                new FailedOperation("Method nod implemented", List.of(new Problem("Method not implemented"))));
    }

    @Override
    public Either<FailedOperation, ProvisionInfo> unprovision(
            ProvisionOperationRequest<?, ? extends Specific> operationRequest) {

        return Either.left(
                new FailedOperation("Method nod implemented", List.of(new Problem("Method not implemented"))));
    }
}
