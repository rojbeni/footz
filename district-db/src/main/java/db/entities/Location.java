package db.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Location {
	@Id
	private Long id;
	private String address;

}
