package pt.unl.fct.di.adc.projeto.entities;

public class User {

    private static final String DEFAULT_ROLE = "ENDUSER";
    private static final String DEFAULT_STATE = "DEACTIVATED";
    private String email;
    private String username;
    private String fullName;
    private String phone;
    private String password;
    private String confirmPassword; // para registo apenas
    private String profile;
    private String role = DEFAULT_ROLE;
    private String state = DEFAULT_STATE;
    // opcionais
    private String ccNumber;
    private String nif;
    private String employer;
    private String job;
    private String address;
    private String employerNIF;
    private String photo;

    public User() {}

    public User(String email, String username, String fullName, String phone, String password, String confirmPassword,
                String profile, String role, String state, String ccNumber,
                String nif, String employer, String job, String address, String employerNIF, String photo) {
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.phone = phone;
        this.password = password;
        this.confirmPassword = password;
        this.profile = profile;
        this.role = (role != null) ? role : DEFAULT_ROLE;
        this.state = (state != null) ? state : DEFAULT_STATE;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String hashedPassword) {
        this.password = hashedPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String hashedPassword) {
        this.confirmPassword = hashedPassword;
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