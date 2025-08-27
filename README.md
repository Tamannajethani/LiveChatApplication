# LiveChatApplication

## Overview

This is a real-time chat application built with Spring Boot (backend) and JavaFX (frontend).
It supports:
User authentication (register/login)
Real-time messaging using WebSocket
File and media sharing (images, videos, documents)
Persistent chat history with MySQL database
Media served from local file storage (uploads/ folder)

## Tech Stack

Backend: Spring Boot 3 (Web, WebSocket, JPA, Security)
Frontend: JavaFX (desktop client)
Database: MySQL
File Storage: Local disk (uploads/) exposed via /files/**
Build Tool: Maven

## Features

🔐 User Authentication – register and login securely
💬 Text Messaging – instant chat with WebSocket
📎 File Sharing – upload and send media (images, video, docs)
🗂️ Chat History – stored in DB and retrievable via REST API
⚡ Real-Time Updates – powered by WebSocket broker (/topic/messages)

## Architecture Flow

User registers/logs in → via REST (AuthController).
User sends a message:
Text: sent via WebSocket → persisted in DB → broadcasted.
Media: file uploaded to uploads/ → DB stores fileUrl + fileType → broadcasted.
Other clients subscribed to /topic/messages instantly receive the message.
JavaFX client renders:
Text in chat bubbles,
Images in an ImageView,
Videos in a MediaPlayer,
Other files as download links.
Chat history fetched via REST (/messages/chat).

## Data Storage

Database (MySQL): stores users + message metadata (content, fileUrl, fileType, sender, receiver, timestamp).
Uploads Folder: actual media files stored locally in /uploads and served at /files/**.

## API Endpoints

### Authentication
POST /auth/register – register new user
POST /auth/login – login
### Messages
GET /messages/chat?sender={s}&receiver={r} – fetch chat history
POST /messages/sendFile – send media file (persists + broadcasts)
### File Upload
POST /files/upload – upload a file to uploads/ folder
### WebSocket
Send message: /app/sendMessage
Subscribe to messages: /topic/messages

## Internship Task - Next24tech

This project is Task2 for the Java Development Internship at Next24tech.

## Final Status

Text & media chat fully functional
DB + file storage integration working
Real-time updates tested and verified
Project completed and stable
