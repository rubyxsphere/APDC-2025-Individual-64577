package pt.unl.fct.di.adc.projeto.requests;

import java.util.Date;

public class ModifyWorkSheetRequest {

    private String workReference;

    private String description;

    private String targetWorkType;

    private String adjudicationStatus;

    private Date adjudicationDate;

    private Date expectedStartDate;

    private Date expectedConclusionDate;

    private String entityAccount;

    private String adjudicationEntity;

    private String companyNIF;

    private String workStatus;

    private String observations;

    public ModifyWorkSheetRequest() {
    }

    public ModifyWorkSheetRequest(String workReference, String description, String targetWorkType, String adjudicationStatus, Date adjudicationDate, Date expectedStartDate, Date expectedConclusionDate, String entityAccount, String adjudicationEntity, String companyNIF, String workStatus, String observations) {
        this.workReference = workReference;
        this.description = description;
        this.targetWorkType = targetWorkType;
        this.adjudicationStatus = adjudicationStatus;
        this.adjudicationDate = adjudicationDate;
        this.expectedStartDate = expectedStartDate;
        this.expectedConclusionDate = expectedConclusionDate;
        this.entityAccount = entityAccount;
        this.adjudicationEntity = adjudicationEntity;
        this.companyNIF = companyNIF;
        this.workStatus = workStatus;
        this.observations = observations;
    }

    public String getWorkReference() {
        return workReference;
    }

    public void setWorkReference(String workReference) {
        this.workReference = workReference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetWorkType() {
        return targetWorkType;
    }

    public void setTargetWorkType(String targetWorkType) {
        this.targetWorkType = targetWorkType;
    }

    public String getAdjudicationStatus() {
        return adjudicationStatus;
    }

    public void setAdjudicationStatus(String adjudicationStatus) {
        this.adjudicationStatus = adjudicationStatus;
    }

    public Date getAdjudicationDate() {
        return adjudicationDate;
    }

    public void setAdjudicationDate(Date adjudicationDate) {
        this.adjudicationDate = adjudicationDate;
    }

    public Date getExpectedStartDate() {
        return expectedStartDate;
    }

    public void setExpectedStartDate(Date expectedStartDate) {
        this.expectedStartDate = expectedStartDate;
    }

    public Date getExpectedConclusionDate() {
        return expectedConclusionDate;
    }

    public void setExpectedConclusionDate(Date expectedConclusionDate) {
        this.expectedConclusionDate = expectedConclusionDate;
    }

    public String getEntityAccount() {
        return entityAccount;
    }

    public void setEntityAccount(String entityAccount) {
        this.entityAccount = entityAccount;
    }

    public String getAdjudicationEntity() {
        return adjudicationEntity;
    }

    public void setAdjudicationEntity(String adjudicationEntity) {
        this.adjudicationEntity = adjudicationEntity;
    }

    public String getCompanyNIF() {
        return companyNIF;
    }

    public void setCompanyNIF(String companyNIF) {
        this.companyNIF = companyNIF;
    }

    public String getWorkStatus() {
        return workStatus;
    }

    public void setWorkStatus(String workStatus) {
        this.workStatus = workStatus;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}