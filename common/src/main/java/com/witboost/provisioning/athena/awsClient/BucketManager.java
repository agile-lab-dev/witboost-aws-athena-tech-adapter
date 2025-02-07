package com.witboost.provisioning.athena.awsClient;

import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.*;

/**
 * BucketManager provides utility methods for managing Amazon S3 buckets and their contents.
 */
@NoArgsConstructor
@Service
public class BucketManager {

    private final Logger logger = LoggerFactory.getLogger(BucketManager.class);
}
