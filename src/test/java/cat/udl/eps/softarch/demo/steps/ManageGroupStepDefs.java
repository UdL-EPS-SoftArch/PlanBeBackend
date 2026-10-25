package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Group;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.domain.enums.Visibility;
import cat.udl.eps.softarch.demo.repository.GroupRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class ManageGroupStepDefs {
    private final StepDefs stepDefs;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public ManageGroupStepDefs(StepDefs stepDefs, UserRepository userRepository,
        GroupRepository groupRepository) {
        this.stepDefs = stepDefs;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    @Given("There is a group with name {string} with visibility {string} owned by {string}")
    public void thereIsAGroupWithNameWithVisibilityOwnedBy(String name, String visibility, String ownerUsername) {
        Group group = new Group();
        group.setName(name);
        group.setVisibility(Visibility.valueOf(visibility));
        group.setOwnedBy(userRepository.findById(ownerUsername).orElseThrow());
        groupRepository.save(group);
    }

    @When("I create a new group with name {string}")
    public void iCreateANewGroupWithName(String name) throws Throwable {
        Group group = new Group();
        group.setName(name);

        stepDefs.result = stepDefs.mockMvc.perform(
                post("/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(stepDefs.mapper.writeValueAsString(group))
                    .characterEncoding(StandardCharsets.UTF_8)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @And("The new group is owned by {string}")
    public void theNewGroupIsOwnedBy(String username) throws Throwable {
        String newGroupUri = stepDefs.result.andReturn().getResponse().getHeader("Location");
        stepDefs.result = stepDefs.mockMvc.perform(
                get(newGroupUri + "/ownedBy")
                    .accept(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print())
            .andExpect(jsonPath("$.username", is(username)));
    }

    @When("I retrieve the group with name {string}")
    public void iRetrieveGroupByName(String name) throws Throwable {
        Group group = groupRepository.findByName(name).stream().findFirst().orElseThrow();
        stepDefs.result = stepDefs.mockMvc.perform(
                get(group.getUri())
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @When("I edit the group with name {string} to have name {string}")
    public void iEditGroupName(String currentName, String newName) throws Throwable {
        Group group = groupRepository.findByName(currentName).stream().findFirst().orElseThrow();
        stepDefs.result = stepDefs.mockMvc.perform(
                patch(group.getUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(stepDefs.mapper.writeValueAsString(Map.of("name", newName)))
                    .characterEncoding(StandardCharsets.UTF_8)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @When("I delete the group with name {string}")
    public void iDeleteGroupByName(String name) throws Throwable {
        Group group = groupRepository.findByName(name).stream().findFirst().orElseThrow();
        stepDefs.result = stepDefs.mockMvc.perform(
                delete(group.getUri())
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @Then("The list of groups owned by {string} includes {int} named {string}")
    public void theListOfGroupsOwnedByIncludesNamed(String username, int count, String groupName) throws Throwable {
        User owner = userRepository.findById(username).orElseThrow();
        stepDefs.result = stepDefs.mockMvc.perform(
                get("/groups/search/findByOwnedBy?user={userUri}", owner.getUri())
                    .accept(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print())
            .andExpect(jsonPath("$._embedded.groups[?(@.name == '" + groupName + "')]", hasSize(count)));
    }
}
