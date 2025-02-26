package com.witboost.provisioning.athena.service.validation;

import com.witboost.provisioning.athena.awsClient.AthenaManager;
import com.witboost.provisioning.athena.model.AthenaColumn;
import com.witboost.provisioning.athena.model.AthenaOutputPort;
import com.witboost.provisioning.athena.model.AthenaSpecific;
import com.witboost.provisioning.athena.model.TableFormat;
import com.witboost.provisioning.athena.utils.RequestUtils;
import com.witboost.provisioning.athena.utils.typechecker.TypeChecker;
import com.witboost.provisioning.athena.utils.typechecker.TypeCheckerFactory;
import com.witboost.provisioning.framework.service.validation.ComponentValidationService;
import com.witboost.provisioning.model.OperationType;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import com.witboost.provisioning.model.request.OperationRequest;
import io.vavr.control.Either;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.*;

@Service
@Validated
public class OutputPortValidationService implements ComponentValidationService {

    private final Logger logger = LoggerFactory.getLogger(OutputPortValidationService.class);

    private final Function<Region, AthenaClient> athenaClientProvider;

    private final AthenaManager athenaManager;

    public OutputPortValidationService(
            Function<Region, AthenaClient> athenaClientProvider, AthenaManager athenaManager) {
        this.athenaClientProvider = athenaClientProvider;
        this.athenaManager = athenaManager;
    }

    @Override
    public Either<FailedOperation, Void> validate(
            @Valid OperationRequest<?, ? extends Specific> operationRequest, OperationType operationType) {

        Either<FailedOperation, AthenaOutputPort<? extends Specific>> component =
                RequestUtils.getAthenaOutputPort(operationRequest);
        if (component.isLeft()) return Either.left(component.getLeft());

        var athenaSpecific = RequestUtils.getAthenaSpecific(component.get());
        if (athenaSpecific.isLeft()) return Either.left(athenaSpecific.getLeft());

        if (operationType.equals(OperationType.PROVISION) || operationType.equals(OperationType.VALIDATE)) {

            return RequestUtils.extractStorageAreaInfo(operationRequest, athenaSpecific.get())
                    .flatMap(storageAreaInfo -> RequestUtils.extractStorageAreaRegion(storageAreaInfo)
                            .flatMap(storageAreaRegion -> {
                                AthenaClient athenaClient = athenaClientProvider.apply(Region.of(storageAreaRegion));
                                String catalog =
                                        athenaSpecific.get().getSourceTable().getCatalog();
                                String database =
                                        athenaSpecific.get().getSourceTable().getDatabase();
                                String table =
                                        athenaSpecific.get().getSourceTable().getName();

                                if (component.get().getDataContract() == null
                                        || component.get().getDataContract().getSchema() == null) {
                                    String error = String.format(
                                            "Validation error for output port '%s': the Data Contract or its schema is null. "
                                                    + "Please define the required columns in the Data Contract schema.",
                                            component.get().getName());
                                    logger.error(error);
                                    return Either.left(new FailedOperation(error, List.of(new Problem(error))));
                                }

                                // The cast is safe because getAthenaSpecific() ensures that component.get() is an
                                // AthenaOutputPort<AthenaSpecific>
                                AthenaOutputPort<AthenaSpecific> athenaOutputPort =
                                        (AthenaOutputPort<AthenaSpecific>) component.get();

                                return athenaManager
                                        .checkDatabaseExists(athenaClient, catalog, database)
                                        .flatMap(exists -> exists
                                                ? validateTable(
                                                        athenaClient, catalog, database, table, athenaOutputPort)
                                                : validateDataContractSchema(athenaOutputPort, table, database));
                            }));
        }

        return Either.right(null);
    }

    private Either<FailedOperation, Void> validateTable(
            AthenaClient athenaClient,
            String catalog,
            String database,
            String table,
            AthenaOutputPort<AthenaSpecific> component) {

        Either<FailedOperation, AthenaSpecific> athenaSpecific = RequestUtils.getAthenaSpecific(component);
        if (athenaSpecific.isLeft()) return Either.left(athenaSpecific.getLeft());

        return athenaManager
                .getTableMetadata(athenaClient, catalog, database, table)
                .flatMap(metadataOpt -> metadataOpt.isPresent()
                        ? validateColumnSchema(
                                component.getDataContract().getSchema(),
                                athenaSpecific.get().getSourceTable().getTableFormat(),
                                metadataOpt.get(),
                                component.getName())
                        : validateDataContractSchema(component, table, database));
    }

    private Either<FailedOperation, Void> validateDataContractSchema(
            AthenaOutputPort<AthenaSpecific> outputPort, String table, String database) {
        if (outputPort.getDataContract().getSchema().isEmpty()) {
            String error = String.format(
                    "Validation error for output port '%s': the source database '%s' and/or table '%s' do not exist, and no columns are defined in the output port's Data Contract schema. "
                            + "To resolve this issue, please ensure that the database and table are created or define the required columns in the Data Contract schema.",
                    outputPort.getName(), database, table);
            logger.error(error);
            return Either.left(new FailedOperation(error, List.of(new Problem(error))));
        }

        TypeChecker typeChecker = TypeCheckerFactory.getTypeChecker(
                outputPort.getSpecific().getSourceTable().getTableFormat());

        for (AthenaColumn schemaColumn : outputPort.getDataContract().getSchema()) {
            Either<FailedOperation, String> schemaType = typeChecker.resolveColumnType(schemaColumn);
            if (schemaType.isLeft()) return Either.left(schemaType.getLeft());
        }

        return Either.right(null);
    }

    private Either<FailedOperation, Void> validateColumnSchema(
            List<AthenaColumn> schemaColumns,
            TableFormat tableFormat,
            TableMetadata tableMetadata,
            String componentName) {

        Map<String, String> athenaColumns = tableMetadata.columns().stream()
                .collect(Collectors.toMap(
                        software.amazon.awssdk.services.athena.model.Column::name,
                        software.amazon.awssdk.services.athena.model.Column::type));

        List<String> errors = new ArrayList<>();

        TypeChecker typeChecker = TypeCheckerFactory.getTypeChecker(tableFormat);

        for (AthenaColumn schemaColumn : schemaColumns) {
            String columnName = schemaColumn.getName();

            Either<FailedOperation, String> schemaType = typeChecker.resolveColumnType(schemaColumn);
            if (schemaType.isLeft()) return Either.left(schemaType.getLeft());

            if (!athenaColumns.containsKey(columnName)) {
                String e = String.format("Column '%s' not found in source table", columnName);
                logger.error(e);
                errors.add(e);
            } else if (!athenaColumns.get(columnName).equalsIgnoreCase(schemaType.get())) {
                String e = String.format(
                        "Type mismatch for column '%s': expected %s (from DataContract), found %s",
                        columnName, schemaType.get(), athenaColumns.get(columnName));
                logger.error(e);
                errors.add(e);
            }
        }

        if (!errors.isEmpty()) {
            String error = String.format(
                    "Validation error for output port '%s': Some columns defined in the Data Contract are either missing from the source table or have mismatched types.",
                    componentName);
            return Either.left(new FailedOperation(error, List.of(new Problem(String.join("; ", errors)))));
        }

        return Either.right(null);
    }
}
