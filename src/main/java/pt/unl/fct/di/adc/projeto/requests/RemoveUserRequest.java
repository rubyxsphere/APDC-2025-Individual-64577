package pt.unl.fct.di.adc.projeto.requests;

public class RemoveUserRequest {

    private String email;

    public RemoveUserRequest() {
    }

    public RemoveUserRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}