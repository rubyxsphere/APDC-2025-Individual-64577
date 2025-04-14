package pt.unl.fct.di.adc.projeto.resources;

import com.google.cloud.datastore.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.projeto.entities.User;
import pt.unl.fct.di.adc.projeto.core.DatastoreService;
import org.apache.commons.codec.digest.DigestUtils;
import java.util.Base64;
import com.google.cloud.datastore.Blob;
import java.util.Map;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RegisterResource {

    private static final String KIND_USER = "User";
    private static final String DEFAULT_ROLE = "ENDUSER";
    private static final String DEFAULT_STATE = "DEACTIVATED";

    private static final String PROFILE_PUBLIC = "PUBLIC";
    private static final String PROFILE_PRIVATE = "PRIVATE";

    private static final String PROPERTY_EMAIL = "email";
    private static final String PROPERTY_USERNAME = "username";
    private static final String PROPERTY_FULL_NAME = "fullName";
    private static final String PROPERTY_PHONE = "phone";
    private static final String PROPERTY_PASSWORD = "password";
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

    private static final String REGEX_EMAIL = ".+@.+\\..+";
    private static final String REGEX_PHONE = "^(\\+\\d{1,3}\\d{8,12}|\\d{9})$";
    private static final String REGEX_PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,}$";
    private static final String REGEX_CC_NUMBER = "^\\d{8}-\\d{1}-[a-zA-Z]{2}\\d{1}$";
    private static final String REGEX_NIF = "\\d{9}";

    private static final String ERROR_MISSING_FIELDS = "Missing required fields";
    private static final String ERROR_INVALID_EMAIL = "Invalid email format";
    private static final String ERROR_INVALID_FULL_NAME = "Full name must contain at least first and last name";
    private static final String ERROR_INVALID_PHONE = "Invalid phone number format (must be +XXX... or 9 digits)";
    private static final String ERROR_PASSWORD_REQUIREMENTS = "Password does not meet requirements";
    private static final String ERROR_PASSWORD_DIFFERENT = "Confirmation password is not equal to password";
    private static final String ERROR_INVALID_PROFILE = "Invalid profile visibility (must be PUBLIC or PRIVATE)";
    private static final String ERROR_INVALID_CC = "Invalid CC number format (must be 00000000-0-AA0)";
    private static final String ERROR_INVALID_NIF = "Invalid NIF format, must be 9 digits";
    private static final String ERROR_INVALID_EMPLOYER_NIF = "Invalid employer NIF format, must be 9 digits";
    private static final String ERROR_USER_EXISTS = "User already exists";
    private static final String ERROR_DATASTORE_FAILED = "Datastore operation failed";
    private static final String ERROR_INVALID_PHOTO_FORMAT = "Invalid photo data format";

    private static final String SUCCESS_USER_REGISTERED = "User registered successfully";

    private final Datastore datastore = DatastoreService.get();
    private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind(KIND_USER);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerUser(User user) {

        if (user == null || user.getEmail() == null || user.getUsername() == null || user.getFullName() == null ||
                user.getPhone() == null || user.getPassword() == null || user.getConfirmPassword() == null || user.getProfile() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_MISSING_FIELDS)).build();
        }
        if (!user.getEmail().matches(REGEX_EMAIL)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_INVALID_EMAIL)).build();
        }
        if (user.getFullName().trim().split("\\s+").length < 2) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_INVALID_FULL_NAME)).build();
        }
        if (!user.getPhone().matches(REGEX_PHONE)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_INVALID_PHONE)).build();
        }
        if (!user.getPassword().matches(REGEX_PASSWORD)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_PASSWORD_REQUIREMENTS)).build();
        }
        if (!user.getPassword().equals(user.getConfirmPassword())) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_PASSWORD_DIFFERENT)).build();
        }
        if (!(PROFILE_PUBLIC.equals(user.getProfile()) || PROFILE_PRIVATE.equals(user.getProfile()))) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_INVALID_PROFILE)).build();
        }
        if (user.getCcNumber() != null && !user.getCcNumber().matches(REGEX_CC_NUMBER)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_INVALID_CC)).build();
        }
        if (user.getNif() != null && !user.getNif().matches(REGEX_NIF)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_INVALID_NIF)).build();
        }
        if (user.getEmployerNIF() != null && !user.getEmployerNIF().matches(REGEX_NIF)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_INVALID_EMPLOYER_NIF)).build();
        }

        String hashedPassword = DigestUtils.sha512Hex(user.getPassword());

        Key userKey = userKeyFactory.newKey(user.getEmail());
        Transaction txn = datastore.newTransaction();
        try {
            Entity existingUser = txn.get(userKey);
            if (existingUser != null) {
                txn.rollback();
                return Response.status(Response.Status.CONFLICT).entity(Map.of("error", ERROR_USER_EXISTS)).build();
            }

            Entity.Builder builder = Entity.newBuilder(userKey)
                    .set(PROPERTY_EMAIL, user.getEmail())
                    .set(PROPERTY_USERNAME, user.getUsername())
                    .set(PROPERTY_FULL_NAME, user.getFullName())
                    .set(PROPERTY_PHONE, user.getPhone())
                    .set(PROPERTY_PASSWORD, hashedPassword)
                    .set(PROPERTY_PROFILE, user.getProfile())
                    .set(PROPERTY_ROLE, DEFAULT_ROLE)
                    .set(PROPERTY_STATE, DEFAULT_STATE);

            if (user.getCcNumber() != null) builder.set(PROPERTY_CC_NUMBER, user.getCcNumber().toUpperCase());
            if (user.getNif() != null) builder.set(PROPERTY_NIF, user.getNif());
            if (user.getEmployer() != null) builder.set(PROPERTY_EMPLOYER, user.getEmployer());
            if (user.getJob() != null) builder.set(PROPERTY_JOB, user.getJob());
            if (user.getAddress() != null) builder.set(PROPERTY_ADDRESS, user.getAddress());
            if (user.getEmployerNIF() != null) builder.set(PROPERTY_EMPLOYER_NIF, user.getEmployerNIF());

            if (user.getPhoto() != null && !user.getPhoto().isEmpty()) {
                try {
                    byte[] photoBytes = Base64.getDecoder().decode(user.getPhoto());

                    Blob photoBlob = Blob.copyFrom(photoBytes);
                    BlobValue photoBlobValue = BlobValue.newBuilder(photoBlob)
                            .setExcludeFromIndexes(true)
                            .build();

                    builder.set(PROPERTY_PHOTO, photoBlobValue);

                } catch (IllegalArgumentException e) {
                    txn.rollback();
                    return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_INVALID_PHOTO_FORMAT)).build();
                }
            }

            txn.add(builder.build());
            txn.commit();

            return Response.ok(Map.of("success", SUCCESS_USER_REGISTERED)).build();

        } catch (DatastoreException e) {
            if (txn.isActive()) {
                txn.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", ERROR_DATASTORE_FAILED + ": " + e.getMessage())).build();
        } catch (Exception e) {
            if (txn.isActive()) {
                txn.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", "An unexpected error occurred during registration: " + e.getMessage())).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }
}