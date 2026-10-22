package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Group;
import cat.udl.eps.softarch.demo.domain.GroupRole;
import cat.udl.eps.softarch.demo.domain.Membership;
import cat.udl.eps.softarch.demo.domain.MembershipStatus;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.GroupRepository;
import cat.udl.eps.softarch.demo.repository.MembershipRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

public class GroupMembershipStepDefs {
    private final StepDefs stepDefs;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;

    public GroupMembershipStepDefs(StepDefs stepDefs, UserRepository userRepository, GroupRepository groupRepository,
        MembershipRepository membershipRepository) {
        this.stepDefs = stepDefs;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
    }

    @Given("There is a membership of {string} in the group with name {string} with role {string}")
    public void thereIsAMembershipOfInTheGroupWithNameWithRole(String memberUsername, String groupName, String role) {
        Group group = groupRepository.findByName(groupName).stream().findFirst().orElseThrow();
        User member = userRepository.findById(memberUsername).orElseThrow();

        // The owner of the group is the one adding the members.
        String ownerUsername = group.getOwnedBy() == null ? memberUsername : group.getOwnedBy().getId();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            ownerUsername, null, java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        try {
            Membership membership = membershipRepository.findByGroupIdAndMemberId(group.getId(), memberUsername)
                .orElseGet(Membership::new);
            membership.setGroup(group);
            membership.setMember(member);
            membership.setRole(GroupRole.valueOf(role));
            membership.setStatus(MembershipStatus.ACTIVE);
            membership.setJoinedAt(Instant.now());
            membershipRepository.save(membership);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @When("I change the role of the membership of {string} in the group with name {string} to {string}")
    public void iChangeTheRoleOfTheMembershipOfInTheGroupWithNameTo(String memberUsername, String groupName, String role)
        throws Throwable {
        Group group = groupRepository.findByName(groupName).stream().findFirst().orElseThrow();
        Membership membership = membershipRepository.findByGroupIdAndMemberId(group.getId(), memberUsername).orElseThrow();
        Map<String, Object> body = Map.of("role", role);

        stepDefs.result = stepDefs.mockMvc.perform(
                patch(membership.getUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(stepDefs.mapper.writeValueAsString(body))
                    .characterEncoding(StandardCharsets.UTF_8)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @When("I remove the membership of {string} in the group with name {string}")
    public void iRemoveTheMembershipOfInTheGroupWithName(String memberUsername, String groupName) throws Throwable {
        Group group = groupRepository.findByName(groupName).stream().findFirst().orElseThrow();
        Membership membership = membershipRepository.findByGroupIdAndMemberId(group.getId(), memberUsername).orElseThrow();

        // No Accept header: Spring Data REST answers 204 No Content to a DELETE without a body representation.
        stepDefs.result = stepDefs.mockMvc.perform(
                delete(membership.getUri())
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @And("The membership of {string} in the group with name {string} has role {string}")
    public void theMembershipOfInTheGroupWithNameHasRole(String memberUsername, String groupName, String role) {
        Group group = groupRepository.findByName(groupName).stream().findFirst().orElseThrow();
        Membership membership = membershipRepository.findByGroupIdAndMemberId(group.getId(), memberUsername).orElseThrow();
        assertThat(membership.getRole().name(), is(role));
    }

    @And("There is no membership of {string} in the group with name {string}")
    public void thereIsNoMembershipOfInTheGroupWithName(String memberUsername, String groupName) {
        Group group = groupRepository.findByName(groupName).stream().findFirst().orElseThrow();
        assertThat(membershipRepository.findByGroupIdAndMemberId(group.getId(), memberUsername).isPresent(), is(false));
    }
}
