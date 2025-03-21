package com.witboost.provisioning.athena.service.provision;

import com.fasterxml.jackson.databind.JsonNode;
import com.witboost.provisioning.athena.awsClient.AthenaManager;
import com.witboost.provisioning.athena.awsClient.LakeFormationManager;
import com.witboost.provisioning.athena.model.AthenaColumn;
import com.witboost.provisioning.athena.model.AthenaTable;
import com.witboost.provisioning.athena.model.AthenaView;
import com.witboost.provisioning.athena.model.TableFormat;
import com.witboost.provisioning.athena.service.validation.OutputPortValidationService;
import com.witboost.provisioning.athena.utils.RequestUtils;
import com.witboost.provisioning.framework.service.ProvisionService;
import com.witboost.provisioning.model.OperationType;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import com.witboost.provisioning.model.request.ProvisionOperationRequest;
import com.witboost.provisioning.model.status.ProvisionInfo;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.lakeformation.LakeFormationClient;
import software.amazon.awssdk.services.sts.StsClient;

@Service
public class OutputPortProvisionService implements ProvisionService {

    private final Logger logger = LoggerFactory.getLogger(OutputPortProvisionService.class);

    @Value("${enforceLakeFormation: false}")
    private String enforceLakeFormation;

    private final OutputPortValidationService outputPortValidationService;
    private final Function<Region, AthenaClient> athenaClientProvider;
    private final Function<Region, LakeFormationClient> lakeFormationClientProvider;
    private final Function<Region, GlueClient> glueClientProvider;
    private final AthenaManager athenaManager;
    private final LakeFormationManager lakeFormationManager;
    private final StsClient stsClient;

    public OutputPortProvisionService(
            OutputPortValidationService outputPortValidationService,
            Function<Region, AthenaClient> athenaClientProvider,
            Function<Region, LakeFormationClient> lakeFormationClientProvider,
            Function<Region, GlueClient> glueClientProvider,
            StsClient stsClient,
            AthenaManager athenaManager,
            LakeFormationManager lakeFormationManager) {
        this.outputPortValidationService = outputPortValidationService;
        this.athenaClientProvider = athenaClientProvider;
        this.lakeFormationClientProvider = lakeFormationClientProvider;
        this.glueClientProvider = glueClientProvider;
        this.stsClient = stsClient;
        this.athenaManager = athenaManager;
        this.lakeFormationManager = lakeFormationManager;
    }

    @Override
    public Either<FailedOperation, ProvisionInfo> provision(
            ProvisionOperationRequest<?, ? extends Specific> operationRequest) {

        return outputPortValidationService
                .validate(operationRequest, OperationType.PROVISION)
                .flatMap(ignored -> RequestUtils.getAthenaOutputPort(operationRequest)
                        .flatMap(outputPort -> RequestUtils.getAthenaSpecific(outputPort)
                                .flatMap(athenaSpecific -> RequestUtils.extractStorageAreaInfo(
                                                operationRequest, athenaSpecific)
                                        .flatMap(storageAreaInfo -> RequestUtils.extractStorageAreaRegion(
                                                        storageAreaInfo)
                                                .flatMap(storageAreaRegion -> {
                                                    AthenaClient athenaClient =
                                                            athenaClientProvider.apply(Region.of(storageAreaRegion));
                                                    LakeFormationClient lakeFormationClient =
                                                            lakeFormationClientProvider.apply(
                                                                    Region.of(storageAreaRegion));
                                                    GlueClient glueClient =
                                                            glueClientProvider.apply(Region.of(storageAreaRegion));

                                                    AthenaTable sourceTable = athenaSpecific.getSourceTable();
                                                    AthenaView targetView = athenaSpecific.getView();

                                                    return createOutputLocation(storageAreaInfo, outputPort)
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
                                                                            sourceTable.getTableFormat(),
                                                                            outputPort
                                                                                    .getDataContract()
                                                                                    .getSchema()))
                                                                    .flatMap(ignored4 -> createView(
                                                                            athenaClient,
                                                                            lakeFormationClient,
                                                                            glueClient,
                                                                            outputLocation,
                                                                            sourceTable,
                                                                            targetView,
                                                                            outputPort
                                                                                    .getDataContract()
                                                                                    .getSchema()))
                                                                    .map(ignored5 -> {
                                                                        var publicInfo = Map.of(
                                                                                "view",
                                                                                Map.of(
                                                                                        "type", "string",
                                                                                        "label", "View name",
                                                                                        "value", targetView.getName()),
                                                                                "database",
                                                                                Map.of(
                                                                                        "type", "string",
                                                                                        "label", "Database",
                                                                                        "value",
                                                                                                targetView
                                                                                                        .getDatabase()),
                                                                                "catalog",
                                                                                Map.of(
                                                                                        "type", "string",
                                                                                        "label", "Catalog",
                                                                                        "value",
                                                                                                targetView
                                                                                                        .getCatalog()),
                                                                                "region",
                                                                                Map.of(
                                                                                        "type", "string",
                                                                                        "label", "AWS region",
                                                                                        "value", storageAreaRegion));

                                                                        Map<String, @NotNull Map<String, String>>
                                                                                privateInfo = new HashMap<>(publicInfo);
                                                                        privateInfo.put(
                                                                                "s3Location",
                                                                                Map.of(
                                                                                        "type", "string",
                                                                                        "label", "S3 location",
                                                                                        "value", outputLocation));

                                                                        return ProvisionInfo.builder()
                                                                                .privateInfo(Optional.of(privateInfo))
                                                                                .publicInfo(Optional.of(publicInfo))
                                                                                .build();
                                                                    }));
                                                })))));
    }

    @Override
    public Either<FailedOperation, ProvisionInfo> unprovision(
            ProvisionOperationRequest<?, ? extends Specific> operationRequest) {

        return outputPortValidationService
                .validate(operationRequest, OperationType.UNPROVISION)
                .flatMap(ignored -> RequestUtils.getAthenaOutputPort(operationRequest)
                        .flatMap(outputPort -> RequestUtils.getAthenaSpecific(outputPort)
                                .flatMap(athenaSpecific -> RequestUtils.extractStorageAreaInfo(
                                                operationRequest, athenaSpecific)
                                        .flatMap(storageAreaInfo -> RequestUtils.extractStorageAreaRegion(
                                                        storageAreaInfo)
                                                .flatMap(storageAreaRegion -> {
                                                    AthenaClient athenaClient =
                                                            athenaClientProvider.apply(Region.of(storageAreaRegion));
                                                    AthenaView athenaView = athenaSpecific.getView();

                                                    return createOutputLocation(storageAreaInfo, outputPort)
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

                                                                return ProvisionInfo.builder()
                                                                        .privateInfo(Optional.of(info))
                                                                        .publicInfo(Optional.of(info))
                                                                        .build();
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
            TableFormat tableFormat,
            List<AthenaColumn> schema) {

        return athenaManager
                .getTableMetadata(athenaClient, catalog, database, name)
                .flatMap(metadata -> {
                    if (metadata.isEmpty()) {
                        return athenaManager.createTable(
                                athenaClient, outputLocation, catalog, database, name, tableFormat, schema);
                    } else {
                        return Either.right(null);
                    }
                });
    }

    private Either<FailedOperation, Void> createView(
            AthenaClient athenaClient,
            LakeFormationClient lakeFormationClient,
            GlueClient glueClient,
            String outputLocation,
            AthenaTable athenaTable,
            AthenaView athenaView,
            List<AthenaColumn> columns) {

        if (!enforceLakeFormation.equals("true"))
            return athenaManager.createView(athenaClient, outputLocation, athenaTable, athenaView, columns);

        try {
            String accountId = stsClient.getCallerIdentity().account();

            String awsServiceRoleForLakeFormationDataAccessArn =
                    "arn:aws:iam::{accountID}:role/aws-service-role/lakeformation.amazonaws.com/AWSServiceRoleForLakeFormationDataAccess"
                            .replace("{accountID}", accountId);

            return athenaManager
                    .getTableLocation(glueClient, athenaTable)
                    .flatMap(tableLocation -> extractS3Arn(tableLocation).flatMap(locationArn -> lakeFormationManager
                            .registerDataLakeLocation(
                                    lakeFormationClient, locationArn, awsServiceRoleForLakeFormationDataAccessArn)
                            .flatMap(ignored -> athenaManager.createMultiDialectView(
                                    athenaClient, outputLocation, athenaTable, athenaView, columns))));

        } catch (Exception e) {
            String error = String.format(
                    "An unexpected error occurred while getting AWS caller identity. Details: %s", e.getMessage());
            logger.error(error, e);
            return Either.left(new FailedOperation(error, List.of(new Problem(error, e))));
        }
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
            JsonNode storageAreaInfo, com.witboost.provisioning.model.Component<? extends Specific> component) {

        String[] componentIdParts = component.getId().split(":");
        String dpVersion = componentIdParts[componentIdParts.length - 2];
        String formattedComponentName = componentIdParts[componentIdParts.length - 1];
        String folderPath = "v" + dpVersion + "/athena/" + formattedComponentName;

        Either<FailedOperation, String> bucketName = extractBucketName(storageAreaInfo);
        if (bucketName.isLeft()) return Either.left(bucketName.getLeft());

        return Either.right(buildS3Url(bucketName.get(), folderPath));
    }

    protected Either<FailedOperation, String> extractS3Arn(String s3Location) {
        if (s3Location == null || !s3Location.startsWith("s3://")) {
            String error = "An error occurred while extracting S3 arn. Invalid S3 location: " + s3Location;
            logger.error(error);
            return Either.left(new FailedOperation(error, List.of(new Problem(error))));
        }

        String pathWithoutPrefix = s3Location.substring(5);
        String[] parts = pathWithoutPrefix.split("/", 2);
        String bucket = parts[0];
        String path = parts.length > 1 ? "/" + parts[1] : "";

        return Either.right("arn:aws:s3:::" + bucket + path);
    }
}
