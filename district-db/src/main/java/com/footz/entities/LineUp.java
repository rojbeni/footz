package com.footz.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class LineUp {
	@Id
	private Long id;
	@ManyToOne
	@JoinColumn(name = "match", nullable = false)
	private Match match;
	@ManyToOne
	@JoinColumn(name = "user", nullable = false)
	private User user;
	@ManyToOne
	@JoinColumn(name = "team", nullable = false)
	private Team team;

}
