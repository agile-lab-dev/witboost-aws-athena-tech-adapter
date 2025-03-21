package com.witboost.provisioning.athena.awsClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.witboost.provisioning.athena.model.AthenaColumn;
import com.witboost.provisioning.athena.model.AthenaTable;
import com.witboost.provisioning.athena.model.AthenaView;
import com.witboost.provisioning.athena.model.TableFormat;
import com.witboost.provisioning.model.common.FailedOperation;
import io.vavr.control.Either;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.*;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.glue.model.GetTableRequest;
import software.amazon.awssdk.services.glue.model.GetTableResponse;
import software.amazon.awssdk.services.glue.model.StorageDescriptor;
import software.amazon.awssdk.services.glue.model.Table;
import software.amazon.awssdk.services.sts.StsClient;

@SpringBootTest
class AthenaManagerTest {

    @Autowired
    private AthenaManager athenaManager;

    private AthenaClient athenaClient;

    private final String catalog = "awsCatalog";
    private final String database = "testDatabase";
    private final String tableName = "testTable";
    private final String outputLocation = "s3://fake-location";

    @MockBean
    StsClient stsClient;

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

    @Test
    void createDatabase_success_returnsRight() {
        // Simulate successful query submission by returning a valid query execution ID.
        StartQueryExecutionResponse response =
                StartQueryExecutionResponse.builder().queryExecutionId("1234").build();
        when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenReturn(response);
        GetQueryExecutionResponse executionResponse = GetQueryExecutionResponse.builder()
                .queryExecution(QueryExecution.builder()
                        .status(QueryExecutionStatus.builder()
                                .state(QueryExecutionState.SUCCEEDED)
                                .build())
                        .build())
                .build();
        when(athenaClient.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenReturn(executionResponse);

        Either<FailedOperation, Void> result =
                athenaManager.createDatabase(athenaClient, outputLocation, catalog, database);
        assertTrue(result.isRight(), "Expected createDatabase to succeed");
    }

    @Test
    void createDatabase_AthenaException_returnsLeft() {
        AthenaException athenaEx = org.mockito.Mockito.mock(AthenaException.class);
        when(athenaEx.awsErrorDetails())
                .thenReturn(software.amazon.awssdk.awscore.exception.AwsErrorDetails.builder()
                        .errorMessage("Athena error occurred")
                        .build());
        when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenThrow(athenaEx);

        Either<FailedOperation, Void> result =
                athenaManager.createDatabase(athenaClient, outputLocation, catalog, database);
        assertTrue(result.isLeft(), "Expected createDatabase to fail due to AthenaException");
        FailedOperation failure = result.getLeft();
        assertTrue(
                failure.message().contains("Athena-specific error during query submission"),
                "Expected error message to mention Athena-specific error");
    }

    @Test
    void createDatabase_genericException_returnsLeft() {
        when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenThrow(new RuntimeException("Generic exception"));

        Either<FailedOperation, Void> result =
                athenaManager.createDatabase(athenaClient, outputLocation, catalog, database);
        assertTrue(result.isLeft(), "Expected createDatabase to fail due to generic exception");
        FailedOperation failure = result.getLeft();
        assertTrue(
                failure.message().contains("An unexpected error occurred during query submission"),
                "Expected error message to mention unexpected error");
    }

    @Test
    void createTable_success_returnsRight() {
        AthenaColumn column1 = new AthenaColumn();
        column1.setName("id");
        column1.setDataType("STRING");
        AthenaColumn column2 = new AthenaColumn();
        column2.setName("name");
        column2.setDataType("STRING");
        List<AthenaColumn> columns = List.of(column1, column2);

        StartQueryExecutionResponse response = StartQueryExecutionResponse.builder()
                .queryExecutionId("tableQueryId")
                .build();
        when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenReturn(response);
        GetQueryExecutionResponse executionResponse = GetQueryExecutionResponse.builder()
                .queryExecution(QueryExecution.builder()
                        .status(QueryExecutionStatus.builder()
                                .state(QueryExecutionState.SUCCEEDED)
                                .build())
                        .build())
                .build();
        when(athenaClient.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenReturn(executionResponse);

        Either<FailedOperation, Void> result = athenaManager.createTable(
                athenaClient, outputLocation, catalog, database, tableName, TableFormat.ICEBERG, columns);
        assertTrue(result.isRight(), "Expected createTable to succeed");
    }

    @Test
    void createView_success_returnsRight() {
        AthenaTable athenaTable = new AthenaTable();
        athenaTable.setCatalog(catalog);
        athenaTable.setDatabase(database);
        athenaTable.setName(tableName);

        AthenaView athenaView = new AthenaView();
        athenaView.setCatalog(catalog);
        athenaView.setDatabase("viewDatabase");
        athenaView.setName("testView");

        AthenaColumn column1 = new AthenaColumn();
        column1.setName("id");
        column1.setDataType("STRING");
        AthenaColumn column2 = new AthenaColumn();
        column2.setName("name");
        column2.setDataType("STRING");
        List<AthenaColumn> columns = List.of(column1, column2);

        StartQueryExecutionResponse response = StartQueryExecutionResponse.builder()
                .queryExecutionId("tableQueryId")
                .build();
        when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenReturn(response);
        GetQueryExecutionResponse executionResponse = GetQueryExecutionResponse.builder()
                .queryExecution(QueryExecution.builder()
                        .status(QueryExecutionStatus.builder()
                                .state(QueryExecutionState.SUCCEEDED)
                                .build())
                        .build())
                .build();
        when(athenaClient.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenReturn(executionResponse);

        Either<FailedOperation, Void> result =
                athenaManager.createView(athenaClient, outputLocation, athenaTable, athenaView, columns);
        assertTrue(result.isRight(), "Expected createView to succeed");
    }

    @Test
    void dropView_success_returnsRight() {
        AthenaView athenaView = new AthenaView();
        athenaView.setCatalog(catalog);
        athenaView.setDatabase(database);
        athenaView.setName("viewToDrop");

        StartQueryExecutionResponse response = StartQueryExecutionResponse.builder()
                .queryExecutionId("dropViewId")
                .build();
        when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenReturn(response);
        GetQueryExecutionResponse executionResponse = GetQueryExecutionResponse.builder()
                .queryExecution(QueryExecution.builder()
                        .status(QueryExecutionStatus.builder()
                                .state(QueryExecutionState.SUCCEEDED)
                                .build())
                        .build())
                .build();
        when(athenaClient.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenReturn(executionResponse);

        Either<FailedOperation, Void> result = athenaManager.dropView(athenaClient, outputLocation, athenaView);
        assertTrue(result.isRight(), "Expected dropView to succeed");
    }

    @Test
    void createColumnsListForSelectStatement_empty_returnsAsterisk() throws Exception {
        Method method = AthenaManager.class.getDeclaredMethod("createColumnsListForSelectStatement", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        String result = (String) method.invoke(athenaManager, Collections.emptyList());
        assertEquals("*", result, "Expected '*' when column list is empty");
    }

    @Test
    void createColumnsListForSelectStatement_nonEmpty_returnsCommaSeparatedList() throws Exception {
        Method method = AthenaManager.class.getDeclaredMethod("createColumnsListForSelectStatement", List.class);
        method.setAccessible(true);

        com.witboost.provisioning.model.Column column1 = new com.witboost.provisioning.model.Column();
        column1.setName("id");
        column1.setDataType("STRING");
        com.witboost.provisioning.model.Column column2 = new com.witboost.provisioning.model.Column();
        column2.setName("name");
        column2.setDataType("STRING");
        List<com.witboost.provisioning.model.Column> columns = List.of(column1, column2);
        @SuppressWarnings("unchecked")
        String result = (String) method.invoke(athenaManager, columns);
        assertEquals("id, name", result, "Expected comma-separated column names");
    }

    @Test
    void waitForQueryToComplete_shouldSucceedWhenQuerySucceeds() {
        when(athenaClient.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenReturn(GetQueryExecutionResponse.builder()
                        .queryExecution(QueryExecution.builder()
                                .status(QueryExecutionStatus.builder()
                                        .state(QueryExecutionState.SUCCEEDED)
                                        .build())
                                .build())
                        .build());

        Either<FailedOperation, Void> result = athenaManager.waitForQueryToComplete(athenaClient, "query-id");

        assertTrue(result.isRight());
    }

    @Test
    void waitForQueryToComplete_shouldFailOnTimeout() {
        when(athenaClient.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenAnswer(invocation -> GetQueryExecutionResponse.builder()
                        .queryExecution(QueryExecution.builder()
                                .status(QueryExecutionStatus.builder()
                                        .state(QueryExecutionState.RUNNING)
                                        .build())
                                .build())
                        .build());

        Either<FailedOperation, Void> result = athenaManager.waitForQueryToComplete(athenaClient, "query-id");

        assertTrue(result.isLeft());
        assertTrue(result.getLeft().message().contains("timeout"));
    }

    @Test
    void waitForQueryToComplete_shouldFailWhenQueryFails() {
        when(athenaClient.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenReturn(GetQueryExecutionResponse.builder()
                        .queryExecution(QueryExecution.builder()
                                .status(QueryExecutionStatus.builder()
                                        .state(QueryExecutionState.FAILED)
                                        .stateChangeReason("Syntax error")
                                        .build())
                                .build())
                        .build());

        Either<FailedOperation, Void> result = athenaManager.waitForQueryToComplete(athenaClient, "query-id");

        assertTrue(result.isLeft());
        assertTrue(result.getLeft().message().contains("Query query-id failed"));
    }

    @Test
    void waitForQueryToComplete_shouldFailWhenQueryIsCancelled() {
        when(athenaClient.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenReturn(GetQueryExecutionResponse.builder()
                        .queryExecution(QueryExecution.builder()
                                .status(QueryExecutionStatus.builder()
                                        .state(QueryExecutionState.CANCELLED)
                                        .stateChangeReason("User cancelled")
                                        .build())
                                .build())
                        .build());

        Either<FailedOperation, Void> result = athenaManager.waitForQueryToComplete(athenaClient, "query-id");

        assertTrue(result.isLeft());
        assertTrue(result.getLeft().message().contains("Query query-id cancelled"));
    }

    @Test
    void submitQuery_shouldHandleAthenaException() {
        when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenThrow(AthenaException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder()
                                .errorMessage("Error message")
                                .build())
                        .message("Access Denied")
                        .build());

        Either<FailedOperation, String> result =
                athenaManager.submitQuery(athenaClient, "s3://output-location", "catalog", "SELECT * FROM table");

        assertTrue(result.isLeft());
        assertTrue(result.getLeft().message().contains("Athena-specific error during query submission"));
    }

    @Test
    void testWaitForQueryToCompleteHandlesException() {

        String queryExecutionId = "test-query-id";

        when(athenaClient.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenThrow(new RuntimeException("runtime exception"));
        Either<FailedOperation, Void> result = athenaManager.waitForQueryToComplete(athenaClient, queryExecutionId);

        assertTrue(result.isLeft());
        assertTrue(result.getLeft().message().contains("An unexpected error waiting for the completion"));
    }

    @Test
    void testCreateMultiDialectView() {
        AthenaTable athenaTable = new AthenaTable();
        athenaTable.setCatalog(catalog);
        athenaTable.setDatabase(database);
        athenaTable.setName(tableName);

        AthenaView athenaView = new AthenaView();
        athenaView.setCatalog(catalog);
        athenaView.setDatabase("viewDatabase");
        athenaView.setName("testView");

        AthenaColumn column1 = new AthenaColumn();
        column1.setName("id");
        column1.setDataType("STRING");
        AthenaColumn column2 = new AthenaColumn();
        column2.setName("name");
        column2.setDataType("STRING");
        List<AthenaColumn> columns = List.of(column1, column2);

        StartQueryExecutionResponse response = StartQueryExecutionResponse.builder()
                .queryExecutionId("tableQueryId")
                .build();
        when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenReturn(response);
        GetQueryExecutionResponse executionResponse = GetQueryExecutionResponse.builder()
                .queryExecution(QueryExecution.builder()
                        .status(QueryExecutionStatus.builder()
                                .state(QueryExecutionState.SUCCEEDED)
                                .build())
                        .build())
                .build();
        when(athenaClient.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenReturn(executionResponse);

        Either<FailedOperation, Void> result = athenaManager.createMultiDialectView(
                athenaClient, "s3://output-location", athenaTable, athenaView, columns);

        System.out.println(result);
        assertTrue(result.isRight());
    }

    @Test
    void testGetTableLocationSuccess() {
        GlueClient glueClient = mock(GlueClient.class);
        AthenaManager athenaManager = new AthenaManager();

        GetTableResponse response = GetTableResponse.builder()
                .table(Table.builder()
                        .storageDescriptor(StorageDescriptor.builder()
                                .location("s3://bucket/path/")
                                .build())
                        .build())
                .build();

        when(glueClient.getTable(any(GetTableRequest.class))).thenReturn(response);

        AthenaTable athenaTable = new AthenaTable();
        athenaTable.setCatalog(catalog);
        athenaTable.setDatabase(database);
        athenaTable.setName(tableName);

        Either<FailedOperation, String> result = athenaManager.getTableLocation(glueClient, athenaTable);

        assertTrue(result.isRight());
        assertEquals("s3://bucket/path/", result.get());
    }

    @Test
    void testGetTableLocationFailure() {
        GlueClient glueClient = mock(GlueClient.class);

        when(glueClient.getTable(any(GetTableRequest.class))).thenThrow(RuntimeException.class);

        Either<FailedOperation, String> result = athenaManager.getTableLocation(glueClient, new AthenaTable());

        assertTrue(result.isLeft());
        assertTrue(result.getLeft().message().contains("An unexpected error occurred while getting table location"));
    }

    @Test
    void testValidateTimeoutWithReflection() throws Exception {
        AthenaManager athenaManager = new AthenaManager();

        Field field = AthenaManager.class.getDeclaredField("queryTimeoutSeconds");
        field.setAccessible(true);
        field.set(athenaManager, 0);

        Exception exception = assertThrows(IllegalArgumentException.class, athenaManager::validateTimeout);
        assertTrue(exception.getMessage().contains("Query timeout must be greater than zero"));
    }
}
