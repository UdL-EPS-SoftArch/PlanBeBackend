package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Invitation;
import cat.udl.eps.softarch.demo.domain.InvitationStatus;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.ParticipationStatus;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.domain.Meetup.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.Meetup.Visibility;
import cat.udl.eps.softarch.demo.repository.InvitationRepository;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

public class InviteToMeetupStepDefs {
    private final StepDefs stepDefs;
    private final UserRepository userRepository;
    private final MeetupRepository meetupRepository;
    private final InvitationRepository invitationRepository;
    private final ParticipationRepository participationRepository;

    public InviteToMeetupStepDefs(StepDefs stepDefs, UserRepository userRepository,
        MeetupRepository meetupRepository, InvitationRepository invitationRepository,
        ParticipationRepository participationRepository) {
        this.stepDefs = stepDefs;
        this.userRepository = userRepository;
        this.meetupRepository = meetupRepository;
        this.invitationRepository = invitationRepository;
        this.participationRepository = participationRepository;
    }

    @Given("There is a meetup with title {string} organized by {string}")
    public void thereIsAMeetupWithTitleOrganizedBy(String title, String organizerUsername) {
        thereIsAMeetupWithTitleOrganizedByWithCapacity(title, organizerUsername, 10);
    }

    @Given("There is a meetup with title {string} organized by {string} with capacity {int}")
    public void thereIsAMeetupWithTitleOrganizedByWithCapacity(String title, String organizerUsername, int capacity) {
        Meetup meetup = new Meetup();
        meetup.setTitle(title);
        meetup.setCapacity(capacity);
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

    @Given("The meetup {string} has status {string}")
    public void theMeetupHasStatus(String title, String status) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        meetup.setStatus(MeetupStatus.valueOf(status));
        RunAs.admin(() -> meetupRepository.save(meetup));
    }

    @Given("{string} is a confirmed participant of the meetup {string}")
    public void isAConfirmedParticipantOfTheMeetup(String username, String title) {
        Participation participation = new Participation();
        participation.setParticipant(userRepository.findById(username).orElseThrow());
        participation.setMeetup(meetupRepository.findByTitle(title).orElseThrow());
        participation.setStatus(ParticipationStatus.CONFIRMED);
        participation.setJoinedAt(Instant.now());
        participationRepository.save(participation);
    }

    @Given("The invitation of {string} to the meetup {string} was already answered with {string}")
    public void theInvitationWasAlreadyAnsweredWith(String inviteeUsername, String title, String status) {
        Invitation invitation = findInvitation(inviteeUsername, title);
        invitation.setStatus(InvitationStatus.valueOf(status));
        invitationRepository.save(invitation);
    }

    @When("I answer the invitation of {string} to the meetup {string} with {string}")
    public void iAnswerTheInvitationWith(String inviteeUsername, String title, String status) throws Throwable {
        JSONObject answer = new JSONObject();
        answer.put("status", status);
        patchInvitation(findInvitation(inviteeUsername, title), answer);
    }

    @When("I move the invitation of {string} to the meetup {string} to the meetup {string}")
    public void iMoveTheInvitationToTheMeetup(String inviteeUsername, String title, String newTitle) throws Throwable {
        JSONObject change = new JSONObject();
        change.put("meetup", meetupRepository.findByTitle(newTitle).orElseThrow().getUri());
        patchInvitation(findInvitation(inviteeUsername, title), change);
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

    @And("{string} has a participation in the meetup {string} with status {string}")
    public void hasAParticipationInTheMeetupWithStatus(String username, String title, String status) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        Participation participation = participationRepository
            .findByParticipantIdAndMeetupId(username, meetup.getId()).orElseThrow();
        assertThat(participation.getStatus().name(), is(status));
    }

    @And("{string} has no participation in the meetup {string}")
    public void hasNoParticipationInTheMeetup(String username, String title) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        assertThat(participationRepository.findByParticipantIdAndMeetupId(username, meetup.getId()).isPresent(), is(false));
    }

    @When("I list the meetups visible to me")
    public void iListTheMeetupsVisibleToMe() throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                get("/meetups/search/findVisibleMeetups")
                    .accept(MediaType.APPLICATION_JSON)
                    .with(user(stepDefs.currentUser).roles("USER")))
            .andDo(print());
    }

    private void patchInvitation(Invitation invitation, JSONObject body) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                patch(invitation.getUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString())
                    .characterEncoding(StandardCharsets.UTF_8)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    private Invitation findInvitation(String inviteeUsername, String title) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        return invitationRepository.findByInviteeIdAndMeetupId(inviteeUsername, meetup.getId()).orElseThrow();
    }
}
