package com.opslog.checkin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingTierRepository extends JpaRepository<TrainingTier, Long> {

    List<TrainingTier> findAllByOrderByIntensityAsc();
}
