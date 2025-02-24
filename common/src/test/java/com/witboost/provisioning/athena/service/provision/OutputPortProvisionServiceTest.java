package com.witboost.provisioning.athena.service.provision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.witboost.provisioning.athena.awsClient.AthenaManager;
import com.witboost.provisioning.athena.awsClient.BucketManager;
import com.witboost.provisioning.athena.config.ClassProviderBean;
import com.witboost.provisioning.athena.config.ConfigurationBean;
import com.witboost.provisioning.athena.model.AthenaSpecific;
import com.witboost.provisioning.athena.model.AthenaTable;
import com.witboost.provisioning.athena.model.AthenaView;
import com.witboost.provisioning.athena.service.validation.OutputPortValidationService;
import com.witboost.provisioning.athena.utils.ResourceUtils;
import com.witboost.provisioning.framework.openapi.model.DescriptorKind;
import com.witboost.provisioning.framework.openapi.model.ProvisioningRequest;
import com.witboost.provisioning.framework.service.validation.ValidationConfiguration;
import com.witboost.provisioning.framework.service.validation.ValidationServiceImpl;
import com.witboost.provisioning.model.DataContract;
import com.witboost.provisioning.model.OperationType;
import com.witboost.provisioning.model.OutputPort;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import com.witboost.provisioning.model.request.OperationRequest;
import com.witboost.provisioning.model.request.ProvisionOperationRequest;
import io.vavr.control.Either;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.Column;
import software.amazon.awssdk.services.athena.model.TableMetadata;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest
@AutoConfigureMockMvc
class OutputPortProvisionServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private ProvisionOperationRequest<?, ? extends Specific> request;

    @MockitoBean
    private Function<Region, AthenaClient> athenaClientProvider;

    @MockitoBean
    private Function<Region, S3Client> s3ClientProvider;

    @MockitoBean
    private AthenaClient athenaClient;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private AthenaManager athenaManager;

    @MockitoBean
    private BucketManager bucketManager;

    @MockitoBean
    private OutputPortValidationService outputPortValidationService;

    @Autowired
    private OutputPortProvisionService outputPortProvisionService;

    private final String mockProvisionEndpoint = "http://127.0.0.1:8888/v1/provision";
    private final String mockUnprovisionEndpoint = "http://127.0.0.1:8888/v1/unprovision";

    private OutputPort outputPort;
    private ClassProviderBean classProviderBean;
    private ValidationConfiguration validationConfiguration;
    private ValidationServiceImpl validationServiceImpl;
    private AthenaSpecific athenaSpecific;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(athenaClientProvider.apply(any(Region.class))).thenReturn(athenaClient);
        when(s3ClientProvider.apply(any(Region.class))).thenReturn(s3Client);

        initializeOutputPort();
        when(request.getComponent()).thenReturn(Optional.of(outputPort));

        classProviderBean = new ClassProviderBean();
        validationConfiguration = new ConfigurationBean().validationConfiguration(outputPortValidationService);
        validationServiceImpl = new ValidationServiceImpl(
                validationConfiguration,
                classProviderBean.componentClassProvider(),
                classProviderBean.specificClassProvider());
    }

    private void initializeOutputPort() {
        outputPort = new OutputPort<>();
        outputPort.setName("op_component");
        outputPort.setKind("outputport");
        outputPort.setId("op_id");
        outputPort.setDescription("op_desc");

        DataContract dataContract = new DataContract();
        dataContract.setSchema(new ArrayList<>());
        outputPort.setDataContract(dataContract);

        AthenaSpecific athenaSpecific = new AthenaSpecific();
        athenaSpecific.setStorageAreaId("storage-id");

        AthenaTable athenaTable = new AthenaTable();
        athenaTable.setCatalog("awsCatalog");
        athenaTable.setDatabase("helloworld");
        athenaTable.setName("sourceTable");

        AthenaView athenaView = new AthenaView();
        athenaView.setCatalog("awsCatalog");
        athenaView.setDatabase("db");
        athenaView.setName("view");

        athenaSpecific.setSourceTable(athenaTable);
        athenaSpecific.setView(athenaView);
        outputPort.setSpecific(athenaSpecific);
    }

    // Test successful provision with a correct descriptor when the source table does not exist.
    @Test
    void testProvisionCorrectDescriptor_shouldReturnSuccessfulResponse() throws Exception {

        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport.yml");

        when(outputPortValidationService.validate(any(OperationRequest.class), eq(OperationType.PROVISION)))
                .thenReturn(Either.right(null));
        when(bucketManager.createFolder(any(S3Client.class), anyString(), anyString()))
                .thenReturn(Either.right(null));
        when(athenaManager.checkDatabaseExists(any(AthenaClient.class), anyString(), anyString()))
                .thenReturn(Either.right(false));
        when(athenaManager.createDatabase(any(AthenaClient.class), anyString(), anyString(), anyString()))
                .thenReturn(Either.right(null));
        when(athenaManager.getTableMetadata(any(AthenaClient.class), anyString(), anyString(), anyString()))
                .thenReturn(Either.right(Optional.empty()));
        when(athenaManager.createTable(
                        any(AthenaClient.class), anyString(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(Either.right(null));
        when(athenaManager.createView(
                        any(AthenaClient.class), anyString(), any(AthenaTable.class), any(AthenaView.class), anyList()))
                .thenReturn(Either.right(null));

        MvcResult result = mockMvc.perform(post(mockProvisionEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assert (result.getResponse().getContentAsString().contains("\"status\":\"COMPLETED\""));
    }

    // Test successful provision when the source table already exists.
    @Test
    void testProvisionCorrectDescriptorSourceTableExists_shouldReturnSuccessfulResponse() throws Exception {

        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport.yml");

        when(outputPortValidationService.validate(any(OperationRequest.class), eq(OperationType.PROVISION)))
                .thenReturn(Either.right(null));
        when(bucketManager.createFolder(any(S3Client.class), anyString(), anyString()))
                .thenReturn(Either.right(null));
        when(athenaManager.checkDatabaseExists(any(AthenaClient.class), anyString(), anyString()))
                .thenReturn(Either.right(false));
        when(athenaManager.createDatabase(any(AthenaClient.class), anyString(), anyString(), anyString()))
                .thenReturn(Either.right(null));
        TableMetadata tableMetadata = TableMetadata.builder()
                .name("users")
                .columns(List.of(
                        Column.builder().name("id").type("STRING").build(),
                        Column.builder().name("name").type("STRING").build()))
                .build();
        when(athenaManager.getTableMetadata(any(AthenaClient.class), anyString(), anyString(), anyString()))
                .thenReturn(Either.right(Optional.of(tableMetadata)));
        when(athenaManager.createTable(
                        any(AthenaClient.class), anyString(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(Either.right(null));
        when(athenaManager.createView(
                        any(AthenaClient.class), anyString(), any(AthenaTable.class), any(AthenaView.class), anyList()))
                .thenReturn(Either.right(null));

        MvcResult result = mockMvc.perform(post(mockProvisionEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assert (result.getResponse().getContentAsString().contains("\"status\":\"COMPLETED\""));
    }

    // Test provision failure when bucket name is missing in the descriptor.
    @Test
    void testProvisionMissingBucketInfo_shouldReturnFailedOperation() throws Exception {

        ProvisioningRequest provisioningRequest =
                createProvisioningRequest("/descriptor_outputport_missingBucketName.yml");

        when(outputPortValidationService.validate(any(OperationRequest.class), eq(OperationType.PROVISION)))
                .thenReturn(Either.right(null));

        MvcResult result = mockMvc.perform(post(mockProvisionEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse()
                .getContentAsString()
                .contains("The dependant storage component is not including the bucket name"));
    }

    // Test provision failure when the dependant storageAreaId does not match any component in the descriptor.
    @Test
    void testProvisionWrongDependantStorageAreaId_shouldReturnFailedOperation() throws Exception {

        ProvisioningRequest provisioningRequest =
                createProvisioningRequest("/descriptor_outputport_wrongStorageAreaId.yml");

        when(outputPortValidationService.validate(any(OperationRequest.class), eq(OperationType.PROVISION)))
                .thenReturn(Either.right(null));

        MvcResult result = mockMvc.perform(post(mockProvisionEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse()
                .getContentAsString()
                .contains("The specific.storageAreaId field does not match any component in the descriptor"));
    }

    // Validation failure: the validation service returns a FailedOperation.
    @Test
    void testProvisionValidationFailure_shouldReturnFailedOperation() throws Exception {
        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport.yml");
        FailedOperation failure =
                new FailedOperation("Validation error", Collections.singletonList(new Problem("Validation error")));
        when(outputPortValidationService.validate(any(OperationRequest.class), eq(OperationType.PROVISION)))
                .thenReturn(Either.left(failure));

        MvcResult result = mockMvc.perform(post(mockProvisionEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Validation error"));
    }

    // S3 folder creation failure: simulate a failure when creating the outputLocation in S3 bucket.
    @Test
    void testProvisionBucketFolderCreationFailure_shouldReturnFailedOperation() throws Exception {
        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport.yml");

        // Validation passes
        when(outputPortValidationService.validate(any(OperationRequest.class), eq(OperationType.PROVISION)))
                .thenReturn(Either.right(null));
        // Simulate folder creation failure
        FailedOperation folderFailure = new FailedOperation(
                "Bucket folder creation error", Collections.singletonList(new Problem("Bucket folder error")));
        when(bucketManager.createFolder(any(S3Client.class), anyString(), anyString()))
                .thenReturn(Either.left(folderFailure));

        MvcResult result = mockMvc.perform(post(mockProvisionEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Bucket folder creation error"));
    }

    // Database creation failure: simulate a failure during the database creation step.
    @Test
    void testProvisionDatabaseCreationFailure_shouldReturnFailedOperation() throws Exception {
        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport.yml");

        when(outputPortValidationService.validate(any(OperationRequest.class), eq(OperationType.PROVISION)))
                .thenReturn(Either.right(null));
        when(bucketManager.createFolder(any(S3Client.class), anyString(), anyString()))
                .thenReturn(Either.right(null));
        // Simulate that the database does not exist and creation returns an error.
        FailedOperation dbFailure = new FailedOperation(
                "Database creation error", Collections.singletonList(new Problem("Database creation error")));
        when(athenaManager.checkDatabaseExists(any(AthenaClient.class), anyString(), anyString()))
                .thenReturn(Either.right(false));
        when(athenaManager.createDatabase(any(AthenaClient.class), anyString(), anyString(), anyString()))
                .thenReturn(Either.left(dbFailure));

        MvcResult result = mockMvc.perform(post(mockProvisionEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Database creation error"));
    }

    // Table creation failure: simulate a failure during table creation.
    @Test
    void testProvisionCreateTableFailure_shouldReturnFailedOperation() throws Exception {
        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport.yml");

        when(outputPortValidationService.validate(any(OperationRequest.class), eq(OperationType.PROVISION)))
                .thenReturn(Either.right(null));
        when(bucketManager.createFolder(any(S3Client.class), anyString(), anyString()))
                .thenReturn(Either.right(null));
        // Simulate that the database exists.
        when(athenaManager.checkDatabaseExists(any(AthenaClient.class), anyString(), anyString()))
                .thenReturn(Either.right(true));
        // Simulate that the table does not exist, so createTable is called, but returns an error.
        when(athenaManager.getTableMetadata(any(AthenaClient.class), anyString(), anyString(), anyString()))
                .thenReturn(Either.right(Optional.empty()));
        FailedOperation tableFailure = new FailedOperation(
                "Table creation error", Collections.singletonList(new Problem("Table creation error")));
        when(athenaManager.createTable(
                        any(AthenaClient.class), anyString(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(Either.left(tableFailure));

        MvcResult result = mockMvc.perform(post(mockProvisionEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Table creation error"));
    }

    // View creation failure: simulate a failure during view creation.
    @Test
    void testProvisionCreateViewFailure_shouldReturnFailedOperation() throws Exception {
        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport.yml");

        when(outputPortValidationService.validate(any(OperationRequest.class), eq(OperationType.PROVISION)))
                .thenReturn(Either.right(null));
        when(bucketManager.createFolder(any(S3Client.class), anyString(), anyString()))
                .thenReturn(Either.right(null));
        when(athenaManager.checkDatabaseExists(any(AthenaClient.class), anyString(), anyString()))
                .thenReturn(Either.right(true));
        // Simulate that the table does not exist, so createTable is called and succeeds.
        when(athenaManager.getTableMetadata(any(AthenaClient.class), anyString(), anyString(), anyString()))
                .thenReturn(Either.right(Optional.empty()));
        when(athenaManager.createTable(
                        any(AthenaClient.class), anyString(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(Either.right(null));
        // Simulate failure during view creation.
        FailedOperation viewFailure = new FailedOperation(
                "View creation error", Collections.singletonList(new Problem("View creation error")));
        when(athenaManager.createView(
                        any(AthenaClient.class), anyString(), any(AthenaTable.class), any(AthenaView.class), anyList()))
                .thenReturn(Either.left(viewFailure));

        MvcResult result = mockMvc.perform(post(mockProvisionEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("View creation error"));
    }

    // Test successful unprovision with a correct descriptor.
    @Test
    void testUnprovisionCorrectDescriptor_shouldReturnSuccessfulResponse() throws Exception {

        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport.yml");

        when(outputPortValidationService.validate(any(OperationRequest.class), eq(OperationType.UNPROVISION)))
                .thenReturn(Either.right(null));
        when(bucketManager.createFolder(any(S3Client.class), anyString(), anyString()))
                .thenReturn(Either.right(null));
        when(athenaManager.dropView(any(AthenaClient.class), anyString(), any(AthenaView.class)))
                .thenReturn(Either.right(null));

        MvcResult result = mockMvc.perform(post(mockUnprovisionEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assert (result.getResponse().getContentAsString().contains("\"status\":\"COMPLETED\""));
    }

    // Validation failure for unprovision: the validation service returns a FailedOperation.
    @Test
    void testUnprovisionValidationFailure_shouldReturnFailedOperation() throws Exception {
        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport.yml");
        FailedOperation failure = new FailedOperation(
                "Validation error unprovision", Collections.singletonList(new Problem("Validation error unprovision")));
        when(outputPortValidationService.validate(any(OperationRequest.class), eq(OperationType.UNPROVISION)))
                .thenReturn(Either.left(failure));

        MvcResult result = mockMvc.perform(post(mockUnprovisionEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Validation error unprovision"));
    }

    // Failure during drop view: simulate an error when dropping the view.
    @Test
    void testUnprovisionDropViewFailure_shouldReturnFailedOperation() throws Exception {
        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport.yml");

        when(outputPortValidationService.validate(any(OperationRequest.class), eq(OperationType.UNPROVISION)))
                .thenReturn(Either.right(null));
        when(bucketManager.createFolder(any(S3Client.class), anyString(), anyString()))
                .thenReturn(Either.right(null));
        FailedOperation dropFailure =
                new FailedOperation("Drop view error", Collections.singletonList(new Problem("Drop view error")));
        when(athenaManager.dropView(any(AthenaClient.class), anyString(), any(AthenaView.class)))
                .thenReturn(Either.left(dropFailure));

        MvcResult result = mockMvc.perform(post(mockUnprovisionEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("Drop view error"));
    }

    private ProvisioningRequest createProvisioningRequest(String resourcePath) throws IOException {
        String ymlDescriptor = ResourceUtils.getContentFromResource(resourcePath);
        return new ProvisioningRequest(DescriptorKind.COMPONENT_DESCRIPTOR, ymlDescriptor, false);
    }

    private com.witboost.provisioning.model.Column createColumn(String name, String dataType) {
        com.witboost.provisioning.model.Column column = new com.witboost.provisioning.model.Column();
        column.setName(name);
        column.setDataType(dataType);
        return column;
    }

    private void setDataContract(OutputPort outputPort, com.witboost.provisioning.model.Column... columns) {
        DataContract dataContract = new DataContract();
        dataContract.setSchema(List.of(columns));
        outputPort.setDataContract(dataContract);
    }
}
