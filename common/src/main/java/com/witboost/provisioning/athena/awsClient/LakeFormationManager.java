package com.witboost.provisioning.athena.awsClient;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

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
     * @param locationArn         the ARN of the data lake location to register.
     * @param roleArn             the ARN of the IAM role to use for the registration.
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

            return right(null);

        } catch (AlreadyExistsException e) {
            logger.info("Data lake location already exists: {}", locationArn);
            return right(null);
        } catch (Exception e) {
            String error = String.format(
                    "An unexpected error occurred while registering data lake location %s on Lake Formation. Details %s",
                    locationArn, e.getMessage());
            logger.error(error, e);
            return left(new FailedOperation(error, List.of(new Problem(error, e))));
        }
    }

    /**
     * Assigns permissions to a data lake principal on a specified resource.
     *
     * @param lakeFormationClient the AWS Lake Formation client used to interact with Lake Formation.
     * @param dataLakePrincipal   the data lake principal (e.g., user, role) to whom permissions will be granted.
     * @param resource            the resource (e.g., database, table) to which permissions will be granted.
     * @param permissions         one or more {@link Permission} values that specify the permissions to be granted.
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

            return right(null);

        } catch (Exception e) {
            String error = String.format(
                    "An unexpected error occurred while assigning permissions on Lake Formation. Details %s",
                    e.getMessage());
            logger.error(error, e);
            return left(new FailedOperation(error, List.of(new Problem(error, e))));
        }
    }

    /**
     * Retrieves the list of permissions associated with a principal on a specific Lake Formation resource.
     *
     * @param lakeFormationClient the AWS Lake Formation client used to send the request.
     * @param dataLakePrincipal   the principal whose permissions will be retrieved.
     * @param resource            the specific Lake Formation resource (e.g., database, table).
     * @param resourceType        the type of the resource (e.g., {@code DATABASE}, {@code TABLE}).
     * @param resourceName        the name of the resource, used in logging and error messages.
     * @return {@code Either.left(FailedOperation)} if an error occurs,
     *         {@code Either.right(List<PrincipalResourcePermissions>)} containing permissions on success.
     */
    public Either<FailedOperation, List<PrincipalResourcePermissions>> listPermissions(
            LakeFormationClient lakeFormationClient,
            DataLakePrincipal dataLakePrincipal,
            Resource resource,
            DataLakeResourceType resourceType,
            String resourceName) {
        try {
            logger.info(
                    "Listing permissions. [Principal: {}, Resource: {}, ResourceType: {}, ResourceName: {}]",
                    dataLakePrincipal.dataLakePrincipalIdentifier(),
                    resource,
                    resourceType,
                    resourceName);

            ListPermissionsRequest listPermissionsRequest = ListPermissionsRequest.builder()
                    .principal(dataLakePrincipal)
                    .resource(resource)
                    .resourceType(resourceType)
                    .build();
            ListPermissionsResponse listPermissionsResponse =
                    lakeFormationClient.listPermissions(listPermissionsRequest);

            logger.debug(
                    "Successfully retrieved permissions. [Count: {}, Principal: {}]",
                    listPermissionsResponse.principalResourcePermissions().size(),
                    dataLakePrincipal.dataLakePrincipalIdentifier());

            return right(listPermissionsResponse.principalResourcePermissions());

        } catch (Exception e) {
            logger.error(
                    "Failed to list permissions. [Principal: {}, ResourceType: {}, ResourceName: {}, Error: {}]",
                    dataLakePrincipal.dataLakePrincipalIdentifier(),
                    resourceType,
                    resourceName,
                    e.getMessage(),
                    e);

            String error = String.format(
                    "An unexpected error occurred while listing the permissions of principal '%s' on Lake Formation resource %s '%s'. Details: %s",
                    dataLakePrincipal.dataLakePrincipalIdentifier(),
                    resourceType.toString(),
                    resourceName,
                    e.getMessage());
            return left(new FailedOperation(error, List.of(new Problem(error, e))));
        }
    }
}
