package cat.udl.eps.softarch.demo.domain;

import cat.udl.eps.softarch.demo.domain.Entity.UriEntity;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "DemoUser") //Avoid collision with system table User
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends UriEntity<String> implements UserDetails {

	public static PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public static final int MINIMUM_AGE = 18;

	@Id
	private String id;

	@NotBlank
	@Email
	@Column(unique = true)
	private String email;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@NotBlank
	@Length(min = 8, max = 256)
	private String password;

	@Transient
	@JsonIgnore
	private boolean passwordReset;

	@JsonIgnore
	private boolean admin;

	@Length(max = 50)
	private String displayName;

	@Length(max = 500)
	private String bio;

	@URL
	@Length(max = 2048)
	private String avatarUrl;

	@Length(max = 100)
	private String city;

	// Personal data: written through the API, but only serialized by getPersonalData() when visible
	@Past
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private LocalDate birthDate;

	@Enumerated(EnumType.STRING)
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Gender gender;

	private boolean showPersonalData = false;

	@ManyToMany
	@JsonIdentityReference(alwaysAsId = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Set<Interest> interests = new HashSet<>();

	public void setPassword(String password) {
		this.password = password;
		this.passwordReset = true;
	}

	public void encodePassword() {
		this.password = passwordEncoder.encode(this.password);
		this.passwordReset = false;
	}

	@JsonIgnore
	public Integer getAge() {
		return birthDate == null ? null : Period.between(birthDate, LocalDate.now()).getYears();
	}

	@JsonIgnore
	@AssertTrue(message = "must be at least " + MINIMUM_AGE + " years old")
	public boolean isOldEnough() {
		return birthDate == null || getAge() >= MINIMUM_AGE;
	}

	@JsonAnyGetter
	public Map<String, Object> getPersonalData() {
		Map<String, Object> personalData = new LinkedHashMap<>();
		if (isPersonalDataVisible()) {
			if (birthDate != null) {
				personalData.put("birthDate", birthDate.toString());
				personalData.put("age", getAge());
			}
			if (gender != null) {
				personalData.put("gender", gender);
			}
		}
		return personalData;
	}

	private boolean isPersonalDataVisible() {
		if (showPersonalData) return true;
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && (authentication.getName().equals(id) ||
			authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
	}

	@Override
	public String getUsername() { return id; }

	public void setUsername(String username) { this.id = username; }

	@Override
	@JsonValue(value = false)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return AuthorityUtils.commaSeparatedStringToAuthorityList(admin ? "ROLE_USER,ROLE_ADMIN" : "ROLE_USER");
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
