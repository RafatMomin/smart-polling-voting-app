package onetoone.Users.dto;

public class ResetPasswordRequest {
    private String token;
    private String newPassword;

    // getters and setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
