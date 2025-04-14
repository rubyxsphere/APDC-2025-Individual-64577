package pt.unl.fct.di.adc.projeto.services;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import pt.unl.fct.di.adc.projeto.entities.WorkSheet;
import pt.unl.fct.di.adc.projeto.requests.ModifyWorkSheetRequest;
import pt.unl.fct.di.adc.projeto.core.AuthValidator;
import pt.unl.fct.di.adc.projeto.core.DatastoreService;
import pt.unl.fct.di.adc.projeto.util.AuthToken;
import java.util.Map;
import java.util.Set;

@Path("/workSheets")
@Produces(MediaType.APPLICATION_JSON)
public class WorkSheetService {

    private static final String KIND_WORK_SHEET = "WorkSheet";

    private static final String ROLE_BACKOFFICE = "BACKOFFICE";
    private static final String ROLE_PARTNER = "PARTNER";

    private static final String PROPERTY_WORK_REFERENCE = "workReference";
    private static final String PROPERTY_DESCRIPTION = "description";
    private static final String PROPERTY_TARGET_WORK_TYPE = "targetWorkType";
    private static final String PROPERTY_ADJUDICATION_STATUS = "adjudicationStatus";
    private static final String PROPERTY_ADJUDICATION_DATE = "adjudicationDate";
    private static final String PROPERTY_EXPECTED_START_DATE = "expectedStartDate";
    private static final String PROPERTY_EXPECTED_CONCLUSION_DATE = "expectedConclusionDate";
    private static final String PROPERTY_ENTITY_ACCOUNT = "entityAccount";
    private static final String PROPERTY_ADJUDICATION_ENTITY = "adjudicationEntity";
    private static final String PROPERTY_COMPANY_NIF = "companyNIF";
    private static final String PROPERTY_WORK_STATUS = "workStatus";
    private static final String PROPERTY_OBSERVATIONS = "observations";

    private static final String ERROR_INSUFFICIENT_PERMISSION_CREATE = "Insufficient permission to create work sheets";
    private static final String ERROR_INSUFFICIENT_PERMISSION_MODIFY = "Insufficient permission to modify work sheets";
    private static final String ERROR_MISSING_REQUIRED_ATTRIBUTES = "Missing required attributes for work sheet creation";
    private static final String ERROR_WORK_SHEET_NOT_FOUND = "Work sheet not found";
    private static final String ERROR_WORK_SHEET_ALREADY_EXISTS = "Work sheet with this reference already exists";
    private static final String ERROR_TOKEN_INVALID = "Invalid or missing token";
    private static final String ERROR_DATASTORE_FAILED = "Datastore operation failed";
    private static final String ERROR_MISSING_WORK_REFERENCE = "Missing required attribute: workReference";

    private static final String ERROR_INVALID_TARGET_TYPE = "Invalid targetWorkType value. Must be 'Public Property' or 'Private Property'.";
    private static final String ERROR_INVALID_ADJUDICATION_STATUS = "Invalid adjudicationStatus value. Must be 'ADJUDICATED' or 'NOT ADJUDICATED'.";
    private static final String ERROR_INVALID_WORK_STATUS = "Invalid workStatus value. Must be 'NOT STARTED', 'IN PROGRESS', or 'CONCLUDED'.";

    private static final String SUCCESS_WORK_SHEET_CREATED = "Work sheet created successfully";
    private static final String SUCCESS_WORK_SHEET_MODIFIED = "Work sheet modified successfully";

    private static final Set<String> VALID_TARGET_TYPES = Set.of(WorkSheet.TARGET_TYPE_PUBLIC, WorkSheet.TARGET_TYPE_PRIVATE);
    private static final Set<String> VALID_ADJUDICATION_STATUSES = Set.of(WorkSheet.STATUS_ADJUDICATED, WorkSheet.STATUS_NOT_ADJUDICATED);
    private static final Set<String> VALID_WORK_STATUSES = Set.of(WorkSheet.WORK_STATUS_NOT_STARTED, WorkSheet.WORK_STATUS_IN_PROGRESS, WorkSheet.WORK_STATUS_CONCLUDED);


    private final Datastore datastore = DatastoreService.get();
    private final KeyFactory workSheetKeyFactory = datastore.newKeyFactory().setKind(KIND_WORK_SHEET);

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createWorkSheet(WorkSheet ws, @Context HttpHeaders headers) {

        AuthToken authToken = AuthValidator.extractFromHeader(headers);
        if (authToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", ERROR_TOKEN_INVALID)).build();
        }
        String requesterRole = authToken.getRole();

        if (!ROLE_BACKOFFICE.equals(requesterRole)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", ERROR_INSUFFICIENT_PERMISSION_CREATE))
                    .build();
        }

        if (ws.getWorkReference() == null || ws.getDescription() == null || ws.getTargetWorkType() == null || ws.getAdjudicationStatus() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ERROR_MISSING_REQUIRED_ATTRIBUTES))
                    .build();
        }

        if (!VALID_TARGET_TYPES.contains(ws.getTargetWorkType())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ERROR_INVALID_TARGET_TYPE))
                    .build();
        }

        if (!VALID_ADJUDICATION_STATUSES.contains(ws.getAdjudicationStatus())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ERROR_INVALID_ADJUDICATION_STATUS))
                    .build();
        }

        if (WorkSheet.STATUS_ADJUDICATED.equals(ws.getAdjudicationStatus()) && ws.getWorkStatus() != null && !VALID_WORK_STATUSES.contains(ws.getWorkStatus())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ERROR_INVALID_WORK_STATUS))
                    .build();
        }


        Key workSheetKey = workSheetKeyFactory.newKey(ws.getWorkReference());
        Entity.Builder builder = Entity.newBuilder(workSheetKey)
                .set(PROPERTY_WORK_REFERENCE, ws.getWorkReference())
                .set(PROPERTY_DESCRIPTION, ws.getDescription())
                .set(PROPERTY_TARGET_WORK_TYPE, ws.getTargetWorkType())
                .set(PROPERTY_ADJUDICATION_STATUS, ws.getAdjudicationStatus());

        if (WorkSheet.STATUS_ADJUDICATED.equals(ws.getAdjudicationStatus())) {
            if (ws.getAdjudicationDate() != null)
                builder.set(PROPERTY_ADJUDICATION_DATE, Timestamp.of(ws.getAdjudicationDate()));
            if (ws.getExpectedStartDate() != null)
                builder.set(PROPERTY_EXPECTED_START_DATE, Timestamp.of(ws.getExpectedStartDate()));
            if (ws.getExpectedConclusionDate() != null)
                builder.set(PROPERTY_EXPECTED_CONCLUSION_DATE, Timestamp.of(ws.getExpectedConclusionDate()));
            if (ws.getEntityAccount() != null) builder.set(PROPERTY_ENTITY_ACCOUNT, ws.getEntityAccount());
            if (ws.getAdjudicationEntity() != null)
                builder.set(PROPERTY_ADJUDICATION_ENTITY, ws.getAdjudicationEntity());
            if (ws.getCompanyNIF() != null) builder.set(PROPERTY_COMPANY_NIF, ws.getCompanyNIF());
            if (ws.getWorkStatus() != null)
                builder.set(PROPERTY_WORK_STATUS, ws.getWorkStatus());
            if (ws.getObservations() != null) builder.set(PROPERTY_OBSERVATIONS, ws.getObservations());
        }

        Entity workSheetEntity = builder.build();

        Transaction txn = datastore.newTransaction();
        try {
            txn.add(workSheetEntity);
            txn.commit();
            return Response.status(Response.Status.CREATED)
                    .entity(Map.of("success", SUCCESS_WORK_SHEET_CREATED))
                    .build();
        } catch (DatastoreException e) {
            if (txn.isActive()) {
                txn.rollback();
            }
            if (e.getCode() == 6) { // ALREADY_EXISTS code
                return Response.status(Response.Status.CONFLICT)
                        .entity(Map.of("error", ERROR_WORK_SHEET_ALREADY_EXISTS))
                        .build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", ERROR_DATASTORE_FAILED)).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @POST
    @Path("/modify")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyWorkSheet(ModifyWorkSheetRequest req, @Context HttpHeaders headers) {

        AuthToken authToken = AuthValidator.extractFromHeader(headers);
        if (authToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", ERROR_TOKEN_INVALID)).build();
        }
        String requesterRole = authToken.getRole();

        if (!ROLE_BACKOFFICE.equals(requesterRole) && !ROLE_PARTNER.equals(requesterRole)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", ERROR_INSUFFICIENT_PERMISSION_MODIFY))
                    .build();
        }

        if (req.getWorkReference() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ERROR_MISSING_WORK_REFERENCE))
                    .build();
        }

        if (req.getTargetWorkType() != null && !VALID_TARGET_TYPES.contains(req.getTargetWorkType())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ERROR_INVALID_TARGET_TYPE))
                    .build();
        }
        if (req.getAdjudicationStatus() != null && !VALID_ADJUDICATION_STATUSES.contains(req.getAdjudicationStatus())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ERROR_INVALID_ADJUDICATION_STATUS))
                    .build();
        }
        if (req.getWorkStatus() != null && !VALID_WORK_STATUSES.contains(req.getWorkStatus())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ERROR_INVALID_WORK_STATUS))
                    .build();
        }

        Transaction txn = datastore.newTransaction();
        try {
            Key workSheetKey = workSheetKeyFactory.newKey(req.getWorkReference());
            Entity currentWorkSheet = txn.get(workSheetKey);

            if (currentWorkSheet == null) {
                txn.rollback();
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", ERROR_WORK_SHEET_NOT_FOUND))
                        .build();
            }

            Entity.Builder builder = Entity.newBuilder(currentWorkSheet);

            if (req.getDescription() != null) builder.set(PROPERTY_DESCRIPTION, req.getDescription());
            if (req.getTargetWorkType() != null) builder.set(PROPERTY_TARGET_WORK_TYPE, req.getTargetWorkType());
            if (req.getAdjudicationStatus() != null) builder.set(PROPERTY_ADJUDICATION_STATUS, req.getAdjudicationStatus());
            if (req.getAdjudicationDate() != null) builder.set(PROPERTY_ADJUDICATION_DATE, Timestamp.of(req.getAdjudicationDate()));
            if (req.getExpectedStartDate() != null) builder.set(PROPERTY_EXPECTED_START_DATE, Timestamp.of(req.getExpectedStartDate()));
            if (req.getExpectedConclusionDate() != null) builder.set(PROPERTY_EXPECTED_CONCLUSION_DATE, Timestamp.of(req.getExpectedConclusionDate()));
            if (req.getEntityAccount() != null) builder.set(PROPERTY_ENTITY_ACCOUNT, req.getEntityAccount());
            if (req.getAdjudicationEntity() != null) builder.set(PROPERTY_ADJUDICATION_ENTITY, req.getAdjudicationEntity());
            if (req.getCompanyNIF() != null) builder.set(PROPERTY_COMPANY_NIF, req.getCompanyNIF());
            if (req.getWorkStatus() != null) builder.set(PROPERTY_WORK_STATUS, req.getWorkStatus());
            if (req.getObservations() != null) builder.set(PROPERTY_OBSERVATIONS, req.getObservations());

            txn.put(builder.build());
            txn.commit();

            return Response.ok(Map.of("success", SUCCESS_WORK_SHEET_MODIFIED)).build();

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
}