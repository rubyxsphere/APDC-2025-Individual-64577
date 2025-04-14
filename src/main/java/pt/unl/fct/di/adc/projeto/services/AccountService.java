package pt.unl.fct.di.adc.projeto.services;

import com.google.cloud.datastore.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import pt.unl.fct.di.adc.projeto.requests.ChangeAccountAttributesRequest;
import pt.unl.fct.di.adc.projeto.requests.RemoveUserRequest;
import pt.unl.fct.di.adc.projeto.core.AuthValidator;
import pt.unl.fct.di.adc.projeto.core.DatastoreService;
import pt.unl.fct.di.adc.projeto.util.AuthToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/account")
@Produces(MediaType.APPLICATION_JSON)
public class AccountService {

    private static final String KIND_USER = "User";
    private static final String KIND_AUTH_TOKEN = "AuthToken";

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_BACKOFFICE = "BACKOFFICE";
    private static final String ROLE_ENDUSER = "ENDUSER";
    private static final String ROLE_PARTNER = "PARTNER";

    private static final String PROPERTY_ROLE = "role";
    private static final String PROPERTY_STATE = "state";
    private static final String PROPERTY_EMAIL = "email";
    private static final String PROPERTY_USERNAME = "username";
    private static final String PROPERTY_FULL_NAME = "fullName";
    private static final String PROPERTY_PHONE = "phone";
    private static final String PROPERTY_PROFILE = "profile";
    private static final String PROPERTY_CC_NUMBER = "ccNumber";
    private static final String PROPERTY_NIF = "nif";
    private static final String PROPERTY_EMPLOYER = "employer";
    private static final String PROPERTY_JOB = "job";
    private static final String PROPERTY_ADDRESS = "address";
    private static final String PROPERTY_EMPLOYER_NIF = "employerNIF";
    private static final String PROPERTY_PHOTO = "photo";

    private static final String ERROR_MISSING_EMAIL = "Missing input: email is required";
    private static final String ERROR_TARGET_USER_NOT_FOUND = "Target user not found";
    private static final String ERROR_CANNOT_DELETE_OWN_ACCOUNT = "Cannot delete own account";
    private static final String ERROR_INSUFFICIENT_PERMISSION = "Insufficient permission";
    private static final String ERROR_CANNOT_CHANGE_USERNAME = "Not allowed to change username";
    private static final String ERROR_CANNOT_CHANGE_ROLE_STATE = "Not allowed to change role or state";
    private static final String ERROR_TOKEN_INVALID = "Invalid or missing token";
    private static final String ERROR_DATASTORE_FAILED = "Datastore operation failed";

    private static final String SUCCESS_USER_REMOVED = "User account removed successfully";
    private static final String SUCCESS_ATTRIBUTES_UPDATED = "User attributes updated successfully";

    private final Datastore datastore = DatastoreService.get();
    private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind(KIND_USER);

    @POST
    @Path("/removeUser")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeUserAccount(RemoveUserRequest req, @Context HttpHeaders headers) {
        if (req == null || req.getEmail() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_MISSING_EMAIL)).build();
        }
        String targetEmail = req.getEmail();

        AuthToken authToken = AuthValidator.extractFromHeader(headers);
        if (authToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", ERROR_TOKEN_INVALID)).build();
        }
        String requesterRole = authToken.getRole();
        String requesterEmail = authToken.getEmail();

        if (requesterEmail.equals(targetEmail)) {
            return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", ERROR_CANNOT_DELETE_OWN_ACCOUNT)).build();
        }

        Transaction txn = datastore.newTransaction();
        try {
            Key targetUserKey = userKeyFactory.newKey(targetEmail);
            Entity targetUser = txn.get(targetUserKey);
            if (targetUser == null) {
                txn.rollback();
                return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", ERROR_TARGET_USER_NOT_FOUND)).build();
            }
            String targetRole = targetUser.getString(PROPERTY_ROLE);

            boolean allowed = switch (requesterRole) {
                case ROLE_ADMIN -> true;
                case ROLE_BACKOFFICE -> isBackofficeAllowedToRemove(targetRole);
                default -> false;
            };

            if (!allowed) {
                txn.rollback();
                return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", ERROR_INSUFFICIENT_PERMISSION)).build();
            }

            deleteAuthTokensForUser(txn, targetEmail);

            txn.delete(targetUserKey);

            txn.commit();
            return Response.ok(Map.of("success", SUCCESS_USER_REMOVED)).build();

        } catch (DatastoreException e) {
            if (txn.isActive()) {
                txn.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", ERROR_DATASTORE_FAILED)).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    private void deleteAuthTokensForUser(Transaction txn, String email) {
        Query<Key> query = Query.newKeyQueryBuilder()
                .setKind(KIND_AUTH_TOKEN)
                .setFilter(StructuredQuery.PropertyFilter.eq(PROPERTY_EMAIL, email))
                .build();
        QueryResults<Key> keys = txn.run(query);
        List<Key> keysToDelete = new ArrayList<>();
        keys.forEachRemaining(keysToDelete::add);
        if (!keysToDelete.isEmpty()) {
            txn.delete(keysToDelete.toArray(new Key[0]));
        }
    }

    private boolean isBackofficeAllowedToRemove(String targetRole) {
        return ROLE_ENDUSER.equals(targetRole) || ROLE_PARTNER.equals(targetRole);
    }

    @POST
    @Path("/changeAttributes")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeAccountAttributes(ChangeAccountAttributesRequest req, @Context HttpHeaders headers) {
        if (req == null || req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_MISSING_EMAIL)).build();
        }
        String targetEmailFromRequest = req.getEmail().trim();

        AuthToken authToken = AuthValidator.extractFromHeader(headers);
        if (authToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", ERROR_TOKEN_INVALID)).build();
        }
        String requesterRole = authToken.getRole();
        String requesterEmail = authToken.getEmail();

        Transaction txn = datastore.newTransaction();
        try {
            Key targetUserKey = userKeyFactory.newKey(targetEmailFromRequest);
            Entity targetUser = txn.get(targetUserKey);
            if (targetUser == null) {
                txn.rollback();
                return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", ERROR_TARGET_USER_NOT_FOUND)).build();
            }
            String targetRole = targetUser.getString(PROPERTY_ROLE);
            String actualTargetEmail = targetUser.getKey().getName();
            boolean isSelfModification = requesterEmail.equals(actualTargetEmail);

            boolean canModifyTarget = false;
            if (ROLE_ADMIN.equals(requesterRole)) {
                canModifyTarget = true;
            } else if (ROLE_BACKOFFICE.equals(requesterRole) && (ROLE_ENDUSER.equals(targetRole) || ROLE_PARTNER.equals(targetRole))) {
                canModifyTarget = true;
            } else if (isSelfModification && (ROLE_ENDUSER.equals(requesterRole) || ROLE_PARTNER.equals(requesterRole))) {
                canModifyTarget = true;
            }

            if (!canModifyTarget) {
                txn.rollback();
                return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", ERROR_INSUFFICIENT_PERMISSION)).build();
            }

            String requestedUsername = req.getUsername();
            if (requestedUsername != null && !requestedUsername.equals(targetUser.getString(PROPERTY_USERNAME))) {
                if (!ROLE_ADMIN.equals(requesterRole)) {
                    txn.rollback();
                    return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", ERROR_CANNOT_CHANGE_USERNAME)).build();
                }
            }

            boolean changingRoleOrState = req.getRole() != null || req.getState() != null;
            if (isSelfModification && (ROLE_ENDUSER.equals(requesterRole) || ROLE_PARTNER.equals(requesterRole)) && changingRoleOrState) {
                txn.rollback();
                return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", ERROR_CANNOT_CHANGE_ROLE_STATE)).build();
            }

            Entity.Builder updatedUserBuilder = Entity.newBuilder(targetUser);

            if (requestedUsername != null && ROLE_ADMIN.equals(requesterRole)) {
                updatedUserBuilder.set(PROPERTY_USERNAME, requestedUsername);
            }

            if (req.getFullName() != null) {
                String fullName = req.getFullName().trim();
                if (!fullName.isEmpty()) {
                    updatedUserBuilder.set(PROPERTY_FULL_NAME, fullName);
                }
            }

            String requestedPhone = req.getPhone();
            if (requestedPhone != null) {
                updatedUserBuilder.set(PROPERTY_PHONE, requestedPhone);
            } else {
                updatedUserBuilder.remove(PROPERTY_PHONE);
            }

            String requestedProfile = req.getProfile();
            if (requestedProfile != null) {
                updatedUserBuilder.set(PROPERTY_PROFILE, requestedProfile);
            } else {
                updatedUserBuilder.remove(PROPERTY_PROFILE);
            }

            String requestedCcNumber = req.getCcNumber();
            if (requestedCcNumber != null) {
                updatedUserBuilder.set(PROPERTY_CC_NUMBER, requestedCcNumber);
            } else {
                updatedUserBuilder.remove(PROPERTY_CC_NUMBER);
            }

            String requestedNif = req.getNif();
            if (requestedNif != null) {
                updatedUserBuilder.set(PROPERTY_NIF, requestedNif);
            } else {
                updatedUserBuilder.remove(PROPERTY_NIF);
            }

            String requestedEmployer = req.getEmployer();
            if (requestedEmployer != null) {
                updatedUserBuilder.set(PROPERTY_EMPLOYER, requestedEmployer);
            } else {
                updatedUserBuilder.remove(PROPERTY_EMPLOYER);
            }

            String requestedJob = req.getJob();
            if (requestedJob != null) {
                updatedUserBuilder.set(PROPERTY_JOB, requestedJob);
            } else {
                updatedUserBuilder.remove(PROPERTY_JOB);
            }

            String requestedAddress = req.getAddress();
            if (requestedAddress != null) {
                updatedUserBuilder.set(PROPERTY_ADDRESS, requestedAddress);
            } else {
                updatedUserBuilder.remove(PROPERTY_ADDRESS);
            }

            String requestedEmployerNIF = req.getEmployerNIF();
            if (requestedEmployerNIF != null) {
                updatedUserBuilder.set(PROPERTY_EMPLOYER_NIF, requestedEmployerNIF);
            } else {
                updatedUserBuilder.remove(PROPERTY_EMPLOYER_NIF);
            }

            String requestedPhoto = req.getPhoto();
            if (requestedPhoto != null) {
                if (requestedPhoto.isEmpty()) {
                    updatedUserBuilder.remove(PROPERTY_PHOTO);
                } else {
                    try {
                        byte[] photoBytes = java.util.Base64.getDecoder().decode(requestedPhoto);
                        Blob photoBlob = Blob.copyFrom(photoBytes);
                        BlobValue photoBlobValue = BlobValue.newBuilder(photoBlob)
                                .setExcludeFromIndexes(true)
                                .build();
                        updatedUserBuilder.set(PROPERTY_PHOTO, photoBlobValue);
                    } catch (IllegalArgumentException e) {
                        txn.rollback();
                        return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Invalid photo data format (must be Base64)")).build();
                    }
                }
            }

            if (ROLE_ADMIN.equals(requesterRole) || ROLE_BACKOFFICE.equals(requesterRole)) {
                if (req.getRole() != null) {
                    updatedUserBuilder.set(PROPERTY_ROLE, req.getRole());
                }
                if (req.getState() != null) {
                    updatedUserBuilder.set(PROPERTY_STATE, req.getState());
                }
            }

            txn.put(updatedUserBuilder.build());
            txn.commit();
            return Response.ok(Map.of("success", SUCCESS_ATTRIBUTES_UPDATED)).build();

        } catch (DatastoreException e) {
            if (txn.isActive()) {
                txn.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", ERROR_DATASTORE_FAILED)).build();
        } catch (Exception e) {
            if (txn.isActive()) {
                txn.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", "An unexpected error occurred.")).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }
}