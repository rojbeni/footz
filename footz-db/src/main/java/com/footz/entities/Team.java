package com.footz.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
@Data
@Entity
public class Team {
	@Id
	private Long id;
	@Column(unique = true)
	private String name;

}
