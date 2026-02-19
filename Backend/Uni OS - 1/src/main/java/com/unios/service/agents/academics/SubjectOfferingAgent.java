package com.unios.service.agents.academics;

import com.unios.optimizer.domain.Room;
import com.unios.optimizer.domain.SubjectClass;
import com.unios.optimizer.service.AllocationSolverService;
import com.unios.model.Batch;
import com.unios.model.Faculty;
import com.unios.model.SubjectOffering;
import com.unios.repository.BatchRepository;
import com.unios.repository.FacultyRepository;
import com.unios.repository.SubjectOfferingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubjectOfferingAgent {

    private final SubjectOfferingRepository subjectOfferingRepository;
    private final BatchRepository batchRepository;
    private final FacultyRepository facultyRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public SubjectOfferingAgent(SubjectOfferingRepository subjectOfferingRepository,
            BatchRepository batchRepository,
            FacultyRepository facultyRepository,
            AllocationSolverService allocationSolverService,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.subjectOfferingRepository = subjectOfferingRepository;
        this.batchRepository = batchRepository;
        this.facultyRepository = facultyRepository;
        this.allocationSolverService = allocationSolverService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SubjectOffering createOffering(String subjectName, String slot, Integer capacity, Integer credits,
            String prerequisite, Long facultyId, Long batchId) {
        // Validate Batch
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        // Validate Faculty
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        // Validate Slot Uniqueness for Batch (Naive check, will be optimized later)
        // With optimization, we might allow creating multiple initially and then
        // solving.
        // But for now, keep this check or relax it?
        // Prompt: "Instead of manual assignment: Call AllocationSolverService"
        // If I keep this check, I can't overlapping slots even if rooms are different?
        // Actually, existing check `findByBatchIdAndSlot` enforces strict slot
        // constraint.
        // I will relax it or just leave it for manual creation.
        // User wants "Subject slots" allocated.
        // So maybe `createOffering` should NOT take a slot?
        // Converting `createOffering` to not require slot might break other things.
        // I will keep `createOffering` as is for "legacy/manual" but add
        // `optimizeSchedule` to re-assign.

        Optional<SubjectOffering> existing = subjectOfferingRepository.findByBatchIdAndSlot(batchId, slot);
        if (existing.isPresent()) {
            throw new RuntimeException("Slot " + slot + " is already occupied for this batch.");
        }

        SubjectOffering offering = new SubjectOffering();
        offering.setSubjectName(subjectName);
        offering.setSlot(slot);
        offering.setCapacity(capacity);
        offering.setCredits(credits);
        offering.setPrerequisite(prerequisite);
        offering.setFaculty(faculty);
        offering.setBatch(batch);
        offering.setActive(false); // Default inactive

        SubjectOffering saved = subjectOfferingRepository.save(offering);

        eventPublisher.publishEvent(new com.unios.domain.events.SubjectOfferedEvent(this, saved.getId(), batchId));

        return saved;
    }

    @Transactional
    public void optimizeSchedule(Long batchId) {
        List<SubjectOffering> offerings = subjectOfferingRepository.findByBatchId(batchId);

        // 1. Prepare Data
        List<Room> rooms = new ArrayList<>();
        rooms.add(new Room("R1", "Hall A", 60));
        rooms.add(new Room("R2", "Hall B", 60));
        rooms.add(new Room("R3", "Hall C", 60));
        rooms.add(new Room("R4", "Hall D", 60));
        rooms.add(new Room("R5", "Hall E", 60));

        List<String> slots = List.of("A", "B", "C", "D", "E", "F");

        List<SubjectClass> planningClasses = offerings.stream()
                .map(o -> new SubjectClass(o.getId(), o.getSubjectName(), o.getCapacity())) // Assuming Capacity =
                                                                                            // Enrolled for now? Or 0?
                // Using capacity as proxy for enrolled count for optimization (worst case)
                .collect(Collectors.toList());

        // 2. Solve
        List<SubjectClass> optimized = allocationSolverService.solveSubjectAllocation(rooms, planningClasses, slots);

        // 3. Persist
        for (SubjectClass cls : optimized) {
            SubjectOffering offering = subjectOfferingRepository.findById(cls.getSubjectOfferingId()).orElseThrow();
            if (cls.getRoom() != null) {
                offering.setRoom(cls.getRoom().getId());
            }
            if (cls.getSlot() != null) {
                offering.setSlot(cls.getSlot());
            }
            subjectOfferingRepository.save(offering);
        }
    }

    @Transactional
    public void activateOffering(Long offeringId) {
        SubjectOffering offering = subjectOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new RuntimeException("Offering not found"));
        offering.setActive(true);
        subjectOfferingRepository.save(offering);

        eventPublisher.publishEvent(
                new com.unios.domain.events.SubjectOfferedEvent(this, offeringId, offering.getBatch().getId()));
    }

}
