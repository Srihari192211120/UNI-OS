package com.unios.config;

import com.unios.model.Faculty;
import com.unios.model.Role;
import com.unios.model.Student;
import com.unios.model.User;
import com.unios.repository.FacultyRepository;
import com.unios.repository.StudentRepository;
import com.unios.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository,
            StudentRepository studentRepository,
            FacultyRepository facultyRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // Admin
            if (!userRepository.existsByEmail("admin@unios.com")) {
                User admin = new User();
                admin.setEmail("admin@unios.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
            }

            // Faculty
            if (!userRepository.existsByEmail("faculty@unios.com")) {
                User facultyUser = new User();
                facultyUser.setEmail("faculty@unios.com");
                facultyUser.setPassword(passwordEncoder.encode("faculty123"));
                facultyUser.setRole(Role.FACULTY);
                // Save user first via cascade or explicit save?
                // Since Faculty has OneToOne with User and CascadeType.ALL, we can save
                // Faculty.
                // But Faculty needs the user reference.

                Faculty faculty = new Faculty();
                faculty.setUser(facultyUser);
                faculty.setDepartment("Computer Science");
                facultyRepository.save(faculty);
            }

            // Student
            if (!userRepository.existsByEmail("student@unios.com")) {
                User studentUser = new User();
                studentUser.setEmail("student@unios.com");
                studentUser.setPassword(passwordEncoder.encode("student123"));
                studentUser.setRole(Role.STUDENT);

                Student student = new Student();
                student.setUser(studentUser);
                // batch is nullable in Student entity as per my previous tool call (I didn't
                // add @NotNull on batch, just @ManyToOne)
                studentRepository.save(student);
            }
        };
    }
}
