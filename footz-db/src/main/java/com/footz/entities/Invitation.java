package com.footz.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
@Entity
public class Invitation {
    @Id
    private Long    id;
    @ManyToOne
    @JoinColumn(name = "match", nullable = false)
    private Match   match;
    @ManyToOne
    @JoinColumn(name = "team", nullable = false)
    private Team    team;
    private boolean status;
}
