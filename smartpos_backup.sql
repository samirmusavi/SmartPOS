-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: smartpos_db
-- ------------------------------------------------------
-- Server version	8.0.46

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

--
-- Table structure for table `admin`
--

DROP TABLE IF EXISTS `admin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `full_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_gfn44sntic2k93auag97juyij` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin`
--

LOCK TABLES `admin` WRITE;
/*!40000 ALTER TABLE `admin` DISABLE KEYS */;
INSERT INTO `admin` VALUES (1,'System Administrator','$2a$10$9rUnCcEiaPg0IuXGu807BeuGxgLJjDuna9lNKn9fv3LLYFMGaJ3Le','admin'),(2,'My Admin','$2a$10$KfTEXaA3S43Z3PoAOhvnaeEe6f8fENHs7rWqy5IINkPGBVy6E.rRO','myadmin');
/*!40000 ALTER TABLE `admin` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `app_user`
--

DROP TABLE IF EXISTS `app_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `app_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('OWNER','MANAGER','CASHIER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('ACTIVE','INACTIVE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `business_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_1j9d9a06i600gd43uu3km82jw` (`email`),
  KEY `FKja8nw1r4vgddoeb22g37gi1wq` (`business_id`),
  CONSTRAINT `FKja8nw1r4vgddoeb22g37gi1wq` FOREIGN KEY (`business_id`) REFERENCES `business` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `app_user`
--

LOCK TABLES `app_user` WRITE;
/*!40000 ALTER TABLE `app_user` DISABLE KEYS */;
INSERT INTO `app_user` VALUES (1,'2026-05-12 13:10:14.327612','ajm@gmail.com','Ahmad Mossavi','$2a$10$F6tJ9qIZKPU4woMnc5aEc.6QI53yzA8JPBpzppgkYph0Ncjw8MtiK','OWNER','ACTIVE',1),(2,'2026-05-15 14:28:56.984983','s@gmail.com','Sudais Mossavi','$2a$10$uvzhWHG8RjrjZHCNrYqx2OjxIbNttZ1lM8It/TZonTF0lP.6Llz8y','OWNER','ACTIVE',3),(3,'2026-05-15 14:33:08.294663','samimo@gmAIL.COM','Samim','$2a$10$SHBNd46x2n679zf57QX7cOQLLjzehJ5WVyLi8bzsWHKBwOa0yDNeu','MANAGER','ACTIVE',3);
/*!40000 ALTER TABLE `app_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `branch`
--

DROP TABLE IF EXISTS `branch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `branch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `business_id` bigint NOT NULL,
  `city` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `country` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_main_branch` bit(1) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `branch`
--

LOCK TABLES `branch` WRITE;
/*!40000 ALTER TABLE `branch` DISABLE KEYS */;
INSERT INTO `branch` VALUES (1,NULL,4,'Dubai','UAE','2026-05-12 17:05:07.000000',_binary '','Main Branch',NULL,'ACTIVE','2026-05-12 17:05:07.000000'),(2,NULL,4,'Abu Dhabi','UAE','2026-05-12 17:05:07.000000',_binary '\0','Abu Dhabi Branch',NULL,'ACTIVE','2026-05-12 17:05:07.000000'),(3,'Gold Center, Dubai',1,'Gold Center, Dubai','UAE','2026-05-12 13:25:33.136317',_binary '\0','Ajmal Mossavi Jewellery','+971561515866','ACTIVE','2026-05-12 13:25:33.136317'),(4,'Gold Souq, Dubai',1,'Gold Souq, Dubai','UAE','2026-05-12 13:26:12.574087',_binary '\0','Ajmal Mossavi Bullion','+971561515866','ACTIVE','2026-05-12 13:26:12.574087'),(5,NULL,1,'Ajmal Mossavi Bullion','UAE','2026-05-12 15:51:12.497119',_binary '','Main Branch',NULL,'ACTIVE','2026-05-12 15:51:12.497119'),(6,'',3,'Dubai','UAE','2026-05-15 14:32:34.382624',_binary '\0','Dubai Mall','','ACTIVE','2026-05-15 14:32:34.382624'),(7,NULL,3,'Sudais Mossavi Jewellery LLC','UAE','2026-05-16 13:22:59.515724',_binary '','Main Branch',NULL,'ACTIVE','2026-05-16 13:22:59.515724');
/*!40000 ALTER TABLE `branch` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `branch_inventory`
--

DROP TABLE IF EXISTS `branch_inventory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `branch_inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `quantity` double NOT NULL DEFAULT '0',
  `updated_at` datetime(6) DEFAULT NULL,
  `total_weight_grams` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK8u7edaqormnd493mu1mnbefha` (`branch_id`,`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=70 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `branch_inventory`
--

LOCK TABLES `branch_inventory` WRITE;
/*!40000 ALTER TABLE `branch_inventory` DISABLE KEYS */;
INSERT INTO `branch_inventory` VALUES (1,1,1,25,NULL,NULL),(2,1,2,30,NULL,NULL),(3,1,3,50,NULL,NULL),(4,1,4,10,NULL,NULL),(5,1,5,100,NULL,NULL),(6,1,6,500,NULL,NULL),(7,1,7,200,NULL,NULL),(8,1,8,150,NULL,NULL),(9,1,9,80,NULL,NULL),(10,1,10,200,NULL,NULL),(16,2,1,0,NULL,NULL),(17,2,2,0,NULL,NULL),(18,2,3,0,NULL,NULL),(19,2,4,0,NULL,NULL),(20,2,5,0,NULL,NULL),(21,2,6,0,NULL,NULL),(22,2,7,0,NULL,NULL),(23,2,8,0,NULL,NULL),(24,2,9,0,NULL,NULL),(25,2,10,0,NULL,NULL),(31,4,11,5365.98,'2026-05-19 15:07:56.877184',NULL),(32,3,11,3,'2026-05-15 14:19:44.154849',NULL),(33,5,11,10,'2026-05-12 15:51:12.515208',NULL),(34,5,12,0,'2026-05-15 14:42:47.005635',NULL),(35,4,12,24,'2026-05-15 14:43:07.156800',NULL),(36,3,12,0,'2026-05-15 14:42:47.010319',NULL),(37,5,13,0,'2026-05-19 15:06:56.501314',NULL),(38,4,13,103,'2026-05-23 14:06:28.623125',NULL),(39,3,13,10,'2026-05-22 16:17:28.214132',NULL),(40,5,14,0,'2026-05-19 15:07:36.102366',NULL),(41,4,14,20011,'2026-05-23 14:04:00.286877',NULL),(42,3,14,0,'2026-05-19 15:07:36.112022',NULL),(43,5,15,0,'2026-05-19 15:08:56.031364',NULL),(44,4,15,10863.05,'2026-06-02 16:09:57.564312',NULL),(45,3,15,0,'2026-05-19 15:08:56.049528',NULL),(46,5,16,0,'2026-05-20 11:26:29.638207',NULL),(47,4,16,79,'2026-05-23 13:16:34.133897',NULL),(48,3,16,0,'2026-05-20 11:26:29.643210',NULL),(49,5,17,0,'2026-05-20 15:22:32.052109',NULL),(50,4,17,95,'2026-06-02 10:33:56.203735',NULL),(51,3,17,0,'2026-05-20 15:22:32.068295',NULL),(52,5,18,0,'2026-05-20 15:24:57.535637',NULL),(53,4,18,70000,'2026-06-04 12:42:15.442687',NULL),(54,3,18,0,'2026-05-20 15:24:57.544002',NULL),(55,5,19,0,'2026-05-22 16:16:41.523916',NULL),(56,4,19,50,'2026-05-23 13:25:29.222674',NULL),(57,3,19,0,'2026-05-22 16:16:41.535923',NULL),(58,5,20,0,'2026-06-02 15:05:41.821986',NULL),(59,4,20,25,'2026-06-02 15:05:41.823999',NULL),(60,3,20,0,'2026-06-02 15:05:41.824699',NULL),(61,5,21,0,'2026-06-02 16:09:04.616923',NULL),(62,4,21,1950.11,'2026-06-02 16:09:04.631511',NULL),(63,3,21,0,'2026-06-02 16:09:04.633576',NULL),(64,5,22,0,'2026-06-04 08:36:51.060042',NULL),(65,4,22,14000,'2026-06-04 12:38:54.328230',NULL),(66,3,22,0,'2026-06-04 08:36:51.069764',NULL),(67,5,23,0,'2026-06-04 12:44:41.019672',NULL),(68,4,23,20,'2026-06-04 12:44:41.032104',NULL),(69,3,23,0,'2026-06-04 12:44:41.039260',NULL);
/*!40000 ALTER TABLE `branch_inventory` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `branch_price_override`
--

DROP TABLE IF EXISTS `branch_price_override`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `branch_price_override` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `product_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK76y9woq6f653s2qgvy7ak1h0f` (`branch_id`,`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `branch_price_override`
--

LOCK TABLES `branch_price_override` WRITE;
/*!40000 ALTER TABLE `branch_price_override` DISABLE KEYS */;
/*!40000 ALTER TABLE `branch_price_override` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `branch_product_archive`
--

DROP TABLE IF EXISTS `branch_product_archive`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `branch_product_archive` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `archived_at` datetime(6) DEFAULT NULL,
  `branch_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtnos752no50sodu5dc1hoadg` (`branch_id`,`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `branch_product_archive`
--

LOCK TABLES `branch_product_archive` WRITE;
/*!40000 ALTER TABLE `branch_product_archive` DISABLE KEYS */;
INSERT INTO `branch_product_archive` VALUES (1,'2026-05-19 15:08:21.376572',4,11),(2,'2026-05-19 15:08:25.378286',4,12);
/*!40000 ALTER TABLE `branch_product_archive` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `branch_supply_inventory`
--

DROP TABLE IF EXISTS `branch_supply_inventory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `branch_supply_inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `quantity` int NOT NULL,
  `supply_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKpule533u51x1ivwe5nv0kyp0q` (`branch_id`,`supply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `branch_supply_inventory`
--

LOCK TABLES `branch_supply_inventory` WRITE;
/*!40000 ALTER TABLE `branch_supply_inventory` DISABLE KEYS */;
/*!40000 ALTER TABLE `branch_supply_inventory` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `business`
--

DROP TABLE IF EXISTS `business`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `business` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expiry_date` datetime(6) DEFAULT NULL,
  `joined_date` datetime(6) DEFAULT NULL,
  `owner_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `plan` enum('BASIC','BUSINESS','ENTERPRISE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `receipt_footer` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `receipt_logo_base64` text COLLATE utf8mb4_unicode_ci,
  `status` enum('ACTIVE','INACTIVE','SUSPENDED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ktb1t97n9aeupitfjsdpjcmuq` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `business`
--

LOCK TABLES `business` WRITE;
/*!40000 ALTER TABLE `business` DISABLE KEYS */;
INSERT INTO `business` VALUES (1,'Ajmal Mossavi Bullion','ajmalmossavijewellery@gmail.com','2026-06-12 13:22:53.616449','2026-05-12 13:10:14.181043','Ahmad Mossavi','+971561515866','ENTERPRISE','Thank You Shopping!',NULL,'ACTIVE'),(3,'Sudais Mossavi Jewellery LLC','s@gmail.com','2026-06-15 14:30:01.942475','2026-05-15 14:28:56.820707','Sudais Mossavi','+971522519560','ENTERPRISE',NULL,NULL,'ACTIVE');
/*!40000 ALTER TABLE `business` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cash_day_sheet`
--

DROP TABLE IF EXISTS `cash_day_sheet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cash_day_sheet` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint DEFAULT NULL,
  `bundles10` int NOT NULL,
  `bundles100` int NOT NULL,
  `bundles1000` int NOT NULL,
  `bundles200` int NOT NULL,
  `bundles5` int NOT NULL,
  `bundles50` int NOT NULL,
  `bundles500` int NOT NULL,
  `business_id` bigint NOT NULL,
  `coins` decimal(10,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `hand_loan_total` decimal(15,2) NOT NULL,
  `mix_notes` decimal(15,2) NOT NULL,
  `opening_balance` decimal(15,2) NOT NULL,
  `opening_overridden` bit(1) NOT NULL,
  `sheet_date` date NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cash_day_sheet`
--

LOCK TABLES `cash_day_sheet` WRITE;
/*!40000 ALTER TABLE `cash_day_sheet` DISABLE KEYS */;
INSERT INTO `cash_day_sheet` VALUES (1,4,0,0,100,0,0,0,1000,1,10.00,'2026-05-22 15:16:00.574328',10000.00,250.00,2000000.00,_binary '','2026-05-22','2026-05-22 15:39:50.570919'),(2,4,0,0,0,0,0,0,0,1,0.00,'2026-05-22 15:23:58.378977',0.00,0.00,0.00,_binary '\0','2026-05-21','2026-05-22 15:23:58.378977'),(3,4,0,1700,3000,0,0,0,500,1,30.00,'2026-05-23 11:28:22.435232',100.00,51761.80,4495034.80,_binary '\0','2026-05-23','2026-05-23 14:20:50.614236'),(4,4,0,0,0,0,0,0,0,1,0.00,'2026-06-02 10:15:18.724755',0.00,0.00,3471761.80,_binary '\0','2026-06-02','2026-06-02 10:15:18.724755'),(5,4,0,0,0,0,0,0,0,1,0.00,'2026-06-02 12:22:04.980802',0.00,0.00,0.00,_binary '\0','2026-05-20','2026-06-02 12:22:04.980802'),(6,4,0,0,0,0,0,0,0,1,0.00,'2026-06-02 12:22:11.004726',0.00,0.00,0.00,_binary '\0','2026-05-19','2026-06-02 12:22:11.004726'),(7,4,0,0,0,0,0,0,0,1,0.00,'2026-06-02 12:22:55.001287',0.00,0.00,3471761.80,_binary '\0','2026-06-01','2026-06-02 12:22:55.001287'),(8,4,0,0,0,0,0,0,0,1,0.00,'2026-06-03 10:01:25.834549',0.00,0.00,3470761.80,_binary '\0','2026-06-03','2026-06-03 10:01:25.834549'),(9,4,0,0,0,0,0,0,0,1,0.00,'2026-06-04 13:46:53.188660',0.00,0.00,3470761.80,_binary '\0','2026-06-04','2026-06-04 13:46:53.188660'),(10,4,0,0,0,0,0,0,0,1,0.00,'2026-06-05 13:17:30.483112',0.00,0.00,3470761.80,_binary '\0','2026-06-05','2026-06-05 13:17:30.483112');
/*!40000 ALTER TABLE `cash_day_sheet` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cash_entry`
--

DROP TABLE IF EXISTS `cash_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cash_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount_in` decimal(15,2) NOT NULL,
  `amount_out` decimal(15,2) NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  `business_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `entry_type` enum('SALE','EXPENSE','RETURN','MANUAL_IN','MANUAL_OUT','OPENING','HAND_LOAN_IN','HAND_LOAN_OUT') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `party_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reference_id` bigint DEFAULT NULL,
  `sheet_date` date NOT NULL,
  `sort_order` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cash_entry`
--

LOCK TABLES `cash_entry` WRITE;
/*!40000 ALTER TABLE `cash_entry` DISABLE KEYS */;
INSERT INTO `cash_entry` VALUES (2,0.00,50000.00,4,1,'2026-05-22 15:17:29.301960','','PLATFORM','MANUAL_OUT','HONOR PLATFORM',NULL,'2026-05-22',1),(3,10000.00,0.00,4,1,'2026-05-22 15:18:30.930093','','PLATFORM','MANUAL_IN','XA MARKET',NULL,'2026-05-22',2),(5,62422.50,0.00,4,1,'2026-05-22 15:24:02.923411',NULL,'AMB-2026-00012 | Ahmad Mossavi','SALE','Walk-in Customer',15,'2026-05-21',0),(6,531240.78,0.00,4,1,'2026-05-22 15:24:02.928410',NULL,'AMB-2026-00013 | Ahmad Mossavi','SALE','Walk-in Customer',16,'2026-05-21',1),(7,0.00,5000.00,4,1,'2026-05-22 15:24:02.939900',NULL,'Return: RET-AMB-2026-00003','RETURN','Ring 18K',3,'2026-05-21',2),(8,0.00,55000.02,4,1,'2026-05-22 15:24:02.943917',NULL,'Return: RET-AMB-2026-00004','RETURN','Bangles',4,'2026-05-21',3),(9,0.00,531240.00,4,1,'2026-05-22 15:24:02.945904',NULL,'Return: RET-AMB-2026-00005','RETURN','Gold 995',5,'2026-05-21',4),(10,0.00,15000.00,4,1,'2026-05-22 15:41:49.932693','','Loan','MANUAL_OUT','Ahmad',NULL,'2026-05-22',3),(11,1885034.69,0.00,4,1,'2026-05-22 16:04:33.329236',NULL,'AMB-2026-00014 | Ahmad Mossavi','SALE','Sudais Mossavi Bullion',17,'2026-05-22',0),(12,665000.11,0.00,4,1,'2026-05-22 16:04:33.333246',NULL,'AMB-2026-00015 | Ahmad Mossavi','SALE','Sudais Mossavi Bullion',18,'2026-05-22',1),(13,0.00,1000.00,4,1,'2026-05-23 12:07:26.353054','','s','HAND_LOAN_OUT','sdsfdas',NULL,'2026-05-23',1),(14,1000.00,0.00,4,1,'2026-05-23 12:07:41.805643','','dfg','HAND_LOAN_IN','df',NULL,'2026-05-23',2),(15,0.00,100.00,4,1,'2026-05-23 12:08:03.406021','','f','HAND_LOAN_OUT','f',NULL,'2026-05-23',3),(17,65520.00,0.00,4,1,'2026-05-23 13:18:51.937805','','sell 8x bangles','MANUAL_IN','sudais',NULL,'2026-05-23',4),(21,11166000.00,0.00,4,1,'2026-05-23 13:33:58.330762','','20kg 999.9 gold','MANUAL_IN','sudais',NULL,'2026-05-23',5),(22,246000.00,0.00,4,1,'2026-05-23 13:35:20.700543','','400g gold','MANUAL_IN','sudais',NULL,'2026-05-23',6),(26,0.00,5288901.00,4,1,'2026-05-23 14:13:17.475128','','gold 995 10kg','MANUAL_OUT','sudais',NULL,'2026-05-23',7),(27,0.00,450000.00,4,1,'2026-05-23 14:14:21.943051','','gold 18k','MANUAL_OUT','sudais',NULL,'2026-05-23',8),(28,0.00,5433057.00,4,1,'2026-05-23 14:15:14.448691','','gold 999.9','MANUAL_OUT','sudais',NULL,'2026-05-23',9),(29,0.00,1328735.00,4,1,'2026-05-23 14:16:19.473511','','silver','MANUAL_OUT','sudais',NULL,'2026-05-23',10),(30,0.00,1000.00,4,1,'2026-06-02 12:15:50.149457','','gg','HAND_LOAN_OUT','AL',NULL,'2026-06-02',1);
/*!40000 ALTER TABLE `cash_entry` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customer`
--

DROP TABLE IF EXISTS `customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `full_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_visit_at` datetime(6) DEFAULT NULL,
  `loyalty_points` int NOT NULL,
  `notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_spent` decimal(10,2) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `visit_count` int NOT NULL,
  `linked_supplier_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer`
--

LOCK TABLES `customer` WRITE;
/*!40000 ALTER TABLE `customer` DISABLE KEYS */;
INSERT INTO `customer` VALUES (1,1,'2026-05-15 13:55:59.463216','','HONOR PLATFORM',NULL,0,'','',0.00,'2026-05-15 13:57:01.600033',0,NULL),(2,1,'2026-05-15 13:56:11.468666','','HONOR','2026-05-19 16:40:00.036904',1115708,'','',1115708.94,'2026-05-19 16:40:00.067733',2,NULL),(3,1,'2026-05-15 13:56:44.398345','','XA PLATFORM',NULL,0,'','',0.00,'2026-05-15 13:56:44.398345',0,NULL),(4,1,'2026-05-15 13:57:26.988896','','XA','2026-06-04 12:38:54.464816',2764441,'','',2764442.30,'2026-06-04 12:38:54.478809',4,NULL),(5,1,'2026-05-15 13:57:43.700990','','ARK BULLION','2026-05-20 11:21:15.306326',527768,'','',527768.00,'2026-05-20 11:21:15.318241',1,NULL),(6,1,'2026-05-21 12:35:58.454992','sudaismussavi@gmail.com','Sudais Mossavi Bullion','2026-05-23 13:30:51.606679',14670729,'Sudais','+971522519560',14670729.99,'2026-05-23 13:30:51.621405',6,NULL),(7,1,'2026-06-04 12:40:46.956407','INFO@TOPGOLD.COM','TOP GOLD','2026-06-04 12:42:15.457021',1333299,'','050055500',1333299.31,'2026-06-04 12:42:15.464513',1,NULL);
/*!40000 ALTER TABLE `customer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `expense`
--

DROP TABLE IF EXISTS `expense`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `expense` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(10,2) NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  `business_id` bigint NOT NULL,
  `category` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expense_date` date NOT NULL,
  `frequency` enum('MONTHLY','WEEKLY','YEARLY') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `recurring` bit(1) NOT NULL,
  `recurring_end_date` date DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `expense`
--

LOCK TABLES `expense` WRITE;
/*!40000 ALTER TABLE `expense` DISABLE KEYS */;
INSERT INTO `expense` VALUES (1,70000.00,4,1,'Rent','2026-05-12 15:24:30.578169','','2026-01-01','YEARLY',_binary '','2026-01-02','Yearly Rent Branch','2026-05-12 15:25:00.710294'),(3,70000.00,4,1,'Rent','2026-05-12 15:26:27.465271','','2026-01-01',NULL,_binary '\0',NULL,'Yearly Rent Branch','2026-05-12 15:26:38.522798'),(4,150000.00,4,1,'Personal','2026-05-12 15:35:12.333476','','2026-02-28',NULL,_binary '\0',NULL,'Ajmal Personal','2026-05-12 15:35:12.333476'),(5,120000.00,4,1,'Personal','2026-05-12 15:36:08.934689','','2026-03-31',NULL,_binary '\0',NULL,'Ajmal Personal','2026-05-12 15:36:08.934689'),(6,60000.00,4,1,'Personal','2026-05-12 15:37:14.189353','','2026-04-30',NULL,_binary '\0',NULL,'Ajmal Personal','2026-05-12 15:37:14.189353'),(7,1000.00,4,1,'Salary','2026-05-15 14:20:49.166600','','2026-05-01',NULL,_binary '\0',NULL,'Sudais Salary','2026-05-15 14:20:49.166600'),(8,1000.00,4,1,'Salary','2026-05-20 14:14:28.058330','','2026-05-20','MONTHLY',_binary '',NULL,'Sudais Salary','2026-05-20 14:14:28.058330'),(9,100000.00,4,1,'Rent','2026-05-20 14:14:52.839762','','2026-05-20','YEARLY',_binary '',NULL,'BR Rent','2026-05-20 14:15:00.076947');
/*!40000 ALTER TABLE `expense` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fix_settlement`
--

DROP TABLE IF EXISTS `fix_settlement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fix_settlement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `metal_fix_id` bigint NOT NULL,
  `settlement_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SFX-{SEQ}',
  `business_id` bigint NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  `party_id` bigint NOT NULL,
  `party_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `weight_grams` double NOT NULL,
  `fixed_rate` decimal(10,2) NOT NULL,
  `exchange_rate` decimal(10,4) NOT NULL DEFAULT '3.6740',
  `settlement_aed` decimal(15,2) NOT NULL,
  `original_aed` decimal(15,2) NOT NULL,
  `difference_cr_dr` decimal(15,2) DEFAULT NULL COMMENT '+ve = CR for party, -ve = DR for party',
  `settled_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `settlement_date` date NOT NULL,
  `notes` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `settlement_number` (`settlement_number`),
  KEY `idx_fs_fix` (`metal_fix_id`),
  KEY `idx_fs_party_date` (`business_id`,`party_id`,`settlement_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fix_settlement`
--

LOCK TABLES `fix_settlement` WRITE;
/*!40000 ALTER TABLE `fix_settlement` DISABLE KEYS */;
/*!40000 ALTER TABLE `fix_settlement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `kyc_document`
--

DROP TABLE IF EXISTS `kyc_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kyc_document` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `document_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_size` bigint DEFAULT NULL,
  `supplier_id` bigint NOT NULL,
  `uploaded_at` datetime(6) DEFAULT NULL,
  `uploaded_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `kyc_document`
--

LOCK TABLES `kyc_document` WRITE;
/*!40000 ALTER TABLE `kyc_document` DISABLE KEYS */;
INSERT INTO `kyc_document` VALUES (1,'EMIRATES_ID','AJMAL LOGO.jpg','C:\\Users\\AHMAD\\Downloads\\SmartPOS\\uploads\\kyc\\6\\97f8e25a-1f02-427c-9550-68137cd05936.jpg',58646,6,'2026-06-04 13:45:03.395976','Ahmad Mossavi'),(2,'PASSPORT','db5a1f6c-64d3-4177-ade8-8080947438bf.jpg','C:\\Users\\AHMAD\\Downloads\\SmartPOS\\uploads\\kyc\\6\\e0ddd1db-4f64-4408-a4ff-c86698db44b8.jpg',75643,6,'2026-06-04 13:45:03.477797','Ahmad Mossavi'),(3,'TRADE_LICENSE','Acceptance Letter.pdf','C:\\Users\\AHMAD\\Downloads\\SmartPOS\\uploads\\kyc\\6\\6c70ad52-5335-4b08-8be4-79eec6c6d35e.pdf',280604,6,'2026-06-04 13:45:03.558297','Ahmad Mossavi'),(4,'OTHER','CASH SHEET - 21-5-26 (1).pdf','C:\\Users\\AHMAD\\Downloads\\SmartPOS\\uploads\\kyc\\6\\776f42b4-2433-4b21-ac04-df7aa0761771.pdf',67283,6,'2026-06-04 13:45:03.612064','Ahmad Mossavi'),(5,'EMIRATES_ID','AJMAL LOG.jpg','C:\\Users\\AHMAD\\Downloads\\SmartPOS\\uploads\\kyc\\6\\cf1b417d-3593-4962-8249-9ecaf4fc0a6c.jpg',168227,6,'2026-06-04 13:45:24.826882','Ahmad Mossavi');
/*!40000 ALTER TABLE `kyc_document` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metal_fix`
--

DROP TABLE IF EXISTS `metal_fix`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metal_fix` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_id` bigint NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  `fix_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'HPF-{SEQ}',
  `party_id` bigint NOT NULL,
  `party_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fix_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SALE_FIX / PURCHASE_FIX',
  `metal_type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'GOLD / SILVER',
  `purity` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `weight_grams` double NOT NULL,
  `transaction_rate` decimal(10,2) NOT NULL COMMENT 'USD per troy oz at time of deal',
  `discount_premium` decimal(10,2) NOT NULL DEFAULT '0.00',
  `discount_premium_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PER_OZ' COMMENT 'PER_OZ / PERCENTAGE',
  `effective_rate` decimal(10,2) DEFAULT NULL,
  `exchange_rate` decimal(10,4) NOT NULL DEFAULT '3.6740',
  `total_aed` decimal(15,2) DEFAULT NULL,
  `margin_percent` decimal(5,2) DEFAULT NULL,
  `margin_amount` decimal(15,2) DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN / FIXED / SETTLED',
  `fixed_rate` decimal(10,2) DEFAULT NULL,
  `fixed_date` date DEFAULT NULL,
  `settlement_amount` decimal(15,2) DEFAULT NULL,
  `settlement_date` date DEFAULT NULL,
  `linked_sale_id` bigint DEFAULT NULL,
  `linked_purchase_id` bigint DEFAULT NULL,
  `notes` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `fix_number` (`fix_number`),
  KEY `idx_mf_business_party` (`business_id`,`party_id`),
  KEY `idx_mf_business_status` (`business_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metal_fix`
--

LOCK TABLES `metal_fix` WRITE;
/*!40000 ALTER TABLE `metal_fix` DISABLE KEYS */;
/*!40000 ALTER TABLE `metal_fix` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `party_ledger_entry`
--

DROP TABLE IF EXISTS `party_ledger_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `party_ledger_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_id` bigint NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  `party_id` bigint NOT NULL,
  `party_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `voucher_type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SAL / PUR / REC / PAY / HPF / SFX / ADJ',
  `voucher_number` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `voucher_date` date NOT NULL,
  `narration` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `aed_debit` decimal(15,2) NOT NULL DEFAULT '0.00',
  `aed_credit` decimal(15,2) NOT NULL DEFAULT '0.00',
  `metal_debit` double NOT NULL DEFAULT '0',
  `metal_credit` double NOT NULL DEFAULT '0',
  `metal_type` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'GOLD / SILVER',
  `purity` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reference_id` bigint DEFAULT NULL,
  `reference_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SALE / PURCHASE / RECEIPT / PAYMENT / FIX / SETTLEMENT',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ple_party` (`business_id`,`party_id`),
  KEY `idx_ple_date` (`business_id`,`party_id`,`voucher_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `party_ledger_entry`
--

LOCK TABLES `party_ledger_entry` WRITE;
/*!40000 ALTER TABLE `party_ledger_entry` DISABLE KEYS */;
/*!40000 ALTER TABLE `party_ledger_entry` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `password_reset_token`
--

DROP TABLE IF EXISTS `password_reset_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `token_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKli7wollcmb8tibymo3s94o57h` (`user_id`),
  CONSTRAINT `FKli7wollcmb8tibymo3s94o57h` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `password_reset_token`
--

LOCK TABLES `password_reset_token` WRITE;
/*!40000 ALTER TABLE `password_reset_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `password_reset_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product`
--

DROP TABLE IF EXISTS `product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `barcode` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `business_id` bigint NOT NULL,
  `category` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cost_price` decimal(10,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `image_path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `quantity` double NOT NULL DEFAULT '0',
  `supplier_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `purity` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit_type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_scrap` bit(1) NOT NULL,
  `total_weight_grams` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_44c6umvphppa3226vhmagmviu` (`barcode`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product`
--

LOCK TABLES `product` WRITE;
/*!40000 ALTER TABLE `product` DISABLE KEYS */;
INSERT INTO `product` VALUES (1,_binary '','IP16PM001',4,'Electronics',4200.00,'2026-05-12 17:05:07.000000',NULL,'iPhone 16 Pro Max',5499.00,25,NULL,'2026-05-12 17:05:07.000000',NULL,'',_binary '\0',NULL),(2,_binary '','SGS25001',4,'Electronics',3100.00,'2026-05-12 17:05:07.000000',NULL,'Samsung Galaxy S25',4299.00,30,NULL,'2026-05-12 17:05:07.000000',NULL,'',_binary '\0',NULL),(3,_binary '','APP2001',4,'Electronics',650.00,'2026-05-12 17:05:07.000000',NULL,'AirPods Pro 2',999.00,50,NULL,'2026-05-12 17:05:07.000000',NULL,'',_binary '\0',NULL),(4,_binary '','MBA3001',4,'Electronics',3800.00,'2026-05-12 17:05:07.000000',NULL,'MacBook Air M3',4999.00,10,NULL,'2026-05-12 17:05:07.000000',NULL,'',_binary '\0',NULL),(5,_binary '','NVA001',4,'Personal Care',22.00,'2026-05-12 17:05:07.000000',NULL,'Nivea Body Lotion',45.00,100,NULL,'2026-05-12 17:05:07.000000',NULL,'',_binary '\0',NULL),(6,_binary '','CC330001',4,'Beverages',1.50,'2026-05-12 17:05:07.000000',NULL,'Coca Cola 330ml',3.50,500,NULL,'2026-05-12 17:05:07.000000',NULL,'',_binary '\0',NULL),(7,_binary '','RB250001',4,'Beverages',4.50,'2026-05-12 17:05:07.000000',NULL,'Red Bull 250ml',8.00,200,NULL,'2026-05-12 17:05:07.000000',NULL,'',_binary '\0',NULL),(8,_binary '','ALM1L001',4,'Dairy',4.00,'2026-05-12 17:05:07.000000',NULL,'Almarai Milk 1L',7.50,150,NULL,'2026-05-12 17:05:07.000000',NULL,'',_binary '\0',NULL),(9,_binary '','ARC250001',4,'Beverages',18.00,'2026-05-12 17:05:07.000000',NULL,'Arabic Coffee 250g',35.00,80,NULL,'2026-05-12 17:05:07.000000',NULL,'',_binary '\0',NULL),(10,_binary '','USBC1M001',4,'Accessories',8.00,'2026-05-12 17:05:07.000000',NULL,'USB-C Cable 1m',25.00,200,NULL,'2026-05-12 17:05:07.000000',NULL,'',_binary '\0',NULL),(11,_binary '','995.0',1,'Gold Bar',550000.00,'2026-05-12 15:11:11.058014',NULL,'Gold 995.0',560000.00,5378.98,NULL,'2026-05-19 15:07:56.947312',NULL,'',_binary '\0',NULL),(12,_binary '','5000252485',1,'Ring',0.00,'2026-05-15 14:42:46.998405',NULL,'Ring 21K',1.00,24,NULL,'2026-05-15 14:43:07.176528',NULL,'',_binary '\0',NULL),(13,_binary '','',1,'Ring',0.00,'2026-05-19 15:06:56.431524','/uploads/products/1/13_1780403325931.jpg','Ring 18K',0.00,103,NULL,'2026-06-02 16:07:01.819092','750','PCS',_binary '\0',350),(14,_binary '','5635',1,'Gold',0.00,'2026-05-19 15:07:36.089972',NULL,'Gold 999.9',0.00,20011,NULL,'2026-06-02 12:09:43.944347','999.9','GRAM',_binary '\0',NULL),(15,_binary '','23',1,'Gold',0.00,'2026-05-19 15:08:56.020581',NULL,'Gold 995',0.00,10863.05,NULL,'2026-06-02 16:09:57.586708','995','GRAM',_binary '\0',NULL),(16,_binary '','000',1,'Jewellery',0.00,'2026-05-20 11:26:29.630587',NULL,'Bangles',0.00,79,NULL,'2026-05-23 13:16:34.241173','750','PCS',_binary '\0',NULL),(17,_binary '','f',1,'Jewellery 21K',0.00,'2026-05-20 15:22:32.052109',NULL,'Earrings 21K',0.00,95,NULL,'2026-06-02 10:33:56.310987','875','PCS',_binary '\0',NULL),(18,_binary '','S',1,'Silver Bar',0.00,'2026-05-20 15:24:57.525963',NULL,'Silver',0.00,70000,NULL,'2026-06-04 12:42:15.461068','999.9','GRAM',_binary '\0',NULL),(19,_binary '','0264',1,'jewellery',0.00,'2026-05-22 16:16:41.518361',NULL,'ring 14k',0.00,50,NULL,'2026-06-04 12:43:14.955932','585','PCS',_binary '\0',149.999),(20,_binary '','0000',1,'Earrings',0.00,'2026-06-02 15:05:41.792806',NULL,'Earrings 18K',0.00,25,NULL,'2026-06-04 12:44:08.134959','750','PCS',_binary '\0',119.25),(21,_binary '','0258',1,'Scrap',0.00,'2026-06-02 16:09:04.579298',NULL,'Scrap',0.00,1950,NULL,'2026-06-02 16:09:15.932236','996','GRAM',_binary '\0',NULL),(22,_binary '','00255',1,'GOLD',0.00,'2026-06-04 08:36:51.029015',NULL,'Gold 9999',0.00,14000,NULL,'2026-06-04 12:38:54.473912','1000','GRAM',_binary '\0',NULL),(23,_binary '',NULL,1,'LL',0.00,'2026-06-04 12:44:41.002456',NULL,'LLL',0.00,20,NULL,'2026-06-04 12:44:41.002456','585','PCS',_binary '\0',NULL);
/*!40000 ALTER TABLE `product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_supply_template`
--

DROP TABLE IF EXISTS `product_supply_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_supply_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `quantity_per_unit` double NOT NULL,
  `product_id` bigint NOT NULL,
  `supply_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKpfc1q5m99mxlr7w1h1iy8ogln` (`product_id`,`supply_id`),
  KEY `FK51x0b65ccu6qgq9c8rcd9qxt5` (`supply_id`),
  CONSTRAINT `FK51x0b65ccu6qgq9c8rcd9qxt5` FOREIGN KEY (`supply_id`) REFERENCES `supply` (`id`),
  CONSTRAINT `FKdt6hsbydwr55il2a9lj927r1u` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_supply_template`
--

LOCK TABLES `product_supply_template` WRITE;
/*!40000 ALTER TABLE `product_supply_template` DISABLE KEYS */;
/*!40000 ALTER TABLE `product_supply_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchase`
--

DROP TABLE IF EXISTS `purchase`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount_paid` decimal(15,2) NOT NULL,
  `branch_id` bigint NOT NULL,
  `business_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `due_amount` decimal(15,2) NOT NULL,
  `exchange_rate` decimal(10,4) DEFAULT NULL,
  `invoice_number` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_method` enum('CASH','BANK_TRANSFER','CHEQUE','GOLD_EXCHANGE','CREDIT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `purchase_date` datetime(6) NOT NULL,
  `status` enum('FULLY_PAID','PARTIALLY_PAID','DUE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `supplier_id` bigint DEFAULT NULL,
  `supplier_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_amount` decimal(15,2) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `cash_amount_paid` decimal(15,2) DEFAULT NULL,
  `gold_payment_oz_rate` decimal(10,2) DEFAULT NULL,
  `gold_payment_purity` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gold_payment_value` decimal(15,2) DEFAULT NULL,
  `gold_payment_weight_grams` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_magogbyy02wj7flyrww7cxx1b` (`invoice_number`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase`
--

LOCK TABLES `purchase` WRITE;
/*!40000 ALTER TABLE `purchase` DISABLE KEYS */;
INSERT INTO `purchase` VALUES (1,5500000.00,4,1,'2026-05-23 13:01:10.225945','Ahmad Mossavi',0.00,3.6740,'PUR-AJMA-20260523-001',NULL,'CASH','2026-05-23 12:59:00.000000','FULLY_PAID',NULL,'Honor',5314947.42,'2026-05-23 13:01:10.225945',NULL,NULL,NULL,NULL,NULL),(2,5500000.00,4,1,'2026-05-23 13:01:32.576869','Ahmad Mossavi',0.00,3.6740,'PUR-AJMA-20260523-002',NULL,'CASH','2026-05-23 12:59:00.000000','FULLY_PAID',NULL,'Honor',5314947.42,'2026-05-23 13:01:32.576869',NULL,NULL,NULL,NULL,NULL),(3,271.65,4,1,'2026-05-23 14:01:06.930813','Ahmad Mossavi',1328465.20,3.6740,'PUR-AJMA-20260523-003',NULL,'CASH','2026-05-23 13:59:00.000000','PARTIALLY_PAID',NULL,'sudais',1328736.85,'2026-05-23 14:01:06.931502',NULL,NULL,NULL,NULL,NULL),(4,5433057.00,4,1,'2026-05-23 14:04:00.283140','Ahmad Mossavi',0.36,3.6740,'PUR-AJMA-20260523-004',NULL,'CASH','2026-05-23 14:02:00.000000','PARTIALLY_PAID',NULL,'sudais',5433057.36,'2026-05-23 14:04:00.283140',NULL,NULL,NULL,NULL,NULL),(5,450000.00,4,1,'2026-05-23 14:06:28.607403','Ahmad Mossavi',0.00,3.6740,'PUR-AJMA-20260523-005',NULL,'CASH','2026-05-23 14:05:00.000000','FULLY_PAID',NULL,'sudais',450000.00,'2026-05-23 14:06:28.607403',NULL,NULL,NULL,NULL,NULL),(6,529.00,4,1,'2026-05-23 14:09:23.736702','Ahmad Mossavi',0.00,3.6740,'PUR-AJMA-20260523-006',NULL,'CASH','2026-05-23 14:07:00.000000','FULLY_PAID',NULL,'sudais',528.89,'2026-05-23 14:09:23.736702',NULL,NULL,NULL,NULL,NULL),(7,5288901.57,4,1,'2026-05-23 14:11:08.096591','Ahmad Mossavi',0.00,3.6740,'PUR-AJMA-20260523-007',NULL,'CASH','2026-05-23 14:09:00.000000','PARTIALLY_PAID',NULL,'sudais',5288901.57,'2026-05-23 14:11:08.096591',NULL,NULL,NULL,NULL,NULL),(8,135015.00,4,1,'2026-06-03 10:23:19.080237','Ahmad Mossavi',0.00,3.6740,'PUR-AJMA-20260603-001',NULL,'CASH','2026-06-03 10:15:00.000000','FULLY_PAID',NULL,'a',135013.17,'2026-06-03 10:23:19.080237',NULL,NULL,NULL,NULL,NULL),(9,135015.00,4,1,'2026-06-03 10:24:15.951980','Ahmad Mossavi',0.00,3.6740,'PUR-AJMA-20260603-002',NULL,'CASH','2026-06-03 10:15:00.000000','FULLY_PAID',NULL,'a',135013.17,'2026-06-03 10:24:15.951980',NULL,NULL,NULL,NULL,NULL),(10,530000.00,4,1,'2026-06-04 08:37:51.614625','Ahmad Mossavi',366.68,3.6740,'PUR-AJMA-20260604-001',NULL,'CASH','2026-06-04 08:36:00.000000','PARTIALLY_PAID',NULL,'HR',530366.68,'2026-06-04 08:37:51.614625',NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `purchase` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchase_item`
--

DROP TABLE IF EXISTS `purchase_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `gold_oz_rate` decimal(10,2) DEFAULT NULL,
  `price_per_gram` decimal(10,4) DEFAULT NULL,
  `pricing_method` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_id` bigint DEFAULT NULL,
  `product_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `purchase_id` bigint NOT NULL,
  `purity` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` double NOT NULL,
  `scrap_purity` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_price` decimal(15,2) NOT NULL,
  `unit_price` decimal(15,2) NOT NULL,
  `unit_type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `weight_grams` double DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_item`
--

LOCK TABLES `purchase_item` WRITE;
/*!40000 ALTER TABLE `purchase_item` DISABLE KEYS */;
INSERT INTO `purchase_item` VALUES (1,'2026-05-23 13:01:10.292336',4500.00,NULL,'OZ_RATE',14,'Gold 999.9',1,'999.9',10000,NULL,5314947.42,531.49,'GRAM',10000),(2,'2026-05-23 13:01:32.594665',4500.00,NULL,'OZ_RATE',14,'Gold 999.9',2,'999.9',10000,NULL,5314947.42,531.49,'GRAM',10000),(3,'2026-05-23 14:01:06.940255',75.00,NULL,'OZ_RATE',18,'Silver',3,'999.9',150000,NULL,1328736.85,8.86,'GRAM',150000),(4,'2026-05-23 14:04:00.286877',NULL,NULL,'FIXED_PRICE',14,'Gold 999.9',4,'999.9',20000,NULL,5433057.36,271.65,'PCS',NULL),(5,'2026-05-23 14:06:28.625123',NULL,NULL,'FIXED_PRICE',13,'Ring 18K',5,'750',100,NULL,450000.00,4500.00,'PCS',NULL),(6,'2026-05-23 14:09:23.748849',4500.00,NULL,'OZ_RATE',15,'Gold 995',6,'995',1,NULL,528.89,528.89,'PCS',NULL),(7,'2026-05-23 14:11:08.110335',4500.00,NULL,'OZ_RATE',15,'Gold 995',7,'995',10000,NULL,5288901.57,528.89,'GRAM',10000),(8,'2026-06-03 10:23:19.146052',76.00,NULL,'OZ_RATE',18,'Silver',8,'999.9',15000,NULL,135013.17,9.00,'GRAM',15000),(9,'2026-06-03 10:24:15.965890',76.00,NULL,'OZ_RATE',18,'Silver',9,'999.9',15000,NULL,135013.17,9.00,'GRAM',15000),(10,'2026-06-04 08:37:51.664536',4500.00,NULL,'OZ_RATE',22,'Gold 9999',10,'1000',1000,NULL,530366.68,531.55,'GRAM',1000);
/*!40000 ALTER TABLE `purchase_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchase_order`
--

DROP TABLE IF EXISTS `purchase_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint DEFAULT NULL,
  `business_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expected_delivery` date DEFAULT NULL,
  `notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `product_id` bigint NOT NULL,
  `product_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity_ordered` double NOT NULL DEFAULT '0',
  `quantity_received` double DEFAULT NULL,
  `received_date` date DEFAULT NULL,
  `status` enum('DRAFT','PENDING','RECEIVED','CANCELLED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `supplier_id` bigint NOT NULL,
  `supplier_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_cost` decimal(10,2) DEFAULT NULL,
  `unit_cost` decimal(10,2) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_order`
--

LOCK TABLES `purchase_order` WRITE;
/*!40000 ALTER TABLE `purchase_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `purchase_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchase_return`
--

DROP TABLE IF EXISTS `purchase_return`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_return` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `business_id` bigint NOT NULL,
  `created_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `purchase_id` bigint NOT NULL,
  `reason` enum('WRONG_ITEM','DAMAGED','SUPPLIER_ERROR','QUALITY_ISSUE','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `return_invoice_number` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `returned_at` datetime(6) DEFAULT NULL,
  `supplier_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_return_amount` decimal(15,2) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_cpg1uirltplm1yq43xaewr6jx` (`return_invoice_number`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_return`
--

LOCK TABLES `purchase_return` WRITE;
/*!40000 ALTER TABLE `purchase_return` DISABLE KEYS */;
INSERT INTO `purchase_return` VALUES (1,4,1,'Ahmad Mossavi',NULL,9,'WRONG_ITEM','PRET-AJMA-20260603-001','2026-06-03 14:27:03.801253','a',9.00),(2,4,1,'Ahmad Mossavi',NULL,9,'WRONG_ITEM','PRET-AJMA-20260603-002','2026-06-03 15:31:48.576224','a',134991.00);
/*!40000 ALTER TABLE `purchase_return` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchase_return_item`
--

DROP TABLE IF EXISTS `purchase_return_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_return_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint DEFAULT NULL,
  `product_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `purchase_item_id` bigint DEFAULT NULL,
  `purchase_return_id` bigint NOT NULL,
  `purity` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `return_quantity` double NOT NULL,
  `return_weight_grams` double DEFAULT NULL,
  `total_return_amount` decimal(15,2) DEFAULT NULL,
  `unit_price` decimal(15,2) DEFAULT NULL,
  `unit_type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_return_item`
--

LOCK TABLES `purchase_return_item` WRITE;
/*!40000 ALTER TABLE `purchase_return_item` DISABLE KEYS */;
INSERT INTO `purchase_return_item` VALUES (1,18,'Silver',9,1,'999.9',1,NULL,9.00,9.00,'GRAM'),(2,18,'Silver',9,2,'999.9',14999,NULL,134991.00,9.00,'GRAM');
/*!40000 ALTER TABLE `purchase_return_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `receipt_sequence`
--

DROP TABLE IF EXISTS `receipt_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `receipt_sequence` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `last_sequence` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `year` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmebffoa91f5ay82gy04lnfkxm` (`branch_id`,`year`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `receipt_sequence`
--

LOCK TABLES `receipt_sequence` WRITE;
/*!40000 ALTER TABLE `receipt_sequence` DISABLE KEYS */;
INSERT INTO `receipt_sequence` VALUES (1,3,3,'2026-05-15 14:17:45.604222',2026),(2,4,22,'2026-06-04 12:42:15.464513',2026);
/*!40000 ALTER TABLE `receipt_sequence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sale`
--

DROP TABLE IF EXISTS `sale`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sale` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint DEFAULT NULL,
  `business_id` bigint NOT NULL,
  `cashier_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cost_at_sale` decimal(10,2) NOT NULL,
  `payment_method` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price_at_sale` decimal(10,2) NOT NULL,
  `quantity_sold` double NOT NULL DEFAULT '0',
  `receipt_number` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sale_date` datetime(6) DEFAULT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `transaction_id` bigint DEFAULT NULL,
  `product_id` bigint NOT NULL,
  `purity` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scrap_purity` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKonrcqwf09u6spb6ty6sh11jh5` (`product_id`),
  CONSTRAINT `FKonrcqwf09u6spb6ty6sh11jh5` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sale`
--

LOCK TABLES `sale` WRITE;
/*!40000 ALTER TABLE `sale` DISABLE KEYS */;
INSERT INTO `sale` VALUES (1,3,1,'Ahmad Mossavi',550000.00,'Cash',560000.00,1,'AMJ-2026-00001','2026-05-15 14:17:24.724607',560000.00,1,11,NULL,NULL,NULL),(2,3,1,'Ahmad Mossavi',550000.00,'Cash',560000.00,1,'AMJ-2026-00002','2026-05-15 14:17:41.788374',560000.00,2,11,NULL,NULL,NULL),(3,3,1,'Ahmad Mossavi',550000.00,'Cash',560000.00,1,'AMJ-2026-00003','2026-05-15 14:17:45.590616',560000.00,3,11,NULL,NULL,NULL),(4,4,1,'Ahmad Mossavi',0.00,'Cash',1.00,1,'AMB-2026-00001','2026-05-15 14:43:07.173163',1.00,4,12,NULL,NULL,NULL),(5,4,1,'Ahmad Mossavi',550000.00,'Cash',560000.00,1,'AMB-2026-00002','2026-05-18 13:04:55.813344',560000.00,5,11,NULL,NULL,NULL),(6,4,1,'Ahmad Mossavi',0.00,'Cash',0.00,1,'AMB-2026-00003','2026-05-19 15:17:14.882435',0.00,6,15,'995','GRAM',NULL),(7,4,1,'Ahmad Mossavi',0.00,'Cash',528.89,1000,'AMB-2026-00004','2026-05-19 16:21:36.212386',528890.16,7,15,'995','GRAM',NULL),(8,4,1,'Ahmad Mossavi',0.00,'Cash',528.89,1000,'AMB-2026-00005','2026-05-19 16:40:00.043800',528890.16,8,15,'995','GRAM',NULL),(9,4,1,'Ahmad Mossavi',0.00,'Cash',531.49,1,'AMB-2026-00006','2026-05-19 16:50:46.854873',531.49,9,14,'999.9','GRAM',NULL),(10,4,1,'Ahmad Mossavi',0.00,'Cash',531.49,1000,'AMB-2026-00007','2026-05-20 11:19:01.764406',531494.74,10,14,'999.9','GRAM',NULL),(11,4,1,'Ahmad Mossavi',0.00,'Cash',528.89,500,'AMB-2026-00007','2026-05-20 11:19:01.764406',264445.08,10,15,'995','GRAM',NULL),(12,4,1,'Ahmad Mossavi',0.00,'Cash',528.89,1000,'AMB-2026-00008','2026-05-20 11:21:15.310397',528890.16,11,15,'995','GRAM',NULL),(13,4,1,'Ahmad Mossavi',0.00,'Cash',531.49,60,'AMB-2026-00009','2026-05-20 11:23:58.624745',31889.68,12,14,'999.9','GRAM',NULL),(14,4,1,'Ahmad Mossavi',0.00,'Cash',30000.00,1,'AMB-2026-00010','2026-05-20 11:29:47.724900',30000.00,13,16,'750','PCS',NULL),(15,4,1,'Ahmad Mossavi',0.00,'Cash',2750.00,2,'AMB-2026-00010','2026-05-20 11:29:47.724900',5500.00,13,13,'750','PCS',NULL),(16,4,1,'Ahmad Mossavi',0.00,'Cash',11000.00,6,'AMB-2026-00011','2026-05-20 15:16:39.083362',66000.00,14,16,'750','PCS',NULL),(17,4,1,'Ahmad Mossavi',0.00,'Cash',531.49,10,'AMB-2026-00011','2026-05-20 15:16:39.100192',5314.95,14,14,'999.9','GRAM',NULL),(18,4,1,'Ahmad Mossavi',0.00,'Cash',5000.00,1,'AMB-2026-00012','2026-05-21 12:25:37.146075',5000.00,15,13,'750','PCS',NULL),(19,4,1,'Ahmad Mossavi',0.00,'Cash',9166.67,6,'AMB-2026-00012','2026-05-21 12:25:37.163194',55000.00,15,16,'750','PCS',NULL),(20,4,1,'Ahmad Mossavi',0.00,'Cash',531.24,1000,'AMB-2026-00013','2026-05-21 12:37:26.262625',531240.78,16,15,'995','GRAM',NULL),(21,4,1,'Ahmad Mossavi',0.00,'Cash',8.98,200000,'AMB-2026-00014','2026-05-22 13:44:46.071778',1795271.13,17,18,'999.9','GRAM',NULL),(22,4,1,'Ahmad Mossavi',0.00,'Cash',531.24,1000,'AMB-2026-00015','2026-05-22 16:02:44.416676',531240.78,18,15,'995','GRAM',NULL),(23,4,1,'Ahmad Mossavi',0.00,'Cash',9.04,15000,'AMB-2026-00015','2026-05-22 16:02:44.419578',135531.16,18,18,'999.9','GRAM',NULL),(24,4,1,'Ahmad Mossavi',0.00,'Cash',9166.67,6,'AMB-2026-00016','2026-05-22 16:09:06.186304',55000.00,19,16,'750','PCS',NULL),(25,4,1,'Ahmad Mossavi',0.00,'Cash',17500.00,35,'AMB-2026-00017','2026-05-22 16:14:50.329867',612500.00,20,13,'750','PCS',NULL),(26,4,1,'Ahmad Mossavi',0.00,'Cash',7800.00,8,'AMB-2026-00018','2026-05-23 13:16:34.232774',62400.00,21,16,'750','PCS',NULL),(27,4,1,'Ahmad Mossavi',0.00,'Cash',2080.00,50,'AMB-2026-00019','2026-05-23 13:25:29.239195',104000.00,22,17,'875','PCS',NULL),(28,4,1,'Ahmad Mossavi',0.00,'Cash',2600.00,50,'AMB-2026-00019','2026-05-23 13:25:29.239195',130000.00,22,19,'585','PCS',NULL),(29,4,1,'Ahmad Mossavi',0.00,'Cash',531.73,20000,'AMB-2026-00020','2026-05-23 13:30:51.606679',10634619.23,23,14,'999.9','GRAM',NULL),(30,4,1,'Ahmad Mossavi',0.00,'Cash',531.55,2000,'AMB-2026-00021','2026-06-04 12:38:54.467807',1063095.79,24,22,'1000','GRAM',NULL),(31,4,1,'Ahmad Mossavi',0.00,'Cash',8.89,150000,'AMB-2026-00022','2026-06-04 12:42:15.458025',1333299.31,25,18,'999.9','GRAM',NULL);
/*!40000 ALTER TABLE `sale` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sale_return`
--

DROP TABLE IF EXISTS `sale_return`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sale_return` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint DEFAULT NULL,
  `business_id` bigint NOT NULL,
  `notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `processed_by` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_id` bigint NOT NULL,
  `product_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity_returned` double NOT NULL DEFAULT '0',
  `reason` enum('DEFECTIVE','WRONG_ITEM','CUSTOMER_CHANGE','EXPIRED','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `refund_amount` decimal(10,2) NOT NULL,
  `returned_at` datetime(6) DEFAULT NULL,
  `sale_id` bigint NOT NULL,
  `invoice_number` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `transaction_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ol5361lct3ym018raclygnvkw` (`invoice_number`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sale_return`
--

LOCK TABLES `sale_return` WRITE;
/*!40000 ALTER TABLE `sale_return` DISABLE KEYS */;
INSERT INTO `sale_return` VALUES (1,3,1,'','Ahmad Mossavi',11,'Gold 995.0',1,'DEFECTIVE',560000.00,'2026-05-15 14:19:44.167972',3,NULL,NULL),(2,4,1,'','Ahmad Mossavi',11,'Gold 995.0',1,'DEFECTIVE',560000.00,'2026-05-18 13:06:29.553562',5,NULL,NULL),(3,4,1,'','Ahmad Mossavi',13,'Ring 18K',1,'CUSTOMER_CHANGE',5000.00,'2026-05-21 14:08:24.922830',18,'RET-AMB-2026-00003',15),(4,4,1,'','Ahmad Mossavi',16,'Bangles',6,'CUSTOMER_CHANGE',55000.02,'2026-05-21 14:08:24.989181',19,'RET-AMB-2026-00004',15),(5,4,1,'','Ahmad Mossavi',15,'Gold 995',1000,'CUSTOMER_CHANGE',531240.00,'2026-05-21 14:37:47.302489',20,'RET-AMB-2026-00005',16);
/*!40000 ALTER TABLE `sale_return` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sale_transaction`
--

DROP TABLE IF EXISTS `sale_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sale_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount_received` decimal(10,2) DEFAULT NULL,
  `branch_id` bigint DEFAULT NULL,
  `business_id` bigint NOT NULL,
  `cashier_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `change_given` decimal(10,2) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `customer_id` bigint DEFAULT NULL,
  `customer_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_method` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `receipt_number` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `subtotal` decimal(10,2) NOT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `vat_amount` decimal(10,2) NOT NULL,
  `discount_amount` decimal(10,2) DEFAULT NULL,
  `due_amount` decimal(10,2) DEFAULT NULL,
  `exchange_rate` decimal(10,4) DEFAULT NULL,
  `gold_oz_rate` decimal(10,2) DEFAULT NULL,
  `pricing_method` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `vat_percent` double NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_6igk87ftdmomliabs35laernv` (`receipt_number`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sale_transaction`
--

LOCK TABLES `sale_transaction` WRITE;
/*!40000 ALTER TABLE `sale_transaction` DISABLE KEYS */;
INSERT INTO `sale_transaction` VALUES (1,588000.00,3,1,'Ahmad Mossavi',0.00,'2026-05-15 14:17:24.721840',2,'HONOR','Cash','AMJ-2026-00001',560000.00,588000.00,28000.00,NULL,NULL,NULL,NULL,NULL,'',0),(2,600000.00,3,1,'Ahmad Mossavi',12000.00,'2026-05-15 14:17:41.774697',4,'XA','Cash','AMJ-2026-00002',560000.00,588000.00,28000.00,NULL,NULL,NULL,NULL,NULL,'',0),(3,600000.00,3,1,'Ahmad Mossavi',12000.00,'2026-05-15 14:17:45.590616',4,'XA','Cash','AMJ-2026-00003',560000.00,588000.00,28000.00,NULL,NULL,NULL,NULL,NULL,'',0),(4,5.00,4,1,'Ahmad Mossavi',3.95,'2026-05-15 14:43:07.156800',NULL,NULL,'Cash','AMB-2026-00001',1.00,1.05,0.05,NULL,NULL,NULL,NULL,NULL,'',0),(5,588000.00,4,1,'Ahmad Mossavi',0.00,'2026-05-18 13:04:55.780978',NULL,NULL,'Cash','AMB-2026-00002',560000.00,588000.00,28000.00,NULL,NULL,NULL,NULL,NULL,'',0),(6,530000.00,4,1,'Ahmad Mossavi',530000.00,'2026-05-19 15:17:14.870107',NULL,NULL,'Cash','AMB-2026-00003',0.00,0.00,0.00,0.00,0.00,NULL,NULL,NULL,'FULLY_PAID',5),(7,527710.00,4,1,'Ahmad Mossavi',1.06,'2026-05-19 16:21:36.195342',4,'XA','Cash','AMB-2026-00004',528890.16,527708.94,0.00,1181.22,0.00,3.6740,4500.00,'Gold/Silver Bullion','FULLY_PAID',0),(8,500000.00,4,1,'Ahmad Mossavi',0.00,'2026-05-19 16:40:00.009808',2,'HONOR','Cash','AMB-2026-00005',528890.16,527708.94,0.00,1181.22,27708.94,3.6740,4500.00,'Gold/Silver Bullion','PARTIALLY_PAID',0),(9,533.00,4,1,'Ahmad Mossavi',1.51,'2026-05-19 16:50:46.854873',NULL,NULL,'Cash','AMB-2026-00006',531.49,531.49,0.00,0.00,0.00,3.6740,4500.00,'Gold/Silver Bullion','FULLY_PAID',0),(10,795000.00,4,1,'Ahmad Mossavi',1422.61,'2026-05-20 11:19:01.744411',NULL,'Sudais','Cash','AMB-2026-00007',795939.82,793577.39,0.00,2362.44,0.00,3.6740,4500.00,'Gold/Silver Bullion','FULLY_PAID',0),(11,500000.00,4,1,'Ahmad Mossavi',0.00,'2026-05-20 11:21:15.300688',5,'ARK BULLION','Cash','AMB-2026-00008',528890.16,527768.00,0.00,1122.16,27768.00,3.6740,4500.00,'Gold/Silver Bullion','PARTIALLY_PAID',0),(12,32959.17,4,1,'Ahmad Mossavi',0.01,'2026-05-20 11:23:58.624745',NULL,'SAD','Cash','AMB-2026-00009',31889.68,32959.16,1569.48,500.00,0.00,3.6740,4500.00,'Gold/Silver Bullion','FULLY_PAID',5),(13,37300.00,4,1,'Ahmad Mossavi',25.00,'2026-05-20 11:29:47.721930',NULL,NULL,'Cash','AMB-2026-00010',35500.00,37275.00,1775.00,0.00,0.00,3.6700,4500.00,'Gold/Silver Bullion','FULLY_PAID',5),(14,NULL,4,1,'Ahmad Mossavi',0.00,'2026-05-20 15:16:39.082481',NULL,NULL,'Cash','AMB-2026-00011',71314.95,73620.70,3505.75,1200.00,73620.70,3.6740,4500.00,'Gold/Silver Bullion','PARTIALLY_PAID',5),(15,62500.00,4,1,'Ahmad Mossavi',77.50,'2026-05-21 12:25:37.130164',NULL,NULL,'Cash','AMB-2026-00012',60000.00,62422.50,2972.50,550.00,0.00,3.6740,4500.00,'Gold/Silver Bullion','FULLY_PAID',5),(16,550000.00,4,1,'Ahmad Mossavi',18759.22,'2026-05-21 12:37:26.262625',NULL,NULL,'Cash','AMB-2026-00013',531240.78,531240.78,0.00,0.00,0.00,3.6740,4520.00,'Gold/Silver Bullion','RETURNED',0),(17,1900000.00,4,1,'Ahmad Mossavi',14965.31,'2026-05-22 13:44:46.039880',6,'Sudais Mossavi Bullion','Cash','AMB-2026-00014',1795271.13,1885034.69,89763.56,0.00,0.00,3.6740,76.00,'Gold/Silver Bullion','FULLY_PAID',5),(18,NULL,4,1,'Ahmad Mossavi',0.00,'2026-05-22 16:02:44.391576',6,'Sudais Mossavi Bullion','Cash','AMB-2026-00015',666771.94,665000.11,0.00,1771.83,665000.11,3.6740,76.00,'Gold/Silver Bullion','PARTIALLY_PAID',0),(19,58000.00,4,1,'Ahmad Mossavi',775.00,'2026-05-22 16:09:06.179386',NULL,'WLD','Cash','AMB-2026-00016',55000.00,57225.00,2725.00,500.00,0.00,3.6740,4520.00,'Gold/Silver Bullion','FULLY_PAID',5),(20,643200.00,4,1,'Ahmad Mossavi',75.00,'2026-05-22 16:14:50.324864',6,'Sudais Mossavi Bullion','Cash','AMB-2026-00017',612500.00,643125.00,30625.00,0.00,0.00,3.6740,4520.00,'Gold/Silver Bullion','FULLY_PAID',5),(21,66000.00,4,1,'Ahmad Mossavi',480.00,'2026-05-23 13:16:34.216285',6,'Sudais Mossavi Bullion','Cash','AMB-2026-00018',62400.00,65520.00,3120.00,0.00,0.00,3.6740,2300.00,'Gold/Silver Bullion','FULLY_PAID',5),(22,250000.00,4,1,'Ahmad Mossavi',4300.00,'2026-05-23 13:25:29.222674',6,'Sudais Mossavi Bullion','Cash','AMB-2026-00019',234000.00,245700.00,11700.00,0.00,0.00,3.6740,2300.00,'Gold/Silver Bullion','FULLY_PAID',5),(23,12000000.00,4,1,'Ahmad Mossavi',833649.81,'2026-05-23 13:30:51.602662',6,'Sudais Mossavi Bullion','Cash','AMB-2026-00020',10634619.23,11166350.19,531730.96,0.00,0.00,3.6740,4500.00,'Gold/Silver Bullion','FULLY_PAID',5),(24,1061000.00,4,1,'Ahmad Mossavi',266.64,'2026-06-04 12:38:54.428452',4,'XA','Cash','AMB-2026-00021',1063095.79,1060733.36,0.00,2362.44,0.00,3.6740,4500.00,'Gold/Silver Bullion','FULLY_PAID',0),(25,1333300.00,4,1,'Ahmad Mossavi',0.69,'2026-06-04 12:42:15.452069',7,'TOP GOLD','Cash','AMB-2026-00022',1333299.31,1333299.31,0.00,0.00,0.00,3.6740,75.00,'Gold/Silver Bullion','FULLY_PAID',0);
/*!40000 ALTER TABLE `sale_transaction` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_adjustment`
--

DROP TABLE IF EXISTS `stock_adjustment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_adjustment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `adjusted_at` datetime(6) DEFAULT NULL,
  `adjusted_by` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `adjustment` double NOT NULL DEFAULT '0',
  `branch_id` bigint DEFAULT NULL,
  `business_id` bigint NOT NULL,
  `new_quantity` double NOT NULL DEFAULT '0',
  `notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `old_quantity` double NOT NULL DEFAULT '0',
  `reason` enum('MANUAL_COUNT','DAMAGED','STOLEN','SUPPLIER_RETURN','RECEIVED_STOCK','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKb4wya5bif1qjb6ip287dj09k1` (`product_id`),
  CONSTRAINT `FKb4wya5bif1qjb6ip287dj09k1` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_adjustment`
--

LOCK TABLES `stock_adjustment` WRITE;
/*!40000 ALTER TABLE `stock_adjustment` DISABLE KEYS */;
INSERT INTO `stock_adjustment` VALUES (1,'2026-05-12 15:11:28.941767','Ahmad Mossavi',4,4,1,5,'',1,'MANUAL_COUNT',11),(2,'2026-05-12 15:17:00.740486','Ahmad Mossavi',5,3,1,5,'',0,'MANUAL_COUNT',11),(3,'2026-05-19 15:07:56.929919','Ahmad Mossavi',5360.98,4,1,5365.98,'',5,'MANUAL_COUNT',11),(4,'2026-05-19 16:41:11.551441','Ahmad Mossavi',0.020000000000436557,4,1,3362,'',3361.9799999999996,'MANUAL_COUNT',15),(5,'2026-05-20 11:22:38.437266','Ahmad Mossavi',0.03999999999996362,4,1,81,'',80.96000000000004,'MANUAL_COUNT',14),(6,'2026-06-02 10:33:56.295821','Ahmad Mossavi',30,4,1,95,'',65,'RECEIVED_STOCK',17),(7,'2026-06-02 12:09:57.850412','Ahmad Mossavi',55000,4,1,205000,'',150000,'RECEIVED_STOCK',18),(8,'2026-06-02 16:09:57.566340','Ahmad Mossavi',0.049999999999272404,4,1,10863.05,'',10863,'SUPPLIER_RETURN',15);
/*!40000 ALTER TABLE `stock_adjustment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_transfer`
--

DROP TABLE IF EXISTS `stock_transfer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_transfer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_id` bigint NOT NULL,
  `completed_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `from_branch_id` bigint NOT NULL,
  `from_branch_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `product_id` bigint NOT NULL,
  `product_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity` double NOT NULL DEFAULT '0',
  `status` enum('PENDING','COMPLETED','CANCELLED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `to_branch_id` bigint NOT NULL,
  `to_branch_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_transfer`
--

LOCK TABLES `stock_transfer` WRITE;
/*!40000 ALTER TABLE `stock_transfer` DISABLE KEYS */;
INSERT INTO `stock_transfer` VALUES (1,1,'Ahmad Mossavi','2026-05-22 16:10:25.130258','Ahmad Mossavi',4,'Ajmal Mossavi Bullion','',13,'Ring 18K',10,'COMPLETED',3,'Ajmal Mossavi Jewellery','2026-05-22 16:17:28.226026');
/*!40000 ALTER TABLE `stock_transfer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `supplier`
--

DROP TABLE IF EXISTS `supplier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `business_id` bigint NOT NULL,
  `contact_person` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `linked_customer_id` bigint DEFAULT NULL,
  `emirates_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id_expiry_date` date DEFAULT NULL,
  `is_customer` tinyint(1) NOT NULL DEFAULT '0',
  `is_supplier` tinyint(1) NOT NULL DEFAULT '1',
  `kyc_notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `party_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `kyc_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'NOT_VERIFIED',
  `passport_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trade_license_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_supplier_party_code` (`business_id`,`party_code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supplier`
--

LOCK TABLES `supplier` WRITE;
/*!40000 ALTER TABLE `supplier` DISABLE KEYS */;
INSERT INTO `supplier` VALUES (1,NULL,4,'Ahmed Al Maktoum','2026-05-12 17:05:07.000000','ahmed@uaeelec.ae','UAE Electronics LLC',NULL,'+971501111111','2026-05-12 17:05:07.000000',NULL,NULL,NULL,0,1,NULL,NULL,'NOT_VERIFIED',NULL,NULL),(2,NULL,4,'Sara Hassan','2026-05-12 17:05:07.000000','sara@gulfdist.ae','Gulf Distribution Co',NULL,'+971502222222','2026-05-12 17:05:07.000000',NULL,NULL,NULL,0,1,NULL,NULL,'NOT_VERIFIED',NULL,NULL),(3,'HIND PLAZA, GOLD SOUK',1,'MR. ZARIF','2026-06-04 13:16:36.416299','INFO@TOPGOLD.COM','TOP GOLD','','0500500511','2026-06-04 13:16:42.672442',NULL,'',NULL,1,1,'',NULL,'PENDING','',''),(4,'GOLD CENTER, GOLD SOUK',1,'MR. KHALID SHAH','2026-06-04 13:18:32.962164','INFO@SPHINAL.COM','SPHINAL JEWELLERY','','0555500511','2026-06-04 13:19:14.423558',NULL,'',NULL,1,1,'',NULL,'PENDING','',''),(5,'LONDON, UK',1,'MR. ABDUL SATTAR','2026-06-04 13:20:23.994851','ST@GMAIL.COM','ABDUL SATTAR','','+4451150025','2026-06-04 13:20:23.994851',NULL,'',NULL,1,0,'',NULL,'NOT_VERIFIED','',''),(6,'',1,'MR. SHAHID','2026-06-04 13:45:03.149206','','SS GOLD','','','2026-06-04 13:45:03.149206',NULL,'','2027-05-05',0,1,'',NULL,'VERIFIED','','');
/*!40000 ALTER TABLE `supplier` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `supply`
--

DROP TABLE IF EXISTS `supply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supply` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_id` bigint NOT NULL,
  `category` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cost_per_unit` decimal(10,2) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `minimum_threshold` int NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` int NOT NULL,
  `supplier_id` bigint DEFAULT NULL,
  `unit` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supply`
--

LOCK TABLES `supply` WRITE;
/*!40000 ALTER TABLE `supply` DISABLE KEYS */;
/*!40000 ALTER TABLE `supply` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `supply_adjustment`
--

DROP TABLE IF EXISTS `supply_adjustment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supply_adjustment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `adjusted_at` datetime(6) DEFAULT NULL,
  `adjusted_by` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `adjustment` int NOT NULL,
  `business_id` bigint NOT NULL,
  `new_quantity` int NOT NULL,
  `notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `old_quantity` int NOT NULL,
  `supply_id` bigint NOT NULL,
  `supply_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` enum('RECEIVED','USED','MANUAL','DAMAGED','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supply_adjustment`
--

LOCK TABLES `supply_adjustment` WRITE;
/*!40000 ALTER TABLE `supply_adjustment` DISABLE KEYS */;
/*!40000 ALTER TABLE `supply_adjustment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `supply_transfer`
--

DROP TABLE IF EXISTS `supply_transfer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supply_transfer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_id` bigint NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `completed_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `from_branch_id` bigint NOT NULL,
  `notes` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` int NOT NULL,
  `status` enum('PENDING','COMPLETED','CANCELLED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `supply_id` bigint NOT NULL,
  `to_branch_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supply_transfer`
--

LOCK TABLES `supply_transfer` WRITE;
/*!40000 ALTER TABLE `supply_transfer` DISABLE KEYS */;
/*!40000 ALTER TABLE `supply_transfer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_branch`
--

DROP TABLE IF EXISTS `user_branch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_branch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assigned_at` datetime(6) DEFAULT NULL,
  `branch_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK8jfc3frcetcft0p9i7xq4ek79` (`user_id`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_branch`
--

LOCK TABLES `user_branch` WRITE;
/*!40000 ALTER TABLE `user_branch` DISABLE KEYS */;
INSERT INTO `user_branch` VALUES (1,'2026-05-15 14:33:08.304646',6,3);
/*!40000 ALTER TABLE `user_branch` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `voucher_sequence`
--

DROP TABLE IF EXISTS `voucher_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `voucher_sequence` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_id` bigint NOT NULL,
  `voucher_type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_number` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_vs_business_type` (`business_id`,`voucher_type`),
  UNIQUE KEY `UK8pp6f9439dsqh9yknuxydqhg4` (`business_id`,`voucher_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `voucher_sequence`
--

LOCK TABLES `voucher_sequence` WRITE;
/*!40000 ALTER TABLE `voucher_sequence` DISABLE KEYS */;
/*!40000 ALTER TABLE `voucher_sequence` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-08 18:17:52
