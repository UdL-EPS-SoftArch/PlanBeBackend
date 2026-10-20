package cat.udl.eps.softarch.demo.steps;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.function.Supplier;

/**
 * Given steps write test data straight through the repositories, outside any HTTP request. Doing it as an ADMIN
 * keeps them working when repositories add method security (the meetup branch #23 puts
 * {@code @PreAuthorize("@meetupSecurity.canSave(#entity)")} on {@code MeetupRepository.save}, which refuses an
 * empty security context). The previous context is always restored.
 */
public final class RunAs {

    private RunAs() {}

    public static <T> T admin(Supplier<T> action) {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken("test-setup", null, "ROLE_ADMIN"));
        SecurityContextHolder.setContext(context);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
