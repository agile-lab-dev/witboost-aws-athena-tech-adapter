package com.witboost.provisioning.athena.awsClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.witboost.provisioning.model.common.FailedOperation;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.lakeformation.LakeFormationClient;
import software.amazon.awssdk.services.lakeformation.model.*;

class LakeFormationManagerTest {

    private LakeFormationManager lakeFormationManager;
    private LakeFormationClient lakeFormationClient;
    private final String locationArn = "arn:aws:s3:::test-bucket";
    private final String roleArn = "arn:aws:iam::123456789012:role/LakeFormationRole";

    @BeforeEach
    void setUp() {
        lakeFormationManager = new LakeFormationManager();
        lakeFormationClient = mock(LakeFormationClient.class);
    }

    @Test
    void registerDataLakeLocation_success_returnsRight() {

        Either<FailedOperation, Void> result =
                lakeFormationManager.registerDataLakeLocation(lakeFormationClient, locationArn, roleArn);

        assertTrue(result.isRight(), "Expected registerDataLakeLocation to succeed");
    }

    @Test
    void registerDataLakeLocation_alreadyExists_returnsRight() {
        doThrow(AlreadyExistsException.builder()
                        .message("Resource already exists")
                        .build())
                .when(lakeFormationClient)
                .registerResource(any(RegisterResourceRequest.class));

        Either<FailedOperation, Void> result =
                lakeFormationManager.registerDataLakeLocation(lakeFormationClient, locationArn, roleArn);

        assertTrue(result.isRight(), "Expected registerDataLakeLocation to succeed when resource already exists");
    }

    @Test
    void registerDataLakeLocation_unexpectedError_returnsLeft() {
        doThrow(RuntimeException.class).when(lakeFormationClient).registerResource(any(RegisterResourceRequest.class));

        Either<FailedOperation, Void> result =
                lakeFormationManager.registerDataLakeLocation(lakeFormationClient, locationArn, roleArn);

        assertTrue(result.isLeft(), "Expected registerDataLakeLocation to fail on unexpected error");
    }

    @Test
    void assignPermissions_success_returnsRight() {
        DataLakePrincipal principal = DataLakePrincipal.builder()
                .dataLakePrincipalIdentifier("testUser")
                .build();
        Resource resource = Resource.builder()
                .database(DatabaseResource.builder().name("testDatabase").build())
                .build();
        Permission permission = Permission.SELECT;

        Either<FailedOperation, Void> result =
                lakeFormationManager.assignPermissions(lakeFormationClient, principal, resource, permission);

        assertTrue(result.isRight(), "Expected assignPermissions to succeed");
    }

    @Test
    void assignPermissions_unexpectedError_returnsLeft() {
        DataLakePrincipal principal = DataLakePrincipal.builder()
                .dataLakePrincipalIdentifier("testUser")
                .build();
        Resource resource = Resource.builder()
                .database(DatabaseResource.builder().name("testDatabase").build())
                .build();
        Permission permission = Permission.SELECT;

        doThrow(RuntimeException.class).when(lakeFormationClient).grantPermissions(any(GrantPermissionsRequest.class));

        Either<FailedOperation, Void> result =
                lakeFormationManager.assignPermissions(lakeFormationClient, principal, resource, permission);

        assertTrue(result.isLeft(), "Expected assignPermissions to fail on unexpected error");
    }
}
