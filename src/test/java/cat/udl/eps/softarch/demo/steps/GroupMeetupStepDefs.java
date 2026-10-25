package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Group;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.GroupRepository;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/** RF-5.4 (Epic #29, issue #33): a GROUP meetup is visible and joinable only by the active members of its group. */
public class GroupMeetupStepDefs {

    private final StepDefs stepDefs;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final MeetupRepository meetupRepository;

    public GroupMeetupStepDefs(StepDefs stepDefs, UserRepository userRepository, GroupRepository groupRepository,
                               MeetupRepository meetupRepository) {
        this.stepDefs = stepDefs;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.meetupRepository = meetupRepository;
    }

    /** Stores a group meetup directly: the API creation path is covered by the scenarios that POST it. */
    @Given("a group meetup {string} exists in the group {string} created by {string}")
    public void aGroupMeetupExists(String title, String groupName, String creatorUsername) {
        Group group = group(groupName);
        User creator = userRepository.findById(creatorUsername).orElseThrow();

        Meetup meetup = new Meetup();
        meetup.setTitle(title);
        meetup.setCapacity(10);
        meetup.setVisibility(Meetup.Visibility.GROUP);
        meetup.setStatus(Meetup.MeetupStatus.OPEN);
        meetup.setOrganizer(creator);
        meetup.setGroup(group);
        meetup.setStartDate(Instant.now().plusSeconds(3600));
        meetup.setEndDate(Instant.now().plusSeconds(7200));
        RunAs.admin(() -> meetupRepository.save(meetup));
    }

    @When("I create a group meetup {string} for the group {string}")
    public void iCreateAGroupMeetupForTheGroup(String title, String groupName) throws Throwable {
        createGroupMeetup(title, group(groupName));
    }

    @When("I create a group meetup {string} for no group")
    public void iCreateAGroupMeetupForNoGroup(String title) throws Throwable {
        createGroupMeetup(title, null);
    }

    private void createGroupMeetup(String title, Group group) throws Throwable {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("capacity", 10);
        body.put("visibility", "GROUP");
        body.put("startDate", Instant.now().plusSeconds(3600).toString());
        body.put("endDate", Instant.now().plusSeconds(7200).toString());
        if (group != null) {
            body.put("group", group.getUri());
        }

        stepDefs.result = stepDefs.mockMvc.perform(
                post("/meetups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(stepDefs.mapper.writeValueAsString(body))
                    .characterEncoding(StandardCharsets.UTF_8)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @Then("The meetup {string} belongs to the group {string}")
    public void theMeetupBelongsToTheGroup(String title, String groupName) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        assertThat(meetup.getGroup() == null ? null : meetup.getGroup().getName(), is(groupName));
    }

    @When("I join the group meetup {string}")
    public void iJoinTheGroupMeetup(String title) throws Throwable {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();

        stepDefs.result = stepDefs.mockMvc.perform(
                post("/participations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(stepDefs.mapper.writeValueAsString(Map.of("meetup", meetup.getUri())))
                    .characterEncoding(StandardCharsets.UTF_8)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    /**
     * A non-member cannot join a GROUP meetup: the meetup is not readable for them, so resolving the
     * {@code meetup} URI in the request body already fails (400). If it were resolvable, the participation
     * handler would answer 403. Either way the request is rejected and no participation is created.
     */
    @Then("The join request is rejected")
    public void theJoinRequestIsRejected() throws Throwable {
        int status = stepDefs.result.andReturn().getResponse().getStatus();
        assertThat("Joining a group meetup must be rejected, but got " + status, status,
            anyOf(is(400), is(403), is(404)));
    }

    private Group group(String name) {
        return groupRepository.findByName(name).stream().findFirst()
            .orElseThrow(() -> new AssertionError("No group named " + name));
    }
}
