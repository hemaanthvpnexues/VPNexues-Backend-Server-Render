package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.OtpChallenge;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    Optional<OtpChallenge> findByChannelAndIdentifier(String channel, String identifier);
}
