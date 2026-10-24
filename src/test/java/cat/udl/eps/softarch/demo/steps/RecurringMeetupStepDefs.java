package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Meetup.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.Meetup.Visibility;
import cat.udl.eps.softarch.demo.domain.enums.Recurrence;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONObject;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

public class RecurringMeetupStepDefs {
    private final StepDefs stepDefs;
    private final MeetupRepository meetupRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;

    public RecurringMeetupStepDefs(StepDefs stepDefs, MeetupRepository meetupRepository,
        UserRepository userRepository, ParticipationRepository participationRepository) {
        this.stepDefs = stepDefs;
        this.meetupRepository = meetupRepository;
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
    }

    @Given("The meetup {string} organized by {string} starts in {int} days and repeats {string}")
    public void theMeetupStartsInDaysAndRepeats(String title, String organizer, int days, String recurrence) {
        saveRecurringMeetup(title, organizer, days, recurrence, null);
    }

    @Given("The meetup {string} organized by {string} starts in {int} days and repeats {string} until {int} days from now")
    public void theMeetupStartsInDaysAndRepeatsUntil(String title, String organizer, int days, String recurrence,
        int endInDays) {
        saveRecurringMeetup(title, organizer, days, recurrence, LocalDate.now(ZoneOffset.UTC).plusDays(endInDays));
    }

    @When("I finish the meetup {string}")
    public void iFinishTheMeetup(String title) throws Throwable {
        iSetTheStatusOfTheMeetupTo(title, "FINISHED");
    }

    @When("I set the status of the meetup {string} to {string}")
    public void iSetTheStatusOfTheMeetupTo(String title, String status) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                patch(firstOccurrence(title).getUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new JSONObject().put("status", status).toString())
                    .characterEncoding(StandardCharsets.UTF_8)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @When("I create the meetup {string} as the next occurrence of {string}")
    public void iCreateTheMeetupAsTheNextOccurrenceOf(String title, String previousTitle) throws Throwable {
        Instant startDate = Instant.now().plus(Duration.ofDays(9));
        JSONObject meetup = new JSONObject();
        meetup.put("title", title);
        meetup.put("capacity", 10);
        meetup.put("startDate", startDate.toString());
        meetup.put("endDate", startDate.plus(Duration.ofHours(2)).toString());
        meetup.put("previousOccurrence", firstOccurrence(previousTitle).getUri());

        stepDefs.result = stepDefs.mockMvc.perform(
                post("/meetups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(meetup.toString())
                    .characterEncoding(StandardCharsets.UTF_8)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @When("I set the previous occurrence of {string} to {string}")
    public void iSetThePreviousOccurrenceOf(String title, String previousTitle) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                patch(firstOccurrence(title).getUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new JSONObject().put("previousOccurrence", firstOccurrence(previousTitle).getUri()).toString())
                    .characterEncoding(StandardCharsets.UTF_8)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @Then("The meetup {string} has a next occurrence starting {int} days after it")
    public void theMeetupHasANextOccurrenceStartingDaysAfterIt(String title, int days) {
        Meetup previous = firstOccurrence(title);
        assertNextStartsAt(previous, previous.getStartDate().plus(Duration.ofDays(days)));
    }

    @Then("The meetup {string} has a next occurrence starting {int} month after it")
    public void theMeetupHasANextOccurrenceStartingMonthAfterIt(String title, int months) {
        Meetup previous = firstOccurrence(title);
        assertNextStartsAt(previous,
            previous.getStartDate().atZone(ZoneOffset.UTC).plusMonths(months).toInstant());
    }

    @Then("The meetup {string} has no next occurrence")
    public void theMeetupHasNoNextOccurrence(String title) {
        assertThat(meetupRepository.findByPreviousOccurrenceId(firstOccurrence(title).getId()), hasSize(0));
    }

    @Then("The meetup {string} has exactly one next occurrence")
    public void theMeetupHasExactlyOneNextOccurrence(String title) {
        assertThat(meetupRepository.findByPreviousOccurrenceId(firstOccurrence(title).getId()), hasSize(1));
    }

    @And("The next occurrence of {string} is an open copy without participants")
    public void theNextOccurrenceIsAnOpenCopyWithoutParticipants(String title) {
        Meetup previous = firstOccurrence(title);
        Meetup next = meetupRepository.findByPreviousOccurrenceId(previous.getId()).get(0);
        assertThat(next.getStatus(), is(MeetupStatus.OPEN));
        assertThat(next.getTitle(), is(previous.getTitle()));
        assertThat(next.getDescription(), is(previous.getDescription()));
        assertThat(next.getCapacity(), is(previous.getCapacity()));
        assertThat(next.getVisibility(), is(previous.getVisibility()));
        assertThat(next.getOrganizer().getId(), is(previous.getOrganizer().getId()));
        assertThat(next.getRecurrence(), is(previous.getRecurrence()));
        assertThat(next.getLocation().getCity(), is(previous.getLocation().getCity()));
        assertThat(participationRepository.findByMeetupIdAndStatusIn(previous.getId(),
            java.util.EnumSet.allOf(cat.udl.eps.softarch.demo.domain.ParticipationStatus.class)), hasSize(1));
        assertThat(participationRepository.findByMeetupIdAndStatusIn(next.getId(),
            java.util.EnumSet.allOf(cat.udl.eps.softarch.demo.domain.ParticipationStatus.class)), hasSize(0));
    }

    @When("I create the meetup {string} starting on {string} with recurrence {string}")
    public void iCreateTheMeetupWithRecurrence(String title, String start, String recurrence) throws Throwable {
        createMeetup(title, start, recurrence, null);
    }

    @When("I create the meetup {string} starting on {string} with recurrence {string} until {string}")
    public void iCreateTheMeetupWithRecurrenceUntil(String title, String start, String recurrence, String end)
        throws Throwable {
        createMeetup(title, start, recurrence, end);
    }

    @And("The meetup {string} has recurrence {string} until {string}")
    public void theMeetupHasRecurrenceUntil(String title, String recurrence, String end) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        assertThat(meetup.getRecurrence().name(), is(recurrence));
        assertThat(meetup.getRecurrenceEnd(), is(LocalDate.parse(end)));
    }

    @And("The meetup {string} has recurrence {string} and no end")
    public void theMeetupHasRecurrenceAndNoEnd(String title, String recurrence) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        assertThat(meetup.getRecurrence().name(), is(recurrence));
        assertThat(meetup.getRecurrenceEnd(), is(nullValue()));
    }

    private void saveRecurringMeetup(String title, String organizer, int days, String recurrence, LocalDate end) {
        // Truncated to seconds so the expected dates compare exactly after the database round trip
        Instant start = Instant.now().plus(Duration.ofDays(days)).truncatedTo(ChronoUnit.SECONDS);
        Meetup meetup = new Meetup();
        meetup.setTitle(title);
        meetup.setDescription("Every week at the club");
        meetup.setCapacity(10);
        meetup.setStartDate(start);
        meetup.setEndDate(start.plus(Duration.ofHours(2)));
        meetup.setLocation(new cat.udl.eps.softarch.demo.domain.Location("Club", "Street 1", "Lleida", null, null));
        meetup.setVisibility(Visibility.PUBLIC);
        meetup.setStatus(MeetupStatus.OPEN);
        meetup.setRecurrence(Recurrence.valueOf(recurrence));
        meetup.setRecurrenceEnd(end);
        meetup.setOrganizer(userRepository.findById(organizer).orElseThrow());
        RunAs.admin(() -> meetupRepository.save(meetup));
    }

    /** Next occurrences keep the title: the original is the one with no previous occurrence. */
    private Meetup firstOccurrence(String title) {
        return meetupRepository.findAll().stream()
            .filter(m -> m.getTitle().equals(title) && m.getPreviousOccurrence() == null)
            .findFirst().orElseThrow();
    }

    private void assertNextStartsAt(Meetup previous, Instant expectedStart) {
        List<Meetup> next = meetupRepository.findByPreviousOccurrenceId(previous.getId());
        assertThat(next, hasSize(1));
        assertThat(next.get(0).getStartDate(), is(expectedStart));
        assertThat(Duration.between(next.get(0).getStartDate(), next.get(0).getEndDate()),
            is(Duration.between(previous.getStartDate(), previous.getEndDate())));
    }

    private void createMeetup(String title, String start, String recurrence, String end) throws Throwable {
        Instant startDate = Instant.parse(start);
        JSONObject meetup = new JSONObject();
        meetup.put("title", title);
        meetup.put("capacity", 10);
        meetup.put("startDate", startDate.toString());
        meetup.put("endDate", startDate.plus(Duration.ofHours(2)).toString());
        // An empty recurrence leaves the field out of the request, to check the default
        if (!recurrence.isEmpty()) meetup.put("recurrence", recurrence);
        if (end != null) meetup.put("recurrenceEnd", end);

        stepDefs.result = stepDefs.mockMvc.perform(
                post("/meetups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(meetup.toString())
                    .characterEncoding(StandardCharsets.UTF_8)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }
}
