package cat.udl.eps.softarch.demo.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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
    
    @DecimalMax("90.0")
    @DecimalMin("-90.0")
    private Double latitude;
    
    @DecimalMax("180.0")
    @DecimalMin("-180.0")
    private Double longitude;
}
