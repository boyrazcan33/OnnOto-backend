package com.onnoto.onnoto_backend.service;


import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StationService {
    private final StationRepository stationRepository;

    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    public Optional<Station> getStationById(String id) {
        return stationRepository.findById(id);
    }

    public List<Station> getStationsByCity(String city) {
        return stationRepository.findByCity(city);
    }

    public List<Station> getNearbyStations(double longitude, double latitude, double radiusInMeters) {
        return stationRepository.findNearbyStations(longitude, latitude, radiusInMeters);
    }

    public Station saveStation(Station station) {
        return stationRepository.save(station);
    }
}