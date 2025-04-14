package pt.unl.fct.di.adc.projeto.requests;

public class ChangeStateRequest {

    private String email;

    private String newState;

    public ChangeStateRequest() {
    }

    public ChangeStateRequest(String email, String newState) {
        this.email = email;
        this.newState = newState;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNewState() {
        return newState;
    }

    public void setNewState(String newState) {
        this.newState = newState;
    }
}