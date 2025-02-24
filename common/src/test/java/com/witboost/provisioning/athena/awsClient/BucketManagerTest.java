package com.witboost.provisioning.athena.awsClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.witboost.provisioning.model.common.FailedOperation;
import io.vavr.control.Either;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

public class BucketManagerTest {

    private BucketManager bucketManager;
    private S3Client s3Client;

    @BeforeEach
    void setUp() {
        bucketManager = new BucketManager();
        s3Client = mock(S3Client.class);
    }

    @Test
    public void testCreateFolder_success() {
        String bucketName = "my-bucket";
        String folderPath = "my-folder";

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mock(PutObjectResponse.class));
        S3Waiter s3Waiter = mock(S3Waiter.class);
        when(s3Client.waiter()).thenReturn(s3Waiter);

        WaiterResponse waiterResponse = mock(WaiterResponse.class);
        ResponseOrException<HeadObjectResponse> responseOrException = mock(ResponseOrException.class);
        when(waiterResponse.matched()).thenReturn(responseOrException);

        Optional<HeadObjectResponse> response = Optional.of(mock(HeadObjectResponse.class));
        when(responseOrException.response()).thenReturn(response);

        when(s3Waiter.waitUntilObjectExists(any(HeadObjectRequest.class), any(WaiterOverrideConfiguration.class)))
                .thenReturn(waiterResponse);

        Either<FailedOperation, Void> result = bucketManager.createFolder(s3Client, bucketName, folderPath);

        assertTrue(result.isRight());
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    public void testCreateFolder_failure() {
        String bucketName = "my-bucket";
        String folderPath = "my-folder/";

        doThrow(new RuntimeException("S3 service unavailable"))
                .when(s3Client)
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));

        Either<FailedOperation, Void> result = bucketManager.createFolder(s3Client, bucketName, folderPath);

        assertTrue(result.isLeft());
        assertNotNull(result.getLeft());
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
