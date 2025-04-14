package pt.unl.fct.di.adc.projeto.services;

import com.google.appengine.repackaged.org.apache.commons.codec.digest.DigestUtils;
import com.google.cloud.datastore.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import pt.unl.fct.di.adc.projeto.requests.ChangePasswordRequest;
import pt.unl.fct.di.adc.projeto.requests.ChangeRoleRequest;
import pt.unl.fct.di.adc.projeto.requests.ChangeStateRequest;
import pt.unl.fct.di.adc.projeto.core.AuthValidator;
import pt.unl.fct.di.adc.projeto.core.DatastoreService;
import pt.unl.fct.di.adc.projeto.util.AuthToken;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
public class UserService {

    private static final String KIND_USER = "User";
    private static final String KIND_AUTH_TOKEN = "AuthToken";

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_BACKOFFICE = "BACKOFFICE";
    private static final String ROLE_ENDUSER = "ENDUSER";
    private static final String ROLE_PARTNER = "PARTNER";

    private static final String STATE_ACTIVATED = "ACTIVATED";
    private static final String STATE_DEACTIVATED = "DEACTIVATED";
    private static final String STATE_SUSPENDED = "SUSPENDED";
    private static final Set<String> VALID_STATES = new HashSet<>(Set.of(STATE_ACTIVATED, STATE_DEACTIVATED, STATE_SUSPENDED));

    private static final String PROPERTY_ROLE = "role";
    private static final String PROPERTY_STATE = "state";
    private static final String PROPERTY_EMAIL = "email";
    private static final String PROPERTY_PASSWORD = "password";

    private static final String ERROR_MISSING_INPUT = "Missing input";
    private static final String ERROR_TARGET_USER_NOT_FOUND = "Target user not found";
    private static final String ERROR_CANNOT_CHANGE_OWN_ROLE = "Cannot change own role";
    private static final String ERROR_INSUFFICIENT_PERMISSION = "Insufficient permission";
    private static final String ERROR_INVALID_STATE = "Invalid state";
    private static final String ERROR_PASSWORDS_DO_NOT_MATCH = "New passwords do not match";
    private static final String ERROR_PASSWORD_REQUIREMENTS = "Password does not meet requirements";
    private static final String ERROR_USER_NOT_FOUND = "User not found";
    private static final String ERROR_INCORRECT_CURRENT_PASSWORD = "Incorrect current password";

    private static final String SUCCESS_PASSWORD_CHANGED = "Password changed successfully";

    private final Datastore datastore = DatastoreService.get();
    private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind(KIND_USER);

    @POST
    @Path("/changeRole")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeRole(ChangeRoleRequest req, @Context HttpHeaders headers) {
        if (req.getEmail() == null || req.getNewRole() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_MISSING_INPUT)).build();
        }

        AuthToken authToken = AuthValidator.extractFromHeader(headers);
        if (authToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", "Invalid or missing token")).build();
        }
        String requesterRole = authToken.getRole();
        String requesterEmail = authToken.getEmail();

        if (requesterEmail.equals(req.getEmail())) {
            return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", ERROR_CANNOT_CHANGE_OWN_ROLE)).build();
        }

        Transaction txn = datastore.newTransaction();
        try {
            Key targetUserKey = userKeyFactory.newKey(req.getEmail());
            Entity targetUser = txn.get(targetUserKey);
            if (targetUser == null) {
                txn.rollback();
                return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", ERROR_TARGET_USER_NOT_FOUND)).build();
            }
            String currentTargetRole = targetUser.getString(PROPERTY_ROLE);

            if (!canChangeRole(requesterRole, currentTargetRole, req.getNewRole())) {
                txn.rollback();
                return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", ERROR_INSUFFICIENT_PERMISSION)).build();
            }

            Entity updatedUser = Entity.newBuilder(targetUser)
                    .set(PROPERTY_ROLE, req.getNewRole())
                    .build();
            txn.put(updatedUser);

            updateAuthTokensRoleInTransaction(txn, req.getEmail(), req.getNewRole());

            txn.commit();
            return Response.ok(Map.of("success", true)).build();

        } catch (DatastoreException e) {
            if (txn.isActive()) {
                txn.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", "Datastore operation failed")).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }


    @POST
    @Path("/changeState")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeState(ChangeStateRequest req, @Context HttpHeaders headers) {
        if (req.getEmail() == null || req.getNewState() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_MISSING_INPUT)).build();
        }
        if (!VALID_STATES.contains(req.getNewState())) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_INVALID_STATE)).build();
        }

        AuthToken authToken = AuthValidator.extractFromHeader(headers);
        if (authToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", "Invalid or missing token")).build();
        }
        String requesterRole = authToken.getRole();

        Transaction txn = datastore.newTransaction();
        try {
            Key targetUserKey = userKeyFactory.newKey(req.getEmail());
            Entity targetUser = txn.get(targetUserKey);
            if (targetUser == null) {
                txn.rollback();
                return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", ERROR_TARGET_USER_NOT_FOUND)).build();
            }
            String currentState = targetUser.getString(PROPERTY_STATE);

            if (!canChangeState(requesterRole, currentState, req.getNewState())) {
                txn.rollback();
                return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", ERROR_INSUFFICIENT_PERMISSION)).build();
            }

            Entity updatedUser = Entity.newBuilder(targetUser)
                    .set(PROPERTY_STATE, req.getNewState())
                    .build();
            txn.put(updatedUser);

            txn.commit();
            return Response.ok(Map.of("success", true)).build();

        } catch (DatastoreException e) {
            if (txn.isActive()) {
                txn.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", "Datastore operation failed")).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @POST
    @Path("/changePassword")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changePassword(ChangePasswordRequest req, @Context HttpHeaders headers) {

        if (req.getCurrentPassword() == null || req.getNewPassword() == null || req.getConfirmNewPassword() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_MISSING_INPUT)).build();
        }
        if (!req.getNewPassword().equals(req.getConfirmNewPassword())) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_PASSWORDS_DO_NOT_MATCH)).build();
        }

        if (!req.getNewPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,}$")) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", ERROR_PASSWORD_REQUIREMENTS)).build();
        }

        AuthToken authToken = AuthValidator.extractFromHeader(headers);
        if (authToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", "Invalid or missing token")).build();
        }
        String requesterEmail = authToken.getEmail();

        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = userKeyFactory.newKey(requesterEmail);
            Entity user = txn.get(userKey);
            if (user == null) {
                txn.rollback();
                return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", ERROR_USER_NOT_FOUND)).build();
            }

            String storedPasswordHash = user.getString(PROPERTY_PASSWORD);
            if (!DigestUtils.sha512Hex(req.getCurrentPassword()).equals(storedPasswordHash)) {
                txn.rollback();
                return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", ERROR_INCORRECT_CURRENT_PASSWORD)).build();
            }

            String hashedNewPassword = DigestUtils.sha512Hex(req.getNewPassword());

            Entity updatedUser = Entity.newBuilder(user)
                    .set(PROPERTY_PASSWORD, hashedNewPassword)
                    .build();
            txn.put(updatedUser);

            txn.commit();
            return Response.ok(Map.of("success", SUCCESS_PASSWORD_CHANGED)).build();

        } catch (DatastoreException e) {
            if (txn.isActive()) {
                txn.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", "Datastore operation failed")).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    private boolean canChangeState(String requesterRole, String currentState, String newState) {
        return switch (requesterRole) {
            case ROLE_ADMIN -> true;
            case ROLE_BACKOFFICE -> (STATE_ACTIVATED.equals(currentState) && STATE_DEACTIVATED.equals(newState)) ||
                    (STATE_DEACTIVATED.equals(currentState) && STATE_ACTIVATED.equals(newState));
            default -> false;
        };
    }

    private boolean canChangeRole(String requesterRole, String currentTargetRole, String newTargetRole) {
        return switch (requesterRole) {
            case ROLE_ADMIN -> true;
            case ROLE_BACKOFFICE -> (ROLE_ENDUSER.equals(currentTargetRole) && ROLE_PARTNER.equals(newTargetRole)) ||
                    (ROLE_PARTNER.equals(currentTargetRole) && ROLE_ENDUSER.equals(newTargetRole));
            default -> false;
        };
    }

    private void updateAuthTokensRoleInTransaction(Transaction txn, String email, String newRole) throws DatastoreException {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(KIND_AUTH_TOKEN)
                .setFilter(StructuredQuery.PropertyFilter.eq(PROPERTY_EMAIL, email))
                .build();
        QueryResults<Entity> results = txn.run(query);

        while (results.hasNext()) {
            Entity tokenEntity = results.next();
            Entity updatedToken = Entity.newBuilder(tokenEntity)
                    .set(PROPERTY_ROLE, newRole)
                    .build();
            txn.put(updatedToken);
        }
    }
}