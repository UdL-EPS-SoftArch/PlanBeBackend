package cat.udl.eps.softarch.demo.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Location {
    private String name;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
}
