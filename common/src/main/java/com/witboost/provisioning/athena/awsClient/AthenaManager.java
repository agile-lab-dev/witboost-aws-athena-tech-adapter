package com.witboost.provisioning.athena.awsClient;

import com.witboost.provisioning.athena.model.AthenaColumn;
import com.witboost.provisioning.athena.model.AthenaTable;
import com.witboost.provisioning.athena.model.AthenaView;
import com.witboost.provisioning.athena.model.TableFormat;
import com.witboost.provisioning.athena.utils.AthenaTableSQLGenerator;
import com.witboost.provisioning.athena.utils.typechecker.TypeCheckerFactory;
import com.witboost.provisioning.model.Column;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import io.vavr.control.Either;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.*;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.glue.model.GetTableRequest;
import software.amazon.awssdk.services.glue.model.GetTableResponse;
import software.amazon.awssdk.services.glue.model.Table;

@NoArgsConstructor
@Service
public class AthenaManager {

    private final Logger logger = LoggerFactory.getLogger(AthenaManager.class);

    @Value("${queryTimeoutSeconds:60}")
    private int queryTimeoutSeconds;

    @PostConstruct
    public void validateTimeout() {
        if (queryTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("Query timeout must be greater than zero. "
                    + "Please check the 'QUERY_TIMEOUT_SECONDS' environment variable or application configuration.");
        }
    }

    /**
     * Checks if a specific database exists within a given Athena catalog.
     *
     * @param athenaClient the Athena client used to interact with the service
     * @param catalog      the catalog name where the database should be located
     * @param database     the name of the database to check for existence
     * @return {@code Either<FailedOperation, Boolean>}:
     *         - {@code Right(true)} if the database exists
     *         - {@code Right(false)} if the database does not exist
     *         - {@code Left(FailedOperation)} if an error occurs during the operation
     */
    public Either<FailedOperation, Boolean> checkDatabaseExists(
            @NotNull AthenaClient athenaClient, @NotBlank String catalog, @NotBlank String database) {
        logger.debug("Checking if database '{}' exists in catalog '{}'", database, catalog);
        try {
            ListDatabasesResponse listDatabasesResponse = athenaClient.listDatabases(
                    ListDatabasesRequest.builder().catalogName(catalog).build());

            if (!listDatabasesResponse.hasDatabaseList()) return Either.right(false);

            boolean exists = listDatabasesResponse.databaseList().stream()
                    .anyMatch(db -> db.name().equalsIgnoreCase(database));

            if (!exists) {
                logger.debug("Database '{}' does not exist in catalog '{}'", database, catalog);
                return Either.right(false);
            }

            logger.debug("Database '{}' found in catalog '{}'", database, catalog);
            return Either.right(true);

        } catch (Exception e) {
            String error = String.format(
                    "An unexpected error occurred while checking the existence of database %s in %s. Details: %s",
                    database, catalog, e.getMessage());
            logger.error(error, e);
            return Either.left(new FailedOperation(error, List.of(new Problem(error, e))));
        }
    }

    /**
     * Retrieves metadata for a specific table within a given database and catalog in Athena.
     *
     * @param athenaClient the Athena client used to interact with the service
     * @param catalog      the catalog name where the database is located
     * @param database     the name of the database containing the table
     * @param tableName    the name of the table whose metadata is to be retrieved
     * @return {@code Either<FailedOperation, Optional<TableMetadata>>}:
     *         - {@code Right(Optional.of(TableMetadata))} if the table exists
     *         - {@code Right(Optional.empty())} if the table does not exist
     *         - {@code Left(FailedOperation)} if an error occurs during the operation
     */
    public Either<FailedOperation, Optional<TableMetadata>> getTableMetadata(
            @NotNull AthenaClient athenaClient,
            @NotBlank String catalog,
            @NotBlank String database,
            @NotBlank String tableName) {
        logger.debug(
                "Retrieving metadata for table '{}' in database '{}' (catalog: '{}')", tableName, database, catalog);
        try {
            ListTableMetadataResponse listTableResponse =
                    athenaClient.listTableMetadata(ListTableMetadataRequest.builder()
                            .catalogName(catalog)
                            .databaseName(database)
                            .build());

            Optional<TableMetadata> tableMetadata = listTableResponse.tableMetadataList().stream()
                    .filter(table -> table.name().equalsIgnoreCase(tableName))
                    .findFirst();

            if (tableMetadata.isEmpty()) {
                logger.debug(
                        "Table '{}' does not exist in database '{}' (catalog: '{}')", tableName, database, catalog);
                return Either.right(Optional.empty());
            }

            logger.info("Table '{}' found in database '{}' (catalog: '{}')", tableName, database, catalog);
            return Either.right(tableMetadata);

        } catch (Exception e) {
            String error = String.format(
                    "An unexpected error occurred while retrieving metadata for table %s in database %s (catalog: %s). Details: %s",
                    tableName, database, catalog, e.getMessage());
            logger.error(error, e);
            return Either.left(new FailedOperation(error, List.of(new Problem(error, e))));
        }
    }

    /**
     * Submits a query to Athena and returns the execution ID.
     *
     * @param athenaClient   the Athena client used to interact with the service
     * @param outputLocation the S3 location where query results are stored
     * @param catalog        the Athena catalog name
     * @param queryString    the SQL query string to execute
     * @return {@code Either<FailedOperation, String>} containing execution ID if successful, or error details
     */
    protected Either<FailedOperation, String> submitQuery(
            @NotNull AthenaClient athenaClient,
            @NotBlank String outputLocation,
            @NotBlank String catalog,
            @NotBlank String queryString) {
        logger.debug("Submitting query to Athena: {}", queryString);
        try {
            QueryExecutionContext queryExecutionContext =
                    QueryExecutionContext.builder().catalog(catalog).build();

            // Specifies where the results of the query should go.
            ResultConfiguration resultConfiguration =
                    ResultConfiguration.builder().outputLocation(outputLocation).build();

            StartQueryExecutionRequest request = StartQueryExecutionRequest.builder()
                    .queryString(queryString)
                    .queryExecutionContext(queryExecutionContext)
                    .resultConfiguration(resultConfiguration)
                    .build();

            StartQueryExecutionResponse queryExecution = athenaClient.startQueryExecution(request);

            logger.debug("Query submitted successfully, Execution ID: {}", queryExecution.queryExecutionId());
            return Either.right(queryExecution.queryExecutionId());

        } catch (AthenaException e) {
            String error = String.format(
                    "Athena-specific error during query submission: %s",
                    e.awsErrorDetails().errorMessage());
            logger.error(error, e);
            return Either.left(new FailedOperation(error, List.of(new Problem(error, e))));
        } catch (Exception e) {
            String error = String.format("An unexpected error occurred during query submission: %s", e.getMessage());
            logger.error(error, e);
            return Either.left(new FailedOperation(error, List.of(new Problem(error, e))));
        }
    }

    protected Either<FailedOperation, Void> waitForQueryToComplete(AthenaClient athenaClient, String queryExecutionId) {
        try {
            long startTime = System.currentTimeMillis();
            long timeout = queryTimeoutSeconds * 1000L;

            while (true) {
                GetQueryExecutionRequest getQueryExecutionRequest = GetQueryExecutionRequest.builder()
                        .queryExecutionId(queryExecutionId)
                        .build();

                GetQueryExecutionResponse getQueryExecutionResponse =
                        athenaClient.getQueryExecution(getQueryExecutionRequest);
                QueryExecutionState state =
                        getQueryExecutionResponse.queryExecution().status().state();

                switch (state) {
                    case SUCCEEDED:
                        return Either.right(null);
                    case FAILED:
                        String errorFailedQuery = String.format(
                                "Query %s failed. Result: %s",
                                queryExecutionId,
                                getQueryExecutionResponse
                                        .queryExecution()
                                        .status()
                                        .stateChangeReason());
                        logger.error(errorFailedQuery);
                        return Either.left(
                                new FailedOperation(errorFailedQuery, List.of(new Problem(errorFailedQuery))));
                    case CANCELLED:
                        String errorCancelledQuery = String.format(
                                "Query %s cancelled. %s",
                                queryExecutionId,
                                getQueryExecutionResponse
                                        .queryExecution()
                                        .status()
                                        .stateChangeReason());
                        logger.error(errorCancelledQuery);
                        return Either.left(
                                new FailedOperation(errorCancelledQuery, List.of(new Problem(errorCancelledQuery))));
                    default:
                        if (System.currentTimeMillis() - startTime > timeout) {
                            String errorTimeOut = String.format(
                                    "Query %s timeout: execution time exceeded %d seconds",
                                    queryExecutionId, queryTimeoutSeconds);
                            logger.error(errorTimeOut);
                            return Either.left(new FailedOperation(errorTimeOut, List.of(new Problem(errorTimeOut))));
                        }

                        Thread.sleep(5000);
                }
            }

        } catch (Exception e) {
            String error = String.format(
                    "An unexpected error waiting for the completion of the %s query execution. Details: %s",
                    queryExecutionId, e.getMessage());
            logger.error(error, e);
            return Either.left(new FailedOperation(error, List.of(new Problem(error, e))));
        }
    }

    /**
     * Executes a DDL (Data Definition Language) statement in Athena.
     *
     * @param athenaClient   the Athena client used to interact with the service
     * @param outputLocation the S3 location where query results are stored
     * @param catalog        the Athena catalog name
     * @param query          the SQL DDL query to execute
     * @return {@code Either<FailedOperation, Void>} indicating success or failure
     */
    private Either<FailedOperation, Void> executeDDL(
            @NotNull AthenaClient athenaClient,
            @NotBlank String outputLocation,
            @NotBlank String catalog,
            @NotBlank String query) {
        return submitQuery(athenaClient, outputLocation, catalog, query)
                .flatMap(queryID -> waitForQueryToComplete(athenaClient, queryID));
    }

    /**
     * Creates a new database in Athena if it does not already exist.
     *
     * @param athenaClient   the Athena client used to interact with the service
     * @param outputLocation the S3 location where query results are stored
     * @param catalog        the Athena catalog name
     * @param database       the name of the database to create
     * @return {@code Either<FailedOperation, Void>} indicating success or failure
     *         - {@code Right(Void)} if the database was successfully created or already exists
     *         - {@code Left(FailedOperation)} if an error occurs during the operation
     */
    public Either<FailedOperation, Void> createDatabase(
            @NotNull AthenaClient athenaClient,
            @NotBlank String outputLocation,
            @NotBlank String catalog,
            @NotBlank String database) {
        logger.info("Starting creation of database '{}' in catalog '{}'", database, catalog);
        String query = String.format("CREATE DATABASE IF NOT EXISTS %s;", database);

        Either<FailedOperation, Void> result = executeDDL(athenaClient, outputLocation, catalog, query);

        if (result.isRight()) {
            logger.info("Database '{}' created successfully in catalog '{}'", database, catalog);
        } else {
            logger.error(
                    "Failed to create database '{}' in catalog '{}'. Error: {}", database, catalog, result.getLeft());
        }

        return result;
    }

    /**
     * Creates a new table in Athena with the specified schema if it does not already exist.
     *
     * @param athenaClient   the Athena client used to interact with the service
     * @param outputLocation the S3 location where query results are stored
     * @param catalog        the Athena catalog name
     * @param database       the database name where the table should be created
     * @param name           the name of the table to create
     * @param schema         the schema definition of the table
     * @return {@code Either<FailedOperation, Void>} indicating success or failure
     *         - {@code Right(Void)} if the table was successfully created or already exists
     *         - {@code Left(FailedOperation)} if an error occurs during the operation
     */
    public Either<FailedOperation, Void> createTable(
            @NotNull AthenaClient athenaClient,
            @NotBlank String outputLocation,
            @NotBlank String catalog,
            @NotBlank String database,
            @NotBlank String name,
            @NotBlank TableFormat tableFormat,
            @Valid @NotNull List<AthenaColumn> schema) {

        AthenaTableSQLGenerator athenaTableSQLGenerator =
                new AthenaTableSQLGenerator(TypeCheckerFactory.getTypeChecker(tableFormat));

        logger.info("Starting creation of table '{}' in database '{}'", name, database);

        Either<FailedOperation, String> sqlQuery = athenaTableSQLGenerator.generateCreateTableSQL(
                database, name, tableFormat.name(), outputLocation, schema);

        if (sqlQuery.isLeft()) {
            logger.error("Failed to generate CREATE TABLE SQL for table '{}'. Error: {}", name, sqlQuery.getLeft());
            return Either.left(sqlQuery.getLeft());
        }

        Either<FailedOperation, Void> result = executeDDL(athenaClient, outputLocation, catalog, sqlQuery.get());

        if (result.isRight()) {
            logger.info("Table '{}' created successfully in database '{}'", name, database);
        } else {
            logger.error("Failed to create table '{}' in database '{}'. Error: {}", name, database, result.getLeft());
        }

        return result;
    }

    /**
     * Creates or replaces a view in Athena.
     *
     * @param athenaClient   the Athena client used to interact with the service
     * @param outputLocation the S3 location where query results are stored
     * @param athenaTable    the underlying table used in the view
     * @param athenaView     the view to create or replace
     * @param columns        the column list for the view
     * @return {@code Either<FailedOperation, Void>} indicating success or failure
     *         - {@code Right(Void)} if the view was successfully created or replaced
     *         - {@code Left(FailedOperation)} if an error occurs during the operation
     */
    public Either<FailedOperation, Void> createView(
            @NotNull AthenaClient athenaClient,
            @NotBlank String outputLocation,
            @Valid @NotNull AthenaTable athenaTable,
            @Valid @NotNull AthenaView athenaView,
            @Valid @NotNull List<AthenaColumn> columns) {

        String columnList = createColumnsListForSelectStatement(columns);

        String query = String.format(
                "CREATE OR REPLACE VIEW %s.%s AS SELECT %s FROM %s.%s;",
                athenaView.getDatabase(),
                athenaView.getName(),
                columnList,
                athenaTable.getDatabase(),
                athenaTable.getName());

        logger.info("Starting creation of view '{}' in database '{}'", athenaView.getName(), athenaView.getDatabase());
        logger.debug("Generated SQL for view '{}': {}", athenaView.getName(), query);

        Either<FailedOperation, Void> result = executeDDL(athenaClient, outputLocation, athenaView.getCatalog(), query);

        if (result.isRight()) {
            logger.info(
                    "View '{}' created successfully in database '{}'", athenaView.getName(), athenaView.getDatabase());
        } else {
            logger.error(
                    "Failed to create view '{}' in database '{}'. Error: {}",
                    athenaView.getName(),
                    athenaView.getDatabase(),
                    result.getLeft());
        }

        return result;
    }

    /**
     * Drops a view from Athena if it exists.
     *
     * @param athenaClient   the Athena client used to interact with the service
     * @param outputLocation the S3 location where query results are stored
     * @param athenaView     the view to be dropped
     * @return {@code Either<FailedOperation, Void>} indicating success or failure
     *         - {@code Right(Void)} if the view was successfully dropped or did not exist
     *         - {@code Left(FailedOperation)} if an error occurs during the operation
     */
    public Either<FailedOperation, Void> dropView(
            @NotNull AthenaClient athenaClient,
            @NotBlank String outputLocation,
            @Valid @NotNull AthenaView athenaView) {

        String query = String.format("DROP VIEW IF EXISTS %s.%s;", athenaView.getDatabase(), athenaView.getName());
        logger.info("Dropping view '{}' from database '{}'", athenaView.getName(), athenaView.getDatabase());
        logger.debug("Generated SQL for dropping view '{}': {}", athenaView.getName(), query);

        Either<FailedOperation, Void> result = executeDDL(athenaClient, outputLocation, athenaView.getCatalog(), query);

        if (result.isRight()) {
            logger.info(
                    "View '{}' successfully deleted from database '{}'",
                    athenaView.getName(),
                    athenaView.getDatabase());
        } else {
            logger.error(
                    "Failed to delete view '{}' from database '{}'. Error: {}",
                    athenaView.getName(),
                    athenaView.getDatabase(),
                    result.getLeft());
        }

        return result;
    }

    private String createColumnsListForSelectStatement(List<AthenaColumn> columnList) {

        if (columnList.isEmpty()) return "*";

        List<String> columnsList = new ArrayList<>();

        for (Column viewColumn : columnList) {
            String viewColumnName = viewColumn.getName();
            columnsList.add(viewColumnName);
        }

        return String.join(", ", columnsList);
    }

    /**
     * Creates or replaces a PROTECTED MULTI DIALECT view in Athena.
     *
     * @param athenaClient   the Athena client used to interact with the service
     * @param outputLocation the S3 location where query results are stored
     * @param athenaTable    the underlying table used in the view
     * @param athenaView     the view to create or replace
     * @param columns        the column list for the view
     * @return {@code Either<FailedOperation, Void>} indicating success or failure
     *         - {@code Right(Void)} if the multi-dialect view was successfully created or replaced
     *         - {@code Left(FailedOperation)} if an error occurs during the operation
     */
    public Either<FailedOperation, Void> createMultiDialectView(
            @NotNull AthenaClient athenaClient,
            @NotBlank String outputLocation,
            @Valid @NotNull AthenaTable athenaTable,
            @Valid @NotNull AthenaView athenaView,
            @Valid @NotNull List<AthenaColumn> columns) {

        String columnList = createColumnsListForSelectStatement(columns);

        String query = String.format(
                "CREATE OR REPLACE PROTECTED MULTI DIALECT VIEW %s.%s SECURITY DEFINER " + "AS SELECT %s FROM %s.%s;",
                athenaView.getDatabase(),
                athenaView.getName(),
                columnList,
                athenaTable.getDatabase(),
                athenaTable.getName());

        logger.info(
                "Starting creation of PROTECTED MULTI DIALECT view '{}' in database '{}'",
                athenaView.getName(),
                athenaView.getDatabase());
        logger.debug("Generated SQL for multi-dialect view '{}': {}", athenaView.getName(), query);

        Either<FailedOperation, Void> result = executeDDL(athenaClient, outputLocation, athenaView.getCatalog(), query);

        if (result.isRight()) {
            logger.info(
                    "PROTECTED MULTI DIALECT view '{}' created successfully in database '{}'",
                    athenaView.getName(),
                    athenaView.getDatabase());
        } else {
            logger.error(
                    "Failed to create PROTECTED MULTI DIALECT view '{}' in database '{}'. Error: {}",
                    athenaView.getName(),
                    athenaView.getDatabase(),
                    result.getLeft());
        }

        return result;
    }

    public Either<FailedOperation, String> getTableLocation(
            @NotNull GlueClient glueClient, @Valid @NotNull AthenaTable athenaTable) {
        try {

            GetTableRequest request = GetTableRequest.builder()
                    .databaseName(athenaTable.getDatabase())
                    .name(athenaTable.getName())
                    .build();

            GetTableResponse response = glueClient.getTable(request);
            Table table = response.table();

            String location = table.storageDescriptor().location();

            return Either.right(location);

        } catch (Exception e) {
            String error = String.format(
                    "An unexpected error occurred while getting table location of %s: %s.%s",
                    athenaTable.getDatabase(), athenaTable.getName(), e.getMessage());
            logger.error(error, e);
            return Either.left(new FailedOperation(error, List.of(new Problem(error, e))));
        }
    }
}
