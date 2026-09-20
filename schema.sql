-- Schema for adaptive_dosing_control (MySQL 8)
-- Structure only, generated from a mysqldump; INSERT data omitted intentionally.

--
-- Table structure for table `alarm_log`
DROP TABLE IF EXISTS `alarm_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alarm_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `alarm_name` varchar(255) NOT NULL,
  `event_time` datetime(6) NOT NULL,
  `priority` varchar(255) NOT NULL,
  `source_path` varchar(255) DEFAULT NULL,
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dosing_formulas`
DROP TABLE IF EXISTS `dosing_formulas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dosing_formulas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `approved_at` datetime(6) DEFAULT NULL,
  `approved_by` varchar(255) DEFAULT NULL,
  `base_dose` decimal(10,4) NOT NULL,
  `chemical_type` enum('ANTISCALANT','CAUSTIC','COAGULANT','SBS','SH') NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `max_dose` decimal(10,4) NOT NULL,
  `min_dose` decimal(10,4) NOT NULL,
  `notes` text,
  `status` enum('ACTIVE','APPROVED','DRAFT','RETIRED') NOT NULL,
  `version` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `formula_inputs`
DROP TABLE IF EXISTS `formula_inputs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `formula_inputs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `coefficient` decimal(12,6) NOT NULL,
  `reading_tag` varchar(255) NOT NULL,
  `formula_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKdywyn3beomqlkntff5w4fnxhh` (`formula_id`),
  CONSTRAINT `FKdywyn3beomqlkntff5w4fnxhh` FOREIGN KEY (`formula_id`) REFERENCES `dosing_formulas` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `process_readings`
DROP TABLE IF EXISTS `process_readings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `process_readings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reading_time` datetime(6) NOT NULL,
  `tag_name` varchar(255) NOT NULL,
  `value` decimal(14,4) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10705 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

