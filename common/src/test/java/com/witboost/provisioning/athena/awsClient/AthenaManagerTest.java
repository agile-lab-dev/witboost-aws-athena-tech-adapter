package com.witboost.provisioning.athena.awsClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.witboost.provisioning.model.common.FailedOperation;
import io.vavr.control.Either;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.Database;
import software.amazon.awssdk.services.athena.model.ListDatabasesRequest;
import software.amazon.awssdk.services.athena.model.ListDatabasesResponse;
import software.amazon.awssdk.services.athena.model.ListTableMetadataRequest;
import software.amazon.awssdk.services.athena.model.ListTableMetadataResponse;
import software.amazon.awssdk.services.athena.model.TableMetadata;

class AthenaManagerTest {

    private AthenaManager athenaManager;
    private AthenaClient athenaClient;

    private final String catalog = "awsCatalog";
    private final String database = "testDatabase";
    private final String tableName = "testTable";

    @BeforeEach
    void setUp() {
        athenaManager = new AthenaManager();
        athenaClient = mock(AthenaClient.class);
    }

    // Test for checkDatabaseExists when the database exists
    @Test
    void checkDatabaseExists_whenDatabaseExists_returnsTrue() {
        // Create a ListDatabasesResponse with one database that matches (ignoring case)
        Database db = Database.builder().name("TestDatabase").build();
        ListDatabasesResponse response =
                ListDatabasesResponse.builder().databaseList(List.of(db)).build();
        when(athenaClient.listDatabases(any(ListDatabasesRequest.class))).thenReturn(response);

        Either<FailedOperation, Boolean> result = athenaManager.checkDatabaseExists(athenaClient, catalog, database);
        assertTrue(result.isRight(), "Expected result to be Right");
        assertTrue(result.get(), "Expected database to exist");
    }

    // Test for checkDatabaseExists when the database list is null
    @Test
    void checkDatabaseExists_whenDatabaseListIsNull_returnsFalse() {
        // Create a ListDatabasesResponse with one database that matches (ignoring case)
        Database db = Database.builder().name("TestDatabase").build();
        ListDatabasesResponse response = ListDatabasesResponse.builder().build();
        when(athenaClient.listDatabases(any(ListDatabasesRequest.class))).thenReturn(response);

        Either<FailedOperation, Boolean> result = athenaManager.checkDatabaseExists(athenaClient, catalog, database);
        assertTrue(result.isRight(), "Expected result to be Right");
        assertFalse(result.get(), "Expected database not to exist");
    }

    // Test for checkDatabaseExists when the database does not exist
    @Test
    void checkDatabaseExists_whenDatabaseDoesNotExist_returnsFalse() {
        // Create a ListDatabasesResponse with a database that does not match the provided name
        Database db = Database.builder().name("OtherDatabase").build();
        ListDatabasesResponse response =
                ListDatabasesResponse.builder().databaseList(List.of(db)).build();
        when(athenaClient.listDatabases(any(ListDatabasesRequest.class))).thenReturn(response);

        Either<FailedOperation, Boolean> result = athenaManager.checkDatabaseExists(athenaClient, catalog, database);
        assertTrue(result.isRight(), "Expected result to be Right");
        assertFalse(result.get(), "Expected database not to exist");
    }

    // Test for checkDatabaseExists when an exception is thrown
    @Test
    void checkDatabaseExists_whenExceptionThrown_returnsFailedOperation() {
        when(athenaClient.listDatabases(any(ListDatabasesRequest.class)))
                .thenThrow(new RuntimeException("Test exception"));

        Either<FailedOperation, Boolean> result = athenaManager.checkDatabaseExists(athenaClient, catalog, database);
        assertTrue(result.isLeft(), "Expected result to be Left due to exception");
        FailedOperation failedOperation = result.getLeft();
        assertNotNull(failedOperation, "FailedOperation should not be null");
        assertTrue(failedOperation.message().contains("An unexpected error occurred"), "Expected error message");
    }

    // Test for getTableMetadata when the table exists
    @Test
    void getTableMetadata_whenTableExists_returnsTableMetadata() {
        // Create a TableMetadata object that matches the tableName (ignoring case)
        TableMetadata metadata = TableMetadata.builder().name("TestTable").build();
        ListTableMetadataResponse response = ListTableMetadataResponse.builder()
                .tableMetadataList(List.of(metadata))
                .build();
        when(athenaClient.listTableMetadata(any(ListTableMetadataRequest.class)))
                .thenReturn(response);

        Either<FailedOperation, Optional<TableMetadata>> result =
                athenaManager.getTableMetadata(athenaClient, catalog, database, tableName);
        assertTrue(result.isRight(), "Expected result to be Right");
        Optional<TableMetadata> metadataOpt = result.get();
        assertTrue(metadataOpt.isPresent(), "Expected table metadata to be present");
        assertEquals("TestTable", metadataOpt.get().name(), "Expected table name to match");
    }

    // Test for getTableMetadata when the table does not exist
    @Test
    void getTableMetadata_whenTableDoesNotExist_returnsEmptyOptional() {
        // Return a response with an empty list
        ListTableMetadataResponse response = ListTableMetadataResponse.builder()
                .tableMetadataList(Collections.emptyList())
                .build();
        when(athenaClient.listTableMetadata(any(ListTableMetadataRequest.class)))
                .thenReturn(response);

        Either<FailedOperation, Optional<TableMetadata>> result =
                athenaManager.getTableMetadata(athenaClient, catalog, database, tableName);
        assertTrue(result.isRight(), "Expected result to be Right");
        Optional<TableMetadata> metadataOpt = result.get();
        assertTrue(metadataOpt.isEmpty(), "Expected no table metadata to be present");
    }

    // Test for getTableMetadata when an exception is thrown
    @Test
    void getTableMetadata_whenExceptionThrown_returnsFailedOperation() {
        when(athenaClient.listTableMetadata(any(ListTableMetadataRequest.class)))
                .thenThrow(new RuntimeException("Test exception"));

        Either<FailedOperation, Optional<TableMetadata>> result =
                athenaManager.getTableMetadata(athenaClient, catalog, database, tableName);
        assertTrue(result.isLeft(), "Expected result to be Left due to exception");
        var failedOperation = result.getLeft();
        assertNotNull(failedOperation, "FailedOperation should not be null");
        assertTrue(
                failedOperation.message().contains("An unexpected error occurred"),
                "Expected error message to indicate an unexpected error");
    }
}
