package com.witboost.provisioning.athena.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.witboost.provisioning.athena.model.AthenaSpecific;
import com.witboost.provisioning.athena.model.AthenaTable;
import com.witboost.provisioning.athena.model.AthenaView;
import com.witboost.provisioning.model.DataContract;
import com.witboost.provisioning.model.OutputPort;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.Workload;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.request.OperationRequest;
import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test class for RequestUtils.
 */
class RequestUtilsTest {

    @Mock
    private OperationRequest<?, ? extends Specific> request;

    private OutputPort outputPort;
    private AthenaSpecific athenaSpecific;
    private RequestUtils requestUtils;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        initializeOutputPort();
        when(request.getComponent()).thenReturn(Optional.of(outputPort));
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

    @Test
    void getOutputPort_shouldReturnSuccessfulResponse() {
        var result = RequestUtils.getOutputPort(request);
        assert result.isRight();
        assert result.get().getName().equalsIgnoreCase("op_component");
    }

    @Test
    void getOutputPortEmptyComponent_shouldReturnFailedOperation() {
        when(request.getComponent()).thenReturn(Optional.empty());
        var result = RequestUtils.getOutputPort(request);
        assert result.isLeft();
        assert result.getLeft().message().contains("Component is missing");
    }

    @Test
    void getOutputPortWrongComponentKind_shouldReturnFailedOperation() {
        Workload workload = new Workload();
        workload.setKind("workload");
        when(request.getComponent()).thenReturn(Optional.of(workload));
        var result = RequestUtils.getOutputPort(request);
        assert result.isLeft();
        assert result.getLeft().message().contains("Component null is not an OutputPort");
    }

    @Test
    void getOutputPortWrongOutputPortKind_shouldReturnFailedOperation() {
        Workload workload = new Workload();
        workload.setKind("outputport");
        when(request.getComponent()).thenReturn(Optional.of(workload));
        var result = RequestUtils.getOutputPort(request);
        assert result.isLeft();
        assert result.getLeft().message().contains("Component null is not an OutputPort");
    }

    @Test
    void getAthenaSpecific_shouldReturnSuccessfulResponse() {
        Either<FailedOperation, AthenaSpecific> result = RequestUtils.getAthenaSpecific(outputPort);
        assert result.isRight();
        assert result.get().getSourceTable().getName().equalsIgnoreCase("sourceTable");
    }

    @Test
    void getAthenaSpecificInvalidComponentSpecific_shouldReturnFailedOperation() {
        outputPort.setSpecific(new Specific());
        Either<FailedOperation, AthenaSpecific> result = RequestUtils.getAthenaSpecific(outputPort);
        assert result.isLeft();
        assert result.getLeft().message().contains("Invalid Specific type of op_component. Expected AthenaSpecific.");
    }

    @Test
    void extractStorageAreaRegion_shouldReturnSuccessfulResponse() throws Exception {
        JsonNode validStorageAreaInfo = objectMapper.readTree("{\"specific\": {\"region\": \"us-east-1\"}}");
        Either<FailedOperation, String> result = RequestUtils.extractStorageAreaRegion(validStorageAreaInfo);
        assert result.isRight();
        assertEquals("us-east-1", result.get());
    }

    @Test
    void extractStorageAreaRegionMissingSpecific_shouldReturnFailedOperation() throws Exception {
        JsonNode storageAreaInfoMissingSpecific = objectMapper.readTree("{}");
        Either<FailedOperation, String> result = RequestUtils.extractStorageAreaRegion(storageAreaInfoMissingSpecific);
        assert result.isLeft();
        assertTrue(
                result.getLeft().message().contains("The dependent storage component does not include the AWS region"));
    }

    @Test
    void extractStorageAreaRegionMissingRegion_shouldReturnFailedOperation() throws Exception {
        JsonNode storageAreaInfoMissingRegion = objectMapper.readTree("{\"specific\": {}}");
        Either<FailedOperation, String> result = RequestUtils.extractStorageAreaRegion(storageAreaInfoMissingRegion);
        assert result.isLeft();
        assertTrue(
                result.getLeft().message().contains("The dependent storage component does not include the AWS region"));
    }
}
