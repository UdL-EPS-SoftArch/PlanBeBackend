package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.ChatMessage;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.ChatMessageRepository;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONObject;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MeetupChatStepDefs {

    private final StepDefs stepDefs;
    private final UserRepository userRepository;
    private final MeetupRepository meetupRepository;
    private final ChatMessageRepository chatMessageRepository;

    public MeetupChatStepDefs(StepDefs stepDefs, UserRepository userRepository,
                              MeetupRepository meetupRepository, ChatMessageRepository chatMessageRepository) {
        this.stepDefs = stepDefs;
        this.userRepository = userRepository;
        this.meetupRepository = meetupRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Given("{string} has already sent the chat message {string} to the meetup {string}")
    public void hasAlreadySentTheChatMessage(String username, String text, String title) {
        ChatMessage message = new ChatMessage();
        message.setSender(userRepository.findById(username).orElseThrow());
        message.setMeetup(meetup(title));
        message.setText(text);
        chatMessageRepository.save(message);
    }

    @Given("the meetup {string} has chat messages sent by {string}:")
    public void theMeetupHasChatMessagesSentBy(String title, String username, List<Map<String, String>> rows) {
        User sender = userRepository.findById(username).orElseThrow();
        Meetup meetup = meetup(title);
        for (Map<String, String> row : rows) {
            ChatMessage message = new ChatMessage();
            message.setSender(sender);
            message.setMeetup(meetup);
            message.setText(row.get("text"));
            message.setSentAt(Instant.parse(row.get("sentAt")));
            chatMessageRepository.save(message);
        }
    }

    @When("I send the chat message {string} to the meetup {string}")
    public void iSendTheChatMessage(String text, String title) throws Throwable {
        JSONObject body = new JSONObject()
            .put("text", text)
            .put("meetup", meetup(title).getUri());

        stepDefs.result = stepDefs.mockMvc.perform(
                post("/chatMessages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString())
                    .characterEncoding(StandardCharsets.UTF_8)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @When("I retrieve the chat message {string} of the meetup {string}")
    public void iRetrieveTheChatMessage(String text, String title) throws Throwable {
        ChatMessage message = message(title, text);
        stepDefs.result = stepDefs.mockMvc.perform(
                get(message.getUri())
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @When("I list the chat messages of the meetup {string}")
    public void iListTheChatMessages(String title) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                get("/chatMessages/search/findByMeetupId?meetupId=" + meetup(title).getId() + "&size=100")
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @Then("The chat message {string} of the meetup {string} was sent by {string}")
    public void theChatMessageWasSentBy(String text, String title, String username) {
        assertThat(message(title, text).getSender().getId(), is(username));
    }

    @Then("The chat messages are ordered {string}, {string}, {string}")
    public void theChatMessagesAreOrdered(String first, String second, String third) throws Exception {
        stepDefs.result.andExpect(status().isOk());
        String json = stepDefs.result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> texts = JsonPath.read(json, "$._embedded.chatMessages[*].text");
        assertThat(texts, is(List.of(first, second, third)));
    }

    private Meetup meetup(String title) {
        return meetupRepository.findByTitle(title).orElseThrow();
    }

    private ChatMessage message(String title, String text) {
        return chatMessageRepository.findAllByMeetupId(meetup(title).getId()).stream()
            .filter(message -> message.getText().equals(text))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No chat message \"" + text + "\" in meetup \"" + title + "\""));
    }
}
