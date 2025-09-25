package edu.ccrm.io;

import edu.ccrm.domain.*;
import edu.ccrm.service.CourseService;
import edu.ccrm.service.EnrollmentService;
import edu.ccrm.service.StudentService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter; // Import the formatter class
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportExportService {
    private static final Path DATA_DIRECTORY = Paths.get("data");
    
    // Define a formatter that matches your CSV's date format (e.g., 20-07-2006)
    private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // --- Student Methods ---
    public void exportStudents(List<Student> students) {
        try {
            Files.createDirectories(DATA_DIRECTORY);
            Path filePath = DATA_DIRECTORY.resolve("students.csv");
            List<String> lines = students.stream()
                .map(s -> String.join(",", 
                    s.getFullName(), 
                    s.getEmail(), 
                    // Use the formatter to write dates in the correct format
                    s.getDateOfBirth().format(CSV_DATE_FORMATTER),
                    s.getRegNo()))
                .collect(Collectors.toList());
            Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to export students: " + e.getMessage());
        }
    }

    public void importStudents(StudentService studentService) {
        Path filePath = DATA_DIRECTORY.resolve("students.csv");
        if (!Files.exists(filePath)) {
            return;
        }
        try (Stream<String> lines = Files.lines(filePath)) {
            lines.map(line -> {
                String[] parts = line.split(",");
                if (parts.length < 4) {
                    System.err.println("Skipping malformed student line: " + line);
                    return null;
                }
                try {
                    // Use the formatter to parse the date from the CSV
                    LocalDate dob = LocalDate.parse(parts[2], CSV_DATE_FORMATTER);
                    return new Student(parts[0], parts[1], dob, parts[3]);
                } catch (DateTimeParseException e) {
                    System.err.println("Skipping student line due to invalid date format: " + line);
                    return null;
                }
            })
            .filter(java.util.Objects::nonNull)
            .forEach(studentService::addStudent);
        } catch (IOException e) {
            System.err.println("Failed to import students: " + e.getMessage());
        }
    }

    // --- Course Methods ---
    public void exportCourses(List<Course> courses) {
        try {
            Files.createDirectories(DATA_DIRECTORY);
            Path filePath = DATA_DIRECTORY.resolve("courses.csv");
            List<String> lines = courses.stream()
                .map(c -> String.join(",",
                    c.getCode(),
                    c.getTitle(),
                    String.valueOf(c.getCredits()),
                    c.getDepartment(),
                    c.getSemester().name()))
                .collect(Collectors.toList());
            Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to export courses: " + e.getMessage());
        }
    }

    public void importCourses(CourseService courseService) {
        Path filePath = DATA_DIRECTORY.resolve("courses.csv");
        if (!Files.exists(filePath)) return;
        
        try (Stream<String> lines = Files.lines(filePath)) {
            lines.map(line -> {
                String[] parts = line.split(",");
                if (parts.length < 5) {
                    System.err.println("Skipping malformed course line: " + line);
                    return null;
                }
                try {
                    return new Course.Builder(parts[0], parts[1])
                            .credits(Integer.parseInt(parts[2]))
                            .department(parts[3])
                            .semester(Semester.valueOf(parts[4]))
                            .build();
                } catch (IllegalArgumentException e) {
                     System.err.println("Skipping course line due to invalid data: " + line);
                     return null;
                }
            })
            .filter(java.util.Objects::nonNull)
            .forEach(courseService::addCourse);
        } catch (IOException e) {
            System.err.println("Failed to import courses: " + e.getMessage());
        }
    }

    // --- Enrollment Methods ---
    public void exportEnrollments(List<Student> students) {
        try {
            Files.createDirectories(DATA_DIRECTORY);
            Path filePath = DATA_DIRECTORY.resolve("enrollments.csv");
            List<String> lines = students.stream()
                .flatMap(student -> student.getEnrolledCourses().stream()
                    .map(enrollment -> {
                        String grade = (enrollment.getGrade() == null) ? "NULL" : enrollment.getGrade().name();
                        return String.join(",",
                            student.getRegNo(),
                            enrollment.getCourse().getCode(),
                            grade);
                    }))
                .collect(Collectors.toList());
            Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to export enrollments: " + e.getMessage());
        }
    }
    
    public void importEnrollments(StudentService studentService, CourseService courseService, EnrollmentService enrollmentService) {
        Path filePath = DATA_DIRECTORY.resolve("enrollments.csv");
        if (!Files.exists(filePath)) return;

        try (Stream<String> lines = Files.lines(filePath)) {
            lines.forEach(line -> {
                String[] parts = line.split(",");
                if (parts.length < 3) {
                     System.err.println("Skipping malformed enrollment line: " + line);
                    return; // a simple return works like 'continue' in a forEach lambda
                }

                Optional<Student> studentOpt = studentService.findStudentByRegNo(parts[0]);
                Optional<Course> courseOpt = courseService.findCourseByCode(parts[1]);
                
                if (studentOpt.isPresent() && courseOpt.isPresent()) {
                    try {
                        try {
                           enrollmentService.enrollStudent(studentOpt.get(), courseOpt.get());
                        } catch (Exception e) {
                           // This is expected if the student is already enrolled
                        }
                        
                        if (!"NULL".equalsIgnoreCase(parts[2])) {
                            enrollmentService.assignGrade(studentOpt.get(), courseOpt.get(), Grade.valueOf(parts[2]));
                        }
                    } catch (Exception e) {
                        System.err.println("Could not process enrollment line: " + line + " | Reason: " + e.getMessage());
                    }
                } else {
                    System.err.println("Skipping enrollment, student or course not found for line: " + line);
                }
            });
        } catch (IOException e) {
            System.err.println("Failed to import enrollments: " + e.getMessage());
        }
    }
}