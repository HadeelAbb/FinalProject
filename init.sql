-- ============================================================================
-- High School Test System (HSTS) - Database Initialization Script
-- Module Version: Complete Multi-Course System Integration (Spring 2026)
-- Domain: Database Schema, Repositories, Live Executions, and AI Study Bot
-- ============================================================================
-- OVERVIEW:
-- This script builds the relational database schema for the HSTS central server
-- and populates it with comprehensive seed data for testing all system scenarios.
--
-- SCHEMA TABLES (13 TABLES TOTAL):
-- 1. users                   - User accounts, hashed passwords, roles & contact info
-- 2. courses                 - Academic courses with departmental subject codes
-- 3. teacher_courses         - Teacher-to-course teaching assignments
-- 4. student_courses         - Student-to-course course enrollments
-- 5. questions               - Bank of questions with versioning, topics, & illustration blobs
-- 6. question_answers        - 4-choice options per question with correctness flags
-- 7. exams                   - Exam entities with approval status, versioning & instructions
-- 8. exam_questions          - Exam-to-question mappings with positive point weights (sum=100)
-- 9. exam_executions         - Scheduled exam sessions, 4-char security codes & time extensions
-- 10. exam_answers           - Student submission headers, auto/manual grades & teacher comments
-- 11. student_selected_answers - Granular student answer choices recorded per question
-- 12. course_bot_configs     - AI Study Bot configurations and syllabus reference materials
-- 13. bot_interactions       - Student-bot conversation history and query tracking
--
-- SEEDED TEST USERS & CREDENTIALS (ALL PASSWORDS = '123456'):
-- • Coordinator:   username = 'coord1'     | role = COORDINATOR | Prof. Timothy Lethbridge
-- • Principal:     username = 'principal1' | role = PRINCIPAL   | Dr. School Principal
-- • Teacher 1:     username = 'teacher1'   | role = TEACHER     | Dr. Robert Laganiere
-- • Teacher 2:     username = 'teacher2'   | role = TEACHER     | Dr. Sarah Connor
-- • Teacher 3:     username = 'teacher3'   | role = TEACHER     | Dr. Alan Turing
-- • Teacher 4:     username = 'teacher4'   | role = TEACHER     | Dr. Grace Hopper
-- • Student 1:     username = 'student1'   | role = STUDENT     | Alice Smith
-- • Student 2:     username = 'student2'   | role = STUDENT     | Bob Miller
-- • Student 3:     username = 'student3'   | role = STUDENT     | Charlie Brown
-- • Student 4:     username = 'student4'   | role = STUDENT     | Diana Prince
-- • Student 5:     username = 'student5'   | role = STUDENT     | Evan Wright
--
-- SEEDED COURSES (4 COURSES):
-- • CS101:   Computer Science Fundamentals (Subject: CS)
-- • MATH201: Linear Algebra & Calculus (Subject: MATH)
-- • BIO301:  General Biology & Genetics (Subject: BIO)
-- • ENG102:  Academic Writing & Composition (Subject: ENG)
--
-- SEEDED QUESTION BANKS (24 QUESTIONS TOTAL — 6 PER COURSE, 4 OPTIONS EACH):
-- • CS101:   11001 to 11006 (CPU, Stacks, Threads, Foreign Keys, Binary Search, QuickSort)
-- • MATH201: 21001 to 21006 (Invertibility, Multiplication, Derivatives, Determinants, Eigenvectors, Integrals)
-- • BIO301:  31001 to 31006 (Mitochondria, Base Pairing, Metaphase, Glycolysis, Punnett Ratios, tRNA)
-- • ENG102:  41001 to 41006 (Thesis Placement, Active Voice, Transitions, MLA Citations, Straw Man, Ethos)
--
-- SEEDED EXAMS & ACTIVE EXECUTIONS (SUM = 100 POINTS EACH):
-- • E72874 (CS101):   APPROVED v1 | Execution: EXSEED01 | Code: HKD6 | 60 min (teacher1)
-- • E88101 (MATH201): APPROVED v1 | Execution: EXSEED02 | Code: MTH1 | 90 min (+10 extra min) (teacher2)
-- • E33001 (BIO301):  APPROVED v1 | Execution: EXBIO01  | Code: BIO1 | 45 min (teacher3)
-- • E44001 (ENG102):  APPROVED v1 | Execution: EXENG01  | Code: ENG2 | 60 min (teacher4)
--
-- SEEDED BOT CONFIGURATIONS (WITH DETAILED KNOWLEDGE SOURCES):
-- • CS101:   CS101 Assistant        | BSTs, QuickSort Big-O, Stack Frames, Relational ACID
-- • MATH201: MATH201 Advanced Helper| Matrix Inverses, Determinant Checks, Eigenvalues, Chain Rule
-- • BIO301:  BIO301 BioBot          | Genetics, Mendelian Ratios, Mitosis Stages, Glycolysis
-- • ENG102:  ENG102 WritingCoach    | Thesis Formatting, Active Voice, MLA Rules, Fallacies
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
                        ('teacher1','123456','Dr. Robert Laganiere','TEACHER','robert@school.edu'),
                        ('teacher2','123456','Dr. Sarah Connor','TEACHER','sarah@school.edu'),
                        ('teacher3','123456','Dr. Alan Turing','TEACHER','alan@school.edu'),
                        ('teacher4','123456','Dr. Grace Hopper','TEACHER','grace@school.edu'),
                        ('student1','123456','Alice Smith','STUDENT','alice@school.edu'),
                        ('student2','123456','Bob Miller','STUDENT','bob@school.edu'),
                        ('student3','123456','Charlie Brown','STUDENT','charlie@school.edu'),
                        ('student4','123456','Diana Prince','STUDENT','diana@school.edu'),
                        ('student5','123456','Evan Wright','STUDENT','evan@school.edu');
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
                          ('MATH201','Linear Algebra & Calculus','MATH'),
                          ('BIO301','General Biology & Genetics','BIO'),
                          ('ENG102','Academic Writing & Composition','ENG');
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
                                  ('teacher3','CS101'),
                                  ('teacher1','MATH201'),
                                  ('teacher2','MATH201'),
                                  ('teacher4','MATH201'),
                                  ('teacher3','BIO301'),
                                  ('teacher4','ENG102');
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
                                  ('student3','CS101'),
                                  ('student1','MATH201'),
                                  ('student2','MATH201'),
                                  ('student4','MATH201'),
                                  ('student1','BIO301'),
                                  ('student3','BIO301'),
                                  ('student5','BIO301'),
                                  ('student2','ENG102'),
                                  ('student4','ENG102'),
                                  ('student5','ENG102');
/*!40000 ALTER TABLE `student_courses` ENABLE KEYS */;
UNLOCK TABLES;

-- ----------------------------------------------------------------------------
-- 5. QUESTIONS TABLE (6 per course, 24 total)
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
-- CS101 Questions
('11001','What is the main function of the CPU?','EASY','Select the best single option.','Architecture','CS101',NULL,NULL,'11001',1,1),
('11002','Which data structure follows LIFO order?','EASY','Select the correct memory structure.','Data Structures','CS101',NULL,NULL,'11002',1,1),
('11003','What is the advantage of multi-threading?','MEDIUM','Select performance benefit.','Concurrency','CS101',NULL,NULL,'11003',1,1),
('11004','What does Foreign Key ensure in relational DBs?','MEDIUM','Select relational property.','Databases','CS101',NULL,NULL,'11004',1,1),
('11005','What is binary search time complexity?','HARD','Assume a sorted array.','Algorithms','CS101',NULL,NULL,'11005',1,1),
('11006','What is the worst-case time complexity of QuickSort?','HARD','Assume unbalanced partition.','Algorithms','CS101',NULL,NULL,'11006',1,1),

-- MATH201 Questions
('21001','Under what condition is a square matrix A invertible?','EASY','Select condition.','Linear Algebra','MATH201',NULL,NULL,'21001',1,1),
('21002','For matrix multiplication AB to be defined, what must match?','EASY','Select dimension rule.','Linear Algebra','MATH201',NULL,NULL,'21002',1,1),
('21003','What is the derivative of f(x) = x^3 - 4x + 7?','MEDIUM','Apply power rule.','Calculus','MATH201',NULL,NULL,'21003',1,1),
('21004','What is the determinant of matrix [[3, 2], [1, 4]]?','MEDIUM','Compute ad - bc.','Linear Algebra','MATH201',NULL,NULL,'21004',1,1),
('21005','If Av = λv for non-zero vector v, what is λ called?','HARD','Select spectral term.','Linear Algebra','MATH201',NULL,NULL,'21005',1,1),
('21006','What is the indefinite integral of (2x + 5) dx?','HARD','Apply integration rules.','Calculus','MATH201',NULL,NULL,'21006',1,1),

-- BIO301 Questions
('31001','Which organelle is known as the powerhouse of the eukaryotic cell?','EASY','Select organelle.','Cell Biology','BIO301',NULL,NULL,'31001',1,1),
('31002','In double-stranded DNA, adenine (A) pairs with which base?','EASY','Select base pairing.','Genetics','BIO301',NULL,NULL,'31002',1,1),
('31003','During which phase of mitosis do chromosomes align at the equatorial plane?','MEDIUM','Select cell cycle stage.','Cell Division','BIO301',NULL,NULL,'31003',1,1),
('31004','What is the primary organic end-product of glycolysis?','MEDIUM','Select molecule.','Metabolism','BIO301',NULL,NULL,'31004',1,1),
('31005','In a monohybrid cross of two heterozygous parents (Aa x Aa), what is the expected phenotypic ratio for a dominant trait?','HARD','Assume complete dominance.','Genetics','BIO301',NULL,NULL,'31005',1,1),
('31006','What molecule carries amino acids to the ribosome during translation?','HARD','Select RNA type.','Molecular Biology','BIO301',NULL,NULL,'31006',1,1),

-- ENG102 Questions
('41001','Where is the thesis statement most traditionally placed in an academic essay?','EASY','Select structure location.','Composition','ENG102',NULL,NULL,'41001',1,1),
('41002','Which sentence is written in the active voice?','EASY','Identify voice.','Grammar','ENG102',NULL,NULL,'41002',1,1),
('41003','What is the primary function of a transitional phrase in an essay?','MEDIUM','Select purpose.','Composition','ENG102',NULL,NULL,'41003',1,1),
('41004','In MLA format, how is an in-text citation correctly formatted for author John Smith, page 45?','MEDIUM','Select citation format.','Research','ENG102',NULL,NULL,'41004',1,1),
('41005','Which logical fallacy occurs when an opponent attacks a distorted version of an argument?','HARD','Select fallacy.','Rhetoric','ENG102',NULL,NULL,'41005',1,1),
('41006','In classical rhetoric, which appeal relies on establishing the speaker credibility?','HARD','Select rhetorical appeal.','Rhetoric','ENG102',NULL,NULL,'41006',1,1);
/*!40000 ALTER TABLE `questions` ENABLE KEYS */;
UNLOCK TABLES;

-- ----------------------------------------------------------------------------
-- 6. QUESTION ANSWERS TABLE (4 options per question, 96 rows total)
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
) ENGINE=InnoDB AUTO_INCREMENT=97 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `question_answers` WRITE;
/*!40000 ALTER TABLE `question_answers` DISABLE KEYS */;
INSERT INTO `question_answers` VALUES
-- CS101 Answers (11001 - 11006)
(1,'11001','Execute instructions and perform arithmetic/logic operations',1),
(2,'11001','Store long-term hard disk files permanently',0),
(3,'11001','Cool the motherboard power components',0),
(4,'11001','Manage physical network cabling switches',0),
(5,'11002','Stack',1),
(6,'11002','Queue',0),
(7,'11002','Binary Search Tree',0),
(8,'11002','Hash Map',0),
(9,'11003','Allows concurrent execution of tasks to maximize CPU utilization',1),
(10,'11003','Prevents all heap memory allocation',0),
(11,'11003','Eliminates the need for operating system interrupts',0),
(12,'11003','Automatically doubles GPU clock speed',0),
(13,'11004','Referential integrity between related database tables',1),
(14,'11004','Faster network socket streaming',0),
(15,'11004','Automatic data encryption at rest',0),
(16,'11004','Immediate garbage collection in the JVM',0),
(17,'11005','O(log n)',1),
(18,'11005','O(1)',0),
(19,'11005','O(n^2)',0),
(20,'11005','O(n log n)',0),
(21,'11006','O(n^2)',1),
(22,'11006','O(log n)',0),
(23,'11006','O(n)',0),
(24,'11006','O(1)',0),

-- MATH201 Answers (21001 - 21006)
(25,'21001','Its determinant is non-zero (det(A) != 0)',1),
(26,'21001','Its determinant is equal to zero (det(A) = 0)',0),
(27,'21001','All elements on the main diagonal must be negative',0),
(28,'21001','The matrix must have strictly odd dimensions',0),
(29,'21002','Number of columns in A must equal number of rows in B',1),
(30,'21002','A and B must have identical row dimensions only',0),
(31,'21002','Both matrices must be square and diagonal',0),
(32,'21002','Determinant of A must equal determinant of B',0),
(33,'21003','3x^2 - 4',1),
(34,'21003','3x^2 - 4x',0),
(35,'21003','x^2 - 4',0),
(36,'21003','3x^3 - 4',0),
(37,'21004','10',1),
(38,'21004','14',0),
(39,'21004','-2',0),
(40,'21004','6',0),
(41,'21005','Eigenvalue',1),
(42,'21005','Characteristic Polynomial',0),
(43,'21005','Determinant Trace',0),
(44,'21005','Orthogonal Basis',0),
(45,'21006','x^2 + 5x + C',1),
(46,'21006','2x^2 + 5x + C',0),
(47,'21006','x^2 + C',0),
(48,'21006','2 + C',0),

-- BIO301 Answers (31001 - 31006)
(49,'31001','Mitochondria',1),
(50,'31001','Ribosome',0),
(51,'31001','Endoplasmic Reticulum',0),
(52,'31001','Golgi Apparatus',0),
(53,'31002','Thymine (T)',1),
(54,'31002','Cytosine (C)',0),
(55,'31002','Guanine (G)',0),
(56,'31002','Uracil (U)',0),
(57,'31003','Metaphase',1),
(58,'31003','Prophase',0),
(59,'31003','Anaphase',0),
(60,'31003','Telophase',0),
(61,'31004','Pyruvate',1),
(62,'31004','Glucose-6-phosphate',0),
(63,'31004','Lactic Acid',0),
(64,'31004','Ribose',0),
(65,'31005','3:1',1),
(66,'31005','1:2:1',0),
(67,'31005','9:3:3:1',0),
(68,'31005','1:1',0),
(69,'31006','tRNA (Transfer RNA)',1),
(70,'31006','mRNA (Messenger RNA)',0),
(71,'31006','rRNA (Ribosomal RNA)',0),
(72,'31006','snRNA (Small Nuclear RNA)',0),

-- ENG102 Answers (41001 - 41006)
(73,'41001','At the end of the introductory paragraph',1),
(74,'41001','In the exact middle of the second body paragraph',0),
(75,'41001','As the first sentence of the Works Cited page',0),
(76,'41001','Inside the title of the paper',0),
(77,'41002','The researcher analyzed the experimental data.',1),
(78,'41002','The experimental data was analyzed by the researcher.',0),
(79,'41002','A decision was reached by the committee members.',0),
(80,'41002','The book had been forgotten by everyone.',0),
(81,'41003','To connect logical ideas and guide the reader between sentences and paragraphs',1),
(82,'41003','To increase the word count arbitrarily',0),
(83,'41003','To replace direct textual evidence and citations',0),
(84,'41003','To introduce unverified claims',0),
(85,'41004','(Smith 45)',1),
(86,'41004','(Smith, p. 45)',0),
(87,'41004','[Smith: 45]',0),
(88,'41004','(John Smith, 45)',0),
(89,'41005','Straw Man Fallacy',1),
(90,'41005','Ad Hominem',0),
(91,'41005','Post Hoc Ergo Propter Hoc',0),
(92,'41005','Circular Reasoning',0),
(93,'41006','Ethos',1),
(94,'41006','Pathos',0),
(95,'41006','Logos',0),
(96,'41006','Kairos',0);
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
                        ('E72874','CS101','CS Fundamentals Midterm','Complete all 6 multiple choice questions within 60 minutes.','Standard CS benchmark exam.',60,'teacher1','APPROVED',NULL,'coord1','HKD6','2026-08-19 17:00:00','2026-09-15 23:59:00','E72874',1,1),
                        ('E88101','MATH201','Calculus & Linear Algebra Final','Answer all 5 questions. Scratch paper allowed.','Ensure students show step-by-step logic.',90,'teacher2','APPROVED',NULL,'coord1','MTH1','2026-08-25 17:00:00','2026-09-20 23:59:00','E88101',1,1),
                        ('E33001','BIO301','Genetics & Cell Biology Test','Answer all 5 questions carefully.','Focus on molecular biology concepts.',45,'teacher3','APPROVED',NULL,'coord1','BIO1','2026-08-26 10:00:00','2026-09-25 23:59:00','E33001',1,1),
                        ('E44001','ENG102','Composition & Rhetoric Assessment','Read each question thoroughly before selecting answer.','Evaluate argumentative synthesis.',60,'teacher4','APPROVED',NULL,'coord1','ENG2','2026-08-26 12:00:00','2026-09-25 23:59:00','E44001',1,1);
/*!40000 ALTER TABLE `exams` ENABLE KEYS */;
UNLOCK TABLES;

-- ----------------------------------------------------------------------------
-- 8. EXAM QUESTIONS TABLE (Weights sum exactly to 100 points per exam)
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
-- CS101 Exam E72874 (20 + 15 + 15 + 15 + 20 + 15 = 100 points)
('E72874','11001',1,20),
('E72874','11002',2,15),
('E72874','11003',3,15),
('E72874','11004',4,15),
('E72874','11005',5,20),
('E72874','11006',6,15),

-- MATH201 Exam E88101 (20 * 5 = 100 points)
('E88101','21001',1,20),
('E88101','21002',2,20),
('E88101','21003',3,20),
('E88101','21004',4,20),
('E88101','21005',5,20),

-- BIO301 Exam E33001 (20 * 5 = 100 points)
('E33001','31001',1,20),
('E33001','31002',2,20),
('E33001','31003',3,20),
('E33001','31004',4,20),
('E33001','31005',5,20),

-- ENG102 Exam E44001 (20 * 5 = 100 points)
('E44001','41001',1,20),
('E44001','41002',2,20),
('E44001','41003',3,20),
('E44001','41004',4,20),
('E44001','41005',5,20);
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
                                  ('EXSEED01','E72874','HKD6','2026-08-24 17:00:00','2026-09-15 23:59:00',0,'teacher1'),
                                  ('EXSEED02','E88101','MTH1','2026-08-24 17:00:00','2026-09-20 23:59:00',10,'teacher2'),
                                  ('EXBIO01','E33001','BIO1','2026-08-26 10:00:00','2026-09-25 23:59:00',0,'teacher3'),
                                  ('EXENG01','E44001','ENG2','2026-08-26 12:00:00','2026-09-25 23:59:00',0,'teacher4');
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
                               ('ANS_CS_01','E72874','EXSEED01','student1',85,85,'Great overall grasp of algorithms and memory.',1,'2026-08-26 17:29:01',0),
                               ('ANS_CS_02','E72874','EXSEED01','student2',70,75,'Review QuickSort worst-case scenarios.',1,'2026-08-26 18:10:22',0),
                               ('ANS_MATH_01','E88101','EXSEED02','student1',88,92,'Solid work on matrix operations.',1,'2026-08-26 17:12:58',0),
                               ('ANS_BIO_01','E33001','EXBIO01','student3',100,100,'Flawless understanding of genetics.',1,'2026-08-26 19:40:15',0),
                               ('ANS_ENG_01','E44001','EXENG01','student4',80,85,'Good rhetoric skills.',1,'2026-08-26 19:55:00',0);
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
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `student_selected_answers` WRITE;
/*!40000 ALTER TABLE `student_selected_answers` DISABLE KEYS */;
INSERT INTO `student_selected_answers` VALUES
-- student1 on CS101 Exam E72874
(1,'ANS_CS_01','11001','Execute instructions and perform arithmetic/logic operations'),
(2,'ANS_CS_01','11002','Stack'),
(3,'ANS_CS_01','11003','Allows concurrent execution of tasks to maximize CPU utilization'),
(4,'ANS_CS_01','11004','Referential integrity between related database tables'),
(5,'ANS_CS_01','11005','O(log n)'),
(6,'ANS_CS_01','11006','O(n)'),

-- student1 on MATH201 Exam E88101
(7,'ANS_MATH_01','21001','Its determinant is non-zero (det(A) != 0)'),
(8,'ANS_MATH_01','21002','Number of columns in A must equal number of rows in B'),
(9,'ANS_MATH_01','21003','3x^2 - 4'),
(10,'ANS_MATH_01','21004','10'),
(11,'ANS_MATH_01','21005','Eigenvalue'),

-- student3 on BIO301 Exam E33001
(12,'ANS_BIO_01','31001','Mitochondria'),
(13,'ANS_BIO_01','31002','Thymine (T)'),
(14,'ANS_BIO_01','31003','Metaphase'),
(15,'ANS_BIO_01','31004','Pyruvate'),
(16,'ANS_BIO_01','31005','3:1'),

-- student4 on ENG102 Exam E44001
(17,'ANS_ENG_01','41001','At the end of the introductory paragraph'),
(18,'ANS_ENG_01','41002','The researcher analyzed the experimental data.'),
(19,'ANS_ENG_01','41003','To connect logical ideas and guide the reader between sentences and paragraphs'),
(20,'ANS_ENG_01','41004','(Smith 45)'),
(21,'ANS_ENG_01','41005','Ad Hominem');
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
                                     ('CS101','CS101 Assistant','Curriculum Focus: CPU Architecture & Registers, LIFO Stack and Queue operations, Multi-threading concurrency and synchronization, Relational Foreign Keys, Binary Search O(log n), and QuickSort partitioning recursion depth. Guide students with hints and algorithmic explanations.',1,'teacher1','2026-08-26 16:37:58'),
                                     ('MATH201','MATH201 Advanced Helper','Curriculum Focus: Matrix inversion methods (2x2 formula and Gauss-Jordan elimination), Determinants and invertibility conditions, Matrix multiplication dimensions, Eigenvalues and Eigenvectors, Derivatives (power rule, chain rule), and Indefinite Integrals.',1,'teacher2','2026-08-26 16:44:33'),
                                     ('BIO301','BIO301 BioBot','Curriculum Focus: Eukaryotic cell organelles (Mitochondria, Ribosomes, Endoplasmic Reticulum), DNA base pairing (A-T, C-G), Mitosis phases (Prophase, Metaphase, Anaphase, Telophase), Glycolysis and ATP production, Mendelian genetics (monohybrid crosses, Punnett squares), and translation with tRNA.',1,'teacher3','2026-08-27 09:15:00'),
                                     ('ENG102','ENG102 WritingCoach','Curriculum Focus: Academic essay structure, Thesis statement formulation and placement, Active vs. Passive voice mechanics, MLA in-text citation format (Author Page), Logical fallacies (Straw Man, Ad Hominem), and Classical Rhetorical Appeals (Ethos, Pathos, Logos).',1,'teacher4','2026-08-27 09:30:00');
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
                                   ('BOT-42dc9551','student1','MATH201','what is matrix inversion','Matrix inversion is the process of finding a matrix A^(-1) such that A * A^(-1) = I. Only square matrices with non-zero determinants can be inverted.','2026-08-26 17:48:55'),
                                   ('BOT-59e18688','student1','MATH201','what is a matrix','A matrix is a rectangular grid of numbers arranged in rows and columns used to represent linear equations and transformations.','2026-08-26 17:48:07'),
                                   ('BOT-7b1c6d2f','student1','CS101','Who won the 2022 World Cup?','I’m happy to help with your CS101 coursework—feel free to ask a question about Binary Search Trees, QuickSort recursion depth, or LIFO Stack principles!','2026-08-26 13:58:59'),
                                   ('BOT-7e01d4a3','student2','CS101','what is a binary tree','A binary tree is a hierarchical data structure where each node has at most two children: a left child and a right child.','2026-08-26 17:44:12'),
                                   ('BOT-caa54d26','student1','CS101','What is an in-order traversal in a Binary Search Tree?','An in-order traversal visits the left subtree, then the root node, and finally the right subtree (left -> node -> right), which traverses BST values in ascending order.','2026-08-26 13:58:26'),
                                   ('BOT-b1030101','student3','BIO301','What happens during Metaphase?','During Metaphase in mitosis, condensed chromosomes align along the equatorial plane (metaphase plate) of the cell before being separated in Anaphase.','2026-08-27 10:12:00'),
                                   ('BOT-e1020101','student4','ENG102','How do I cite in MLA format?','In MLA style, in-text citations use the author\'s last name and page number in parentheses without punctuation, for example: (Smith 45).','2026-08-27 10:15:30');
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