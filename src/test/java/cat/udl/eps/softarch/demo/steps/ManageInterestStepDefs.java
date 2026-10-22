package cat.udl.eps.softarch.demo.steps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import cat.udl.eps.softarch.demo.domain.Interest;
import cat.udl.eps.softarch.demo.repository.InterestRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ManageInterestStepDefs {
  private final StepDefs stepDefs;
  private final InterestRepository interestRepository;

  public ManageInterestStepDefs(StepDefs stepDefs, InterestRepository interestRepository) {
    this.stepDefs = stepDefs;
    this.interestRepository = interestRepository;
  }

  @Given("There is an interest named {string}")
  public void thereIsAnInterestNamed(String name) {
    if (interestRepository.findByName(name).isEmpty()) {
      Interest interest = new Interest();
      interest.setName(name);
      interestRepository.save(interest);
    }
  }

  @When("I create an interest named {string}")
  public void iCreateAnInterestNamed(String name) throws Throwable {
    stepDefs.result = stepDefs.mockMvc.perform(
            post("/interests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(stepDefs.mapper.writeValueAsString(Map.of("name", name)))
                .characterEncoding(StandardCharsets.UTF_8)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate()))
        .andDo(print());
  }

  @When("I create an interest named {string} in the category {string}")
  public void iCreateAnInterestNamedInTheCategory(String name, String category) throws Throwable {
    stepDefs.result = stepDefs.mockMvc.perform(
            post("/interests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(stepDefs.mapper.writeValueAsString(Map.of("name", name, "category", category)))
                .characterEncoding(StandardCharsets.UTF_8)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate()))
        .andDo(print());
  }

  @And("The interest {string} has the category {string}")
  public void theInterestHasTheCategory(String name, String category) {
    assertThat(interestRepository.findByName(name).orElseThrow().getCategory().name(), is(category));
  }

  @When("I list the interests")
  public void iListTheInterests() throws Throwable {
    stepDefs.result = stepDefs.mockMvc.perform(
            get("/interests")
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate()))
        .andDo(print());
  }

  @And("There is an interest named {string} in the catalogue")
  public void thereIsAnInterestNamedInTheCatalogue(String name) {
    assertTrue(interestRepository.findByName(name).isPresent(), "Interest \"" + name + "\" should exist");
  }

  @And("There is no interest named {string} in the catalogue")
  public void thereIsNoInterestNamedInTheCatalogue(String name) {
    assertFalse(interestRepository.findByName(name).isPresent(), "Interest \"" + name + "\" shouldn't exist");
  }

  @And("The interest list contains {string}")
  public void theInterestListContains(String name) throws Throwable {
    stepDefs.result.andExpect(jsonPath("$._embedded.interests[*].name", hasItem(name)));
  }
}
