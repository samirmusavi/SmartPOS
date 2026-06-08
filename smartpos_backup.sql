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
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK8u7edaqormnd493mu1mnbefha` (`branch_id`,`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=58 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `branch_inventory`
--

LOCK TABLES `branch_inventory` WRITE;
/*!40000 ALTER TABLE `branch_inventory` DISABLE KEYS */;
INSERT INTO `branch_inventory` VALUES (1,1,1,25,NULL),(2,1,2,30,NULL),(3,1,3,50,NULL),(4,1,4,10,NULL),(5,1,5,100,NULL),(6,1,6,500,NULL),(7,1,7,200,NULL),(8,1,8,150,NULL),(9,1,9,80,NULL),(10,1,10,200,NULL),(16,2,1,0,NULL),(17,2,2,0,NULL),(18,2,3,0,NULL),(19,2,4,0,NULL),(20,2,5,0,NULL),(21,2,6,0,NULL),(22,2,7,0,NULL),(23,2,8,0,NULL),(24,2,9,0,NULL),(25,2,10,0,NULL),(31,4,11,5365.98,'2026-05-19 15:07:56.877184'),(32,3,11,3,'2026-05-15 14:19:44.154849'),(33,5,11,10,'2026-05-12 15:51:12.515208'),(34,5,12,0,'2026-05-15 14:42:47.005635'),(35,4,12,24,'2026-05-15 14:43:07.156800'),(36,3,12,0,'2026-05-15 14:42:47.010319'),(37,5,13,0,'2026-05-19 15:06:56.501314'),(38,4,13,3,'2026-05-22 16:14:50.313091'),(39,3,13,10,'2026-05-22 16:17:28.214132'),(40,5,14,0,'2026-05-19 15:07:36.102366'),(41,4,14,11,'2026-05-20 15:16:39.049975'),(42,3,14,0,'2026-05-19 15:07:36.112022'),(43,5,15,0,'2026-05-19 15:08:56.031364'),(44,4,15,862,'2026-05-22 16:02:44.309098'),(45,3,15,0,'2026-05-19 15:08:56.049528'),(46,5,16,0,'2026-05-20 11:26:29.638207'),(47,4,16,87,'2026-05-22 16:09:06.168752'),(48,3,16,0,'2026-05-20 11:26:29.643210'),(49,5,17,0,'2026-05-20 15:22:32.052109'),(50,4,17,115,'2026-05-20 15:22:32.067291'),(51,3,17,0,'2026-05-20 15:22:32.068295'),(52,5,18,0,'2026-05-20 15:24:57.535637'),(53,4,18,0,'2026-05-22 16:02:44.354100'),(54,3,18,0,'2026-05-20 15:24:57.544002'),(55,5,19,0,'2026-05-22 16:16:41.523916'),(56,4,19,100,'2026-05-22 16:16:41.525919'),(57,3,19,0,'2026-05-22 16:16:41.535923');
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cash_day_sheet`
--

LOCK TABLES `cash_day_sheet` WRITE;
/*!40000 ALTER TABLE `cash_day_sheet` DISABLE KEYS */;
INSERT INTO `cash_day_sheet` VALUES (1,4,0,0,100,0,0,0,1000,1,10.00,'2026-05-22 15:16:00.574328',10000.00,250.00,2000000.00,_binary '','2026-05-22','2026-05-22 15:39:50.570919'),(2,4,0,0,0,0,0,0,0,1,0.00,'2026-05-22 15:23:58.378977',0.00,0.00,0.00,_binary '\0','2026-05-21','2026-05-22 15:23:58.378977');
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
  `entry_type` enum('SALE','EXPENSE','RETURN','MANUAL_IN','MANUAL_OUT','OPENING') COLLATE utf8mb4_unicode_ci NOT NULL,
  `party_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reference_id` bigint DEFAULT NULL,
  `sheet_date` date NOT NULL,
  `sort_order` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cash_entry`
--

LOCK TABLES `cash_entry` WRITE;
/*!40000 ALTER TABLE `cash_entry` DISABLE KEYS */;
INSERT INTO `cash_entry` VALUES (2,0.00,50000.00,4,1,'2026-05-22 15:17:29.301960','','PLATFORM','MANUAL_OUT','HONOR PLATFORM',NULL,'2026-05-22',1),(3,10000.00,0.00,4,1,'2026-05-22 15:18:30.930093','','PLATFORM','MANUAL_IN','XA MARKET',NULL,'2026-05-22',2),(5,62422.50,0.00,4,1,'2026-05-22 15:24:02.923411',NULL,'AMB-2026-00012 | Ahmad Mossavi','SALE','Walk-in Customer',15,'2026-05-21',0),(6,531240.78,0.00,4,1,'2026-05-22 15:24:02.928410',NULL,'AMB-2026-00013 | Ahmad Mossavi','SALE','Walk-in Customer',16,'2026-05-21',1),(7,0.00,5000.00,4,1,'2026-05-22 15:24:02.939900',NULL,'Return: RET-AMB-2026-00003','RETURN','Ring 18K',3,'2026-05-21',2),(8,0.00,55000.02,4,1,'2026-05-22 15:24:02.943917',NULL,'Return: RET-AMB-2026-00004','RETURN','Bangles',4,'2026-05-21',3),(9,0.00,531240.00,4,1,'2026-05-22 15:24:02.945904',NULL,'Return: RET-AMB-2026-00005','RETURN','Gold 995',5,'2026-05-21',4),(10,0.00,15000.00,4,1,'2026-05-22 15:41:49.932693','','Loan','MANUAL_OUT','Ahmad',NULL,'2026-05-22',3),(11,1885034.69,0.00,4,1,'2026-05-22 16:04:33.329236',NULL,'AMB-2026-00014 | Ahmad Mossavi','SALE','Sudais Mossavi Bullion',17,'2026-05-22',0),(12,665000.11,0.00,4,1,'2026-05-22 16:04:33.333246',NULL,'AMB-2026-00015 | Ahmad Mossavi','SALE','Sudais Mossavi Bullion',18,'2026-05-22',1);
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
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer`
--

LOCK TABLES `customer` WRITE;
/*!40000 ALTER TABLE `customer` DISABLE KEYS */;
INSERT INTO `customer` VALUES (1,1,'2026-05-15 13:55:59.463216','','HONOR PLATFORM',NULL,0,'','',0.00,'2026-05-15 13:57:01.600033',0,NULL),(2,1,'2026-05-15 13:56:11.468666','','HONOR','2026-05-19 16:40:00.036904',1115708,'','',1115708.94,'2026-05-19 16:40:00.067733',2,NULL),(3,1,'2026-05-15 13:56:44.398345','','XA PLATFORM',NULL,0,'','',0.00,'2026-05-15 13:56:44.398345',0,NULL),(4,1,'2026-05-15 13:57:26.988896','','XA','2026-05-19 16:21:36.212387',1703708,'','',1703708.94,'2026-05-19 16:21:36.229084',3,NULL),(5,1,'2026-05-15 13:57:43.700990','','ARK BULLION','2026-05-20 11:21:15.306326',527768,'','',527768.00,'2026-05-20 11:21:15.318241',1,NULL),(6,1,'2026-05-21 12:35:58.454992','sudaistupoi@gmail.com','Sudais Mossavi Bullion','2026-05-22 16:14:50.328869',3193159,'Sudais','+971522519560',3193159.80,'2026-05-22 16:14:50.339650',3,NULL);
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
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_44c6umvphppa3226vhmagmviu` (`barcode`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product`
--

LOCK TABLES `product` WRITE;
/*!40000 ALTER TABLE `product` DISABLE KEYS */;
INSERT INTO `product` VALUES (1,_binary '','IP16PM001',4,'Electronics',4200.00,'2026-05-12 17:05:07.000000',NULL,'iPhone 16 Pro Max',5499.00,25,NULL,'2026-05-12 17:05:07.000000',NULL,''),(2,_binary '','SGS25001',4,'Electronics',3100.00,'2026-05-12 17:05:07.000000',NULL,'Samsung Galaxy S25',4299.00,30,NULL,'2026-05-12 17:05:07.000000',NULL,''),(3,_binary '','APP2001',4,'Electronics',650.00,'2026-05-12 17:05:07.000000',NULL,'AirPods Pro 2',999.00,50,NULL,'2026-05-12 17:05:07.000000',NULL,''),(4,_binary '','MBA3001',4,'Electronics',3800.00,'2026-05-12 17:05:07.000000',NULL,'MacBook Air M3',4999.00,10,NULL,'2026-05-12 17:05:07.000000',NULL,''),(5,_binary '','NVA001',4,'Personal Care',22.00,'2026-05-12 17:05:07.000000',NULL,'Nivea Body Lotion',45.00,100,NULL,'2026-05-12 17:05:07.000000',NULL,''),(6,_binary '','CC330001',4,'Beverages',1.50,'2026-05-12 17:05:07.000000',NULL,'Coca Cola 330ml',3.50,500,NULL,'2026-05-12 17:05:07.000000',NULL,''),(7,_binary '','RB250001',4,'Beverages',4.50,'2026-05-12 17:05:07.000000',NULL,'Red Bull 250ml',8.00,200,NULL,'2026-05-12 17:05:07.000000',NULL,''),(8,_binary '','ALM1L001',4,'Dairy',4.00,'2026-05-12 17:05:07.000000',NULL,'Almarai Milk 1L',7.50,150,NULL,'2026-05-12 17:05:07.000000',NULL,''),(9,_binary '','ARC250001',4,'Beverages',18.00,'2026-05-12 17:05:07.000000',NULL,'Arabic Coffee 250g',35.00,80,NULL,'2026-05-12 17:05:07.000000',NULL,''),(10,_binary '','USBC1M001',4,'Accessories',8.00,'2026-05-12 17:05:07.000000',NULL,'USB-C Cable 1m',25.00,200,NULL,'2026-05-12 17:05:07.000000',NULL,''),(11,_binary '','995.0',1,'Gold Bar',550000.00,'2026-05-12 15:11:11.058014',NULL,'Gold 995.0',560000.00,5378.98,NULL,'2026-05-19 15:07:56.947312',NULL,''),(12,_binary '','5000252485',1,'Ring',0.00,'2026-05-15 14:42:46.998405',NULL,'Ring 21K',1.00,24,NULL,'2026-05-15 14:43:07.176528',NULL,''),(13,_binary '',NULL,1,'Ring',0.00,'2026-05-19 15:06:56.431524',NULL,'Ring 18K',0.00,13,NULL,'2026-05-22 16:17:28.229753','750','PCS'),(14,_binary '','563',1,'Gold',0.00,'2026-05-19 15:07:36.089972',NULL,'Gold 999.9',0.00,11,NULL,'2026-05-20 15:16:39.100192','999.9','GRAM'),(15,_binary '','23',1,'Gold',0.00,'2026-05-19 15:08:56.020581',NULL,'Gold 995',0.00,862,NULL,'2026-05-22 16:02:44.350111','995','GRAM'),(16,_binary '','000',1,'Jewellery',0.00,'2026-05-20 11:26:29.630587',NULL,'Bangles',0.00,87,NULL,'2026-05-22 16:09:06.189601','750','PCS'),(17,_binary '','f',1,'Jewellery 21K',0.00,'2026-05-20 15:22:32.052109',NULL,'Earrings 21K',0.00,115,NULL,'2026-05-20 15:22:32.052109','875','PCS'),(18,_binary '','S',1,'Silver Bar',0.00,'2026-05-20 15:24:57.525963',NULL,'Silver',0.00,0,NULL,'2026-05-22 16:02:44.424579','999.9','GRAM'),(19,_binary '','0264',1,'jewellery',0.00,'2026-05-22 16:16:41.518361',NULL,'ring 14k',0.00,100,NULL,'2026-05-22 16:16:41.519493','585','PCS');
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
INSERT INTO `receipt_sequence` VALUES (1,3,3,'2026-05-15 14:17:45.604222',2026),(2,4,17,'2026-05-22 16:14:50.337963',2026);
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
  PRIMARY KEY (`id`),
  KEY `FKonrcqwf09u6spb6ty6sh11jh5` (`product_id`),
  CONSTRAINT `FKonrcqwf09u6spb6ty6sh11jh5` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sale`
--

LOCK TABLES `sale` WRITE;
/*!40000 ALTER TABLE `sale` DISABLE KEYS */;
INSERT INTO `sale` VALUES (1,3,1,'Ahmad Mossavi',550000.00,'Cash',560000.00,1,'AMJ-2026-00001','2026-05-15 14:17:24.724607',560000.00,1,11,NULL,NULL),(2,3,1,'Ahmad Mossavi',550000.00,'Cash',560000.00,1,'AMJ-2026-00002','2026-05-15 14:17:41.788374',560000.00,2,11,NULL,NULL),(3,3,1,'Ahmad Mossavi',550000.00,'Cash',560000.00,1,'AMJ-2026-00003','2026-05-15 14:17:45.590616',560000.00,3,11,NULL,NULL),(4,4,1,'Ahmad Mossavi',0.00,'Cash',1.00,1,'AMB-2026-00001','2026-05-15 14:43:07.173163',1.00,4,12,NULL,NULL),(5,4,1,'Ahmad Mossavi',550000.00,'Cash',560000.00,1,'AMB-2026-00002','2026-05-18 13:04:55.813344',560000.00,5,11,NULL,NULL),(6,4,1,'Ahmad Mossavi',0.00,'Cash',0.00,1,'AMB-2026-00003','2026-05-19 15:17:14.882435',0.00,6,15,'995','GRAM'),(7,4,1,'Ahmad Mossavi',0.00,'Cash',528.89,1000,'AMB-2026-00004','2026-05-19 16:21:36.212386',528890.16,7,15,'995','GRAM'),(8,4,1,'Ahmad Mossavi',0.00,'Cash',528.89,1000,'AMB-2026-00005','2026-05-19 16:40:00.043800',528890.16,8,15,'995','GRAM'),(9,4,1,'Ahmad Mossavi',0.00,'Cash',531.49,1,'AMB-2026-00006','2026-05-19 16:50:46.854873',531.49,9,14,'999.9','GRAM'),(10,4,1,'Ahmad Mossavi',0.00,'Cash',531.49,1000,'AMB-2026-00007','2026-05-20 11:19:01.764406',531494.74,10,14,'999.9','GRAM'),(11,4,1,'Ahmad Mossavi',0.00,'Cash',528.89,500,'AMB-2026-00007','2026-05-20 11:19:01.764406',264445.08,10,15,'995','GRAM'),(12,4,1,'Ahmad Mossavi',0.00,'Cash',528.89,1000,'AMB-2026-00008','2026-05-20 11:21:15.310397',528890.16,11,15,'995','GRAM'),(13,4,1,'Ahmad Mossavi',0.00,'Cash',531.49,60,'AMB-2026-00009','2026-05-20 11:23:58.624745',31889.68,12,14,'999.9','GRAM'),(14,4,1,'Ahmad Mossavi',0.00,'Cash',30000.00,1,'AMB-2026-00010','2026-05-20 11:29:47.724900',30000.00,13,16,'750','PCS'),(15,4,1,'Ahmad Mossavi',0.00,'Cash',2750.00,2,'AMB-2026-00010','2026-05-20 11:29:47.724900',5500.00,13,13,'750','PCS'),(16,4,1,'Ahmad Mossavi',0.00,'Cash',11000.00,6,'AMB-2026-00011','2026-05-20 15:16:39.083362',66000.00,14,16,'750','PCS'),(17,4,1,'Ahmad Mossavi',0.00,'Cash',531.49,10,'AMB-2026-00011','2026-05-20 15:16:39.100192',5314.95,14,14,'999.9','GRAM'),(18,4,1,'Ahmad Mossavi',0.00,'Cash',5000.00,1,'AMB-2026-00012','2026-05-21 12:25:37.146075',5000.00,15,13,'750','PCS'),(19,4,1,'Ahmad Mossavi',0.00,'Cash',9166.67,6,'AMB-2026-00012','2026-05-21 12:25:37.163194',55000.00,15,16,'750','PCS'),(20,4,1,'Ahmad Mossavi',0.00,'Cash',531.24,1000,'AMB-2026-00013','2026-05-21 12:37:26.262625',531240.78,16,15,'995','GRAM'),(21,4,1,'Ahmad Mossavi',0.00,'Cash',8.98,200000,'AMB-2026-00014','2026-05-22 13:44:46.071778',1795271.13,17,18,'999.9','GRAM'),(22,4,1,'Ahmad Mossavi',0.00,'Cash',531.24,1000,'AMB-2026-00015','2026-05-22 16:02:44.416676',531240.78,18,15,'995','GRAM'),(23,4,1,'Ahmad Mossavi',0.00,'Cash',9.04,15000,'AMB-2026-00015','2026-05-22 16:02:44.419578',135531.16,18,18,'999.9','GRAM'),(24,4,1,'Ahmad Mossavi',0.00,'Cash',9166.67,6,'AMB-2026-00016','2026-05-22 16:09:06.186304',55000.00,19,16,'750','PCS'),(25,4,1,'Ahmad Mossavi',0.00,'Cash',17500.00,35,'AMB-2026-00017','2026-05-22 16:14:50.329867',612500.00,20,13,'750','PCS');
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
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sale_transaction`
--

LOCK TABLES `sale_transaction` WRITE;
/*!40000 ALTER TABLE `sale_transaction` DISABLE KEYS */;
INSERT INTO `sale_transaction` VALUES (1,588000.00,3,1,'Ahmad Mossavi',0.00,'2026-05-15 14:17:24.721840',2,'HONOR','Cash','AMJ-2026-00001',560000.00,588000.00,28000.00,NULL,NULL,NULL,NULL,NULL,'',0),(2,600000.00,3,1,'Ahmad Mossavi',12000.00,'2026-05-15 14:17:41.774697',4,'XA','Cash','AMJ-2026-00002',560000.00,588000.00,28000.00,NULL,NULL,NULL,NULL,NULL,'',0),(3,600000.00,3,1,'Ahmad Mossavi',12000.00,'2026-05-15 14:17:45.590616',4,'XA','Cash','AMJ-2026-00003',560000.00,588000.00,28000.00,NULL,NULL,NULL,NULL,NULL,'',0),(4,5.00,4,1,'Ahmad Mossavi',3.95,'2026-05-15 14:43:07.156800',NULL,NULL,'Cash','AMB-2026-00001',1.00,1.05,0.05,NULL,NULL,NULL,NULL,NULL,'',0),(5,588000.00,4,1,'Ahmad Mossavi',0.00,'2026-05-18 13:04:55.780978',NULL,NULL,'Cash','AMB-2026-00002',560000.00,588000.00,28000.00,NULL,NULL,NULL,NULL,NULL,'',0),(6,530000.00,4,1,'Ahmad Mossavi',530000.00,'2026-05-19 15:17:14.870107',NULL,NULL,'Cash','AMB-2026-00003',0.00,0.00,0.00,0.00,0.00,NULL,NULL,NULL,'FULLY_PAID',5),(7,527710.00,4,1,'Ahmad Mossavi',1.06,'2026-05-19 16:21:36.195342',4,'XA','Cash','AMB-2026-00004',528890.16,527708.94,0.00,1181.22,0.00,3.6740,4500.00,'Gold/Silver Bullion','FULLY_PAID',0),(8,500000.00,4,1,'Ahmad Mossavi',0.00,'2026-05-19 16:40:00.009808',2,'HONOR','Cash','AMB-2026-00005',528890.16,527708.94,0.00,1181.22,27708.94,3.6740,4500.00,'Gold/Silver Bullion','PARTIALLY_PAID',0),(9,533.00,4,1,'Ahmad Mossavi',1.51,'2026-05-19 16:50:46.854873',NULL,NULL,'Cash','AMB-2026-00006',531.49,531.49,0.00,0.00,0.00,3.6740,4500.00,'Gold/Silver Bullion','FULLY_PAID',0),(10,795000.00,4,1,'Ahmad Mossavi',1422.61,'2026-05-20 11:19:01.744411',NULL,'Sudais','Cash','AMB-2026-00007',795939.82,793577.39,0.00,2362.44,0.00,3.6740,4500.00,'Gold/Silver Bullion','FULLY_PAID',0),(11,500000.00,4,1,'Ahmad Mossavi',0.00,'2026-05-20 11:21:15.300688',5,'ARK BULLION','Cash','AMB-2026-00008',528890.16,527768.00,0.00,1122.16,27768.00,3.6740,4500.00,'Gold/Silver Bullion','PARTIALLY_PAID',0),(12,32959.17,4,1,'Ahmad Mossavi',0.01,'2026-05-20 11:23:58.624745',NULL,'SAD','Cash','AMB-2026-00009',31889.68,32959.16,1569.48,500.00,0.00,3.6740,4500.00,'Gold/Silver Bullion','FULLY_PAID',5),(13,37300.00,4,1,'Ahmad Mossavi',25.00,'2026-05-20 11:29:47.721930',NULL,NULL,'Cash','AMB-2026-00010',35500.00,37275.00,1775.00,0.00,0.00,3.6700,4500.00,'Gold/Silver Bullion','FULLY_PAID',5),(14,NULL,4,1,'Ahmad Mossavi',0.00,'2026-05-20 15:16:39.082481',NULL,NULL,'Cash','AMB-2026-00011',71314.95,73620.70,3505.75,1200.00,73620.70,3.6740,4500.00,'Gold/Silver Bullion','PARTIALLY_PAID',5),(15,62500.00,4,1,'Ahmad Mossavi',77.50,'2026-05-21 12:25:37.130164',NULL,NULL,'Cash','AMB-2026-00012',60000.00,62422.50,2972.50,550.00,0.00,3.6740,4500.00,'Gold/Silver Bullion','FULLY_PAID',5),(16,550000.00,4,1,'Ahmad Mossavi',18759.22,'2026-05-21 12:37:26.262625',NULL,NULL,'Cash','AMB-2026-00013',531240.78,531240.78,0.00,0.00,0.00,3.6740,4520.00,'Gold/Silver Bullion','RETURNED',0),(17,1900000.00,4,1,'Ahmad Mossavi',14965.31,'2026-05-22 13:44:46.039880',6,'Sudais Mossavi Bullion','Cash','AMB-2026-00014',1795271.13,1885034.69,89763.56,0.00,0.00,3.6740,76.00,'Gold/Silver Bullion','FULLY_PAID',5),(18,NULL,4,1,'Ahmad Mossavi',0.00,'2026-05-22 16:02:44.391576',6,'Sudais Mossavi Bullion','Cash','AMB-2026-00015',666771.94,665000.11,0.00,1771.83,665000.11,3.6740,76.00,'Gold/Silver Bullion','PARTIALLY_PAID',0),(19,58000.00,4,1,'Ahmad Mossavi',775.00,'2026-05-22 16:09:06.179386',NULL,'WLD','Cash','AMB-2026-00016',55000.00,57225.00,2725.00,500.00,0.00,3.6740,4520.00,'Gold/Silver Bullion','FULLY_PAID',5),(20,643200.00,4,1,'Ahmad Mossavi',75.00,'2026-05-22 16:14:50.324864',6,'Sudais Mossavi Bullion','Cash','AMB-2026-00017',612500.00,643125.00,30625.00,0.00,0.00,3.6740,4520.00,'Gold/Silver Bullion','FULLY_PAID',5);
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_adjustment`
--

LOCK TABLES `stock_adjustment` WRITE;
/*!40000 ALTER TABLE `stock_adjustment` DISABLE KEYS */;
INSERT INTO `stock_adjustment` VALUES (1,'2026-05-12 15:11:28.941767','Ahmad Mossavi',4,4,1,5,'',1,'MANUAL_COUNT',11),(2,'2026-05-12 15:17:00.740486','Ahmad Mossavi',5,3,1,5,'',0,'MANUAL_COUNT',11),(3,'2026-05-19 15:07:56.929919','Ahmad Mossavi',5360.98,4,1,5365.98,'',5,'MANUAL_COUNT',11),(4,'2026-05-19 16:41:11.551441','Ahmad Mossavi',0.020000000000436557,4,1,3362,'',3361.9799999999996,'MANUAL_COUNT',15),(5,'2026-05-20 11:22:38.437266','Ahmad Mossavi',0.03999999999996362,4,1,81,'',80.96000000000004,'MANUAL_COUNT',14);
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supplier`
--

LOCK TABLES `supplier` WRITE;
/*!40000 ALTER TABLE `supplier` DISABLE KEYS */;
INSERT INTO `supplier` VALUES (1,NULL,4,'Ahmed Al Maktoum','2026-05-12 17:05:07.000000','ahmed@uaeelec.ae','UAE Electronics LLC',NULL,'+971501111111','2026-05-12 17:05:07.000000',NULL),(2,NULL,4,'Sara Hassan','2026-05-12 17:05:07.000000','sara@gulfdist.ae','Gulf Distribution Co',NULL,'+971502222222','2026-05-12 17:05:07.000000',NULL);
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
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-22 20:25:44
