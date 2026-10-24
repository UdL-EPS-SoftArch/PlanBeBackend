package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import org.json.JSONObject;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

public class RecurringMeetupStepDefs {
    private final StepDefs stepDefs;
    private final MeetupRepository meetupRepository;

    public RecurringMeetupStepDefs(StepDefs stepDefs, MeetupRepository meetupRepository) {
        this.stepDefs = stepDefs;
        this.meetupRepository = meetupRepository;
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
