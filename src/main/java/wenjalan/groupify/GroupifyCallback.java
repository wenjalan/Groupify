package wenjalan.groupify;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GroupifyCallback {

    // the last code returned by a getCode request
    private static String lastCode = null;

    // receives a Spotify Authentication Callback
    @RequestMapping(value = "/callback")
    public String getCode(@RequestParam(value = "code", defaultValue = "") String code) {
        if (code.isEmpty()) {
            return "error retrieving code";
        }
        else {
            lastCode = code;
            return "you can close this now";
        }
    }

    // returns the last authentication code
    // returns null if none exists yet
    public static String getLastCode() {
        if (lastCode == null || lastCode.isEmpty()) {
            return null;
        }
        else {
            // delete the code and return it
            String code = lastCode;
            lastCode = null;
            return code;
        }
    }

}
