package pt.unl.fct.di.adc.projeto.requests;

public class ChangeRoleRequest {

    private String email;

    private String newRole;

    public ChangeRoleRequest() {
    }

    public ChangeRoleRequest(String email, String newRole) {
        this.email = email;
        this.newRole = newRole;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNewRole() {
        return newRole;
    }

    public void setNewRole(String newRole) {
        this.newRole = newRole;
    }
}