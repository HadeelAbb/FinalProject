-- Disable foreign key checks during initialization
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS student_selected_answers;
DROP TABLE IF EXISTS exam_answers;
DROP TABLE IF EXISTS exam_questions;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS exams;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- -------------------------------------------------------------
-- 1. USERS TABLE
-- -------------------------------------------------------------
CREATE TABLE users (
                       username VARCHAR(50) PRIMARY KEY,
                       password VARCHAR(100) NOT NULL,
                       role VARCHAR(30) NOT NULL
);

-- -------------------------------------------------------------
-- 2. EXAMS TABLE (Chunk 1: Includes execution_code & scheduling)
-- -------------------------------------------------------------
CREATE TABLE exams (
                       exam_id VARCHAR(50) PRIMARY KEY,
                       course_id VARCHAR(50) NOT NULL,
                       title VARCHAR(150) NOT NULL,
                       instructions TEXT,
                       duration_minutes INT NOT NULL,
                       status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                       created_by_teacher_id VARCHAR(50) NOT NULL,
                       approved_by_coordinator_id VARCHAR(50) DEFAULT NULL,
                       rejection_reason TEXT DEFAULT NULL,
                       execution_code VARCHAR(4) DEFAULT NULL, -- Chunk 1 requirement
                       scheduled_start DATETIME DEFAULT NULL,
                       scheduled_end DATETIME DEFAULT NULL,
                       FOREIGN KEY (created_by_teacher_id) REFERENCES users(username) ON DELETE CASCADE
);

-- -------------------------------------------------------------
-- 3. QUESTIONS TABLE
-- -------------------------------------------------------------
CREATE TABLE questions (
                           question_id VARCHAR(50) PRIMARY KEY,
                           text TEXT NOT NULL,
                           difficulty VARCHAR(20) NOT NULL,
                           instructions TEXT,
                           topic VARCHAR(100),
                           course_id VARCHAR(50) NOT NULL
);

-- -------------------------------------------------------------
-- 4. EXAM_QUESTIONS (Junction Table)
-- -------------------------------------------------------------
CREATE TABLE exam_questions (
                                exam_id VARCHAR(50) NOT NULL,
                                question_id VARCHAR(50) NOT NULL,
                                question_order INT NOT NULL,
                                PRIMARY KEY (exam_id, question_id),
                                FOREIGN KEY (exam_id) REFERENCES exams(exam_id) ON DELETE CASCADE,
                                FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE
);

-- -------------------------------------------------------------
-- 5. EXAM_ANSWERS TABLE (Chunk 2: Includes final_score & confirmation)
-- -------------------------------------------------------------
CREATE TABLE exam_answers (
                              exam_answer_id VARCHAR(50) PRIMARY KEY,
                              exam_id VARCHAR(50) NOT NULL,
                              student_id VARCHAR(50) NOT NULL,
                              submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              auto_submitted BOOLEAN DEFAULT FALSE,
                              auto_score DOUBLE DEFAULT 0.0,
                              final_score DOUBLE DEFAULT 0.0,
                              teacher_comment TEXT DEFAULT NULL,
                              grade_confirmed BOOLEAN DEFAULT FALSE,
                              UNIQUE KEY unique_student_exam (student_id, exam_id), -- Enforces 1 submission per student
                              FOREIGN KEY (exam_id) REFERENCES exams(exam_id) ON DELETE CASCADE,
                              FOREIGN KEY (student_id) REFERENCES users(username) ON DELETE CASCADE
);

-- -------------------------------------------------------------
-- 6. STUDENT_SELECTED_ANSWERS TABLE
-- -------------------------------------------------------------
CREATE TABLE student_selected_answers (
                                          exam_answer_id VARCHAR(50) NOT NULL,
                                          question_id VARCHAR(50) NOT NULL,
                                          selected_answer_text TEXT,
                                          PRIMARY KEY (exam_answer_id, question_id),
                                          FOREIGN KEY (exam_answer_id) REFERENCES exam_answers(exam_answer_id) ON DELETE CASCADE,
                                          FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE
);

-- -------------------------------------------------------------
-- SEED DATA FOR TESTING
-- -------------------------------------------------------------
INSERT INTO users (username, password, role) VALUES
                                                 ('teacher1', '1234', 'TEACHER'),
                                                 ('coord1', '1234', 'SUBJECT_COORDINATOR'),
                                                 ('student1', '1234', 'STUDENT'),
                                                 ('student2', '1234', 'STUDENT'),
                                                 ('student3', '1234', 'STUDENT');