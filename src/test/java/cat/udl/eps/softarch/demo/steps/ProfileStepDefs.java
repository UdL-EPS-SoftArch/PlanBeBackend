package cat.udl.eps.softarch.demo.steps;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cat.udl.eps.softarch.demo.domain.Gender;
import cat.udl.eps.softarch.demo.domain.Interest;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.InterestRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

public class ProfileStepDefs {
  private final StepDefs stepDefs;
  private final UserRepository userRepository;
  private final InterestRepository interestRepository;
  private final TransactionTemplate transactionTemplate;

  public ProfileStepDefs(StepDefs stepDefs, UserRepository userRepository,
                         InterestRepository interestRepository, TransactionTemplate transactionTemplate) {
    this.stepDefs = stepDefs;
    this.userRepository = userRepository;
    this.interestRepository = interestRepository;
    this.transactionTemplate = transactionTemplate;
  }

  @Given("User {string} has birth date {string} and gender {string}")
  public void userHasBirthDateAndGender(String username, String birthDate, String gender) {
    User user = userRepository.findById(username).orElseThrow();
    user.setBirthDate(LocalDate.parse(birthDate));
    user.setGender(Gender.valueOf(gender));
    userRepository.save(user);
  }

  @Given("User {string} has the interest {string} assigned")
  public void userHasTheInterestAssigned(String username, String interestName) {
    transactionTemplate.executeWithoutResult(status -> {
      User user = userRepository.findById(username).orElseThrow();
      user.getInterests().add(interestRepository.findByName(interestName).orElseThrow());
      userRepository.save(user);
    });
  }

  @When("I update the profile of user {string} with")
  public void iUpdateTheProfileOfUserWith(String username, Map<String, String> changes) throws Throwable {
    stepDefs.result = stepDefs.mockMvc.perform(
            patch("/users/{username}", username)
                .contentType(MediaType.APPLICATION_JSON)
                .content(stepDefs.mapper.writeValueAsString(changes))
                .characterEncoding(StandardCharsets.UTF_8)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate()))
        .andDo(print());
  }

  @When("I add the interest {string} to user {string}")
  public void iAddTheInterestToUser(String interestName, String username) throws Throwable {
    Interest interest = interestRepository.findByName(interestName).orElseThrow();
    stepDefs.result = stepDefs.mockMvc.perform(
            post("/users/{username}/interests", username)
                .contentType("text/uri-list")
                .content("http://localhost" + interest.getUri())
                .with(AuthenticationStepDefs.authenticate()))
        .andDo(print());
  }

  @When("I remove the interest {string} from user {string}")
  public void iRemoveTheInterestFromUser(String interestName, String username) throws Throwable {
    Interest interest = interestRepository.findByName(interestName).orElseThrow();
    stepDefs.result = stepDefs.mockMvc.perform(
            delete("/users/{username}/interests/{id}", username, interest.getId())
                .with(AuthenticationStepDefs.authenticate()))
        .andDo(print());
  }

  @Then("The profile of user {string} has {string} equal to {string}")
  public void theProfileOfUserHasEqualTo(String username, String field, String value) throws Throwable {
    getProfile(username).andExpect(jsonPath("$." + field, is(value)));
  }

  @And("The profile of user {string} shows the age for birth date {string}")
  public void theProfileOfUserShowsTheAgeForBirthDate(String username, String birthDate) throws Throwable {
    int age = Period.between(LocalDate.parse(birthDate), LocalDate.now()).getYears();
    getProfile(username).andExpect(jsonPath("$.age", is(age)));
  }

  @Then("The profile of user {string} does not show its personal data")
  public void theProfileOfUserDoesNotShowItsPersonalData(String username) throws Throwable {
    getProfile(username)
        .andExpect(jsonPath("$.birthDate").doesNotExist())
        .andExpect(jsonPath("$.gender").doesNotExist())
        .andExpect(jsonPath("$.age").doesNotExist());
  }

  @And("User {string} is not an admin")
  public void userIsNotAnAdmin(String username) {
    assertFalse(userRepository.findById(username).orElseThrow().isAdmin(),
        "User \"" + username + "\" shouldn't be an admin");
  }

  @And("User {string} has the interest {string}")
  public void userHasTheInterest(String username, String interestName) throws Throwable {
    getInterests(username).andExpect(jsonPath("$._embedded.interests[*].name", hasItem(interestName)));
  }

  @And("User {string} does not have the interest {string}")
  public void userDoesNotHaveTheInterest(String username, String interestName) throws Throwable {
    getInterests(username).andExpect(jsonPath("$._embedded.interests[*].name", not(hasItem(interestName))));
  }

  private org.springframework.test.web.servlet.ResultActions getProfile(String username) throws Throwable {
    return stepDefs.mockMvc.perform(
            get("/users/{username}", username)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate()))
        .andDo(print())
        .andExpect(status().isOk());
  }

  private org.springframework.test.web.servlet.ResultActions getInterests(String username) throws Throwable {
    return stepDefs.mockMvc.perform(
            get("/users/{username}/interests", username)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate()))
        .andDo(print())
        .andExpect(status().isOk());
  }
}
