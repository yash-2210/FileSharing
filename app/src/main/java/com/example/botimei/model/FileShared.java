package com.example.botimei.model;

public class FileShared {
    String fileUrl, id, username, sender, filename, IMEI, Contact, SMS;

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getIMEI() {
        return IMEI;
    }

    public void setIMEI(String imei) {
        this.IMEI = imei;
    }

    public String getContact() {
        return Contact;
    }

    public void setContact(String contact) {
        this.Contact = contact;
    }

    public String getSMS() {
        return SMS;
    }

    public void setSms(String SMS) {
        this.SMS = SMS;
    }

    public FileShared() {
    }

    public FileShared(String fileUrl, String id, String username, String sender, String filename, String IMEI, String Contact, String SMS) {
        this.fileUrl = fileUrl;
        this.id = id;
        this.username = username;
        this.sender = sender;
        this.filename = filename;
        this.IMEI = IMEI;
        this.Contact = Contact;
        this.SMS = SMS;
    }
}
