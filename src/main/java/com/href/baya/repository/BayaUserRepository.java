package com.href.baya.repository;

import com.href.baya.model.BayaUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BayaUserRepository extends JpaRepository<BayaUser, Long> {
    BayaUser findByUsername(String username);
    BayaUser findByEmail(String email);
}
