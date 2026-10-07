package cat.udl.eps.softarch.demo.steps;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import io.cucumber.java.en.When;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ChangePasswordStepDefs {
  private final StepDefs stepDefs;

  public ChangePasswordStepDefs(StepDefs stepDefs) {
    this.stepDefs = stepDefs;
  }

  @When("I change the password of user {string} to {string}")
  public void iChangeThePasswordOfUserTo(String username, String password) throws Throwable {
    patchUser(username, Map.of("password", password));
  }

  @When("I change the email of user {string} to {string}")
  public void iChangeTheEmailOfUserTo(String username, String email) throws Throwable {
    patchUser(username, Map.of("email", email));
  }

  private void patchUser(String username, Map<String, String> changes) throws Throwable {
    stepDefs.result = stepDefs.mockMvc.perform(
            patch("/users/{username}", username)
                .contentType(MediaType.APPLICATION_JSON)
                .content(stepDefs.mapper.writeValueAsString(changes))
                .characterEncoding(StandardCharsets.UTF_8)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate()))
        .andDo(print());
  }
}
