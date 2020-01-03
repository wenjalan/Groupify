package wenjalan.groupify.service;

// interface for listening for authentication callbacks
public interface AuthenticationListener {

    void onAuthenticationSuccess(String code, String state);

    void onAuthenticationFailure(String message);

    String partyId();

}
