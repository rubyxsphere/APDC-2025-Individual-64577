package pt.unl.fct.di.adc.projeto.resources;

import com.google.cloud.datastore.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.projeto.requests.UserDataRequest;
import pt.unl.fct.di.adc.projeto.core.AuthValidator;
import pt.unl.fct.di.adc.projeto.core.DatastoreService;
import pt.unl.fct.di.adc.projeto.util.AuthToken;

import java.util.Base64;
import java.util.Map;

import com.google.cloud.datastore.Blob;

import java.util.Set;

@Path("/profile")
@Produces(MediaType.APPLICATION_JSON)
public class ProfileResource {

    private final Datastore datastore = DatastoreService.get();
    private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    private static final String PROPERTY_EMAIL = "email";
    private static final String PROPERTY_USERNAME = "username";
    private static final String PROPERTY_FULL_NAME = "fullName";
    private static final String PROPERTY_PHONE = "phone";
    private static final String PROPERTY_PROFILE = "profile";
    private static final String PROPERTY_ROLE = "role";
    private static final String PROPERTY_STATE = "state";
    private static final String PROPERTY_CC_NUMBER = "ccNumber";
    private static final String PROPERTY_NIF = "nif";
    private static final String PROPERTY_EMPLOYER = "employer";
    private static final String PROPERTY_JOB = "job";
    private static final String PROPERTY_ADDRESS = "address";
    private static final String PROPERTY_EMPLOYER_NIF = "employerNIF";
    private static final String PROPERTY_PHOTO = "photo";

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_BACKOFFICE = "BACKOFFICE";
    private static final String STATE_ACTIVATED = "ACTIVATED";
    private static final String STATE_DEACTIVATED = "DEACTIVATED";
    private static final String STATE_SUSPENDED = "SUSPENDED";
    private static final Set<String> VALID_STATES = Set.of(STATE_ACTIVATED, STATE_DEACTIVATED, STATE_SUSPENDED);

    private static final String ERROR_USER_NOT_FOUND = "User not found for the provided token.";
    private static final String ERROR_DATASTORE_FAILED = "Datastore operation failed.";
    private static final String ERROR_FORBIDDEN_FIELD = "Modification of restricted field(s) not allowed for your role.";
    private static final String ERROR_INVALID_PHOTO_FORMAT = "Invalid photo data format (must be base64)";
    private static final String ERROR_UPDATE_FAILED = "Profile update failed.";
    private static final String ERROR_INVALID_STATE_VALUE = "Invalid state value provided.";
    private static final String ERROR_USERNAME_CHANGE_NOT_ALLOWED = "Username cannot be changed by your role.";
    private static final String ERROR_MISSING_BODY = "Missing request body.";
    private static final String ERROR_ROLE_CHANGE_NOT_ALLOWED = "Role cannot be changed via this endpoint.";
    private static final String ERROR_UNEXPECTED = "An unexpected error occurred: ";

    private static final String SUCCESS_PROFILE_UPDATED = "Profile updated successfully.";


    @GET
    @Path("/me")
    public Response getMyProfile(@Context HttpHeaders headers) {
        AuthToken authToken;
        try {
            authToken = AuthValidator.extractFromHeader(headers);
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus()).entity(Map.of("error", e.getMessage())).build();
        }

        Transaction txn = datastore.newTransaction();
        try {
            String userEmail = authToken.getEmail();
            Key userKey = userKeyFactory.newKey(userEmail);
            Entity userEntity = txn.get(userKey);
            if (userEntity == null) {
                txn.rollback();
                return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", ERROR_USER_NOT_FOUND)).build();
            }

            String base64Photo = null;
            if (userEntity.contains(PROPERTY_PHOTO)) {
                Value<?> photoValue = userEntity.getValue(PROPERTY_PHOTO);
                if (photoValue instanceof BlobValue) {
                    Blob photoBlob = userEntity.getBlob(PROPERTY_PHOTO);
                    if (photoBlob != null) {
                        byte[] photoBytes = photoBlob.toByteArray();
                        if (photoBytes != null && photoBytes.length > 0) {
                            base64Photo = Base64.getEncoder().encodeToString(photoBytes);
                        }
                    }
                } else {
                    System.err.println("Warning: User " + userEmail + " has a photo property that is not a Blob.");
                }
            }

            UserDataRequest profileData = new UserDataRequest(
                    userEntity.getString(PROPERTY_EMAIL),
                    userEntity.contains(PROPERTY_USERNAME) ? userEntity.getString(PROPERTY_USERNAME) : null,
                    userEntity.contains(PROPERTY_FULL_NAME) ? userEntity.getString(PROPERTY_FULL_NAME) : null,
                    userEntity.contains(PROPERTY_PHONE) ? userEntity.getString(PROPERTY_PHONE) : null,
                    userEntity.contains(PROPERTY_PROFILE) ? userEntity.getString(PROPERTY_PROFILE) : null,
                    userEntity.contains(PROPERTY_ROLE) ? userEntity.getString(PROPERTY_ROLE) : null,
                    userEntity.contains(PROPERTY_STATE) ? userEntity.getString(PROPERTY_STATE) : null,
                    userEntity.contains(PROPERTY_CC_NUMBER) ? userEntity.getString(PROPERTY_CC_NUMBER) : null,
                    userEntity.contains(PROPERTY_NIF) ? userEntity.getString(PROPERTY_NIF) : null,
                    userEntity.contains(PROPERTY_EMPLOYER) ? userEntity.getString(PROPERTY_EMPLOYER) : null,
                    userEntity.contains(PROPERTY_JOB) ? userEntity.getString(PROPERTY_JOB) : null,
                    userEntity.contains(PROPERTY_ADDRESS) ? userEntity.getString(PROPERTY_ADDRESS) : null,
                    userEntity.contains(PROPERTY_EMPLOYER_NIF) ? userEntity.getString(PROPERTY_EMPLOYER_NIF) : null,
                    base64Photo
            );
            txn.commit();
            return Response.ok(profileData).build();
        } catch (Exception e) {
            if (txn.isActive()) txn.rollback();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", ERROR_DATASTORE_FAILED)).build();
        } finally {
            if (txn.isActive()) txn.rollback();
        }
    }

    @PUT
    @Path("/me")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateMyProfile(@Context HttpHeaders headers, UserDataRequest req) {
        AuthToken authToken;
        try {
            authToken = AuthValidator.extractFromHeader(headers);
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus()).entity(Map.of("error", e.getMessage())).build();
        }

        if (req == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_MISSING_BODY)).build();
        }

        Transaction txn = datastore.newTransaction();
        try {
            String userEmail = authToken.getEmail();
            Key userKey = userKeyFactory.newKey(userEmail);
            Entity currentUser = txn.get(userKey);

            if (currentUser == null) {
                txn.rollback();
                return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", ERROR_USER_NOT_FOUND)).build();
            }

            String userRole = authToken.getRole();
            boolean isAdmin = ROLE_ADMIN.equals(userRole);
            boolean isBackoffice = ROLE_BACKOFFICE.equals(userRole);
            boolean canChangeState = isAdmin || isBackoffice;

            Entity.Builder builder = Entity.newBuilder(currentUser);

            if (req.getFullName() != null) builder.set(PROPERTY_FULL_NAME, req.getFullName());
            if (req.getPhone() != null) builder.set(PROPERTY_PHONE, req.getPhone());
            if (req.getProfile() != null) builder.set(PROPERTY_PROFILE, req.getProfile());

            if (req.getCcNumber() != null) {
                builder.set(PROPERTY_CC_NUMBER, req.getCcNumber().toUpperCase());
            } else {
                builder.remove(PROPERTY_CC_NUMBER);
            }

            if (req.getNif() != null) {
                builder.set(PROPERTY_NIF, req.getNif());
            } else {
                builder.remove(PROPERTY_NIF);
            }

            if (req.getEmployer() != null) {
                builder.set(PROPERTY_EMPLOYER, req.getEmployer());
            } else {
                builder.remove(PROPERTY_EMPLOYER);
            }

            if (req.getJob() != null) {
                builder.set(PROPERTY_JOB, req.getJob());
            } else {
                builder.remove(PROPERTY_JOB);
            }

            if (req.getAddress() != null) {
                builder.set(PROPERTY_ADDRESS, req.getAddress());
            } else {
                builder.remove(PROPERTY_ADDRESS);
            }

            if (req.getEmployerNIF() != null) {
                builder.set(PROPERTY_EMPLOYER_NIF, req.getEmployerNIF());
            } else {
                builder.remove(PROPERTY_EMPLOYER_NIF);
            }

            if (req.getPhoto() != null) {
                if (req.getPhoto().isEmpty()) {
                    builder.remove(PROPERTY_PHOTO);
                } else {
                    try {
                        byte[] photoBytes = Base64.getDecoder().decode(req.getPhoto());
                        Blob photoBlob = Blob.copyFrom(photoBytes);
                        builder.set(PROPERTY_PHOTO, BlobValue.newBuilder(photoBlob).setExcludeFromIndexes(true).build());
                    } catch (IllegalArgumentException e) {
                        txn.rollback();
                        return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_INVALID_PHOTO_FORMAT)).build();
                    }
                }
            }

            if (req.getUsername() != null && !req.getUsername().equals(currentUser.getString(PROPERTY_USERNAME))) {
                if (!isAdmin) {
                    txn.rollback();
                    return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", ERROR_USERNAME_CHANGE_NOT_ALLOWED)).build();
                }
                builder.set(PROPERTY_USERNAME, req.getUsername());
            }

            if (req.getRole() != null && !req.getRole().equals(currentUser.getString(PROPERTY_ROLE))) {
                txn.rollback();
                return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_ROLE_CHANGE_NOT_ALLOWED)).build();
            }

            if (req.getState() != null && !req.getState().equals(currentUser.getString(PROPERTY_STATE))) {
                if (!canChangeState) {
                    txn.rollback();
                    return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", ERROR_FORBIDDEN_FIELD + " (State)")).build();
                }
                if (!VALID_STATES.contains(req.getState())) {
                    txn.rollback();
                    return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_INVALID_STATE_VALUE)).build();
                }
                builder.set(PROPERTY_STATE, req.getState());
            }

            txn.put(builder.build());
            txn.commit();

            return Response.ok(Map.of("success", SUCCESS_PROFILE_UPDATED)).build();
        } catch (DatastoreException e) {
            if (txn.isActive()) {
                txn.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", ERROR_UPDATE_FAILED)).build();
        } catch (Exception e) {
            if (txn.isActive()) {
                txn.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", ERROR_UNEXPECTED + e.getMessage())).build();
        } finally {
            if (txn != null && txn.isActive()) {
                txn.rollback();
            }
        }
    }
}