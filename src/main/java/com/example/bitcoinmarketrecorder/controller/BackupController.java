package com.example.bitcoinmarketrecorder.controller;

import com.example.bitcoinmarketrecorder.service.DatabaseBackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

  @Autowired private DatabaseBackupService backupService;

  @PostMapping("/manual")
  public ResponseEntity<String> triggerManualBackup() {
    try {
      backupService.manualBackup();
      return ResponseEntity.ok("Backup completed successfully");
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("Backup failed: " + e.getMessage());
    }
  }
}
