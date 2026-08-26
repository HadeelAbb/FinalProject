-- ============================================================================
-- High School Test System (HSTS) - Database Initialization Script
-- Module Version: Final System Integration (Spring 2026)
-- Domain: Database Schema, Repositories, Live Executions, and AI Study Bot
-- ============================================================================
-- OVERVIEW:
-- This script builds the relational database schema for the HSTS central server
-- and populates it with complete seed data for testing all 21 system scenarios.
--
-- SCHEMA TABLES (12 TABLES TOTAL):
-- 1. users                   - User accounts, hashed passwords, roles & contact info[cite: 21]
-- 2. courses                 - Academic courses with departmental subject codes[cite: 21]
-- 3. teacher_courses         - Teacher-to-course teaching assignments[cite: 21]
-- 4. student_courses         - Student-to-course course enrollments[cite: 21]
-- 5. questions               - Bank of questions with versioning, topics, & illustration blobs[cite: 21]
-- 6. question_answers        - 4-choice options per question with correctness flags[cite: 21]
-- 7. exams                   - Exam entities with approval status, versioning & instructions[cite: 21]
-- 8. exam_questions          - Exam-to-question mappings with custom positive point weights (sum=100)[cite: 21]
-- 9. exam_executions         - Scheduled exam sessions, 4-char security codes & time extensions[cite: 21]
-- 10. exam_answers           - Student submission headers, auto/manual grades & teacher comments[cite: 21]
-- 11. student_selected_answers - Granular student answer choices recorded per question[cite: 21]
-- 12. course_bot_configs     - AI Study Bot configurations and syllabus reference materials[cite: 21]
-- 13. bot_interactions       - Student-bot conversation history and query tracking[cite: 21]
--
-- SEEDED TEST USERS & CREDENTIALS:
-- • Student 1:     username = 'student1'   | password = '123456' | role = STUDENT[cite: 21]
-- • Student 2:     username = 'student2'   | password = '123456' | role = STUDENT[cite: 21]
-- • Teacher 1:     username = 'teacher1'   | password = '123456' | role = TEACHER[cite: 21]
-- • Teacher 2:     username = 'teacher2'   | password = '123456' | role = TEACHER[cite: 21]
-- • Coordinator:   username = 'coord1'     | password = '123456' | role = COORDINATOR[cite: 21]
-- • Principal:     username = 'principal1' | password = '123456' | role = PRINCIPAL[cite: 21]
--
-- SEEDED COURSES:
-- • CS101:   Computer Science Fundamentals (Subject: CS)[cite: 21]
-- • MATH201: Linear Algebra & Calculus (Subject: MATH)[cite: 21]
--
-- SEEDED EXAMS & ACTIVE EXECUTIONS:
-- • E72874 (CS101):   APPROVED v1 | Execution: EXSEED01 | Code: HKD6 | 60 min[cite: 21]
-- • E27534 (CS101):   DRAFT v2 (lineage of E72874) | Teacher: teacher1 | 60 min[cite: 21]
-- • E88101 (MATH201): APPROVED v1 | Execution: EXSEED02 | Code: MTH1 | 90 min (+10 extra min granted)[cite: 21]
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `hsts_db`
CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;

USE `hsts_db`;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------------------
-- 1. Table Definitions & Constraints
-- ----------------------------------------------------------------------------

DROP TABLE IF EXISTS `bot_interactions`;
DROP TABLE IF EXISTS `course_bot_configs`;
DROP TABLE IF EXISTS `student_selected_answers`;
DROP TABLE IF EXISTS `exam_answers`;
DROP TABLE IF EXISTS `exam_executions`;
DROP TABLE IF EXISTS `exam_questions`;
DROP TABLE IF EXISTS `question_answers`;
DROP TABLE IF EXISTS `exams`;
DROP TABLE IF EXISTS `questions`;
DROP TABLE IF EXISTS `teacher_courses`;
DROP TABLE IF EXISTS `student_courses`;
DROP TABLE IF EXISTS `courses`;
DROP TABLE IF EXISTS `users`;

-- Stores user identities, authentication credentials, and system roles[cite: 21]
CREATE TABLE `users` (
                         `username` varchar(50) NOT NULL,
                         `password` varchar(255) NOT NULL,
                         `full_name` varchar(100) NOT NULL,
                         `role` enum('STUDENT','TEACHER','COORDINATOR','PRINCIPAL') NOT NULL,
                         `email` varchar(100) DEFAULT NULL,
                         PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Stores academic courses linked to subject faculties[cite: 21]
CREATE TABLE `courses` (
                           `course_id` varchar(10) NOT NULL,
                           `name` varchar(100) NOT NULL,
                           `subject_code` varchar(10) NOT NULL,
                           PRIMARY KEY (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Associates teachers with the courses they are authorized to manage[cite: 21]
CREATE TABLE `teacher_courses` (
                                   `teacher_id` varchar(50) NOT NULL,
                                   `course_id` varchar(10) NOT NULL,
                                   PRIMARY KEY (`teacher_id`,`course_id`),
                                   KEY `course_id` (`course_id`),
                                   CONSTRAINT `teacher_courses_ibfk_1` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`username`) ON DELETE CASCADE,
                                   CONSTRAINT `teacher_courses_ibfk_2` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Associates students with their enrolled courses[cite: 21]
CREATE TABLE `student_courses` (
                                   `student_id` varchar(50) NOT NULL,
                                   `course_id` varchar(10) NOT NULL,
                                   PRIMARY KEY (`student_id`,`course_id`),
                                   KEY `course_id` (`course_id`),
                                   CONSTRAINT `student_courses_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `users` (`username`) ON DELETE CASCADE,
                                   CONSTRAINT `student_courses_ibfk_2` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Stores question bank items with version lineage and optional illustration blobs[cite: 21]
CREATE TABLE `questions` (
                             `question_id` varchar(10) NOT NULL,
                             `text` text NOT NULL,
                             `difficulty` enum('EASY','MEDIUM','HARD') NOT NULL,
                             `instructions` text,
                             `topic` varchar(100) DEFAULT NULL,
                             `course_id` varchar(10) DEFAULT NULL,
                             `image_filename` varchar(255) DEFAULT NULL,
                             `image_data` longblob,
                             `root_question_id` varchar(10) DEFAULT NULL,
                             `version_number` int NOT NULL DEFAULT '1',
                             `is_latest` tinyint(1) NOT NULL DEFAULT '1',
                             PRIMARY KEY (`question_id`),
                             UNIQUE KEY `uk_question_lineage` (`root_question_id`,`version_number`),
                             KEY `course_id` (`course_id`),
                             CONSTRAINT `questions_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Stores the 4 multiple choice options per question[cite: 21]
CREATE TABLE `question_answers` (
                                    `answer_id` int NOT NULL AUTO_INCREMENT,
                                    `question_id` varchar(10) NOT NULL,
                                    `answer_text` varchar(255) NOT NULL,
                                    `is_correct` tinyint(1) DEFAULT '0',
                                    PRIMARY KEY (`answer_id`),
                                    KEY `question_id` (`question_id`),
                                    CONSTRAINT `question_answers_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `questions` (`question_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Stores exams, versioning history, approval workflows, and teacher/student notes[cite: 21]
CREATE TABLE `exams` (
                         `exam_id` varchar(10) NOT NULL,
                         `course_id` varchar(10) NOT NULL,
                         `title` varchar(100) NOT NULL,
                         `instructions` text,
                         `instructions_for_teacher` text,
                         `duration_minutes` int NOT NULL,
                         `created_by_teacher_id` varchar(50) NOT NULL,
                         `status` enum('DRAFT','PENDING_APPROVAL','APPROVED','REJECTED') DEFAULT 'DRAFT',
                         `rejection_reason` text,
                         `approved_by_coordinator_id` varchar(50) DEFAULT NULL,
                         `execution_code` varchar(10) DEFAULT NULL,
                         `scheduled_start` datetime DEFAULT NULL,
                         `scheduled_end` datetime DEFAULT NULL,
                         `root_exam_id` varchar(10) DEFAULT NULL,
                         `version_number` int NOT NULL DEFAULT '1',
                         `is_latest` tinyint(1) NOT NULL DEFAULT '1',
                         PRIMARY KEY (`exam_id`),
                         UNIQUE KEY `uk_exam_lineage` (`root_exam_id`,`version_number`),
                         KEY `course_id` (`course_id`),
                         KEY `created_by_teacher_id` (`created_by_teacher_id`),
                         CONSTRAINT `exams_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`) ON DELETE CASCADE,
                         CONSTRAINT `exams_ibfk_2` FOREIGN KEY (`created_by_teacher_id`) REFERENCES `users` (`username`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Associates questions with exams and assigns custom point weights totaling 100[cite: 21]
CREATE TABLE `exam_questions` (
                                  `exam_id` varchar(10) NOT NULL,
                                  `question_id` varchar(10) NOT NULL,
                                  `question_order` int DEFAULT '1',
                                  `points` int NOT NULL DEFAULT '20',
                                  PRIMARY KEY (`exam_id`,`question_id`),
                                  KEY `question_id` (`question_id`),
                                  CONSTRAINT `exam_questions_ibfk_1` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`exam_id`) ON DELETE CASCADE,
                                  CONSTRAINT `exam_questions_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `questions` (`question_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Stores active/scheduled exam execution windows and teacher-granted extra time[cite: 21]
CREATE TABLE `exam_executions` (
                                   `execution_id` varchar(20) NOT NULL,
                                   `exam_id` varchar(10) NOT NULL,
                                   `execution_code` varchar(4) NOT NULL,
                                   `scheduled_start` datetime DEFAULT NULL,
                                   `scheduled_end` datetime DEFAULT NULL,
                                   `extra_minutes_granted` int DEFAULT '0',
                                   `created_by_teacher_id` varchar(50) NOT NULL,
                                   PRIMARY KEY (`execution_id`),
                                   KEY `exam_id` (`exam_id`),
                                   KEY `created_by_teacher_id` (`created_by_teacher_id`),
                                   CONSTRAINT `exam_executions_ibfk_1` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`exam_id`) ON DELETE CASCADE,
                                   CONSTRAINT `exam_executions_ibfk_2` FOREIGN KEY (`created_by_teacher_id`) REFERENCES `users` (`username`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Stores submitted student exams, auto-graded vs. confirmed scores, and teacher comments[cite: 21]
CREATE TABLE `exam_answers` (
                                `exam_answer_id` varchar(20) NOT NULL,
                                `exam_id` varchar(10) NOT NULL,
                                `execution_id` varchar(20) DEFAULT NULL,
                                `student_id` varchar(50) NOT NULL,
                                `auto_score` double DEFAULT '0',
                                `final_score` double DEFAULT '0',
                                `teacher_comment` text,
                                `grade_confirmed` tinyint(1) DEFAULT '0',
                                `submitted_at` datetime DEFAULT CURRENT_TIMESTAMP,
                                `auto_submitted` tinyint(1) DEFAULT '0',
                                PRIMARY KEY (`exam_answer_id`),
                                KEY `exam_id` (`exam_id`),
                                KEY `execution_id` (`execution_id`),
                                KEY `student_id` (`student_id`),
                                CONSTRAINT `exam_answers_ibfk_1` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`exam_id`) ON DELETE CASCADE,
                                CONSTRAINT `exam_answers_ibfk_2` FOREIGN KEY (`execution_id`) REFERENCES `exam_executions` (`execution_id`) ON DELETE SET NULL,
                                CONSTRAINT `exam_answers_ibfk_3` FOREIGN KEY (`student_id`) REFERENCES `users` (`username`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Stores individual selected answer choices per question for student exam copies[cite: 21]
CREATE TABLE `student_selected_answers` (
                                            `selected_answer_id` int NOT NULL AUTO_INCREMENT,
                                            `exam_answer_id` varchar(20) NOT NULL,
                                            `question_id` varchar(10) NOT NULL,
                                            `selected_answer_text` varchar(255) DEFAULT NULL,
                                            PRIMARY KEY (`selected_answer_id`),
                                            KEY `exam_answer_id` (`exam_answer_id`),
                                            KEY `question_id` (`question_id`),
                                            CONSTRAINT `student_selected_answers_ibfk_1` FOREIGN KEY (`exam_answer_id`) REFERENCES `exam_answers` (`exam_answer_id`) ON DELETE CASCADE,
                                            CONSTRAINT `student_selected_answers_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `questions` (`question_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Stores AI Study Bot configuration topics and teacher reference sources per course[cite: 21]
CREATE TABLE `course_bot_configs` (
                                      `course_id` varchar(10) NOT NULL,
                                      `bot_name` varchar(100) NOT NULL,
                                      `knowledge_sources` text,
                                      `is_active` tinyint(1) DEFAULT '1',
                                      `last_updated_by` varchar(50) DEFAULT NULL,
                                      `last_updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      PRIMARY KEY (`course_id`),
                                      KEY `last_updated_by` (`last_updated_by`),
                                      CONSTRAINT `course_bot_configs_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`) ON DELETE CASCADE,
                                      CONSTRAINT `course_bot_configs_ibfk_2` FOREIGN KEY (`last_updated_by`) REFERENCES `users` (`username`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Stores student question interactions and bot responses for history & usage stats[cite: 21]
CREATE TABLE `bot_interactions` (
                                    `interaction_id` varchar(20) NOT NULL,
                                    `student_id` varchar(50) NOT NULL,
                                    `course_id` varchar(10) NOT NULL,
                                    `user_question` text,
                                    `bot_response` text,
                                    `timestamp` datetime DEFAULT CURRENT_TIMESTAMP,
                                    PRIMARY KEY (`interaction_id`),
                                    KEY `student_id` (`student_id`),
                                    KEY `course_id` (`course_id`),
                                    CONSTRAINT `bot_interactions_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `users` (`username`) ON DELETE CASCADE,
                                    CONSTRAINT `bot_interactions_ibfk_2` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 2. Seed Data Population
-- ----------------------------------------------------------------------------

-- Seed Users: Students, Teachers, Subject Coordinator, and Principal[cite: 21]
INSERT INTO `users` (`username`, `password`, `full_name`, `role`, `email`) VALUES
                                                                               ('coord1', '123456', 'Prof. Timothy Lethbridge', 'COORDINATOR', 'timothy@school.edu'),
                                                                               ('principal1', '123456', 'Dr. School Principal', 'PRINCIPAL', 'principal@school.edu'),
                                                                               ('student1', '123456', 'Alice Smith', 'STUDENT', 'alice@school.edu'),
                                                                               ('student2', '123456', 'Bob Miller', 'STUDENT', 'bob@school.edu'),
                                                                               ('teacher1', '123456', 'Dr. Robert Laganiere', 'TEACHER', 'robert@school.edu'),
                                                                               ('teacher2', '123456', 'Dr. Sarah Connor', 'TEACHER', 'sarah@school.edu')
    ON DUPLICATE KEY UPDATE `full_name` = VALUES(`full_name`), `email` = VALUES(`email`);

-- Seed Academic Courses[cite: 21]
INSERT INTO `courses` (`course_id`, `name`, `subject_code`) VALUES
                                                                ('CS101', 'Computer Science Fundamentals', 'CS'),
                                                                ('MATH201', 'Linear Algebra & Calculus', 'MATH')
    ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `subject_code` = VALUES(`subject_code`);

-- Seed Teacher Course Assignments[cite: 21]
INSERT IGNORE INTO `teacher_courses` (`teacher_id`, `course_id`) VALUES
('teacher1', 'CS101'),
('teacher2', 'CS101'),
('teacher1', 'MATH201'),
('teacher2', 'MATH201');

-- Seed Student Course Enrollments[cite: 21]
INSERT IGNORE INTO `student_courses` (`student_id`, `course_id`) VALUES
('student1', 'CS101'),
('student2', 'CS101'),
('student1', 'MATH201'),
('student2', 'MATH201');

-- Seed CS101 Question Bank across difficulty levels and topics[cite: 21]
INSERT INTO `questions` (`question_id`, `text`, `difficulty`, `instructions`, `topic`, `course_id`, `image_filename`, `image_data`, `root_question_id`, `version_number`, `is_latest`) VALUES
                                                                                                                                                                                           ('11001', 'What is the main function of the CPU?', 'EASY', 'Select the best single option.', 'Architecture', 'CS101', NULL, NULL, '11001', 1, 1),
                                                                                                                                                                                           ('11002', 'Which data structure follows LIFO order?', 'EASY', 'Select the correct memory structure.', 'Data Structures', 'CS101', NULL, NULL, '11002', 1, 1),
                                                                                                                                                                                           ('11003', 'What is the advantage of multi-threading?', 'MEDIUM', 'Select performance benefit.', 'Concurrency', 'CS101', NULL, NULL, '11003', 1, 1),
                                                                                                                                                                                           ('11004', 'What does Foreign Key ensure in relational DBs?', 'MEDIUM', 'Select relational property.', 'Databases', 'CS101', NULL, NULL, '11004', 1, 1),
                                                                                                                                                                                           ('11005', 'What is binary search time complexity?', 'HARD', 'Assume sorted array.', 'Algorithms', 'CS101', NULL, NULL, '11005', 1, 1)
    ON DUPLICATE KEY UPDATE `text` = VALUES(`text`), `difficulty` = VALUES(`difficulty`), `instructions` = VALUES(`instructions`), `topic` = VALUES(`topic`), `is_latest` = VALUES(`is_latest`);

-- Seed 4 Multiple-Choice Answers per Question (1 Correct each)[cite: 21]
INSERT INTO `question_answers` (`answer_id`, `question_id`, `answer_text`, `is_correct`) VALUES
                                                                                             (1, '11001', 'Execute instructions and perform arithmetic/logic operations', 1),
                                                                                             (2, '11001', 'Store long-term hard disk files', 0),
                                                                                             (3, '11002', 'Stack', 1),
                                                                                             (4, '11002', 'Queue', 0),
                                                                                             (5, '11003', 'Allows concurrent execution of tasks to maximize CPU utilization', 1),
                                                                                             (6, '11003', 'Prevents all memory allocation', 0),
                                                                                             (7, '11004', 'Referential Integrity between tables', 1),
                                                                                             (8, '11004', 'Faster network socket streaming', 0),
                                                                                             (9, '11005', 'O(log n)', 1),
                                                                                             (10, '11005', 'O(1)', 0)
    ON DUPLICATE KEY UPDATE `answer_text` = VALUES(`answer_text`), `is_correct` = VALUES(`is_correct`);

-- Seed Exams with Version Lineage (v1 historical & v2 draft)[cite: 21]
INSERT INTO `exams` (`exam_id`, `course_id`, `title`, `instructions`, `instructions_for_teacher`, `duration_minutes`, `created_by_teacher_id`, `status`, `rejection_reason`, `approved_by_coordinator_id`, `execution_code`, `scheduled_start`, `scheduled_end`, `root_exam_id`, `version_number`, `is_latest`) VALUES
                                                                                                                                                                                                                                                                                                                    ('E27534', 'CS101', 'Test-exam', 'NEWComplete all questions within duration.', NULL, 60, 'teacher1', 'DRAFT', NULL, NULL, NULL, NULL, NULL, 'E72874', 2, 1),
                                                                                                                                                                                                                                                                                                                    ('E72874', 'CS101', 'Test-exam', 'Complete all questions within duration.', NULL, 60, 'teacher1', 'APPROVED', NULL, NULL, 'HKD6', '2026-08-19 17:06:43', '2026-09-03 17:06:43', 'E72874', 1, 0),
                                                                                                                                                                                                                                                                                                                    ('E88101', 'MATH201', 'Calculus Midterm', 'Answer all calculus questions.', NULL, 90, 'teacher2', 'APPROVED', NULL, NULL, 'MTH1', '2026-08-25 17:12:58', '2026-09-09 17:12:58', 'E88101', 1, 1)
    ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `instructions` = VALUES(`instructions`), `duration_minutes` = VALUES(`duration_minutes`), `status` = VALUES(`status`), `execution_code` = VALUES(`execution_code`), `scheduled_start` = VALUES(`scheduled_start`), `scheduled_end` = VALUES(`scheduled_end`), `is_latest` = VALUES(`is_latest`);

-- Map 5 Questions per Exam at 20 Points Each (Total = 100)[cite: 21]
INSERT IGNORE INTO `exam_questions` (`exam_id`, `question_id`, `question_order`, `points`) VALUES
('E27534', '11001', 1, 20),
('E27534', '11002', 2, 20),
('E27534', '11003', 3, 20),
('E27534', '11004', 4, 20),
('E27534', '11005', 5, 20),
('E72874', '11001', 1, 20),
('E72874', '11002', 2, 20),
('E72874', '11003', 3, 20),
('E72874', '11004', 4, 20),
('E72874', '11005', 5, 20);

-- Active Scheduled Executions (HKD6 for CS101, MTH1 with +10 extra min for MATH201)[cite: 21]
INSERT INTO `exam_executions` (`execution_id`, `exam_id`, `execution_code`, `scheduled_start`, `scheduled_end`, `extra_minutes_granted`, `created_by_teacher_id`) VALUES
                                                                                                                                                                      ('EXSEED01', 'E72874', 'HKD6', '2026-08-24 17:34:22', '2026-09-09 17:34:22', 0, 'teacher1'),
                                                                                                                                                                      ('EXSEED02', 'E88101', 'MTH1', '2026-08-24 17:34:22', '2026-09-09 17:34:22', 10, 'teacher2')
    ON DUPLICATE KEY UPDATE `execution_code` = VALUES(`execution_code`), `scheduled_start` = VALUES(`scheduled_start`), `scheduled_end` = VALUES(`scheduled_end`), `extra_minutes_granted` = VALUES(`extra_minutes_granted`);

-- Seed Confirmed Student Submissions and Teacher Grade Overrides[cite: 21]
INSERT INTO `exam_answers` (`exam_answer_id`, `exam_id`, `execution_id`, `student_id`, `auto_score`, `final_score`, `teacher_comment`, `grade_confirmed`, `submitted_at`, `auto_submitted`) VALUES
                                                                                                                                                                                                ('ANS_MATH_01', 'E88101', 'EXSEED02', 'student1', 88, 92, 'Solid work on matrix operations.', 1, '2026-08-26 17:12:58', 0),
                                                                                                                                                                                                ('EA40805', 'E72874', 'EXSEED01', 'student1', 20, 25, 'Try harder', 1, '2026-08-26 17:29:01', 0)
    ON DUPLICATE KEY UPDATE `auto_score` = VALUES(`auto_score`), `final_score` = VALUES(`final_score`), `teacher_comment` = VALUES(`teacher_comment`), `grade_confirmed` = VALUES(`grade_confirmed`);

-- Granular Student Answers for Reviewing Exam Copies[cite: 21]
INSERT INTO `student_selected_answers` (`selected_answer_id`, `exam_answer_id`, `question_id`, `selected_answer_text`) VALUES
                                                                                                                           (1, 'EA40805', '11001', 'Store long-term hard disk files'),
                                                                                                                           (2, 'EA40805', '11003', 'Prevents all memory allocation'),
                                                                                                                           (3, 'EA40805', '11002', 'Queue'),
                                                                                                                           (4, 'EA40805', '11005', 'O(log n)'),
                                                                                                                           (5, 'EA40805', '11004', 'Faster network socket streaming')
    ON DUPLICATE KEY UPDATE `selected_answer_text` = VALUES(`selected_answer_text`);

-- AI Study Bot Configurations and Knowledge Scopes[cite: 21]
INSERT INTO `course_bot_configs` (`course_id`, `bot_name`, `knowledge_sources`, `is_active`, `last_updated_by`, `last_updated_at`) VALUES
                                                                                                                                       ('CS101', 'CS101 Assistant', 'Course Topics: Binary Search Trees, QuickSort average and worst-case time complexity, recursion depth, and LIFO Stack principles.', 1, NULL, '2026-08-26 16:37:58'),
                                                                                                                                       ('MATH201', 'MATH201 Advanced Helper', 'Course Topics: Matrix inversion, Eigenvalues and Eigenvectors, Systems of Linear Equations, Derivatives, Integrals, and Vector Spaces.', 1, 'teacher1', '2026-08-26 16:44:33')
    ON DUPLICATE KEY UPDATE `bot_name` = VALUES(`bot_name`), `knowledge_sources` = VALUES(`knowledge_sources`), `is_active` = VALUES(`is_active`), `last_updated_by` = VALUES(`last_updated_by`);

-- Seed Bot Interactions for Student History and Anonymous Teacher Stats[cite: 21]
INSERT INTO `bot_interactions` (`interaction_id`, `student_id`, `course_id`, `user_question`, `bot_response`, `timestamp`) VALUES
                                                                                                                               ('BOT-42dc9551', 'student1', 'MATH201', 'what is matrix invesion', 'Matrix inversion is the process of finding a matrix that undoes another matrix...', '2026-08-26 17:48:55'),
                                                                                                                               ('BOT-59e18688', 'student1', 'MATH201', 'what is a matrix', 'A matrix is a rectangular grid of numbers arranged in rows and columns...', '2026-08-26 17:48:07'),
                                                                                                                               ('BOT-7b1c6d2f', 'student1', 'CS101', 'Who won the 2022 World Cup?', 'I am happy to help with your CS101 coursework—feel free to ask a question about Binary Search Trees, QuickSort, or LIFO Stack principles!', '2026-08-26 13:58:59'),
                                                                                                                               ('BOT-7e01d4a3', 'student2', 'CS101', 'what is a binary tree', 'A binary tree is a data structure made up of nodes with up to two children...', '2026-08-26 17:44:12'),
                                                                                                                               ('BOT-caa54d26', 'student1', 'CS101', 'What is an in-order traversal in a Binary Search Tree?', 'An in-order traversal visits left subtree, current node, then right subtree in ascending order...', '2026-08-26 13:58:26'),
                                                                                                                               ('BOT-cb1cbe0b', 'student2', 'MATH201', 'explain the matrix inversion', 'Sorry, I could not come up with a good answer to that right now.', '2026-08-26 16:47:42'),
                                                                                                                               ('BOT-f36b680a', 'student2', 'MATH201', 'explain the matrix inversion', 'Matrix Inversion (MATH201 – Linear Algebra): For a square matrix A, an inverse is A^-1 such that A * A^-1 = I.', '2026-08-26 16:49:10')
    ON DUPLICATE KEY UPDATE `user_question` = VALUES(`user_question`), `bot_response` = VALUES(`bot_response`);

SET FOREIGN_KEY_CHECKS = 1;