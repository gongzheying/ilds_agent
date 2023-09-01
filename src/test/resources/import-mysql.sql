
CREATE TABLE `tbl_ilds_transfer_package` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `last_modified_at` datetime DEFAULT NULL,
  `version` bigint(20) NOT NULL,
  `destination` varchar(255) DEFAULT NULL,
  `direction` int(11) DEFAULT NULL,
  `end_tokenize_timestamp` bigint(20) NOT NULL,
  `local_file_path` varchar(255) DEFAULT NULL,
  `original_file_path` varchar(255) DEFAULT NULL,
  `original_package_name` varchar(255) DEFAULT NULL,
  `original_package_size` bigint(20) DEFAULT NULL,
  `package_name` varchar(255) DEFAULT NULL,
  `received_date` datetime DEFAULT NULL,
  `sender` varchar(255) DEFAULT NULL,
  `start_tokenize_timestamp` bigint(20) NOT NULL,
  `status` int(11) DEFAULT NULL,
  `transfer_package_type` int(11) DEFAULT NULL,
  `transfer_protocol` varchar(255) DEFAULT NULL,
  `bsp` varchar(15) DEFAULT NULL,
  `trigger_required` tinyint(1) DEFAULT NULL,
  `critical` varchar(1) DEFAULT NULL,
  `output_category` varchar(50) DEFAULT NULL,
  `need_retadd` tinyint(1) DEFAULT NULL,
  `need_hotadd` tinyint(1) DEFAULT NULL,
  `final_package_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_rsrahy56pku3xaplbgij49iqm` (`package_name`)
) ENGINE=InnoDB;

CREATE TABLE `tbl_ilds_transfer_file` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `last_modified_at` datetime DEFAULT NULL,
  `version` bigint(20) NOT NULL,
  `file_name` varchar(255) DEFAULT NULL,
  `file_type` int(11) DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  `transfer_package_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_transfer_package_id_file_name` (`transfer_package_id`,`file_name`),
  KEY `FK_73hihsadpnbyxp7ku0ayy2n3g` (`transfer_package_id`),
  CONSTRAINT `FK_73hihsadpnbyxp7ku0ayy2n3g` FOREIGN KEY (`transfer_package_id`) REFERENCES `tbl_ilds_transfer_package` (`id`)
) ENGINE=InnoDB;


CREATE TABLE `tbl_ilds_transfer_site` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `last_modified_at` datetime DEFAULT NULL,
  `version` bigint(20) NOT NULL,
  `compression_password` varchar(255) DEFAULT NULL,
  `credential_type` int(11) DEFAULT NULL,
  `destination_type` int(11) DEFAULT NULL,
  `encryption_key_name` varchar(255) DEFAULT NULL,
  `file_rename_suffix` varchar(255) DEFAULT NULL,
  `ip` varchar(255) DEFAULT NULL,
  `mft_transfer_site_id` varchar(255) DEFAULT NULL,
  `port` int(11) DEFAULT NULL,
  `remote_path` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `trigger_required` int(1) DEFAULT NULL,
  `dispatcher` varchar(100) DEFAULT NULL,
  `status` int(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_skxlb9d1cpdmhpwjjrjjni1yl` (`username`,`ip`,`port`,`remote_path`)
) ENGINE=InnoDB;


CREATE TABLE `tbl_ilds_pgp_encryption_key` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `last_modified_at` datetime DEFAULT NULL,
  `version` bigint(20) NOT NULL,
  `key_content` longtext,
  `key_name` varchar(255) DEFAULT NULL,
  `transfer_site_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_transfer_site_id_key_name` (`transfer_site_id`,`key_name`),
  KEY `FK_ox1kmm41m7632yq8ont3190b5` (`transfer_site_id`),
  CONSTRAINT `FK_ox1kmm41m7632yq8ont3190b5` FOREIGN KEY (`transfer_site_id`) REFERENCES `tbl_ilds_transfer_site` (`id`)
) ENGINE=InnoDB;


ALTER TABLE `tbl_ilds_transfer_site` ADD `dispatcher_password` varchar(255) NULL;
ALTER TABLE `tbl_ilds_transfer_site` ADD `dispatcher_key_name` varchar(255) NULL;
ALTER TABLE `tbl_ilds_transfer_site` ADD `dispatcher_key_passphrase` varchar(255) NULL;

