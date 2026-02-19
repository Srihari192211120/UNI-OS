package com.unios.service.agents.admissions;

import com.unios.model.*;
import com.unios.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EnrollmentAgent {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public EnrollmentAgent(ApplicationRepository applicationRepository,
            UserRepository userRepository,
            StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository,
            PasswordEncoder passwordEncoder,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void enroll(Long batchId) {
        List<Application> selectedApps = applicationRepository.findByBatchIdAndStatus(batchId, "SELECTED");

        for (Application app : selectedApps) {
            // 1. Create User
            String rawPassword = java.util.UUID.randomUUID().toString().substring(0, 8); // Random 8-char password

            User user = new User();
            user.setEmail(app.getEmail());
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole(Role.STUDENT);
            userRepository.save(user);

            // Log for prototype/demo visibility (Never do this in prod!)
            System.out.println("[SECURITY] Created User: " + app.getEmail() + " | Password: " + rawPassword);

            // 2. Create Student
            Student student = new Student();
            student.setUser(user);
            student.setBatch(app.getBatch());
            studentRepository.save(student);

            // 3. Create Enrollment
            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setBatch(app.getBatch());
            enrollment.setEnrolledAt(LocalDateTime.now());
            enrollmentRepository.save(enrollment);

            // 4. Update Application Status
            app.setStatus("ENROLLED");
            applicationRepository.save(app);
        }

        // Publish Event
        if (!selectedApps.isEmpty()) {
            eventPublisher.publishEvent(new com.unios.domain.events.EnrollmentCompletedEvent(this, batchId));
        }
    }
}
