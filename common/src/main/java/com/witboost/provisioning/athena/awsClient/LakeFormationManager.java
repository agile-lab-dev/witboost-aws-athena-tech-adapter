package com.witboost.provisioning.athena.awsClient;

import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import io.vavr.control.Either;
import java.util.List;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.lakeformation.LakeFormationClient;
import software.amazon.awssdk.services.lakeformation.model.*;

@NoArgsConstructor
@Service
public class LakeFormationManager {

    private final Logger logger = LoggerFactory.getLogger(LakeFormationManager.class);

    /**
     * Registers a data lake location with the provided ARN and role.
     *
     * @param lakeFormationClient the AWS Lake Formation client used to interact with Lake Formation.
     * @param locationArn the ARN of the data lake location to register.
     * @param roleArn the ARN of the IAM role to use for the registration.
     * @return Either a failed operation if an error occurs, or a success with no value (Void).
     */
    public Either<FailedOperation, Void> registerDataLakeLocation(
            LakeFormationClient lakeFormationClient, String locationArn, String roleArn) {
        logger.info("Starting to register data lake location with ARN: {}", locationArn);

        try {
            RegisterResourceRequest registerResourceRequest = RegisterResourceRequest.builder()
                    .resourceArn(locationArn)
                    .useServiceLinkedRole(false)
                    .roleArn(roleArn)
                    .hybridAccessEnabled(true)
                    .build();

            logger.debug("Registering data lake location with request: {}", registerResourceRequest);
            lakeFormationClient.registerResource(registerResourceRequest);
            logger.info("Data lake location registered successfully: {}", locationArn);

            return Either.right(null);

        } catch (AlreadyExistsException e) {
            logger.info("Data lake location already exists: {}", locationArn);
            return Either.right(null);
        } catch (Exception e) {
            String error = String.format(
                    "An unexpected error occurred while registering data lake location %s on Lake Formation. Details %s",
                    locationArn, e.getMessage());
            logger.error(error, e);
            return Either.left(new FailedOperation(error, List.of(new Problem(error, e))));
        }
    }

    /**
     * Assigns permissions to a data lake principal on a specified resource.
     *
     * @param lakeFormationClient the AWS Lake Formation client used to interact with Lake Formation.
     * @param dataLakePrincipal the data lake principal (e.g., user, role) to whom permissions will be granted.
     * @param resource the resource (e.g., database, table) to which permissions will be granted.
     * @param permissions one or more {@link Permission} values that specify the permissions to be granted.
     * @return Either a failed operation if an error occurs, or a success with no value (Void).
     */
    public Either<FailedOperation, Void> assignPermissions(
            LakeFormationClient lakeFormationClient,
            DataLakePrincipal dataLakePrincipal,
            Resource resource,
            Permission... permissions) {
        logger.info("Starting to assign permissions for principal: {} on resource: {}", dataLakePrincipal, resource);

        try {
            GrantPermissionsRequest grantPermissionsRequest = GrantPermissionsRequest.builder()
                    .resource(resource)
                    .principal(dataLakePrincipal)
                    .permissions(permissions)
                    .build();

            logger.debug("Granting permissions with request: {}", grantPermissionsRequest);
            var grantPermissionsResponse = lakeFormationClient.grantPermissions(grantPermissionsRequest);
            logger.info("Permissions granted successfully: {}", grantPermissionsResponse);

            return Either.right(null);

        } catch (Exception e) {
            String error = String.format(
                    "An unexpected error occurred while assigning permissions on Lake Formation. Details %s",
                    e.getMessage());
            logger.error(error, e);
            return Either.left(new FailedOperation(error, List.of(new Problem(error, e))));
        }
    }
}
