package com.unios.repository;

import com.unios.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByBatchId(Long batchId);

    List<Application> findByBatchIdAndStatus(Long batchId, String status);
}
