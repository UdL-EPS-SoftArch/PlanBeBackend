package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Invitation;
import cat.udl.eps.softarch.demo.domain.Location;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Notification;
import cat.udl.eps.softarch.demo.domain.enums.NotificationType;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.ParticipationStatus;
import cat.udl.eps.softarch.demo.repository.InvitationRepository;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.NotificationRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

public class NotificationStepDefs {
    private final StepDefs stepDefs;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final MeetupRepository meetupRepository;
    private final ParticipationRepository participationRepository;
    private final InvitationRepository invitationRepository;

    public NotificationStepDefs(StepDefs stepDefs, UserRepository userRepository,
                                NotificationRepository notificationRepository, MeetupRepository meetupRepository,
                                ParticipationRepository participationRepository,
                                InvitationRepository invitationRepository) {
        this.stepDefs = stepDefs;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.meetupRepository = meetupRepository;
        this.participationRepository = participationRepository;
        this.invitationRepository = invitationRepository;
    }

    // ---------- Given ----------

    @Given("User {string} has an unread {string} notification {string}")
    public void userHasAnUnreadNotification(String username, String type, String message) {
        Notification notification = new Notification();
        notification.setRecipient(userRepository.findById(username).orElseThrow());
        notification.setType(NotificationType.valueOf(type));
        notification.setMessage(message);
        notification.setTargetUri("/users/" + username);
        notification.setCreated(Instant.now());
        notificationRepository.save(notification);
    }

    /** A PUBLIC, OPEN meetup with no participants; adding participants below keeps OPEN/FULL consistent. */
    @Given("There is a public meetup with title {string} organized by {string} and capacity {int}")
    public void thereIsAPublicMeetupWithCapacity(String title, String organizerUsername, int capacity) {
        Meetup meetup = new Meetup();
        meetup.setTitle(title);
        meetup.setCapacity(capacity);
        // Valid dates: PATCH /meetups validates the whole meetup (dates required, end after start), and
        // "Change date" moves the start to 2030-05-01.
        meetup.setStartDate(Instant.now().plus(java.time.Duration.ofDays(1)));
        meetup.setEndDate(Instant.parse("2031-01-01T00:00:00Z"));
        meetup.setVisibility(Meetup.Visibility.PUBLIC);
        meetup.setStatus(Meetup.MeetupStatus.OPEN);
        meetup.setOrganizer(userRepository.findById(organizerUsername).orElseThrow());
        RunAs.admin(() -> meetupRepository.save(meetup));
    }

    /**
     * Stores the participation directly and then sets the meetup to OPEN or FULL from its confirmed
     * participants, as the API would. It does not promote anybody: a WAITING participant next to a free spot
     * is the state just before WaitingList runs, which some scenarios need on purpose.
     */
    @Given("{string} participates in the meetup {string} with status {string}")
    public void participatesInTheMeetupWithStatus(String username, String title, String status) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        Participation participation = new Participation();
        participation.setParticipant(userRepository.findById(username).orElseThrow());
        participation.setMeetup(meetup);
        participation.setStatus(ParticipationStatus.valueOf(status));
        participation.setJoinedAt(Instant.now());
        participationRepository.save(participation);
        if (meetup.getStatus() == Meetup.MeetupStatus.OPEN || meetup.getStatus() == Meetup.MeetupStatus.FULL) {
            long confirmed = participationRepository.countByMeetupIdAndStatus(meetup.getId(), ParticipationStatus.CONFIRMED);
            meetup.setStatus(confirmed >= meetup.getCapacity() ? Meetup.MeetupStatus.FULL : Meetup.MeetupStatus.OPEN);
            RunAs.admin(() -> meetupRepository.save(meetup));
        }
    }

    // ---------- When: /notifications ----------

    @When("I list my notifications")
    public void iListMyNotifications() throws Throwable {
        perform(get("/notifications"));
    }

    @When("I list my unread notifications")
    public void iListMyUnreadNotifications() throws Throwable {
        perform(get("/notifications/search/findUnread"));
    }

    @When("I retrieve the notification {string} of {string}")
    public void iRetrieveTheNotification(String message, String username) throws Throwable {
        perform(get("/notifications/{id}", notification(message, username).getId()));
    }

    @When("I mark the notification {string} of {string} as read")
    public void iMarkTheNotificationAsRead(String message, String username) throws Throwable {
        perform(patch("/notifications/{id}", notification(message, username).getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(new JSONObject().put("read", true).toString()));
    }

    @When("I change the message of the notification {string} of {string} to {string}")
    public void iChangeTheMessageOfTheNotification(String message, String username, String newMessage) throws Throwable {
        perform(patch("/notifications/{id}", notification(message, username).getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(new JSONObject().put("message", newMessage).put("read", true).toString()));
    }

    @When("I create a notification for {string} with message {string}")
    public void iCreateANotificationFor(String username, String message) throws Throwable {
        perform(post("/notifications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new JSONObject()
                .put("recipient", "/users/" + username)
                .put("type", "INVITATION")
                .put("message", message)
                .put("targetUri", "/users/" + username).toString()));
    }

    // ---------- When: events that generate notifications ----------

    @When("I cancel the meetup {string}")
    public void iCancelTheMeetup(String title) throws Throwable {
        patchMeetup(title, new JSONObject().put("status", "CANCELLED"));
    }

    @When("I change the start date of the meetup {string} to {string}")
    public void iChangeTheStartDateOfTheMeetup(String title, String startDate) throws Throwable {
        patchMeetup(title, new JSONObject().put("startDate", Instant.parse(startDate).toString()));
    }

    @When("I change the city of the meetup {string} to {string}")
    public void iChangeTheCityOfTheMeetup(String title, String city) throws Throwable {
        patchMeetup(title, new JSONObject().put("location", new JSONObject().put("city", city)));
    }

    @When("I change the description of the meetup {string} to {string}")
    public void iChangeTheDescriptionOfTheMeetup(String title, String description) throws Throwable {
        patchMeetup(title, new JSONObject().put("description", description));
    }

    @When("I change the capacity of the meetup {string} to {int}")
    public void iChangeTheCapacityOfTheMeetup(String title, int capacity) throws Throwable {
        patchMeetup(title, new JSONObject().put("capacity", capacity));
    }

    @When("I change the status of the meetup {string} to {string}")
    public void iChangeTheStatusOfTheMeetup(String title, String status) throws Throwable {
        patchMeetup(title, new JSONObject().put("status", status));
    }

    @Given("The meetup {string} is in status {string}")
    public void theMeetupIsInStatus(String title, String status) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        meetup.setStatus(Meetup.MeetupStatus.valueOf(status));
        RunAs.admin(() -> meetupRepository.save(meetup));
    }

    @When("{int} users join the meetup {string} at the same time")
    public void usersJoinTheMeetupAtTheSameTime(int count, String title) throws Throwable {
        List<String> usernames = java.util.stream.IntStream.range(0, count).mapToObj(i -> "racer" + i).toList();
        List<Integer> codes = joinAtTheSameTime(usernames, title);
        assertThat(codes, everyItem(is(201)));
    }

    @When("{string} joins the meetup {string} {int} times at the same time")
    public void joinsTheMeetupTimesAtTheSameTime(String username, String title, int times) throws Throwable {
        concurrentJoinCodes = joinAtTheSameTime(java.util.Collections.nCopies(times, username), title);
    }

    @Then("Exactly {int} of those requests is answered {int} and the rest {int}")
    public void exactlyOfThoseRequestsIsAnswered(int expected, int code, int otherCode) {
        assertThat(concurrentJoinCodes.stream().filter(c -> c == code).count(), is((long) expected));
        assertThat(concurrentJoinCodes.stream().filter(c -> c != code).toList(), everyItem(is(otherCode)));
    }

    private List<Integer> concurrentJoinCodes = List.of();

    /** Sends one POST /participations per username, all released at the same instant; returns the codes. */
    private List<Integer> joinAtTheSameTime(List<String> usernames, String title) throws Exception {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        for (String username : new java.util.LinkedHashSet<>(usernames)) {
            if (!userRepository.existsById(username)) {
                cat.udl.eps.softarch.demo.domain.User user = new cat.udl.eps.softarch.demo.domain.User();
                user.setId(username);
                user.setEmail(username + "@sample.app");
                user.setPassword("password");
                user.encodePassword();
                userRepository.save(user);
            }
        }
        String body = new JSONObject().put("meetup", meetup.getUri()).toString();
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(usernames.size());
        try {
            List<java.util.concurrent.Future<Integer>> futures = usernames.stream().map(username -> pool.submit(() -> {
                start.await();
                return stepDefs.mockMvc.perform(post("/participations")
                        .with(org.springframework.security.test.web.servlet.request
                            .SecurityMockMvcRequestPostProcessors.user(username))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andReturn().getResponse().getStatus();
            })).toList();
            start.countDown();
            List<Integer> codes = new java.util.ArrayList<>();
            for (java.util.concurrent.Future<Integer> future : futures) {
                codes.add(future.get(60, java.util.concurrent.TimeUnit.SECONDS));
            }
            return codes;
        } finally {
            pool.shutdownNow();
        }
    }

    @Then("The meetup {string} has {int} confirmed and {int} waiting participants")
    public void theMeetupHasConfirmedAndWaiting(String title, int confirmed, int waiting) {
        Long meetupId = meetupRepository.findByTitle(title).orElseThrow().getId();
        assertThat(participationRepository.countByMeetupIdAndStatus(meetupId, ParticipationStatus.CONFIRMED),
            is((long) confirmed));
        assertThat(participationRepository.countByMeetupIdAndStatus(meetupId, ParticipationStatus.WAITING),
            is((long) waiting));
    }

    @When("I set my participation in the meetup {string} to status {string}")
    public void iSetMyParticipationToStatus(String title, String status) throws Throwable {
        Participation participation = participation(AuthenticationStepDefs.currentUsername, title);
        perform(patch("/participations/{id}", participation.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(new JSONObject().put("status", status).toString()));
    }

    @When("I set the participation of {string} in the meetup {string} to status {string}")
    public void iSetTheParticipationOfToStatus(String username, String title, String status) throws Throwable {
        Participation participation = participation(username, title);
        perform(patch("/participations/{id}", participation.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(new JSONObject().put("status", status).toString()));
    }

    // ---------- Then ----------

    @Then("The response contains {int} notification(s)")
    public void theResponseContainsNotifications(int count) throws Throwable {
        assertThat(listedMessages(), hasSize(count));
    }

    @Then("The response contains {int} {string} notification(s)")
    public void theResponseContainsNotificationsOfType(int count, String type) throws Throwable {
        List<String> types = listed("type");
        assertThat(types, hasSize(count));
        assertThat(types, everyItem(is(NotificationType.valueOf(type).name())));
    }

    @And("The response contains the notification {string}")
    public void theResponseContainsTheNotification(String message) throws Throwable {
        assertThat(listedMessages().contains(message), is(true));
    }

    @Then("The notification {string} of {string} is read")
    public void theNotificationIsRead(String message, String username) {
        assertThat(notification(message, username).isRead(), is(true));
    }

    @Then("The notification {string} of {string} is unread")
    public void theNotificationIsUnread(String message, String username) {
        assertThat(notification(message, username).isRead(), is(false));
    }

    @Then("User {string} has {int} unread {string} notification(s)")
    public void userHasUnreadNotifications(String username, int count, String type) {
        List<Notification> notifications =
            notificationRepository.findByRecipientIdAndType(username, NotificationType.valueOf(type));
        assertThat(notifications, hasSize(count));
        assertThat(notifications.stream().map(Notification::isRead).toList(), everyItem(is(false)));
    }

    @Then("User {string} has no notifications")
    public void userHasNoNotifications(String username) {
        assertThat(notificationRepository.findByRecipientId(username), hasSize(0));
    }

    @And("The {string} notification of {string} points to the meetup {string}")
    public void theNotificationPointsToTheMeetup(String type, String username, String title) {
        String meetupUri = meetupRepository.findByTitle(title).orElseThrow().getUri();
        assertTargets(username, type, meetupUri);
    }

    @And("The {string} notification of {string} points to their invitation to the meetup {string}")
    public void theNotificationPointsToTheInvitation(String type, String username, String title) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        Invitation invitation = invitationRepository.findByInviteeIdAndMeetupId(username, meetup.getId()).orElseThrow();
        assertTargets(username, type, invitation.getUri());
    }

    @Then("The participation of {string} in the meetup {string} has status {string}")
    public void theParticipationHasStatus(String username, String title, String status) {
        assertThat(participation(username, title).getStatus().name(), is(status));
    }

    @Then("The meetup {string} has status {string} and capacity {int}")
    public void theMeetupHasStatusAndCapacity(String title, String status, int capacity) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        assertThat(meetup.getStatus().name(), is(status));
        assertThat(meetup.getCapacity(), is(capacity));
    }

    @Then("The meetup {string} is located in {string}")
    public void theMeetupIsLocatedIn(String title, String city) {
        Location location = meetupRepository.findByTitle(title).orElseThrow().getLocation();
        assertThat(location == null ? null : location.getCity(), is(city));
    }

    // ---------- helpers ----------

    private void perform(MockHttpServletRequestBuilder request) throws Exception {
        stepDefs.result = stepDefs.mockMvc.perform(request
                .characterEncoding(StandardCharsets.UTF_8)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    private void patchMeetup(String title, JSONObject body) throws Exception {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        perform(patch(meetup.getUri()).contentType(MediaType.APPLICATION_JSON).content(body.toString()));
    }

    private Notification notification(String message, String username) {
        return notificationRepository.findByRecipientId(username).stream()
            .filter(n -> n.getMessage().equals(message))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No notification \"" + message + "\" for " + username));
    }

    private Participation participation(String username, String title) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        return participationRepository.findByParticipantIdAndMeetupId(username, meetup.getId()).orElseThrow();
    }

    private void assertTargets(String username, String type, String uri) {
        List<String> targets = notificationRepository
            .findByRecipientIdAndType(username, NotificationType.valueOf(type)).stream()
            .map(Notification::getTargetUri).toList();
        assertThat(targets, containsInAnyOrder(uri));
    }

    private List<String> listedMessages() throws Exception {
        return listed("message");
    }

    private List<String> listed(String field) throws Exception {
        stepDefs.result.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        String json = stepDefs.result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        try {
            return JsonPath.read(json, "$._embedded.notifications[*]." + field);
        } catch (PathNotFoundException e) {
            return List.of(); // Spring Data REST may omit _embedded for an empty page
        }
    }
}
