package com.witboost.provisioning.athena.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AthenaOutputPortTest {

    private AthenaOutputPort<String> outputPort1;
    private AthenaOutputPort<String> outputPort2;
    private AthenaOutputPort<String> outputPort3;

    @BeforeEach
    public void setUp() {
        outputPort1 = createOutputPort();
        outputPort2 = createOutputPort();
        outputPort3 = new AthenaOutputPort<>();

        // Setting different values for outputPort3
        outputPort3.setVersion("2.0");
        outputPort3.setOutputPortType("TypeB");
    }

    private AthenaOutputPort<String> createOutputPort() {
        AthenaOutputPort<String> outputPort = new AthenaOutputPort<>();
        outputPort.setVersion("1.0");
        outputPort.setInfrastructureTemplateId("template-1");
        outputPort.setUseCaseTemplateId(Optional.of("usecase-template-1"));
        outputPort.setDependsOn(Arrays.asList("dep-1", "dep-2"));
        outputPort.setPlatform(Optional.of("platform"));
        outputPort.setTechnology(Optional.of("technology"));
        outputPort.setOutputPortType("TypeA");
        outputPort.setCreationDate(Optional.of("2023-01-01"));
        outputPort.setStartDate(Optional.of("2023-01-02"));
        outputPort.setRetentionTime(Optional.of("P3Y6M4D"));
        outputPort.setProcessDescription(Optional.of("Description"));
        outputPort.setDataContract(null); // Set to null for simplicity
        outputPort.setDataSharingAgreement(null); // Set to null for simplicity
        outputPort.setTags(new ArrayList<>()); // Empty list of tags
        outputPort.setSampleData(Optional.empty()); // Empty Optional sample data
        outputPort.setSemanticLinking(Optional.empty()); // Empty Optional semantic linking

        return outputPort;
    }

    @Test
    public void testToString() {
        String expectedToString = "AthenaOutputPort(version=1.0, infrastructureTemplateId=template-1, "
                + "useCaseTemplateId=Optional[usecase-template-1], dependsOn=[dep-1, dep-2], "
                + "platform=Optional[platform], technology=Optional[technology], outputPortType=TypeA, "
                + "creationDate=Optional[2023-01-01], startDate=Optional[2023-01-02], retentionTime=Optional[P3Y6M4D], "
                + "processDescription=Optional[Description], dataContract=null, dataSharingAgreement=null, tags=[], "
                + "sampleData=Optional.empty, semanticLinking=Optional.empty)";

        assertEquals(expectedToString, outputPort1.toString());
    }

    @Test
    public void testJsonIgnoreProperties() {
        assertTrue(AthenaOutputPort.class.isAnnotationPresent(
                com.fasterxml.jackson.annotation.JsonIgnoreProperties.class));
        com.fasterxml.jackson.annotation.JsonIgnoreProperties propertiesAnnotation =
                AthenaOutputPort.class.getAnnotation(com.fasterxml.jackson.annotation.JsonIgnoreProperties.class);
        assertTrue(propertiesAnnotation.ignoreUnknown());
    }

    @Test
    public void testGetters() {
        assertEquals("1.0", outputPort1.getVersion());
        assertEquals("template-1", outputPort1.getInfrastructureTemplateId());
        assertEquals("usecase-template-1", outputPort1.getUseCaseTemplateId().get());
        assertEquals(Arrays.asList("dep-1", "dep-2"), outputPort1.getDependsOn());
        assertEquals(Optional.of("platform"), outputPort1.getPlatform());
        assertEquals(Optional.of("technology"), outputPort1.getTechnology());
        assertEquals("TypeA", outputPort1.getOutputPortType());
        assertEquals(Optional.of("2023-01-01"), outputPort1.getCreationDate());
        assertEquals(Optional.of("2023-01-02"), outputPort1.getStartDate());
        assertEquals(Optional.of("P3Y6M4D"), outputPort1.getRetentionTime());
        assertEquals(Optional.of("Description"), outputPort1.getProcessDescription());
        assertEquals(new ArrayList<>(), outputPort1.getTags());
        assertEquals(Optional.empty(), outputPort1.getSampleData());
        assertEquals(Optional.empty(), outputPort1.getSemanticLinking());
    }
}
