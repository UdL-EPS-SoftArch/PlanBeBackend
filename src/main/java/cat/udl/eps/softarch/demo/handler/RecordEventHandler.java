package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Record;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.RecordRepository;
import org.springframework.data.rest.core.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
public class RecordEventHandler {
    final RecordRepository recordRepository;

    public RecordEventHandler(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    @HandleBeforeCreate
    public void handleRecordPreCreate(Record record) {
        User owner = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        record.setOwnedBy(owner);
        if (record.getStatus() == null) {
            record.setStatus(Record.Status.PRIVATE);
        }
    }

    @HandleBeforeSave
    public void handleRecordPreSave(Record record) {
        User owner = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!owner.getUsername().equals(record.getOwnedBy().getUsername())) {
            throw new SecurityException("Only the owner can modify the record");
        }
    }
}
