package edu.ccrm.cli;

import edu.ccrm.domain.*;
import edu.ccrm.exception.*;
import edu.ccrm.io.BackupService;
import edu.ccrm.io.ImportExportService;
import edu.ccrm.service.*;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

public class Main {

    // ... (services remain the same) ...
    private static final Scanner scanner = new Scanner(System.in);
    private static final StudentService studentService = new StudentServiceImplemenation();
    private static final CourseService courseService = new CourseServiceImplementation();
    private static final EnrollmentService enrollmentService = new EnrollmentServiceImplementation();
    private static final ImportExportService ioService = new ImportExportService();
    private static final BackupService backupService = new BackupService();


    public static void main(String[] args) {
        System.out.println("Welcome to the Campus Course & Records Manager!");
        
        // --- UPDATED IMPORT SECTION ---
        System.out.println("Loading data from files...");
        ioService.importStudents(studentService);
        ioService.importCourses(courseService);
        // Enrollments must be imported last as they depend on students and courses
        ioService.importEnrollments(studentService, courseService, enrollmentService);

        // This check can be removed, as data will now persist
        if (studentService.getAllStudents().isEmpty() && courseService.getAllCourses().isEmpty()) {
            System.out.println("No data found. You can add new students and courses.");
        }

        boolean exit = false;
        do {
            printMainMenu();
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> handleStudentMenu();
                case "2" -> handleCourseMenu();
                case "3" -> handleEnrollmentMenu();
                case "4" -> handleFileMenu();
                case "9" -> exit = true;
                default -> System.out.println("Invalid choice. Please try again.");
            }
        } while (!exit);
        
        // --- UPDATED EXPORT SECTION ---
        System.out.println("Saving all data to files...");
        ioService.exportStudents(studentService.getAllStudents());
        ioService.exportCourses(courseService.getAllCourses());
        ioService.exportEnrollments(studentService.getAllStudents());

        System.out.println("Thank you for using CCRM. Goodbye!");
        scanner.close();
    }
    
    // ... (All other methods in Main.java remain the same) ...
    
    private static void printMainMenu() {
        System.out.println("\n--- MAIN MENU ---");
        System.out.println("1. Student Management");
        System.out.println("2. Course Management");
        System.out.println("3. Enrollment & Grades");
        System.out.println("4. File Utilities");
        System.out.println("9. Save and Exit");
        System.out.print("Enter your choice: ");
    }
    
    private static void handleStudentMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n-- Student Management --");
            System.out.println("1. Add New Student");
            System.out.println("2. List All Students");
            System.out.println("3. Find Student by Registration Number");
            System.out.println("4. Update Student Details");
            System.out.println("5. Deactivate Student");
            System.out.println("9. Back to Main Menu");
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> addStudent();
                case "2" -> listAllStudents();
                case "3" -> findStudent();
                case "4" -> updateStudent();
                case "5" -> deactivateStudent();
                case "9" -> back = true;
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void handleCourseMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n-- Course Management --");
            System.out.println("1. Add New Course");
            System.out.println("2. List All Courses");
            System.out.println("3. Search Courses by Department");
            System.out.println("9. Back to Main Menu");
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1" -> addCourse();
                case "2" -> listAllCourses();
                case "3" -> searchCourses();
                case "9" -> back = true;
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void handleEnrollmentMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n-- Enrollment & Grades --");
            System.out.println("1. Enroll Student in Course");
            System.out.println("2. Unenroll Student from Course");
            System.out.println("3. Assign Grade");
            System.out.println("4. Print Student Transcript");
            System.out.println("9. Back to Main Menu");
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1" -> enrollStudentInCourse();
                case "2" -> unenrollStudentFromCourse();
                case "3" -> assignGradeToStudent();
                case "4" -> printStudentTranscript();
                case "9" -> back = true;
                default -> System.out.println("Invalid choice.");
            }
        }
    }
    
    private static void handleFileMenu() {
        System.out.println("\n-- File Utilities --");
        System.out.println("1. Create Backup of Current Data");
        System.out.println("2. Show Backup Directory Size");
        System.out.print("Enter your choice: ");
        String choice = scanner.nextLine();
        
        switch(choice) {
            case "1" -> backupService.performBackup();
            case "2" -> {
                long size = backupService.calculateDirectorySize(Paths.get("backups"));
                System.out.printf("Total size of backups directory: %.2f KB%n", size / 1024.0);
            }
            default -> System.out.println("Invalid choice.");
        }
    }

    private static void addStudent() {
        try {
            System.out.print("Enter Full Name: "); String name = scanner.nextLine();
            System.out.print("Enter Email: "); String email = scanner.nextLine();
            System.out.print("Enter Date of Birth (YYYY-MM-DD): "); LocalDate dob = LocalDate.parse(scanner.nextLine());
            System.out.print("Enter Registration Number (e.g., 24BCE10001): "); String regNo = scanner.nextLine();
            
            studentService.addStudent(new Student(name, email, dob, regNo));
            System.out.println("Student '" + name + "' added successfully.");
        } catch (DateTimeParseException e) {
            System.err.println("Error: Invalid date format. Please use YYYY-MM-DD.");
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }

    private static void listAllStudents() {
        System.out.println("\n--- All Students ---");
        List<Student> students = studentService.getAllStudents();
        if (students.isEmpty()) System.out.println("No students found.");
        else students.forEach(student -> System.out.println(student.getProfileDetails()));
    }
    
    private static void findStudent() {
        System.out.print("Enter student registration number to find: ");
        String regNo = scanner.nextLine();
        studentService.findStudentByRegNo(regNo)
                .ifPresentOrElse(s -> System.out.println("Found: " + s.getProfileDetails()),
                        () -> System.out.println("No student found with registration number: " + regNo));
    }
    
    private static void updateStudent() {
        System.out.print("Enter Registration Number of student to update: ");
        String regNo = scanner.nextLine();
        Optional<Student> studentOpt = studentService.findStudentByRegNo(regNo);

        if (studentOpt.isEmpty()) {
            System.out.println("Student not found.");
            return;
        }

        Student student = studentOpt.get();
        System.out.print("Enter new Full Name (or press Enter to keep '" + student.getFullName() + "'): ");
        String name = scanner.nextLine();
        if (!name.isBlank()) {
            student.setFullName(name);
        }

        System.out.print("Enter new Email (or press Enter to keep '" + student.getEmail() + "'): ");
        String email = scanner.nextLine();
        if (!email.isBlank()) {
            student.setEmail(email);
        }
        
        studentService.updateStudent(student);
        System.out.println("Student record updated successfully.");
    }
    
    private static void deactivateStudent() {
        System.out.print("Enter Registration Number of student to deactivate: ");
        String regNo = scanner.nextLine();
        Optional<Student> studentOpt = studentService.findStudentByRegNo(regNo);

        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            student.setActive(false);
            studentService.updateStudent(student);
            System.out.println("Student " + student.getFullName() + " has been deactivated.");
        } else {
            System.out.println("Student not found.");
        }
    }

    private static void addCourse() {
        try {
            System.out.print("Enter Course Code (e.g., CSE0001): "); String code = scanner.nextLine();
            System.out.print("Enter Course Title: "); String title = scanner.nextLine();
            System.out.print("Enter Credits: "); int credits = Integer.parseInt(scanner.nextLine());
            System.out.print("Enter Department: "); String dept = scanner.nextLine();
            
            Course newCourse = new Course.Builder(code, title).credits(credits).department(dept).semester(Semester.FALL).build();
            courseService.addCourse(newCourse);
            System.out.println("Course added successfully: " + title);
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid number format for credits.");
        }
    }
    
    private static void listAllCourses() {
        System.out.println("\n--- All Courses ---");
        List<Course> courses = courseService.getAllCourses();
        if (courses.isEmpty()) System.out.println("No courses found.");
        else courses.forEach(System.out::println);
    }
    
    private static void searchCourses() {
        System.out.print("Enter department to search for: ");
        String dept = scanner.nextLine();
        List<Course> results = courseService.findCoursesByDepartment(dept);
        System.out.println("\n--- Courses in '" + dept + "' ---");
        if (results.isEmpty()) System.out.println("No courses found for this department.");
        else results.forEach(System.out::println);
    }

    private static void enrollStudentInCourse() {
        System.out.print("Enter Student Registration Number: "); String regNo = scanner.nextLine();
        System.out.print("Enter Course Code: "); String courseCode = scanner.nextLine();

        Optional<Student> studentOpt = studentService.findStudentByRegNo(regNo);
        Optional<Course> courseOpt = courseService.findCourseByCode(courseCode);

        if (studentOpt.isEmpty() || courseOpt.isEmpty()) {
            System.err.println("Error: Invalid student registration number or course code.");
            return;
        }

        try {
            enrollmentService.enrollStudent(studentOpt.get(), courseOpt.get());
            System.out.println("Enrollment successful.");
        } catch (Exception e) {
            System.err.println("Enrollment Error: " + e.getMessage());
        }
    }
    
    private static void unenrollStudentFromCourse() {
        System.out.print("Enter Student Registration Number: ");
        String regNo = scanner.nextLine();
        System.out.print("Enter Course Code to unenroll from: ");
        String courseCode = scanner.nextLine();

        Optional<Student> studentOpt = studentService.findStudentByRegNo(regNo);
        Optional<Course> courseOpt = courseService.findCourseByCode(courseCode);

        if (studentOpt.isPresent() && courseOpt.isPresent()) {
             System.out.println("Unenrollment feature to be Implementationemented in EnrollmentService.");
        } else {
            System.err.println("Error: Invalid student or course code.");
        }
    }

    private static void assignGradeToStudent() {
        System.out.print("Enter Student Registration Number: "); String regNo = scanner.nextLine();
        System.out.print("Enter Course Code: "); String courseCode = scanner.nextLine();
        System.out.print("Enter Letter Grade (S, A, B, C, D, E, F): "); String gradeStr = scanner.nextLine().toUpperCase();
        
        try {
            Grade grade = Grade.valueOf(gradeStr);
            Optional<Student> studentOpt = studentService.findStudentByRegNo(regNo);
            Optional<Course> courseOpt = courseService.findCourseByCode(courseCode);
            
            if (studentOpt.isPresent() && courseOpt.isPresent()) {
                enrollmentService.assignGrade(studentOpt.get(), courseOpt.get(), grade);
                System.out.println("Grade assigned successfully.");
            } else {
                System.err.println("Error: Invalid student or course specified.");
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error: Invalid grade. Please use one of S, A, B, C, D, E, F.");
        } catch (NoSuchElementException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    private static void printStudentTranscript() {
        System.out.print("Enter student registration number for transcript: ");
        String regNo = scanner.nextLine();
        studentService.findStudentByRegNo(regNo)
                .ifPresentOrElse(enrollmentService::generateTranscript,
                        () -> System.out.println("No student found with registration number: " + regNo));
    }
}