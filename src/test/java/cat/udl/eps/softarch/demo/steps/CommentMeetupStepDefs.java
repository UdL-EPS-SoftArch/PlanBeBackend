package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Comment;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.CommentRepository;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONObject;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CommentMeetupStepDefs {

    private final StepDefs stepDefs;
    private final UserRepository userRepository;
    private final MeetupRepository meetupRepository;
    private final CommentRepository commentRepository;

    public CommentMeetupStepDefs(StepDefs stepDefs, UserRepository userRepository,
                                 MeetupRepository meetupRepository, CommentRepository commentRepository) {
        this.stepDefs = stepDefs;
        this.userRepository = userRepository;
        this.meetupRepository = meetupRepository;
        this.commentRepository = commentRepository;
    }

    @Given("{string} has already commented {string} on the meetup {string}")
    public void hasAlreadyCommentedOnTheMeetup(String username, String text, String title) {
        User author = userRepository.findById(username).orElseThrow();
        Comment comment = new Comment();
        comment.setAuthor(author);
        comment.setMeetup(meetup(title));
        comment.setText(text);
        commentRepository.save(comment);
    }

    @When("I comment {string} on the meetup {string}")
    public void iCommentOnTheMeetup(String text, String title) throws Throwable {
        JSONObject body = new JSONObject()
            .put("text", text)
            .put("meetup", meetup(title).getUri());

        stepDefs.result = stepDefs.mockMvc.perform(
                post("/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString())
                    .characterEncoding(StandardCharsets.UTF_8)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @When("I list comments")
    public void iListComments() throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                get("/comments")
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @When("I delete the comment {string} on the meetup {string}")
    public void iDeleteTheCommentOnTheMeetup(String text, String title) throws Throwable {
        Comment comment = comment(title, text);
        stepDefs.result = stepDefs.mockMvc.perform(
                delete(comment.getUri())
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @Then("The comment {string} on the meetup {string} was written by {string}")
    public void theCommentWasWrittenBy(String text, String title, String username) {
        assertThat(comment(title, text).getAuthor().getId(), is(username));
    }

    @Then("The meetup {string} has {int} comments")
    public void theMeetupHasComments(String title, int count) {
        assertThat(commentRepository.findByMeetupId(meetup(title).getId()), hasSize(count));
    }

    @Then("The meetup {string} has {int} comment")
    public void theMeetupHasComment(String title, int count) {
        theMeetupHasComments(title, count);
    }

    @Then("The response contains {int} comments")
    public void theResponseContainsComments(int count) throws Exception {
        assertThat(listedTexts(), hasSize(count));
    }

    @Then("The response contains {int} comment")
    public void theResponseContainsComment(int count) throws Exception {
        theResponseContainsComments(count);
    }

    @Then("The response contains the comment {string}")
    public void theResponseContainsTheComment(String text) throws Exception {
        assertThat(String.join("\n", listedTexts()), containsString(text));
    }

    private Meetup meetup(String title) {
        return meetupRepository.findByTitle(title).orElseThrow();
    }

    private Comment comment(String title, String text) {
        return commentRepository.findByMeetupId(meetup(title).getId()).stream()
            .filter(comment -> comment.getText().equals(text))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No comment \"" + text + "\" on meetup \"" + title + "\""));
    }

    private List<String> listedTexts() throws Exception {
        stepDefs.result.andExpect(status().isOk());
        String json = stepDefs.result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        try {
            return JsonPath.read(json, "$._embedded.comments[*].text");
        } catch (PathNotFoundException e) {
            return List.of();
        }
    }
}
