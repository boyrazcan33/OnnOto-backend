package com.onnoto.onnoto_backend.repository;

import com.onnoto.onnoto_backend.model.AnonymousUser;
import com.onnoto.onnoto_backend.model.FavoriteStation;
import com.onnoto.onnoto_backend.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteStationRepository extends JpaRepository<FavoriteStation, Long> {
    List<FavoriteStation> findByUser(AnonymousUser user);

    Optional<FavoriteStation> findByUserAndStation(AnonymousUser user, Station station);

    void deleteByUserAndStation(AnonymousUser user, Station station);
}