package com.witboost.provisioning.athena.awsClient;

import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import io.vavr.control.Either;
import java.util.List;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@NoArgsConstructor
@Service
public class BucketManager {

    private final Logger logger = LoggerFactory.getLogger(BucketManager.class);

    /**
     * Creates a folder-like structure in the specified S3 bucket.
     *
     * @param s3         the {@link S3Client} used to perform the operation.
     * @param bucketName the name of the bucket.
     * @param folderPath the desired folder path.
     * @return an {@link Either} containing {@link FailedOperation} in case of error or {@code null} on success.
     */
    public Either<FailedOperation, Void> createFolder(
            @NotNull S3Client s3, @NotNull String bucketName, @NotNull String folderPath) {
        try {

            logger.info("Starting creation of the folder '{}' in bucket '{}'.", folderPath, bucketName);

            String formattedFolderPath = folderPath.endsWith("/") ? folderPath : folderPath + "/";

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(formattedFolderPath)
                    .build();

            s3.putObject(request, RequestBody.empty());

            logger.info("Folder '{}' in bucket '{}' is successfully created.", formattedFolderPath, bucketName);
            return Either.right(null);

        } catch (Exception e) {
            String error = String.format(
                    "[Bucket: %s, Folder: %s] Error: An unexpected error occurred while creating the folder. Details: %s",
                    bucketName, folderPath, e.getMessage());
            logger.error(error, e);
            return Either.left(new FailedOperation(error, List.of(new Problem(error, e))));
        }
    }
}
