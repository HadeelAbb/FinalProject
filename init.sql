-- =========================================================================
-- UNIFIED HSTS SYSTEM DATA ARCHITECTURE BLUEPRINT (init.sql)
-- Target Database: hsts_database (Aligned Team Standard)
-- =========================================================================
CREATE DATABASE IF NOT EXISTS hsts_db;
USE hsts_db;

-- Drop existing tables sequentially to respect Foreign Key integrity constraints
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS question_answers;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Create Users Identity Table (The Missing Baseline)
CREATE TABLE users (
                       username VARCHAR(50) PRIMARY KEY,
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(30) DEFAULT 'Teacher',
                       is_logged_in TINYINT(1) DEFAULT 0
);

-- 2. Create Courses Schema Table
CREATE TABLE courses (
                         course_id VARCHAR(10) PRIMARY KEY,
                         name VARCHAR(100) NOT NULL
);

-- 3. Create Questions Schema Table
CREATE TABLE questions (
                           question_id VARCHAR(10) PRIMARY KEY,
                           text TEXT NOT NULL,
                           difficulty VARCHAR(20) NOT NULL,
                           instructions TEXT,
                           topic VARCHAR(100) NOT NULL,
                           course_id VARCHAR(10),
                           FOREIGN KEY (course_id) REFERENCES courses(course_id)
);

-- 4. Create Question Answers Schema Table (Cascades on Deletion)
CREATE TABLE question_answers (
                                  id INT AUTO_INCREMENT PRIMARY KEY,
                                  question_id VARCHAR(10),
                                  answer_text TEXT NOT NULL,
                                  is_correct TINYINT(1) DEFAULT 0,
                                  FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE
);

-- =========================================================================
-- SEED DATA SETUP
-- =========================================================================

-- Seed Identity Account
INSERT INTO users (username, password, role, is_logged_in) VALUES
    ('admin', '123456', 'Teacher', 0);

-- Seed Courses
INSERT INTO courses (course_id, name) VALUES
                                          ('11', 'Introduction to Computer Science'),
                                          ('22', 'Mathematical Logic'),
                                          ('33', 'Database Systems');

-- Seed Core Questions (Fully Synchronized with Live DB)
INSERT INTO questions (question_id, text, difficulty, instructions, topic, course_id) VALUES
                                                                                          ('22001', 'Which of the following propositions is logically equivalent to the conditional statement p -> q?', 'MEDIUM', 'Apply standard logical equivalences.', 'Propositional Logic', '22'),
                                                                                          ('11001', 'What is the time complexity of searching in a perfectly balanced Binary Search Tree (BST)?', 'MEDIUM', 'Choose the single most accurate asymptotic upper bound.', 'Data Structures', '11'),
                                                                                          ('11002', 'Which of the following data structures operates strictly on a Last-In, First-Out (LIFO) basis?', 'EASY', 'Select the correct foundational abstract data type.', 'Data Structures', '11'),
                                                                                          ('11003', 'What occurs when a Java subclass defines a method with the same signature as a superclass method?', 'MEDIUM', 'Assume standard object-oriented programming conventions.', 'Object-Oriented Programming', '11'),
                                                                                          ('11004', 'What is the worst-case time complexity of QuickSort Algorithm?', 'MEDIUM', 'Consider standard comparison-based sorting models.', 'Sorting Algorithms', '11'),
                                                                                          ('11005', 'What is the worst-case time complexity of the optimized edit distance dynamic programming algorithm?', 'HARD', 'Select the single best algorithmic complexity.', 'Dynamic Programming', '11'),
                                                                                          ('33001', 'Which relational algebra operation filters out rows from a table based on a specified condition?', 'HARD', 'Select the fundamental unary operator symbol.', 'Relational Algebra', '33');
-- Seed Answers (Fully Synchronized with Live DB)
INSERT INTO question_answers (question_id, answer_text, is_correct) VALUES
                                                                        ('22001', 'q -> p', 0), ('22001', 'not p or q', 1), ('22001', 'p and not q', 0), ('22001', 'not p and not q', 0),
                                                                        ('11001', 'O(1)', 0), ('11001', 'O(log n)', 1), ('11001', 'O(n)', 0), ('11001', 'O(n log n)', 0),
                                                                        ('11002', 'Queue', 0), ('11002', 'Stack', 1), ('11002', 'Singly Linked List', 0), ('11002', 'Binary Tree', 0),
                                                                        ('11003', 'Method Overloading', 0), ('11003', 'Method Overriding', 1), ('11003', 'Compilation Error', 0), ('11003', 'Encapsulation Violation', 0),
                                                                        ('11004', 'O(n)', 0), ('11004', 'O(n log n)', 0), ('11004', 'O(n^2)', 1), ('11004', 'O(1)', 0),
                                                                        ('11005', 'O(n)', 0), ('11005', 'O(m*n)', 1), ('11005', 'O(2^n)', 0), ('11005', 'O(log n)', 0),
                                                                        ('33001', 'Selection (σ)', 1), ('33001', 'Projection (π)', 0), ('33001', 'Join (⋈)', 0), ('33001', 'Union (∪)', 0);