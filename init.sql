-- ============================================================================
-- High School Test System (HSTS) - Database Initialization Script
-- Module Version: Assignment 3 (Spring 2026)
-- Domain: Database Schema, Repositories, and Business Logic Integration
-- ============================================================================
-- OVERVIEW:
-- This script builds the relational database schema for the HSTS backend and
-- populates it with initial seed data required for integration testing.
--
-- SCHEMA TABLES:
-- 1. users                   - User accounts and roles (STUDENT, TEACHER, COORDINATOR, PRINCIPAL)
-- 2. courses                 - Subject courses (CS101, MATH201)
-- 3. questions               - Bank of questions classified by difficulty, topic, and course
-- 4. question_answers        - Multiple-choice options with correctness flags
-- 5. exams                   - Exam drafts and approved exams with 4-character execution codes
-- 6. exam_questions          - Join table linking questions to specific exams with order
-- 7. exam_answers            - Student exam submissions and automated/manual scores
-- 8. student_selected_answers - Individual student answers per submitted exam attempt
--
-- SEEDED TEST USERS & CREDENTIALS:
-- • Student:     username = 'student1'     | password = '123456' | role = STUDENT
-- • Teacher:     username = 'teacher1'     | password = '123456' | role = TEACHER
-- • Coordinator: username = 'coord1'       | password = '123456' | role = COORDINATOR
-- • Principal:   username = 'principal1'   | password = '123456' | role = PRINCIPAL
--
-- SEEDED COURSE & EXAM DATA:
-- • Courses: CS101 (Computer Science Fundamentals), MATH201 (Linear Algebra & Calculus)
-- • Questions: 5 pre-loaded CS101 questions across Architecture, Data Structures, Concurrency, etc.
-- • Active Exam: Exam E72874 (Code: HKD6) linked to all 5 questions, approved and scheduled
-- ============================================================================
CREATE DATABASE IF NOT EXISTS hsts_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hsts_db;

-- --------------------------------------------------------
-- Drop Tables in Reverse Dependency Order (Child tables first)
-- --------------------------------------------------------
DROP TABLE IF EXISTS bot_interactions;
DROP TABLE IF EXISTS student_selected_answers;
DROP TABLE IF EXISTS exam_answers;
DROP TABLE IF EXISTS exam_questions;
DROP TABLE IF EXISTS question_answers;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS exams;
DROP TABLE IF EXISTS exam_executions;
DROP TABLE IF EXISTS student_courses;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS users;

-- --------------------------------------------------------
-- 1. Table Structure: users
-- --------------------------------------------------------
CREATE TABLE users (
                       username VARCHAR(50) PRIMARY KEY,
                       password VARCHAR(255) NOT NULL,
                       full_name VARCHAR(100) NOT NULL,
                       role ENUM('STUDENT', 'TEACHER', 'COORDINATOR', 'PRINCIPAL') NOT NULL,
                       email VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed Users
INSERT INTO users (username, password, full_name, role, email) VALUES
                                                                   ('student1', '123456', 'Alice Smith', 'STUDENT', 'alice@school.edu'),
                                                                   ('teacher1', '123456', 'Dr. Robert Laganiere', 'TEACHER', 'robert@school.edu'),
                                                                   ('coord1', '123456', 'Prof. Timothy Lethbridge', 'COORDINATOR', 'timothy@school.edu'),
                                                                   ('principal1', '123456', 'Dr. School Principal', 'PRINCIPAL', 'principal@school.edu');

-- --------------------------------------------------------
-- 2. Table Structure: courses
-- --------------------------------------------------------
CREATE TABLE courses (
                         course_id VARCHAR(10) PRIMARY KEY,
                         name VARCHAR(100) NOT NULL,
                         subject_code VARCHAR(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed Courses
INSERT INTO courses (course_id, name, subject_code) VALUES
                                                        ('CS101', 'Computer Science Fundamentals', 'CS'),
                                                        ('MATH201', 'Linear Algebra & Calculus', 'MATH');

CREATE TABLE student_courses (
                                 student_id VARCHAR(50) NOT NULL,
                                 course_id VARCHAR(10) NOT NULL,
                                 PRIMARY KEY (student_id, course_id),
                                 FOREIGN KEY (student_id) REFERENCES users(username) ON DELETE CASCADE,
                                 FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO student_courses (student_id, course_id) VALUES
                                                        ('student1', 'CS101'),
                                                        ('student1', 'MATH201');

-- --------------------------------------------------------
-- 3. Table Structure: questions
-- --------------------------------------------------------
CREATE TABLE questions (
                           question_id VARCHAR(10) PRIMARY KEY,
                           text TEXT NOT NULL,
                           difficulty ENUM('EASY', 'MEDIUM', 'HARD') NOT NULL,
                           instructions TEXT,
                           topic VARCHAR(100),
                           course_id VARCHAR(10),
                           FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed Questions
INSERT INTO questions (question_id, text, difficulty, instructions, topic, course_id) VALUES
                                                                                          ('11001', 'What is the main function of the CPU?', 'EASY', 'Select the best single option.', 'Architecture', 'CS101'),
                                                                                          ('11002', 'Which data structure follows LIFO order?', 'EASY', 'Select the correct memory structure.', 'Data Structures', 'CS101'),
                                                                                          ('11003', 'What is the advantage of multi-threading?', 'MEDIUM', 'Select performance benefit.', 'Concurrency', 'CS101'),
                                                                                          ('11004', 'What does Foreign Key ensure in relational DBs?', 'MEDIUM', 'Select relational property.', 'Databases', 'CS101'),
                                                                                          ('11005', 'What is binary search time complexity?', 'HARD', 'Assume sorted array.', 'Algorithms', 'CS101');

-- --------------------------------------------------------
-- 4. Table Structure: question_answers
-- --------------------------------------------------------
CREATE TABLE question_answers (
                                  answer_id INT AUTO_INCREMENT PRIMARY KEY,
                                  question_id VARCHAR(10) NOT NULL,
                                  answer_text VARCHAR(255) NOT NULL,
                                  is_correct TINYINT(1) DEFAULT 0,
                                  FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed Question Choices
INSERT INTO question_answers (question_id, answer_text, is_correct) VALUES
                                                                        ('11001', 'Execute instructions and perform arithmetic/logic operations', 1),
                                                                        ('11001', 'Store long-term hard disk files', 0),
                                                                        ('11002', 'Stack', 1),
                                                                        ('11002', 'Queue', 0),
                                                                        ('11003', 'Allows concurrent execution of tasks to maximize CPU utilization', 1),
                                                                        ('11003', 'Prevents all memory allocation', 0),
                                                                        ('11004', 'Referential Integrity between tables', 1),
                                                                        ('11004', 'Faster network socket streaming', 0),
                                                                        ('11005', 'O(log n)', 1),
                                                                        ('11005', 'O(1)', 0);

-- --------------------------------------------------------
-- 5. Table Structure: exams
-- --------------------------------------------------------
CREATE TABLE exams (
                       exam_id VARCHAR(10) PRIMARY KEY,
                       course_id VARCHAR(10) NOT NULL,
                       title VARCHAR(100) NOT NULL,
                       instructions TEXT,
                       instructions_for_teacher TEXT,
                       duration_minutes INT NOT NULL,
                       created_by_teacher_id VARCHAR(50) NOT NULL,
                       status ENUM('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED') DEFAULT 'DRAFT',
                       rejection_reason TEXT,
                       approved_by_coordinator_id VARCHAR(50),
                       execution_code VARCHAR(10),
                       scheduled_start DATETIME,
                       scheduled_end DATETIME,
                       FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
                       FOREIGN KEY (created_by_teacher_id) REFERENCES users(username) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed Approved Active Exam (Code: HKD6)
INSERT INTO exams (
    exam_id, course_id, title, instructions, duration_minutes,
    created_by_teacher_id, status, execution_code, scheduled_start, scheduled_end
) VALUES (
             'E72874', 'CS101', 'Test-exam', 'Complete all questions within duration.', 60,
             'teacher1', 'APPROVED', 'HKD6', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 14 DAY)
         );

-- --------------------------------------------------------
-- 6. Table Structure: exam_questions (Join Table)
-- --------------------------------------------------------
CREATE TABLE exam_questions (
                                exam_id VARCHAR(10) NOT NULL,
                                question_id VARCHAR(10) NOT NULL,
                                question_order INT DEFAULT 1,
                                PRIMARY KEY (exam_id, question_id),
                                FOREIGN KEY (exam_id) REFERENCES exams(exam_id) ON DELETE CASCADE,
                                FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed Questions Linked to Exam E72874
INSERT INTO exam_questions (exam_id, question_id, question_order) VALUES
                                                                      ('E72874', '11001', 1),
                                                                      ('E72874', '11002', 2),
                                                                      ('E72874', '11003', 3),
                                                                      ('E72874', '11004', 4),
                                                                      ('E72874', '11005', 5);

-- --------------------------------------------------------
-- 7. Table Structure: exam_answers
-- --------------------------------------------------------
CREATE TABLE exam_executions (
                                 execution_id VARCHAR(20) PRIMARY KEY,
                                 exam_id VARCHAR(10) NOT NULL,
                                 execution_code VARCHAR(4) NOT NULL,
                                 scheduled_start DATETIME,
                                 scheduled_end DATETIME,
                                 extra_minutes_granted INT DEFAULT 0,
                                 created_by_teacher_id VARCHAR(50) NOT NULL,
                                 FOREIGN KEY (exam_id) REFERENCES exams(exam_id) ON DELETE CASCADE,
                                 FOREIGN KEY (created_by_teacher_id) REFERENCES users(username) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exam_answers (
                              exam_answer_id VARCHAR(20) PRIMARY KEY,
                              exam_id VARCHAR(10) NOT NULL,
                              execution_id VARCHAR(20),
                              student_id VARCHAR(50) NOT NULL,
                              auto_score DOUBLE DEFAULT 0.0,
                              final_score DOUBLE DEFAULT 0.0,
                              teacher_comment TEXT,
                              grade_confirmed TINYINT(1) DEFAULT 0,
                              submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              auto_submitted TINYINT(1) DEFAULT 0,
                              FOREIGN KEY (exam_id) REFERENCES exams(exam_id) ON DELETE CASCADE,
                              FOREIGN KEY (execution_id) REFERENCES exam_executions(execution_id) ON DELETE SET NULL,
                              FOREIGN KEY (student_id) REFERENCES users(username) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- 8. Table Structure: student_selected_answers
-- --------------------------------------------------------
CREATE TABLE student_selected_answers (
                                          selected_answer_id INT AUTO_INCREMENT PRIMARY KEY,
                                          exam_answer_id VARCHAR(20) NOT NULL,
                                          question_id VARCHAR(10) NOT NULL,
                                          selected_answer_text VARCHAR(255), -- 👈 Renamed to match ExamAnswerRepositoryImpl
                                          FOREIGN KEY (exam_answer_id) REFERENCES exam_answers(exam_answer_id) ON DELETE CASCADE,
                                          FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE bot_interactions (
                                  interaction_id VARCHAR(20) PRIMARY KEY,
                                  student_id VARCHAR(50) NOT NULL,
                                  course_id VARCHAR(10) NOT NULL,
                                  user_question TEXT,
                                  bot_response TEXT,
                                  timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                                  FOREIGN KEY (student_id) REFERENCES users(username) ON DELETE CASCADE,
                                  FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;