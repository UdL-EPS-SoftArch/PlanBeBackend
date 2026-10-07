package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Record;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.RecordRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class ManageRecordStepDefs {
    private final StepDefs stepDefs;
    private final UserRepository userRepository;
    private final RecordRepository recordRepository;

    public ManageRecordStepDefs(StepDefs stepDefs, UserRepository userRepository, 
        RecordRepository recordRepository) {
        this.stepDefs = stepDefs;
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
    }

    @Given("There is a record with name {string} owned by {string}")
    public void thereIsARecordWithNameOwnedBy(String name, String ownerUsername) {
        Record record = new Record();
        record.setName(name);
        User owner = userRepository.findById(ownerUsername).orElseThrow();
        record.setOwnedBy(owner);
        record.setStatus(Record.Status.PRIVATE);
        recordRepository.save(record);
    }

    @When("I create a new record with name {string}")
    public void iCreateANewRecordWithNameOwnedBy(String name) throws Throwable {
        Record record = new Record();
        record.setName(name);

        stepDefs.result = stepDefs.mockMvc.perform(
                post("/records")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(stepDefs.mapper.writeValueAsString(record))
                    .characterEncoding(StandardCharsets.UTF_8)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @And("The new record is owned by {string}")
    public void theNewRecordIsOwnedBy(String username) throws Throwable {
        String newRecordUri = stepDefs.result.andReturn().getResponse().getHeader("Location");
        stepDefs.result = stepDefs.mockMvc.perform(
                get(newRecordUri + "/ownedBy")
                    .accept(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print())
            .andExpect(jsonPath("$.username", is(username)));
    }

    @When("I retrieve the record with name {string}")
    public void iRetrieveRecordByName(String name) throws Throwable {
        Record record = recordRepository.findByName(name).stream().findFirst().orElseThrow();
        stepDefs.result = stepDefs.mockMvc.perform(
                get(record.getUri())
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @Then("The retrieved record has name {string}")
    public void theRetrieveRecordHasName(String name) throws Throwable {
        stepDefs.result.andExpect(jsonPath("$.name", is(name)));
    }

    @Then("The list of records owned by {string} includes {int} named {string}")
    public void theListOfRecordsOwnedByIncludesNamed(String username, int count, String resourceName) throws Throwable {
        User owner = userRepository.findById(username).orElseThrow();
        stepDefs.result = stepDefs.mockMvc.perform(
                get("/records/search/findByOwnedBy?user={userUri}", owner.getUri())
                    .accept(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print())
            .andExpect(jsonPath("$._embedded.records[?(@.name == '" + resourceName + "')]", hasSize(count)));
    }

    @Given("I make the record with name {string} public")
    public void I_make_the_record_with_name_public(String name) throws Throwable {
        Record record = recordRepository.findByName(name).stream().findFirst().orElseThrow();
        stepDefs.result = stepDefs.mockMvc.perform(
                patch(record.getUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"PUBLIC\"}")
                    .characterEncoding(StandardCharsets.UTF_8)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @When("I edit the record with name {string} to have name {string}")
    public void iEditRecordName(String currentName, String newName) throws Throwable {
        Record record = recordRepository.findByName(currentName).stream().findFirst().orElseThrow();
        stepDefs.result = stepDefs.mockMvc.perform(
                patch(record.getUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(stepDefs.mapper.writeValueAsString(Map.of("name", newName)))
                    .characterEncoding(StandardCharsets.UTF_8)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @Then("The modified timestamp of the record with name {string} is after the created one")
    public void modifiedTimestampIsAfterCreated(String name) throws Throwable {
        Record record = recordRepository.findByName(name).stream().findFirst().orElseThrow();
        String json = stepDefs.mockMvc.perform(
                get(record.getUri())
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print())
            .andReturn().getResponse().getContentAsString();
        String created = JsonPath.read(json, "$.created");
        String modified = JsonPath.read(json, "$.modified");
        assertThat("modified should be after created", created, lessThan(modified));
    }

    @When("I delete the record with name {string}")
    public void iDeleteRecordByName(String name) throws Throwable {
        Record record = recordRepository.findByName(name).stream().findFirst().orElseThrow();
        stepDefs.result = stepDefs.mockMvc.perform(
                delete(record.getUri())
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }
}
