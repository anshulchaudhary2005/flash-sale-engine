package com.systemdesign.flashsale.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {
    //void saveAll(List<ReservationEntity> entitiesToSave);
}