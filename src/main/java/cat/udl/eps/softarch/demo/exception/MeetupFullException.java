package cat.udl.eps.softarch.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class MeetupFullException extends RuntimeException {
    public MeetupFullException(String message) {
        super(message);
    }
}
