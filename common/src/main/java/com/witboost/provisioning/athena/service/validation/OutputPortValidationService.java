package com.witboost.provisioning.athena.service.validation;

import com.witboost.provisioning.athena.awsClient.AthenaManager;
import com.witboost.provisioning.athena.utils.RequestUtils;
import com.witboost.provisioning.framework.service.validation.ComponentValidationService;
import com.witboost.provisioning.model.Column;
import com.witboost.provisioning.model.OperationType;
import com.witboost.provisioning.model.OutputPort;
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
        var component = RequestUtils.getOutputPort(operationRequest);
        if (component.isLeft()) return Either.left(component.getLeft());

        var athenaSpecific = RequestUtils.getAthenaSpecific(component.get());
        if (athenaSpecific.isLeft()) return Either.left(athenaSpecific.getLeft());

        AthenaClient athenaClient =
                athenaClientProvider.apply(athenaSpecific.get().getRegion());
        String catalog = athenaSpecific.get().getSourceTable().getCatalog();
        String database = athenaSpecific.get().getSourceTable().getDatabase();
        String table = athenaSpecific.get().getSourceTable().getName();

        if (component.get().getDataContract() == null
                || component.get().getDataContract().getSchema() == null) {
            String error = String.format(
                    "Validation error for output port '%s': the Data Contract or its schema is null. "
                            + "Please define the required columns in the Data Contract schema.",
                    component.get().getName());
            logger.error(error);
            return Either.left(new FailedOperation(error, List.of(new Problem(error))));
        }

        return athenaManager
                .checkDatabaseExists(athenaClient, catalog, database)
                .flatMap(exists -> exists
                        ? validateTable(athenaClient, catalog, database, table, component.get())
                        : validateDataContractSchemaPresence(component.get(), table, database));
    }

    private Either<FailedOperation, Void> validateTable(
            AthenaClient athenaClient, String catalog, String database, String table, OutputPort component) {
        return athenaManager
                .getTableMetadata(athenaClient, catalog, database, table)
                .flatMap(metadataOpt -> metadataOpt.isPresent()
                        ? validateColumnSchema(
                                component.getDataContract().getSchema(), metadataOpt.get(), component.getName())
                        : validateDataContractSchemaPresence(component, table, database));
    }

    private Either<FailedOperation, Void> validateDataContractSchemaPresence(
            OutputPort component, String table, String database) {
        if (component.getDataContract().getSchema().isEmpty()) {
            String error = String.format(
                    "Validation error for output port '%s': the source database '%s' and/or table '%s' do not exist, and no columns are defined in the output port's Data Contract schema. "
                            + "To resolve this issue, please ensure that the database and table are created or define the required columns in the Data Contract schema.",
                    component.getName(), database, table);
            logger.error(error);
            return Either.left(new FailedOperation(error, List.of(new Problem(error))));
        }
        return Either.right(null);
    }

    private Either<FailedOperation, Void> validateColumnSchema(
            List<Column> schemaColumns, TableMetadata tableMetadata, String componentName) {
        Map<String, String> athenaColumns = tableMetadata.columns().stream()
                .collect(Collectors.toMap(
                        software.amazon.awssdk.services.athena.model.Column::name,
                        software.amazon.awssdk.services.athena.model.Column::type));

        List<String> errors = new ArrayList<>();

        for (Column schemaColumn : schemaColumns) {
            String columnName = schemaColumn.getName();
            String schemaType = schemaColumn.getDataType();

            if (!athenaColumns.containsKey(columnName)) {
                errors.add(String.format("Column '%s' not found in source table", columnName));
            } else if (!athenaColumns.get(columnName).equalsIgnoreCase(schemaType)) {
                errors.add(String.format(
                        "Type mismatch for column '%s': expected %s (from DataContract), found %s",
                        columnName, schemaType, athenaColumns.get(columnName)));
            }
        }

        if (!errors.isEmpty()) {
            String error = String.format(
                    "Error validating output port '%s': Some columns defined in the Data Contract are either missing from the source table or have mismatched types.",
                    componentName);
            return Either.left(new FailedOperation(error, List.of(new Problem(String.join("; ", errors)))));
        }

        return Either.right(null);
    }
}
