package com.onnoto.onnoto_backend.repository;

import com.onnoto.onnoto_backend.model.AnonymousUser;
import com.onnoto.onnoto_backend.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    List<UserPreference> findByUser(AnonymousUser user);

    Optional<UserPreference> findByUserAndPreferenceKey(AnonymousUser user, String preferenceKey);
}