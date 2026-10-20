package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Invitation;
import cat.udl.eps.softarch.demo.domain.InvitationStatus;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.domain.Meetup.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.Meetup.Visibility;
import cat.udl.eps.softarch.demo.repository.InvitationRepository;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

public class InviteToMeetupStepDefs {
    private final StepDefs stepDefs;
    private final UserRepository userRepository;
    private final MeetupRepository meetupRepository;
    private final InvitationRepository invitationRepository;

    public InviteToMeetupStepDefs(StepDefs stepDefs, UserRepository userRepository,
        MeetupRepository meetupRepository, InvitationRepository invitationRepository) {
        this.stepDefs = stepDefs;
        this.userRepository = userRepository;
        this.meetupRepository = meetupRepository;
        this.invitationRepository = invitationRepository;
    }

    @Given("There is a meetup with title {string} organized by {string}")
    public void thereIsAMeetupWithTitleOrganizedBy(String title, String organizerUsername) {
        Meetup meetup = new Meetup();
        meetup.setTitle(title);
        meetup.setCapacity(10);
        meetup.setVisibility(Visibility.PRIVATE);
        meetup.setStatus(MeetupStatus.OPEN);
        meetup.setOrganizer(userRepository.findById(organizerUsername).orElseThrow());

        // MeetupRepository.save is guarded by @PreAuthorize, so an authenticated
        // SecurityContext is required when creating the meetup from the test thread
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(organizerUsername, null,
                        java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))));
        try {
            meetupRepository.save(meetup);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Given("{string} has already been invited to the meetup {string}")
    public void hasAlreadyBeenInvitedToTheMeetup(String inviteeUsername, String title) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        Invitation invitation = new Invitation();
        invitation.setInvitee(userRepository.findById(inviteeUsername).orElseThrow());
        invitation.setInvitedBy(meetup.getOrganizer());
        invitation.setMeetup(meetup);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setCreated(Instant.now());
        invitationRepository.save(invitation);
    }

    @When("I invite {string} to the meetup {string}")
    public void iInviteToTheMeetup(String inviteeUsername, String title) throws Throwable {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        JSONObject invitation = new JSONObject();
        invitation.put("invitee", "/users/" + inviteeUsername);
        invitation.put("meetup", meetup.getUri());

        stepDefs.result = stepDefs.mockMvc.perform(
                post("/invitations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invitation.toString())
                    .characterEncoding(StandardCharsets.UTF_8)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @And("The invitation of {string} to the meetup {string} has status {string}")
    public void theInvitationHasStatus(String inviteeUsername, String title, String status) {
        Invitation invitation = findInvitation(inviteeUsername, title);
        assertThat(invitation.getStatus().name(), is(status));
    }

    @And("The invitation of {string} to the meetup {string} was sent by {string}")
    public void theInvitationWasSentBy(String inviteeUsername, String title, String inviterUsername) {
        Invitation invitation = findInvitation(inviteeUsername, title);
        assertThat(invitation.getInvitedBy().getId(), is(inviterUsername));
    }

    @And("There is no invitation of {string} to the meetup {string}")
    public void thereIsNoInvitation(String inviteeUsername, String title) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        assertThat(invitationRepository.existsByInviteeIdAndMeetupId(inviteeUsername, meetup.getId()), is(false));
    }

    private Invitation findInvitation(String inviteeUsername, String title) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        return invitationRepository.findByInviteeIdAndMeetupId(inviteeUsername, meetup.getId()).orElseThrow();
    }
}
