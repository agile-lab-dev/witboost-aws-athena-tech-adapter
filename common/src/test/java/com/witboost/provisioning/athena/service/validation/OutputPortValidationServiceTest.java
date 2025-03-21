package com.witboost.provisioning.athena.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.witboost.provisioning.athena.awsClient.AthenaManager;
import com.witboost.provisioning.athena.config.ClassProviderBean;
import com.witboost.provisioning.athena.config.ConfigurationBean;
import com.witboost.provisioning.athena.model.*;
import com.witboost.provisioning.athena.utils.ResourceUtils;
import com.witboost.provisioning.framework.openapi.model.DescriptorKind;
import com.witboost.provisioning.framework.openapi.model.ProvisioningRequest;
import com.witboost.provisioning.framework.service.validation.ValidationConfiguration;
import com.witboost.provisioning.framework.service.validation.ValidationServiceImpl;
import com.witboost.provisioning.model.OperationType;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import com.witboost.provisioning.model.request.ProvisionOperationRequest;
import io.vavr.control.Either;
import java.io.IOException;
import java.util.ArrayList;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.Column;
import software.amazon.awssdk.services.athena.model.TableMetadata;
import software.amazon.awssdk.services.sts.StsClient;

@SpringBootTest
@AutoConfigureMockMvc
class OutputPortValidationServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private ProvisionOperationRequest<?, ? extends Specific> request;

    @MockBean
    private Function<Region, AthenaClient> athenaClientProvider;

    @MockBean
    private AthenaClient athenaClient;

    @MockBean
    private AthenaManager athenaManager;

    @MockBean
    StsClient stsClient;

    @Autowired
    private OutputPortValidationService outputPortValidationService;

    private final String mockValidateEndpoint = "http://127.0.0.1:8888/v1/validate";
    private AthenaOutputPort outputPort;
    private ClassProviderBean classProviderBean;
    private ValidationConfiguration validationConfiguration;
    private ValidationServiceImpl validationServiceImpl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(athenaClientProvider.apply(any(Region.class))).thenReturn(athenaClient);
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
        outputPort = new AthenaOutputPort();
        outputPort.setName("op_component");
        outputPort.setKind("outputport");
        outputPort.setId("op_id");
        outputPort.setDescription("op_desc");

        AthenaDataContract dataContract = new AthenaDataContract();
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

    // Parsing tests using MockMvc
    // Test with a correct descriptor should return HTTP 200 and valid response content
    @Test
    void testParsingCorrectDescriptor_shouldReturnValidResponse() throws Exception {

        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport.yml");

        // Simulate that the database exists and table metadata is valid
        when(athenaManager.checkDatabaseExists(any(AthenaClient.class), anyString(), anyString()))
                .thenReturn(Either.right(true));
        TableMetadata tableMetadata = TableMetadata.builder()
                .name("users")
                .columns(List.of(
                        Column.builder().name("id").type("INT").build(),
                        Column.builder().name("name").type("STRING").build()))
                .build();
        when(athenaManager.getTableMetadata(any(AthenaClient.class), anyString(), anyString(), anyString()))
                .thenReturn(Either.right(Optional.of(tableMetadata)));

        MvcResult result = mockMvc.perform(post(mockValidateEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assert (result.getResponse().getContentAsString().contains("\"valid\":true"));
    }

    // Test with missing field in the descriptor should return HTTP 400 with appropriate error message
    @Test
    void testParsingMissingField_shouldReturnBadRequest() throws Exception {

        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport_missingField.yml");
        MvcResult result = mockMvc.perform(post(mockValidateEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        String responseContent = result.getResponse().getContentAsString();
        assert (responseContent.contains("storageAreaId must not be blank"));
    }

    // Test with blank field in the descriptor should return HTTP 400 with appropriate error message
    @Test
    void testParsingBlankField_shouldReturnBadRequest() throws Exception {
        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport_blankField.yml");

        MvcResult result = mockMvc.perform(post(mockValidateEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        String responseContent = result.getResponse().getContentAsString();
        assert (responseContent.contains("catalog must not be blank"));
    }

    // Additional business logic tests

    // Test when a required column is missing from the table metadata
    @Test
    public void testMissingColumn_shouldReturnFailedOperation() throws IOException {
        String ymlDescriptor = ResourceUtils.getContentFromResource("/descriptor_outputport.yml");
        ProvisioningRequest provisioningRequest =
                new ProvisioningRequest(DescriptorKind.COMPONENT_DESCRIPTOR, ymlDescriptor, false);

        when(athenaManager.checkDatabaseExists(any(AthenaClient.class), anyString(), anyString()))
                .thenReturn(Either.right(true));

        // Simulate table metadata without the "name" column
        TableMetadata tableMetadata = TableMetadata.builder()
                .name("users")
                .columns(List.of(Column.builder().name("id").type("STRING").build()))
                .build();
        when(athenaManager.getTableMetadata(any(AthenaClient.class), anyString(), anyString(), anyString()))
                .thenReturn(Either.right(Optional.of(tableMetadata)));

        var actualRes = validationServiceImpl.validate(provisioningRequest, OperationType.VALIDATE);

        assert (actualRes.isLeft());
        String expectedError = "Column 'name' not found in source table";
        assert (actualRes.getLeft().problems().get(0).getMessage().contains(expectedError));
    }

    // Test when the component is missing in the request
    @Test
    public void testMissingComponent_shouldReturnFailedOperation() {
        when(request.getComponent()).thenReturn(Optional.empty());
        var actualRes = outputPortValidationService.validate(request, OperationType.PROVISION);

        assert (actualRes.isLeft());
        String expectedError = "Invalid operation request: Component is missing";
        assert (actualRes.getLeft().problems().get(0).getMessage().contains(expectedError));
    }

    // Test when the Specific is of the wrong type (should be AthenaSpecific)
    @Test
    public void testWrongSpecific_shouldReturnFailedOperation() {
        outputPort.setSpecific(new Specific());

        var actualRes = outputPortValidationService.validate(request, OperationType.PROVISION);

        assert (actualRes.isLeft());
        String expectedError = "Invalid Specific type of op_component. Expected AthenaSpecific.";
        assert (actualRes.getLeft().problems().get(0).getMessage().contains(expectedError));
    }

    // Test when there is an error checking database existence
    @Test
    public void testDatabaseExistenceError_shouldReturnFailedOperation() throws Exception {
        ProvisioningRequest provisioningRequest =
                createProvisioningRequest("/descriptor_outputportEmptyDataContractSchema.yml");

        String error = "Error checking database existence";
        when(athenaManager.checkDatabaseExists(any(AthenaClient.class), anyString(), anyString()))
                .thenReturn(Either.left(new FailedOperation(error, List.of(new Problem(error)))));

        MvcResult result = mockMvc.perform(post(mockValidateEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        assert (responseContent.contains(error));
    }

    // Test when the database does not exist and data contract schema is defined (validation should succeed)
    @Test
    public void testDatabaseNotExistsButValidDataContractSchema_shouldSucceed() throws Exception {
        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport.yml");

        when(athenaManager.checkDatabaseExists(any(AthenaClient.class), anyString(), anyString()))
                .thenReturn(Either.right(false));

        MvcResult result = mockMvc.perform(post(mockValidateEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assert (result.getResponse().getContentAsString().contains("\"valid\":true"));
    }

    // Test when the database does not exist and data contract schema is not defined (validation should fail)
    @Test
    public void testDatabaseNotExistsAndEmptyDataContractSchema__shouldReturnFailedOperation() throws Exception {
        ProvisioningRequest provisioningRequest =
                createProvisioningRequest("/descriptor_outputportEmptyDataContractSchema.yml");
        when(athenaManager.checkDatabaseExists(any(AthenaClient.class), anyString(), anyString()))
                .thenReturn(Either.right(false));

        MvcResult result = mockMvc.perform(post(mockValidateEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        String expectedError =
                "the source database 'helloworld' and/or table 'users' do not exist, and no columns are defined in the output port's Data Contract schema.";
        assert (responseContent.contains(expectedError));
    }

    // Test when there is an error retrieving the table metadata
    @Test
    public void testTableMetadataError_shouldReturnFailedOperation() throws Exception {
        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport.yml");

        String error = "Error getting table metadata";
        when(athenaManager.checkDatabaseExists(any(AthenaClient.class), anyString(), anyString()))
                .thenReturn(Either.right(true));
        when(athenaManager.getTableMetadata(any(AthenaClient.class), anyString(), anyString(), anyString()))
                .thenReturn(Either.left(new FailedOperation(error, List.of(new Problem(error)))));

        MvcResult result = mockMvc.perform(post(mockValidateEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        assert (responseContent.contains(error));
    }

    @Test
    public void testDataContractNull_shouldReturnFailedOperation() throws Exception {
        ProvisioningRequest provisioningRequest =
                createProvisioningRequest("/descriptor_outputportNullDataContract.yml");

        MvcResult result = mockMvc.perform(post(mockValidateEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        String error =
                "the Data Contract or its schema is null. Please define the required columns in the Data Contract schema.";
        assert (responseContent.contains(error));
    }

    @Test
    public void testDataContractSchemaNull_shouldReturnFailedOperation() throws Exception {
        ProvisioningRequest provisioningRequest =
                createProvisioningRequest("/descriptor_outputportNullDataContractSchema.yml");

        MvcResult result = mockMvc.perform(post(mockValidateEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        String error =
                "the Data Contract or its schema is null. Please define the required columns in the Data Contract schema.";
        assert (responseContent.contains(error));
    }

    // Test when the table does not exist and the Data Contract schema is empty
    @Test
    public void testMissingTableAndEmptyDataContractSchema_shouldReturnFailedOperation() throws Exception {
        ProvisioningRequest provisioningRequest =
                createProvisioningRequest("/descriptor_outputportEmptyDataContractSchema.yml");
        when(athenaManager.checkDatabaseExists(any(AthenaClient.class), anyString(), anyString()))
                .thenReturn(Either.right(true));
        when(athenaManager.getTableMetadata(any(AthenaClient.class), anyString(), anyString(), anyString()))
                .thenReturn(Either.right(Optional.empty()));

        MvcResult result = mockMvc.perform(post(mockValidateEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        String expectedError =
                "the source database 'helloworld' and/or table 'users' do not exist, and no columns are defined in the output port's Data Contract schema.";
        assert (responseContent.contains(expectedError));
    }

    // Test when the table does not exist but the Data Contract schema is defined
    @Test
    public void testMissingTableButValidDataContractSchema_shouldSucceed() throws Exception {
        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport.yml");
        when(athenaManager.checkDatabaseExists(any(AthenaClient.class), anyString(), anyString()))
                .thenReturn(Either.right(true));
        when(athenaManager.getTableMetadata(any(AthenaClient.class), anyString(), anyString(), anyString()))
                .thenReturn(Either.right(Optional.empty()));

        MvcResult result = mockMvc.perform(post(mockValidateEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        assertEquals(200, result.getResponse().getStatus());
        assert (result.getResponse().getContentAsString().contains("\"valid\":true"));
    }

    // Test when there is a column type mismatch between the Data Contract and table metadata
    @Test
    public void testColumnTypeMismatch_shouldReturnFailedOperation() throws Exception {
        ProvisioningRequest provisioningRequest = createProvisioningRequest("/descriptor_outputport.yml");
        when(athenaManager.checkDatabaseExists(any(AthenaClient.class), anyString(), anyString()))
                .thenReturn(Either.right(true));

        TableMetadata tableMetadata = TableMetadata.builder()
                .name("users")
                .columns(List.of(
                        Column.builder().name("id").type("STRING").build(),
                        Column.builder().name("name").type("STRING").build()))
                .build();
        when(athenaManager.getTableMetadata(any(AthenaClient.class), anyString(), anyString(), anyString()))
                .thenReturn(Either.right(Optional.of(tableMetadata)));

        MvcResult result = mockMvc.perform(post(mockValidateEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        String expectedError = "Type mismatch for column 'id': expected INT (from DataContract)";
        assert (responseContent.contains(expectedError));
    }

    // Test when there is a column type unsupported by ICEBERG - source Database exists
    @Test
    void testUnsupportedIcebergType_shouldReturnFailedOperation() throws Exception {

        ProvisioningRequest provisioningRequest =
                createProvisioningRequest("/descriptor_outputport_UnsupportedIcebergType.yml");

        // Simulate that the database exists and table metadata is valid
        when(athenaManager.checkDatabaseExists(any(AthenaClient.class), anyString(), anyString()))
                .thenReturn(Either.right(true));
        TableMetadata tableMetadata = TableMetadata.builder()
                .name("users")
                .columns(List.of(
                        Column.builder().name("id").type("INT").build(),
                        Column.builder().name("name").type("STRING").build()))
                .build();
        when(athenaManager.getTableMetadata(any(AthenaClient.class), anyString(), anyString(), anyString()))
                .thenReturn(Either.right(Optional.of(tableMetadata)));

        MvcResult result = mockMvc.perform(post(mockValidateEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        String expectedError = "Unsupported data type for column name: VARCHAR";
        assert (responseContent.contains(expectedError));
    }

    // Test when there is a column type unsupported by ICEBERG - source Database does not exist
    @Test
    void testUnsupportedIcebergTypeSourceDatabaseNotExists_shouldReturnFailedOperation() throws Exception {

        ProvisioningRequest provisioningRequest =
                createProvisioningRequest("/descriptor_outputport_UnsupportedIcebergType.yml");

        when(athenaManager.checkDatabaseExists(any(AthenaClient.class), anyString(), anyString()))
                .thenReturn(Either.right(false));

        MvcResult result = mockMvc.perform(post(mockValidateEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(provisioningRequest)))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        String expectedError = "Unsupported data type for column name: VARCHAR";
        assert (responseContent.contains(expectedError));
    }

    // Test when the operation type is "UNPROVISION"
    @Test
    public void testUnprovision_shouldReturnValidResponse() throws IOException {
        String ymlDescriptor = ResourceUtils.getContentFromResource("/descriptor_outputport.yml");
        ProvisioningRequest provisioningRequest =
                new ProvisioningRequest(DescriptorKind.COMPONENT_DESCRIPTOR, ymlDescriptor, false);

        var actualRes = validationServiceImpl.validate(provisioningRequest, OperationType.UNPROVISION);

        assert (actualRes.isRight());
    }

    private ProvisioningRequest createProvisioningRequest(String resourcePath) throws IOException {
        String ymlDescriptor = ResourceUtils.getContentFromResource(resourcePath);
        return new ProvisioningRequest(DescriptorKind.COMPONENT_DESCRIPTOR, ymlDescriptor, false);
    }
}
