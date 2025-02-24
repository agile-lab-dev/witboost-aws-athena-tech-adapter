package com.witboost.provisioning.athena.service.provision;

import com.fasterxml.jackson.databind.JsonNode;
import com.witboost.provisioning.athena.awsClient.AthenaManager;
import com.witboost.provisioning.athena.awsClient.BucketManager;
import com.witboost.provisioning.athena.model.AthenaTable;
import com.witboost.provisioning.athena.model.AthenaView;
import com.witboost.provisioning.athena.service.validation.OutputPortValidationService;
import com.witboost.provisioning.athena.utils.RequestUtils;
import com.witboost.provisioning.framework.service.ProvisionService;
import com.witboost.provisioning.model.Column;
import com.witboost.provisioning.model.OperationType;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import com.witboost.provisioning.model.request.ProvisionOperationRequest;
import com.witboost.provisioning.model.status.ProvisionInfo;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.s3.S3Client;

@Service
public class OutputPortProvisionService implements ProvisionService {

    private final Logger logger = LoggerFactory.getLogger(OutputPortProvisionService.class);

    private final Function<Region, S3Client> s3ClientProvider;
    private final Function<Region, AthenaClient> athenaClientProvider;
    private final AthenaManager athenaManager;
    private final BucketManager bucketManager;
    private final OutputPortValidationService outputPortValidationService;

    public OutputPortProvisionService(
            OutputPortValidationService outputPortValidationService,
            Function<Region, S3Client> s3ClientProvider,
            Function<Region, AthenaClient> athenaClientProvider,
            AthenaManager athenaManager,
            BucketManager bucketManager) {
        this.outputPortValidationService = outputPortValidationService;
        this.s3ClientProvider = s3ClientProvider;
        this.athenaClientProvider = athenaClientProvider;
        this.athenaManager = athenaManager;
        this.bucketManager = bucketManager;
    }

    @Override
    public Either<FailedOperation, ProvisionInfo> provision(
            ProvisionOperationRequest<?, ? extends Specific> operationRequest) {

        return outputPortValidationService
                .validate(operationRequest, OperationType.PROVISION)
                .flatMap(ignored -> RequestUtils.getOutputPort(operationRequest)
                        .flatMap(outputPort -> RequestUtils.getAthenaSpecific(outputPort)
                                .flatMap(athenaSpecific -> RequestUtils.extractStorageAreaInfo(
                                                operationRequest, athenaSpecific)
                                        .flatMap(storageAreaInfo -> RequestUtils.extractStorageAreaRegion(
                                                        storageAreaInfo)
                                                .flatMap(storageAreaRegion -> {
                                                    AthenaClient athenaClient =
                                                            athenaClientProvider.apply(Region.of(storageAreaRegion));
                                                    S3Client s3Client =
                                                            s3ClientProvider.apply(Region.of(storageAreaRegion));

                                                    AthenaTable sourceTable = athenaSpecific.getSourceTable();
                                                    AthenaView targetView = athenaSpecific.getView();

                                                    return createOutputLocation(storageAreaInfo, outputPort, s3Client)
                                                            .flatMap(outputLocation -> createDatabaseIfNotExists(
                                                                            athenaClient,
                                                                            outputLocation,
                                                                            sourceTable.getCatalog(),
                                                                            sourceTable.getDatabase())
                                                                    .flatMap(ignored2 -> createDatabaseIfNotExists(
                                                                            athenaClient,
                                                                            outputLocation,
                                                                            targetView.getCatalog(),
                                                                            targetView.getDatabase()))
                                                                    .flatMap(ignored3 -> createTableIfNotExists(
                                                                            athenaClient,
                                                                            outputLocation,
                                                                            sourceTable.getCatalog(),
                                                                            sourceTable.getDatabase(),
                                                                            sourceTable.getName(),
                                                                            outputPort
                                                                                    .getDataContract()
                                                                                    .getSchema()))
                                                                    .flatMap(ignored4 -> createView(
                                                                            athenaClient,
                                                                            outputLocation,
                                                                            sourceTable,
                                                                            targetView,
                                                                            outputPort
                                                                                    .getDataContract()
                                                                                    .getSchema())))
                                                            .map(ignored5 -> {
                                                                var info = Map.of(
                                                                        "view",
                                                                        Map.of(
                                                                                "type", "string",
                                                                                "label", "View name",
                                                                                "value", targetView.getName()),
                                                                        "database",
                                                                        Map.of(
                                                                                "type", "string",
                                                                                "label", "Database",
                                                                                "value", targetView.getDatabase()),
                                                                        "catalog",
                                                                        Map.of(
                                                                                "type", "string",
                                                                                "label", "Catalog",
                                                                                "value", targetView.getCatalog()));

                                                                ProvisionInfo provisionInfo = ProvisionInfo.builder()
                                                                        .privateInfo(Optional.of(info))
                                                                        .publicInfo(Optional.of(info))
                                                                        .build();

                                                                return provisionInfo;
                                                            });
                                                })))));
    }

    @Override
    public Either<FailedOperation, ProvisionInfo> unprovision(
            ProvisionOperationRequest<?, ? extends Specific> operationRequest) {

        return outputPortValidationService
                .validate(operationRequest, OperationType.UNPROVISION)
                .flatMap(ignored -> RequestUtils.getOutputPort(operationRequest)
                        .flatMap(outputPort -> RequestUtils.getAthenaSpecific(outputPort)
                                .flatMap(athenaSpecific -> RequestUtils.extractStorageAreaInfo(
                                                operationRequest, athenaSpecific)
                                        .flatMap(storageAreaInfo -> RequestUtils.extractStorageAreaRegion(
                                                        storageAreaInfo)
                                                .flatMap(storageAreaRegion -> {
                                                    AthenaClient athenaClient =
                                                            athenaClientProvider.apply(Region.of(storageAreaRegion));
                                                    S3Client s3Client =
                                                            s3ClientProvider.apply(Region.of(storageAreaRegion));
                                                    AthenaView athenaView = athenaSpecific.getView();

                                                    return createOutputLocation(storageAreaInfo, outputPort, s3Client)
                                                            .flatMap(outputLocation -> athenaManager.dropView(
                                                                    athenaClient, outputLocation, athenaView))
                                                            .map(ignored2 -> {
                                                                var info = Map.of(
                                                                        "result",
                                                                        Map.of(
                                                                                "type",
                                                                                "string",
                                                                                "label",
                                                                                "Operation result",
                                                                                "value",
                                                                                String.format(
                                                                                        "View '%s' successfully deleted from database '%s' (catalog '%s').",
                                                                                        athenaView.getName(),
                                                                                        athenaView.getDatabase(),
                                                                                        athenaView.getCatalog())));

                                                                ProvisionInfo provisionInfo = ProvisionInfo.builder()
                                                                        .privateInfo(Optional.of(info))
                                                                        .publicInfo(Optional.of(info))
                                                                        .build();

                                                                return provisionInfo;
                                                            });
                                                })))));
    }

    private Either<FailedOperation, Void> createDatabaseIfNotExists(
            AthenaClient athenaClient, String outputLocation, String catalog, String database) {

        return athenaManager
                .checkDatabaseExists(athenaClient, catalog, database)
                .flatMap(dbExists -> dbExists
                        ? Either.right(null)
                        : athenaManager.createDatabase(athenaClient, outputLocation, catalog, database));
    }

    private Either<FailedOperation, Void> createTableIfNotExists(
            AthenaClient athenaClient,
            String outputLocation,
            String catalog,
            String database,
            String name,
            List<Column> schema) {

        return athenaManager
                .getTableMetadata(athenaClient, catalog, database, name)
                .flatMap(metadata -> {
                    if (metadata.isEmpty()) {
                        return athenaManager.createTable(athenaClient, outputLocation, catalog, database, name, schema);
                    } else {
                        return Either.right(null);
                    }
                });
    }

    private Either<FailedOperation, Void> createView(
            AthenaClient athenaClient,
            String outputLocation,
            AthenaTable athenaTable,
            AthenaView athenaView,
            List<Column> columns) {

        return athenaManager.createView(athenaClient, outputLocation, athenaTable, athenaView, columns);
    }

    private String extractFolderName(String componentId) {
        String[] componentIdParts = componentId.split(":");
        return componentIdParts[componentIdParts.length - 1];
    }

    private String buildS3Url(String bucketName, String folderName) {
        return new StringBuilder("s3://")
                .append(bucketName)
                .append("/")
                .append(folderName)
                .toString();
    }

    private Either<FailedOperation, String> extractBucketName(JsonNode storageAreaInfo) {

        return Option.of(storageAreaInfo.get("info"))
                .flatMap(info -> Option.of(info.get("privateInfo")))
                .flatMap(privateInfoNode -> Option.of(privateInfoNode.get("bucket")))
                .flatMap(bucketNode -> Option.of(bucketNode.get("value")))
                .map(JsonNode::asText)
                .toEither(() -> new FailedOperation(
                        "The dependant storage component is not including the bucket name",
                        Optional.empty(),
                        Optional.empty(),
                        List.of(new Problem("Missing location at info.privateInfo.bucket.value"))));
    }

    private Either<FailedOperation, String> createOutputLocation(
            JsonNode storageAreaInfo,
            com.witboost.provisioning.model.Component<? extends Specific> component,
            S3Client s3Client) {

        return extractBucketName(storageAreaInfo).flatMap(bucket -> {
            String folderName = extractFolderName(component.getId());
            return bucketManager
                    .createFolder(s3Client, bucket, folderName)
                    .map(ignored -> buildS3Url(bucket, folderName));
        });
    }
}
