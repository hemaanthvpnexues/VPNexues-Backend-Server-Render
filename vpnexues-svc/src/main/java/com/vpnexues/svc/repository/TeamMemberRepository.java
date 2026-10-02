package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.TeamMember;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    List<TeamMember> findAllByOrderByDisplayOrderAscCreatedAtAsc();

    List<TeamMember> findByActiveTrueOrderByDisplayOrderAscCreatedAtAsc();

    boolean existsBySlug(String slug);
}
