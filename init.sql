-- ============================================================================
-- High School Test System (HSTS) - Database Initialization Script
-- Module Version: Final System Integration (Spring 2026)
-- Domain: Database Schema, Repositories, Live Executions, and AI Study Bot
-- ============================================================================
-- OVERVIEW:
-- This script builds the relational database schema for the HSTS central server
-- and populates it with complete seed data for testing all 21 system scenarios.
--
-- SCHEMA TABLES (13 TABLES TOTAL):
-- 1. users                   - User accounts, hashed passwords, roles & contact info
-- 2. courses                 - Academic courses with departmental subject codes
-- 3. teacher_courses         - Teacher-to-course teaching assignments
-- 4. student_courses         - Student-to-course course enrollments
-- 5. questions               - Bank of questions with versioning, topics, & illustration blobs
-- 6. question_answers        - 4-choice options per question with correctness flags
-- 7. exams                   - Exam entities with approval status, versioning & instructions
-- 8. exam_questions          - Exam-to-question mappings with custom positive point weights (sum=100)
-- 9. exam_executions         - Scheduled exam sessions, 4-char security codes & time extensions
-- 10. exam_answers           - Student submission headers, auto/manual grades & teacher comments
-- 11. student_selected_answers - Granular student answer choices recorded per question
-- 12. course_bot_configs     - AI Study Bot configurations and syllabus reference materials
-- 13. bot_interactions       - Student-bot conversation history and query tracking
--
-- SEEDED TEST USERS & CREDENTIALS:
-- • Student 1:     username = 'student1'   | password = '123456' | role = STUDENT
-- • Student 2:     username = 'student2'   | password = '123456' | role = STUDENT
-- • Teacher 1:     username = 'teacher1'   | password = '123456' | role = TEACHER
-- • Teacher 2:     username = 'teacher2'   | password = '123456' | role = TEACHER
-- • Coordinator:   username = 'coord1'     | password = '123456' | role = COORDINATOR
-- • Principal:     username = 'principal1' | password = '123456' | role = PRINCIPAL
--
-- SEEDED COURSES:
-- • CS101:   Computer Science Fundamentals (Subject: CS)
-- • MATH201: Linear Algebra & Calculus (Subject: MATH)
--
-- SEEDED EXAMS & ACTIVE EXECUTIONS:
-- • E72874 (CS101):   APPROVED v1 | Execution: EXSEED01 | Code: HKD6 | 60 min
-- • E27534 (CS101):   APPROVED v2 (lineage of E72874) | Teacher: teacher1 | 60 min
-- • E67832 (CS101):   APPROVED v1 | Execution: EX98637  | Code: ABAB | 10 min
-- • E88101 (MATH201): APPROVED v1 | Execution: EXSEED02 | Code: MTH1 | 90 min (+10 extra min granted)
-- ============================================================================

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- ----------------------------------------------------------------------------
-- 1. USERS TABLE
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
                         `username` varchar(50) NOT NULL,
                         `password` varchar(255) NOT NULL,
                         `full_name` varchar(100) NOT NULL,
                         `role` enum('STUDENT','TEACHER','COORDINATOR','PRINCIPAL') NOT NULL,
                         `email` varchar(100) DEFAULT NULL,
                         PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES
                        ('coord1','123456','Prof. Timothy Lethbridge','COORDINATOR','timothy@school.edu'),
                        ('principal1','123456','Dr. School Principal','PRINCIPAL','principal@school.edu'),
                        ('student1','123456','Alice Smith','STUDENT','alice@school.edu'),
                        ('student2','123456','Bob Miller','STUDENT','bob@school.edu'),
                        ('teacher1','123456','Dr. Robert Laganiere','TEACHER','robert@school.edu'),
                        ('teacher2','123456','Dr. Sarah Connor','TEACHER','sarah@school.edu');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

-- ----------------------------------------------------------------------------
-- 2. COURSES TABLE
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `courses` (
                           `course_id` varchar(10) NOT NULL,
                           `name` varchar(100) NOT NULL,
                           `subject_code` varchar(10) NOT NULL,
                           PRIMARY KEY (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `courses` WRITE;
/*!40000 ALTER TABLE `courses` DISABLE KEYS */;
INSERT INTO `courses` VALUES
                          ('CS101','Computer Science Fundamentals','CS'),
                          ('MATH201','Linear Algebra & Calculus','MATH');
/*!40000 ALTER TABLE `courses` ENABLE KEYS */;
UNLOCK TABLES;

-- ----------------------------------------------------------------------------
-- 3. TEACHER COURSES TABLE
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `teacher_courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_courses` (
                                   `teacher_id` varchar(50) NOT NULL,
                                   `course_id` varchar(10) NOT NULL,
                                   PRIMARY KEY (`teacher_id`,`course_id`),
                                   KEY `course_id` (`course_id`),
                                   CONSTRAINT `teacher_courses_ibfk_1` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`username`) ON DELETE CASCADE,
                                   CONSTRAINT `teacher_courses_ibfk_2` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `teacher_courses` WRITE;
/*!40000 ALTER TABLE `teacher_courses` DISABLE KEYS */;
INSERT INTO `teacher_courses` VALUES
                                  ('teacher1','CS101'),
                                  ('teacher2','CS101'),
                                  ('teacher1','MATH201'),
                                  ('teacher2','MATH201');
/*!40000 ALTER TABLE `teacher_courses` ENABLE KEYS */;
UNLOCK TABLES;

-- ----------------------------------------------------------------------------
-- 4. STUDENT COURSES TABLE
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `student_courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_courses` (
                                   `student_id` varchar(50) NOT NULL,
                                   `course_id` varchar(10) NOT NULL,
                                   PRIMARY KEY (`student_id`,`course_id`),
                                   KEY `course_id` (`course_id`),
                                   CONSTRAINT `student_courses_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `users` (`username`) ON DELETE CASCADE,
                                   CONSTRAINT `student_courses_ibfk_2` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `student_courses` WRITE;
/*!40000 ALTER TABLE `student_courses` DISABLE KEYS */;
INSERT INTO `student_courses` VALUES
                                  ('student1','CS101'),
                                  ('student2','CS101'),
                                  ('student1','MATH201'),
                                  ('student2','MATH201');
/*!40000 ALTER TABLE `student_courses` ENABLE KEYS */;
UNLOCK TABLES;

-- ----------------------------------------------------------------------------
-- 5. QUESTIONS TABLE
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `questions` WRITE;
/*!40000 ALTER TABLE `questions` DISABLE KEYS */;
INSERT INTO `questions` VALUES
                            ('11001','What is the main function of the CPU?','EASY','Select the best single option.','Architecture','CS101',NULL,NULL,'11001',1,1),
                            ('11002','Which data structure follows LIFO order?','EASY','Select the correct memory structure.','Data Structures','CS101',NULL,NULL,'11002',1,1),
                            ('11003','What is the advantage of multi-threading?','MEDIUM','Select performance benefit.','Concurrency','CS101',NULL,NULL,'11003',1,1),
                            ('11004','What does Foreign Key ensure in relational DBs?','MEDIUM','Select relational property.','Databases','CS101',NULL,NULL,'11004',1,1),
                            ('11005','What is binary search time complexity?','HARD','Assume sorted array.','Algorithms','CS101',NULL,NULL,'11005',1,1);
/*!40000 ALTER TABLE `questions` ENABLE KEYS */;
UNLOCK TABLES;

-- ----------------------------------------------------------------------------
-- 6. QUESTION ANSWERS TABLE
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `question_answers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question_answers` (
                                    `answer_id` int NOT NULL AUTO_INCREMENT,
                                    `question_id` varchar(10) NOT NULL,
                                    `answer_text` varchar(255) NOT NULL,
                                    `is_correct` tinyint(1) DEFAULT '0',
                                    PRIMARY KEY (`answer_id`),
                                    KEY `question_id` (`question_id`),
                                    CONSTRAINT `question_answers_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `questions` (`question_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `question_answers` WRITE;
/*!40000 ALTER TABLE `question_answers` DISABLE KEYS */;
INSERT INTO `question_answers` VALUES
                                   (1,'11001','Execute instructions and perform arithmetic/logic operations',1),
                                   (2,'11001','Store long-term hard disk files',0),
                                   (3,'11002','Stack',1),
                                   (4,'11002','Queue',0),
                                   (5,'11003','Allows concurrent execution of tasks to maximize CPU utilization',1),
                                   (6,'11003','Prevents all memory allocation',0),
                                   (7,'11004','Referential Integrity between tables',1),
                                   (8,'11004','Faster network socket streaming',0),
                                   (9,'11005','O(log n)',1),
                                   (10,'11005','O(1)',0);
/*!40000 ALTER TABLE `question_answers` ENABLE KEYS */;
UNLOCK TABLES;

-- ----------------------------------------------------------------------------
-- 7. EXAMS TABLE
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `exams`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `exams` WRITE;
/*!40000 ALTER TABLE `exams` DISABLE KEYS */;
INSERT INTO `exams` VALUES
                        ('E27534','CS101','Test-exam','NEWComplete all questions within duration.',NULL,60,'teacher1','APPROVED',NULL,'coord1',NULL,NULL,NULL,'E72874',2,1),
                        ('E67832','CS101','SystestingExam','This is how we try the system for exam building :)','let see if students never see this ',10,'teacher1','APPROVED',NULL,'coord1',NULL,NULL,NULL,'E67832',1,1),
                        ('E72874','CS101','Test-exam','Complete all questions within duration.',NULL,60,'teacher1','APPROVED',NULL,NULL,'HKD6','2026-08-19 17:06:43','2026-09-03 17:06:43','E72874',1,0),
                        ('E88101','MATH201','Calculus Midterm','Answer all calculus questions.',NULL,90,'teacher2','APPROVED',NULL,NULL,'MTH1','2026-08-25 17:12:58','2026-09-09 17:12:58','E88101',1,1);
/*!40000 ALTER TABLE `exams` ENABLE KEYS */;
UNLOCK TABLES;

-- ----------------------------------------------------------------------------
-- 8. EXAM QUESTIONS TABLE
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `exam_questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `exam_questions` WRITE;
/*!40000 ALTER TABLE `exam_questions` DISABLE KEYS */;
INSERT INTO `exam_questions` VALUES
                                 ('E27534','11001',1,20),
                                 ('E27534','11002',2,20),
                                 ('E27534','11003',3,20),
                                 ('E27534','11004',4,20),
                                 ('E27534','11005',5,20),
                                 ('E67832','11001',1,30),
                                 ('E67832','11002',2,25),
                                 ('E67832','11003',3,20),
                                 ('E67832','11004',4,25),
                                 ('E72874','11001',1,20),
                                 ('E72874','11002',2,20),
                                 ('E72874','11003',3,20),
                                 ('E72874','11004',4,20),
                                 ('E72874','11005',5,20);
/*!40000 ALTER TABLE `exam_questions` ENABLE KEYS */;
UNLOCK TABLES;

-- ----------------------------------------------------------------------------
-- 9. EXAM EXECUTIONS TABLE
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `exam_executions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `exam_executions` WRITE;
/*!40000 ALTER TABLE `exam_executions` DISABLE KEYS */;
INSERT INTO `exam_executions` VALUES
                                  ('EX98637','E67832','ABAB','2026-08-26 19:50:00','2026-09-30 23:59:00',0,'teacher1'),
                                  ('EXSEED01','E72874','HKD6','2026-08-24 17:34:22','2026-09-09 17:34:22',0,'teacher1'),
                                  ('EXSEED02','E88101','MTH1','2026-08-24 17:34:22','2026-09-09 17:34:22',10,'teacher2');
/*!40000 ALTER TABLE `exam_executions` ENABLE KEYS */;
UNLOCK TABLES;

-- ----------------------------------------------------------------------------
-- 10. EXAM ANSWERS TABLE
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `exam_answers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `exam_answers` WRITE;
/*!40000 ALTER TABLE `exam_answers` DISABLE KEYS */;
INSERT INTO `exam_answers` VALUES
                               ('ANS_MATH_01','E88101','EXSEED02','student1',88,92,'Solid work on matrix operations.',1,'2026-08-26 17:12:58',0),
                               ('EA40805','E72874','EXSEED01','student1',20,25,'Try harder',1,'2026-08-26 17:29:01',0);
/*!40000 ALTER TABLE `exam_answers` ENABLE KEYS */;
UNLOCK TABLES;

-- ----------------------------------------------------------------------------
-- 11. STUDENT SELECTED ANSWERS TABLE
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `student_selected_answers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `student_selected_answers` WRITE;
/*!40000 ALTER TABLE `student_selected_answers` DISABLE KEYS */;
INSERT INTO `student_selected_answers` VALUES
                                           (1,'EA40805','11001','Store long-term hard disk files'),
                                           (2,'EA40805','11003','Prevents all memory allocation'),
                                           (3,'EA40805','11002','Queue'),
                                           (4,'EA40805','11005','O(log n)'),
                                           (5,'EA40805','11004','Faster network socket streaming');
/*!40000 ALTER TABLE `student_selected_answers` ENABLE KEYS */;
UNLOCK TABLES;

-- ----------------------------------------------------------------------------
-- 12. COURSE BOT CONFIGS TABLE
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `course_bot_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `course_bot_configs` WRITE;
/*!40000 ALTER TABLE `course_bot_configs` DISABLE KEYS */;
INSERT INTO `course_bot_configs` VALUES
                                     ('CS101','CS101 Assistant','Course Topics: Binary Search Trees, QuickSort average and worst-case time complexity, recursion depth, and LIFO Stack principles.',1,NULL,'2026-08-26 16:37:58'),
                                     ('MATH201','MATH201 Advanced Helper','Course Topics: Matrix inversion, Eigenvalues and Eigenvectors, Systems of Linear Equations, Derivatives, Integrals, and Vector Spaces.',1,'teacher1','2026-08-26 16:44:33');
/*!40000 ALTER TABLE `course_bot_configs` ENABLE KEYS */;
UNLOCK TABLES;

-- ----------------------------------------------------------------------------
-- 13. BOT INTERACTIONS TABLE
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `bot_interactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `bot_interactions` WRITE;
/*!40000 ALTER TABLE `bot_interactions` DISABLE KEYS */;
INSERT INTO `bot_interactions` VALUES
                                   ('BOT-42dc9551','student1','MATH201','what is matrix invesion','**Matrix inversion** is the process of finding a matrix that “undoes” another matrix.  \nIf you have a square matrix \\(A\\) (the same number of rows and columns), its inverse—written \\(A^{-1}\\)—satisfies\n\n\\[\nA\\,A^{-1} = A^{-1}\\,A = I,\n\\]\n\nwhere \\(I\\) is the identity matrix (1’s on the diagonal, 0’s elsewhere).  \nIn other words, multiplying a matrix by its inverse gives you the identity matrix, just like dividing by a number gives you 1.\n\nKey points for a high‑school student:\n\n1. **Only square matrices can have inverses.**  \n   A non‑square matrix (more rows than columns or vice versa) can’t be inverted in the usual sense.\n\n2. **Not all square matrices are invertible.**  \n   A matrix is invertible only if it is *non‑singular* (its determinant is not zero).  \n   For a \\(2\\times2\\) matrix  \n   \\[\n   \\begin{pmatrix} a & b\\\\ c & d \\end{pmatrix},\n   \\]\n   the determinant is \\(ad - bc\\). If \\(ad - bc = 0\\), the matrix has no inverse.\n\n3. **Finding the inverse.**  \n   - For \\(2\\times2\\) matrices, use the formula  \n     \\[\n     \\begin{pmatrix} a & b\\\\ c & d \\end{pmatrix}^{-1}\n     = \\frac{1}{ad-bc}\n     \\begin{pmatrix} d & -b\\\\ -c & a \\end{pmatrix}.\n     \\]\n   - For larger matrices, you can use methods such as:\n     * **Gauss–Jordan elimination** on the augmented matrix \\([A \\;|\\; I]\\).\n     * **Adjugate‑and‑determinant** formula (more work, but good for understanding).\n     * Row‑reduction or a calculator/software.\n\n4. **Why it matters.**  \n   The inverse lets you solve systems of linear equations.','2026-08-26 17:48:55'),
                                   ('BOT-59e18688','student1','MATH201','what is a matrix','A **matrix** is just a rectangular grid (or table) of numbers, symbols, or expressions arranged in rows and columns.  \n- Think of it as a list of lists: each inner list is a **row**, and each column runs vertically.  \n- The size (or **shape**) of a matrix is written as *m × n*, where *m* is the number of rows and *n* is the number of columns.  \n- For example, a 2 × 3 matrix looks like  \n  \\[\n  \\begin{bmatrix}\n  1 & 2 & 3\\\\\n  4 & 5 & 6\n  \\end{bmatrix}\n  \\]\n- Matrices are used to represent data, linear transformations, systems of equations, and more.  \n- In MATH201, you’ll see matrices used for **matrix inversion** and solving **systems of linear equations**.  \n\nSo, a matrix is just a convenient way to organize numbers in rows and columns so we can perform algebraic operations on them.','2026-08-26 17:48:07'),
                                   ('BOT-7b1c6d2f','student1','CS101','Who won the 2022 World Cup?','I’m happy to help with your CS101 coursework—feel free to ask a question about Binary Search Trees, QuickSort recursion depth, or LIFO Stack principles!','2026-08-26 13:58:59'),
                                   ('BOT-7e01d4a3','student2','CS101','what is a binary tree','A **binary tree** is a simple data structure made up of “nodes.”  \n- Each node can have **zero, one, or two** children.  \n- If a node has children, one is called the **left child** and the other the **right child**.  \n- The topmost node is called the **root**.  \n- Nodes with no children are called **leaf nodes**.  \n\nSo, a binary tree is just a collection of nodes connected in a parent‑to‑child hierarchy where no node has more than two children. It’s the foundation for more advanced structures like binary search trees, heaps, and expression trees.','2026-08-26 17:44:12'),
                                   ('BOT-c7f4bfaf','student1','CS101','who won the world cup in 2026?','I’m sorry, but I can’t help with that. If you have any questions about your CS101 coursework—like Binary Search Trees, QuickSort, recursion, or stack concepts—feel free to ask!','2026-08-26 20:02:48'),
                                   ('BOT-caa54d26','student1','CS101','What is an in-order traversal in a Binary Search Tree?','An **in‑order traversal** is a way to visit every node in a binary search tree (BST) so that the values come out in sorted (ascending) order.\n\n**How it works**\n\n1. **Traverse the left subtree** – go as far left as possible and visit nodes there first.  \n2. **Visit the current node** – after finishing the left side, look at the node you’re on and record its value.  \n3. **Traverse the right subtree** – finally, visit all nodes in the right subtree.\n\nYou can think of it as: *left → node → right*.  \n\n**Why it sorts the values**\n\nBecause a BST stores all smaller values in the left subtree and larger values in the right subtree, this left‑node‑right order naturally lists the numbers from smallest to largest.\n\n**Example (pseudocode)**\n\n```python\ndef inorder(node):\n    if node is None:\n        return\n    inorder(node.left)       # 1. left\n    print(node.value)        # 2. node\n    inorder(node.right)      # 3. right\n```\n\nRunning `inorder(root)` on a BST will print the elements in ascending order.','2026-08-26 13:58:26'),
                                   ('BOT-cb1cbe0b','student2','MATH201','explain the matrix inversion','Sorry, I couldn\'t come up with a good answer to that right now. Please try rephrasing your question, or check with your instructor.','2026-08-26 16:47:42'),
('BOT-cffc811f','student1','CS101','what is the muffin recipe?','I’m sorry, but I can’t help with recipes. If you have any questions about CS101 topics—like binary search trees, QuickSort complexity, recursion depth, or stack principles—feel free to ask!','2026-08-26 20:06:54'),
('BOT-f36b680a','student2','MATH201','explain the matrix inversion','### Matrix Inversion (MATH201 – Linear Algebra)\n\n**What is a matrix inverse?**  \nFor a square matrix \\(A\\) (same number of rows and columns), an *inverse* is another matrix \\(A^{-1}\\) such that when you multiply them together you get the identity matrix \\(I\\):\n\n\\[\nA\\,A^{-1} = A^{-1}\\,A = I.\n\\]\n\nThe identity matrix \\(I\\) looks like this for a \\(3\\times3\\) matrix:\n\n\\[\nI=\\begin{pmatrix}\n1 & 0 & 0\\\\\n0 & 1 & 0\\\\\n0 & 0 & 1\n\\end{pmatrix}\n\\]\n\nIt behaves like “1” for matrices: any matrix multiplied by \\(I\\) stays unchanged.\n\n---\n\n#### 1. **When does an inverse exist?**\n- Only for **square** matrices (e.g., \\(2\\times2\\), \\(3\\times3\\), …).\n- The matrix must be **invertible** (or *non‑singular*).  \n  A quick check: compute its determinant \\(\\det(A)\\).  \n  - If \\(\\det(A) \\neq 0\\) → inverse exists.  \n  - If \\(\\det(A) = 0\\) → no inverse (the matrix is *singular*).\n\n---\n\n#### 2. **How to find the inverse**\n\n**a) For a \\(2\\times2\\) matrix**\n\nIf\n\n\\[\nA=\\begin{pmatrix}\na & b\\\\\nc & d\n\\end{pmatrix},\n\\quad \\det(A)=ad-bc,\n\\]\n\nthen\n\n\\[\nA^{-1}=\\frac{1}{ad-bc}\\begin{pmatrix}\nd & -b\\\\\n-c & a\n\\end{pmatrix}.\n\\]\n\n**b) For larger matrices**\n\n1. **Adjugate (Adjoint) method**  \n   - Compute the **cofactor** matrix \\(C\\).  \n   - Transpose \\(C\\) to get the **adjugate** \\(\\operatorname{adj}(A)\\).  \n   - Divide by the determinant.','2026-08-26 16:49:10');
/*!40000 ALTER TABLE `bot_interactions` ENABLE KEYS */;
UNLOCK TABLES;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- ============================================================================
-- End of Database Initialization Script
-- ============================================================================