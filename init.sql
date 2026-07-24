-- Disable foreign key checks during schema creation
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `hsts_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `hsts_db`;

-- 1. Users Table
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
                         `username` VARCHAR(50) NOT NULL,
                         `password` VARCHAR(255) NOT NULL,
                         `role` VARCHAR(30) DEFAULT 'Teacher',
                         `is_logged_in` TINYINT(1) DEFAULT '0',
                         PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. Courses Table
DROP TABLE IF EXISTS `courses`;
CREATE TABLE `courses` (
                           `course_id` VARCHAR(10) NOT NULL,
                           `name` VARCHAR(100) NOT NULL,
                           PRIMARY KEY (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. Questions Bank Table
DROP TABLE IF EXISTS `questions`;
CREATE TABLE `questions` (
                             `question_id` VARCHAR(10) NOT NULL,
                             `text` TEXT NOT NULL,
                             `difficulty` VARCHAR(20) NOT NULL,
                             `instructions` TEXT,
                             `topic` VARCHAR(100) NOT NULL,
                             `course_id` VARCHAR(10) DEFAULT NULL,
                             PRIMARY KEY (`question_id`),
                             KEY `course_id` (`course_id`),
                             CONSTRAINT `questions_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. Question Options & Correct Answers Table
DROP TABLE IF EXISTS `question_answers`;
CREATE TABLE `question_answers` (
                                    `id` INT NOT NULL AUTO_INCREMENT,
                                    `question_id` VARCHAR(10) DEFAULT NULL,
                                    `answer_text` TEXT NOT NULL,
                                    `is_correct` TINYINT(1) DEFAULT '0',
                                    PRIMARY KEY (`id`),
                                    KEY `question_id` (`question_id`),
                                    CONSTRAINT `question_answers_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `questions` (`question_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 5. Exams Metadata Table
DROP TABLE IF EXISTS `exams`;
CREATE TABLE `exams` (
                         `exam_id` VARCHAR(20) NOT NULL,
                         `course_id` VARCHAR(10) NOT NULL,
                         `title` VARCHAR(150) NOT NULL,
                         `instructions` TEXT,
                         `duration_minutes` INT NOT NULL,
                         `status` VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                         `created_by_teacher_id` VARCHAR(50) NOT NULL,
                         `approved_by_coordinator_id` VARCHAR(50) DEFAULT NULL,
                         `rejection_reason` TEXT,
                         `scheduled_start` DATETIME DEFAULT NULL,
                         `scheduled_end` DATETIME DEFAULT NULL,
                         PRIMARY KEY (`exam_id`),
                         KEY `course_id` (`course_id`),
                         CONSTRAINT `exams_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 6. Exam-to-Questions Junction Table
DROP TABLE IF EXISTS `exam_questions`;
CREATE TABLE `exam_questions` (
                                  `exam_id` VARCHAR(20) NOT NULL,
                                  `question_id` VARCHAR(10) NOT NULL,
                                  `question_order` INT NOT NULL,
                                  PRIMARY KEY (`exam_id`,`question_id`),
                                  KEY `question_id` (`question_id`),
                                  CONSTRAINT `exam_questions_ibfk_1` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`exam_id`) ON DELETE CASCADE,
                                  CONSTRAINT `exam_questions_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `questions` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 7. Student Exam Answers & Attempts Table (*UPDATED WITH UNIQUE CONSTRAINT*)
DROP TABLE IF EXISTS `exam_answers`;
CREATE TABLE `exam_answers` (
                                `exam_answer_id` VARCHAR(20) NOT NULL,
                                `exam_id` VARCHAR(20) NOT NULL,
                                `student_id` VARCHAR(50) NOT NULL,
                                `started_at` DATETIME DEFAULT NULL,
                                `submitted_at` DATETIME DEFAULT NULL,
                                `auto_submitted` TINYINT(1) DEFAULT '0',
                                `auto_score` DOUBLE DEFAULT NULL,
                                `final_score` DOUBLE DEFAULT NULL,
                                `teacher_comment` TEXT,
                                `grade_confirmed` TINYINT(1) DEFAULT '0',
                                `extra_minutes_granted` INT DEFAULT '0',
                                PRIMARY KEY (`exam_answer_id`),
                                UNIQUE KEY `unique_student_exam` (`student_id`, `exam_id`), -- Enforces 1 attempt per student
                                KEY `exam_id` (`exam_id`),
                                KEY `student_id` (`student_id`),
                                CONSTRAINT `exam_answers_ibfk_1` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`exam_id`),
                                CONSTRAINT `exam_answers_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `users` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 8. Student Selected Choices Mapping Table
DROP TABLE IF EXISTS `student_selected_answers`;
CREATE TABLE `student_selected_answers` (
                                            `exam_answer_id` VARCHAR(20) NOT NULL,
                                            `question_id` VARCHAR(10) NOT NULL,
                                            `selected_answer_text` TEXT,
                                            PRIMARY KEY (`exam_answer_id`,`question_id`),
                                            CONSTRAINT `student_selected_answers_ibfk_1` FOREIGN KEY (`exam_answer_id`) REFERENCES `exam_answers` (`exam_answer_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 9. AI/Bot Interactions Table
DROP TABLE IF EXISTS `bot_interactions`;
CREATE TABLE `bot_interactions` (
                                    `interaction_id` VARCHAR(50) NOT NULL,
                                    `student_id` VARCHAR(50) NOT NULL,
                                    `course_id` VARCHAR(20) DEFAULT NULL,
                                    `user_question` TEXT NOT NULL,
                                    `bot_response` TEXT NOT NULL,
                                    `timestamp` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
                                    PRIMARY KEY (`interaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =============================================================
-- INITIAL SEED DATA
-- =============================================================

INSERT INTO `users` (`username`, `password`, `role`, `is_logged_in`) VALUES
                                                                         ('admin', '123456', 'Teacher', 0),
                                                                         ('teacher1', 'password123', 'TEACHER', 0),
                                                                         ('teacher2', 'password123', 'TEACHER', 0),
                                                                         ('coord1', 'password123', 'COORDINATOR', 0),
                                                                         ('student1', 'password123', 'STUDENT', 0);

INSERT INTO `courses` (`course_id`, `name`) VALUES
                                                ('11', 'Introduction to Computer Science'),
                                                ('22', 'Mathematical Logic'),
                                                ('33', 'Database Systems');

INSERT INTO `questions` (`question_id`, `text`, `difficulty`, `instructions`, `topic`, `course_id`) VALUES
                                                                                                        ('11001', 'What is the time complexity of searching in a perfectly balanced Binary Search Tree (BST)?', 'MEDIUM', 'Choose the single most accurate asymptotic upper bound.', 'Data Structures', '11'),
                                                                                                        ('11002', 'Which of the following data structures operates strictly on a Last-In, First-Out (LIFO) basis?', 'EASY', 'Select the correct foundational abstract data type.', 'Data Structures', '11'),
                                                                                                        ('11003', 'What occurs when a Java subclass defines a method with the same signature as a superclass method?', 'MEDIUM', 'Assume standard object-oriented programming conventions.', 'Object-Oriented Programming', '11'),
                                                                                                        ('22001', 'Which of the following propositions is logically equivalent to the conditional statement p -> q?', 'MEDIUM', 'Apply standard logical equivalences.', 'Propositional Logic', '22'),
                                                                                                        ('33001', 'Which relational algebra operation filters out rows from a table based on a specified condition?', 'HARD', 'Select the fundamental unary operator symbol.', 'Relational Algebra', '33');

INSERT INTO `question_answers` (`id`, `question_id`, `answer_text`, `is_correct`) VALUES
                                                                                      (1, '22001', 'q -> p', 0),
                                                                                      (2, '22001', 'not p or q', 1),
                                                                                      (3, '22001', 'p and not q', 0),
                                                                                      (4, '22001', 'not p and not q', 0),
                                                                                      (5, '11001', 'O(1)', 0),
                                                                                      (6, '11001', 'O(log n)', 1),
                                                                                      (7, '11001', 'O(n)', 0),
                                                                                      (8, '11001', 'O(n log n)', 0),
                                                                                      (9, '11002', 'Queue', 0),
                                                                                      (10, '11002', 'Stack', 1),
                                                                                      (11, '11002', 'Singly Linked List', 0),
                                                                                      (12, '11002', 'Binary Tree', 0),
                                                                                      (13, '11003', 'Method Overloading', 0),
                                                                                      (14, '11003', 'Method Overriding', 1),
                                                                                      (15, '11003', 'Compilation Error', 0),
                                                                                      (16, '11003', 'Encapsulation Violation', 0),
                                                                                      (25, '33001', 'Selection (σ)', 1),
                                                                                      (26, '33001', 'Projection (π)', 0),
                                                                                      (27, '33001', 'Join (⋈)', 0),
                                                                                      (28, '33001', 'Union (∪)', 0);

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;