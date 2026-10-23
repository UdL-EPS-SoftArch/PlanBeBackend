package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Block;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Report;
import cat.udl.eps.softarch.demo.domain.ReportStatus;
import cat.udl.eps.softarch.demo.domain.Review;
import cat.udl.eps.softarch.demo.repository.BlockRepository;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.ReportRepository;
import cat.udl.eps.softarch.demo.repository.ReviewRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Moderation.feature: #48 Block users, #49 Report content, #50 Report management by ADMIN. */
public class ModerationStepDefs {
    private final StepDefs stepDefs;
    private final UserRepository userRepository;
    private final MeetupRepository meetupRepository;
    private final ReviewRepository reviewRepository;
    private final BlockRepository blockRepository;
    private final ReportRepository reportRepository;

    public ModerationStepDefs(StepDefs stepDefs, UserRepository userRepository, MeetupRepository meetupRepository,
                              ReviewRepository reviewRepository, BlockRepository blockRepository,
                              ReportRepository reportRepository) {
        this.stepDefs = stepDefs;
        this.userRepository = userRepository;
        this.meetupRepository = meetupRepository;
        this.reviewRepository = reviewRepository;
        this.blockRepository = blockRepository;
        this.reportRepository = reportRepository;
    }

    // ---------- #48 Block users ----------

    @Given("{string} has already blocked {string}")
    public void hasAlreadyBlocked(String blockerUsername, String blockedUsername) {
        Block block = new Block();
        block.setBlocker(userRepository.findById(blockerUsername).orElseThrow());
        block.setBlocked(userRepository.findById(blockedUsername).orElseThrow());
        block.setCreated(Instant.now());
        blockRepository.save(block);
    }

    @When("I block the user {string}")
    public void iBlockTheUser(String username) throws Throwable {
        perform(post("/blocks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new JSONObject().put("blocked", "/users/" + username).toString()));
    }

    @Then("{string} has blocked {string}")
    public void hasBlocked(String blockerUsername, String blockedUsername) {
        assertThat(blockRepository.existsByBlockerIdAndBlockedId(blockerUsername, blockedUsername), is(true));
    }

    // ---------- #49 Report content ----------

    @Given("{string} has reported the user {string} with reason {string}")
    public void hasReportedTheUser(String reporterUsername, String reportedUsername, String reason) {
        Report report = new Report();
        report.setReporter(userRepository.findById(reporterUsername).orElseThrow());
        report.setTargetUri("/users/" + reportedUsername);
        report.setReason(reason);
        report.setStatus(ReportStatus.OPEN);
        report.setCreated(Instant.now());
        reportRepository.save(report);
    }

    @When("I report the user {string} with reason {string}")
    public void iReportTheUser(String username, String reason) throws Throwable {
        report("/users/" + username, reason);
    }

    @When("I report the review of {string} on the meetup {string} with reason {string}")
    public void iReportTheReview(String reviewerUsername, String title, String reason) throws Throwable {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        Review review = reviewRepository.findByReviewerIdAndMeetupId(reviewerUsername, meetup.getId()).orElseThrow();
        report(review.getUri(), reason);
    }

    /** Reports the meetup with the absolute link the API returns in _links.self.href. */
    @When("I report the meetup {string} by its link with reason {string}")
    public void iReportTheMeetupByItsLink(String title, String reason) throws Throwable {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        report("http://localhost" + meetup.getUri(), reason);
    }

    @And("The report {string} is about the meetup {string}")
    public void theReportIsAboutTheMeetup(String reason, String title) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        assertThat(report(reason).getTargetUri(), is(meetup.getUri()));
    }

    @When("I list the reports")
    public void iListTheReports() throws Throwable {
        perform(get("/reports"));
    }

    @Then("The response contains {int} report(s)")
    public void theResponseContainsReports(int count) throws Throwable {
        assertThat(listedReasons(), hasSize(count));
    }

    @And("The response contains the report {string}")
    public void theResponseContainsTheReport(String reason) throws Throwable {
        assertThat(listedReasons(), hasItem(reason));
    }

    @And("The report {string} was made by {string} about the review of {string} on the meetup {string}")
    public void theReportWasMadeByAboutTheReview(String reason, String reporterUsername, String reviewerUsername,
                                                 String title) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        Review review = reviewRepository.findByReviewerIdAndMeetupId(reviewerUsername, meetup.getId()).orElseThrow();
        Report report = report(reason);
        assertThat(report.getReporter().getId(), is(reporterUsername));
        assertThat(report.getTargetUri(), is(review.getUri()));
    }

    @And("{string} has not reported anything")
    public void hasNotReportedAnything(String username) {
        assertThat(allReports().stream().filter(report -> report.getReporter().getId().equals(username)).toList(),
            is(empty()));
    }

    // ---------- #50 Report management by ADMIN ----------

    @When("I mark the report {string} as {string}")
    public void iMarkTheReportAs(String reason, String status) throws Throwable {
        perform(patch(report(reason).getUri())
            .contentType(MediaType.APPLICATION_JSON)
            .content(new JSONObject().put("status", status).toString()));
    }

    @Then("The report {string} has status {string}")
    public void theReportHasStatus(String reason, String status) {
        assertThat(report(reason).getStatus().name(), is(status));
    }

    // ---------- helpers ----------

    private void report(String targetUri, String reason) throws Exception {
        perform(post("/reports")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new JSONObject().put("targetUri", targetUri).put("reason", reason).toString()));
    }

    private void perform(MockHttpServletRequestBuilder request) throws Exception {
        stepDefs.result = stepDefs.mockMvc.perform(request
                .characterEncoding(StandardCharsets.UTF_8)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    /** The reason identifies the report inside a scenario (the database is cleaned before each one). */
    private Report report(String reason) {
        List<Report> reports = allReports().stream().filter(report -> report.getReason().equals(reason)).toList();
        assertThat("Exactly one report with reason \"" + reason + "\"", reports, hasSize(1));
        return reports.getFirst();
    }

    /** findAll filters by the caller (SpEL): read every report as an ADMIN, like the Given steps write data. */
    private List<Report> allReports() {
        return RunAs.admin(() -> reportRepository.findAll(Pageable.unpaged()).getContent());
    }

    private List<String> listedReasons() throws Exception {
        stepDefs.result.andExpect(status().isOk());
        String json = stepDefs.result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        try {
            return JsonPath.read(json, "$._embedded.reports[*].reason");
        } catch (PathNotFoundException e) {
            return List.of(); // Spring Data REST may omit _embedded for an empty page
        }
    }
}
