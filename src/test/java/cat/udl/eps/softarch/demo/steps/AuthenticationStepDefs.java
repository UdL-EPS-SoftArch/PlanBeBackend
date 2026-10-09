package cat.udl.eps.softarch.demo.steps;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.UserRepository;

public class AuthenticationStepDefs {

    public static String currentUsername;
    public static String currentPassword;

    @Autowired
    private UserRepository userRepository;

    @Before
    public void setup() {
        // Clear authentication credentials at the start of every test.
        AuthenticationStepDefs.currentPassword = "";
        AuthenticationStepDefs.currentUsername = "";
    }

    static RequestPostProcessor authenticate() {
        return currentUsername!=null ? httpBasic(currentUsername, currentPassword) : anonymous();
    }

    @Given("^I login as \"([^\"]*)\" with password \"([^\"]*)\"$")
    public void iLoginAsWithPassword(String username, String password) {
        // Ensure the user exists in the database to avoid 401 Unauthorized during tests
        if (!userRepository.existsById(username)) {
            User user = new User();
            user.setId(username);
            user.setPassword(password);
            user.setEmail(username + "@sample.app");
            user.encodePassword();
            userRepository.save(user);
        }
        AuthenticationStepDefs.currentUsername = username;
        AuthenticationStepDefs.currentPassword = password;
    }

    @Given("^I'm not logged in$")
    public void iMNotLoggedIn() {
        currentUsername = currentPassword = null;
    }
}
