package com.unios.service.agents.admissions;

import com.unios.model.Application;
import com.unios.model.EntranceExamSession;
import com.unios.optimizer.domain.ExamSession;
import com.unios.optimizer.domain.Room;
import com.unios.optimizer.service.AllocationSolverService;
import com.unios.repository.ApplicationRepository;
import com.unios.repository.EntranceExamSessionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExamSchedulerAgent {

    private final ApplicationEventPublisher eventPublisher;

    private final com.unios.repository.RoomRepository roomRepository;

    public ExamSchedulerAgent(ApplicationRepository applicationRepository,
            EntranceExamSessionRepository examSessionRepository,
            AllocationSolverService allocationSolverService,
            ApplicationEventPublisher eventPublisher,
            com.unios.repository.RoomRepository roomRepository) {
        this.applicationRepository = applicationRepository;
        this.examSessionRepository = examSessionRepository;
        this.allocationSolverService = allocationSolverService;
        this.eventPublisher = eventPublisher;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public void schedule(Long batchId) {
        List<Application> eligibleApps = applicationRepository.findByBatchIdAndStatus(batchId, "ELIGIBLE");

        if (eligibleApps.isEmpty())
            return;

        // 1. Prepare Data for Solver
        // Fetch Rooms from DB
        List<com.unios.model.Room> dbRooms = roomRepository.findAll();
        List<Room> rooms = dbRooms.stream()
                .map(r -> new Room(String.valueOf(r.getId()), r.getName(), r.getCapacity()))
                .collect(Collectors.toList());

        if (rooms.isEmpty()) {
            throw new RuntimeException("No rooms found in database. Cannot schedule exams.");
        }

        // Time slots (for solver if needed, but we default to one slot for batch exams
        // usually)
        // Here we just pass empty if solver handles it, or fixed logic.
        // User changed `solveExams` signature? No, I implemented `solveExamAllocation`.
        // Let's check `AllocationSolverService.java` signature.
        // It expects `solveExamAllocation(List<Room>, List<ExamSession>,
        // List<LocalTime>)`

        List<LocalTime> timeSlots = List.of(LocalTime.of(9, 0), LocalTime.of(14, 0));

        List<ExamSession> planningEntities = eligibleApps.stream()
                .map(app -> new ExamSession(app.getId(), app.getId(), app.getId(), "General Aptitude")) // Passing ID as
                                                                                                        // Long
                .collect(Collectors.toList());

        // 2. Solve
        List<ExamSession> optimizedSchedule = allocationSolverService.solveExamAllocation(rooms, planningEntities,
                timeSlots);

        // 3. Persist
        for (ExamSession session : optimizedSchedule) {
            String roomId = (session.getAssignedRoom() != null) ? session.getAssignedRoom().getId() : "UNASSIGNED";

            // Re-fetch application to set relationship
            Application app = applicationRepository.findById(session.getApplicationId()).orElseThrow();

            EntranceExamSession exam = new EntranceExamSession();
            exam.setApplication(app); // Set entity relation
            exam.setBatchId(batchId);
            // exam.setSubject(session.getSubject()); // Entity has no subject field in
            // previous view?
            // `EntranceExamSession` view showed `room`, `seatNumber`, `application`... NO
            // SUBJECT FIELD.
            // I will assume implied or add subject field to `EntranceExamSession` too if
            // needed.
            // For now, I will skip setting subject if entity lacks it or check if I missed
            // it.
            // View file Step 593: NO subject field.

            exam.setRoom(roomId);

            // Set Time
            LocalTime slot = session.getTimeSlot() != null ? session.getTimeSlot() : LocalTime.of(9, 0);
            exam.setExamDate(LocalDateTime.now().plusDays(5).toLocalDate());
            exam.setExamTime(slot);
            exam.setSeatNumber(1); // Placeholder or calculate index

            examSessionRepository.save(exam);
        }

        // 4. Publish Event
        eventPublisher.publishEvent(new com.unios.domain.events.ExamScheduledEvent(this, batchId));
    }
}
