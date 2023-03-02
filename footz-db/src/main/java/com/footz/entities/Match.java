package com.footz.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
@Entity
public class Match {
	@Id
	private Long id;
	@ManyToOne
	@JoinColumn(name = "location", nullable = false)
	private Location location;
	@ManyToOne
	@JoinColumn(name = "organizer", nullable = false)
	private User organizer;

}
