package com.footz.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footz.entities.Invitation;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

}
