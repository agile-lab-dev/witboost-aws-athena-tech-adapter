package com.witboost.provisioning.athena.awsClient;

import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import io.vavr.control.Either;
import java.util.List;
import java.util.Optional;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.*;

@NoArgsConstructor
@Service
public class AthenaManager {

    private final Logger logger = LoggerFactory.getLogger(AthenaManager.class);

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
            AthenaClient athenaClient, String catalog, String database) {
        try {
            ListDatabasesResponse listDatabasesResponse = athenaClient.listDatabases(
                    ListDatabasesRequest.builder().catalogName(catalog).build());

            if (!listDatabasesResponse.hasDatabaseList()) return Either.right(false);

            boolean exists = listDatabasesResponse.databaseList().stream()
                    .anyMatch(db -> db.name().equalsIgnoreCase(database));

            if (!exists) {
                String msg = String.format("Database %s not found in catalog %s", database, catalog);
                logger.info(msg);
                return Either.right(false);
            }

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
            AthenaClient athenaClient, String catalog, String database, String tableName) {
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
                String msg =
                        String.format("Table %s not found in database %s (catalog: %s)", tableName, database, catalog);
                logger.info(msg);
                return Either.right(Optional.empty());
            }

            return Either.right(Optional.of(tableMetadata.get()));

        } catch (Exception e) {
            String error = String.format(
                    "An unexpected error occurred while retrieving metadata for table %s in database %s (catalog: %s). Details: %s",
                    tableName, database, catalog, e.getMessage());
            logger.error(error, e);
            return Either.left(new FailedOperation(error, List.of(new Problem(error, e))));
        }
    }
}
