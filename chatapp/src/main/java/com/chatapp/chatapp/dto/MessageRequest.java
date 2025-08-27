package com.chatapp.chatapp.dto;

public class MessageRequest {
    private String sender;
    private String receiver;

    public MessageRequest(String sender, String receiver, String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

    private String content;

    private String fileUrl;
    private String fileType;

    public String getSender()
    {
        return sender;
    }
    public void setSender(String sender)
    {
        this.sender=sender;
    }
    public String getReceiver()
    {
        return receiver;
    }
    public void setReceiver(String receiver)
    {
        this.receiver=receiver;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content)
    {
        this.content=content;
    }
    public String getFileUrl() {
        return fileUrl;
    }
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileType() {
        return fileType;
    }
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
