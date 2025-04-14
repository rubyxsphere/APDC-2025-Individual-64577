package pt.unl.fct.di.adc.projeto.requests;

public class UserDataRequest {

    private String email;
    private String username;
    private String fullName;
    private String phone;
    private String profile;
    private String role;
    private String state;
    private String ccNumber;
    private String nif;
    private String employer;
    private String job;
    private String address;
    private String employerNIF;
    private String photo;

    public UserDataRequest() {}

    public UserDataRequest(String email, String username, String fullName, String phone, String profile, String role, String state, String ccNumber, String nif, String employer, String job, String address, String employerNIF, String photo) {
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.phone = phone;
        this.profile = profile;
        this.role = role;
        this.state = state;
        this.ccNumber = ccNumber;
        this.nif = nif;
        this.employer = employer;
        this.job = job;
        this.address = address;
        this.employerNIF = employerNIF;
        this.photo = photo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCcNumber() {
        return ccNumber;
    }

    public void setCcNumber(String ccNumber) {
        this.ccNumber = ccNumber;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public String getEmployer() {
        return employer;
    }

    public void setEmployer(String employer) {
        this.employer = employer;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmployerNIF() {
        return employerNIF;
    }

    public void setEmployerNIF(String employerNIF) {
        this.employerNIF = employerNIF;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
}