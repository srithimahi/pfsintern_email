package com.example.emailclientGUI;

import javax.mail.*;
import javax.mail.internet.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class EmailClientGUI extends JFrame {
    private JTextField recipientField;
    private JTextField subjectField;
    private JTextArea messageArea;
    private JButton sendButton;
    private JButton readEmailsButton;
    private JTextArea inboxArea;

    public EmailClientGUI() {
        setTitle("Simple Email Client");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Compose Panel
        JPanel composePanel = new JPanel();
        composePanel.setLayout(new GridLayout(3, 1));

        recipientField = new JTextField();
        composePanel.add(new JLabel("Recipient:"));
        composePanel.add(recipientField);

        subjectField = new JTextField();
        composePanel.add(new JLabel("Subject:"));
        composePanel.add(subjectField);

        messageArea = new JTextArea(10, 30);
        add(new JScrollPane(messageArea), BorderLayout.CENTER);

        // Send Email Button
        sendButton = new JButton("Send Email");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String to = recipientField.getText();
                String subject = subjectField.getText();
                String message = messageArea.getText();
                sendEmail(to, subject, message);
            }
        });

        // Panel for sending and reading emails
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(sendButton, BorderLayout.NORTH);

        // Read Emails button
        readEmailsButton = new JButton("Read Emails");
        readEmailsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                readEmails();
            }
        });
        bottomPanel.add(readEmailsButton, BorderLayout.SOUTH);

        add(composePanel, BorderLayout.NORTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // Inbox Panel
        inboxArea = new JTextArea(10, 30);
        inboxArea.setEditable(false);
        add(new JScrollPane(inboxArea), BorderLayout.EAST);

        addDefaultEmails();
    }

    // Method to load properties from config.properties file
    private Properties loadProperties() {
        Properties properties = new Properties();
        try {
            FileInputStream input = new FileInputStream("config.properties");
            properties.load(input);
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    // Send email method
    private void sendEmail(String to, String subject, String messageBody) {
        Properties properties = loadProperties();
        String from = properties.getProperty("email.user");
        String password = properties.getProperty("email.pass");
        String host = "smtp.gmail.com";

        Properties mailProps = new Properties();
        mailProps.put("mail.smtp.host", host);
        mailProps.put("mail.smtp.port", "587"); // Use 587 for TLS
        mailProps.put("mail.smtp.starttls.enable", "true");
        mailProps.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(mailProps, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(messageBody);
            Transport.send(message);
            JOptionPane.showMessageDialog(this, "Email sent successfully!");
        } catch (MessagingException mex) {
            JOptionPane.showMessageDialog(this, "Error sending email: " + mex.getMessage());
            mex.printStackTrace();
        }
    }

    // Read emails method
    // Read emails method
    // Read emails method
    private void readEmails() {
        new Thread(() -> { // Run in a separate thread to avoid blocking the GUI
            Properties properties = new Properties();
            properties.put("mail.imap.ssl.enable", "true");
            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imap.connectiontimeout", "10000"); // 10 seconds timeout
            properties.put("mail.imap.timeout", "10000"); // 10 seconds timeout

            String username = "srithika20089@gmail.com"; // Replace with your actual email
            String password = "itru exkw lsiu yzlb"; // Use the app password generated

            boolean retry = true;
            int retryCount = 0;
            final int maxRetries = 3;

            while (retry && retryCount < maxRetries) {
                retry = false; // Assume this attempt will succeed
                Session emailSession = Session.getInstance(properties);
                Store store = null;
                Folder emailFolder = null;

                try {
                    System.out.println("Connecting to IMAP server...");
                    store = emailSession.getStore("imaps");
                    store.connect("imap.gmail.com", username, password);
                    System.out.println("Connected to IMAP server.");

                    emailFolder = store.getFolder("INBOX");
                    emailFolder.open(Folder.READ_ONLY);
                    System.out.println("Opened INBOX folder.");

                    Message[] messages = emailFolder.getMessages();
                    System.out.println("Number of messages in inbox: " + messages.length);

                    SwingUtilities.invokeLater(() -> {
                        inboxArea.setText(""); // Clear previous emails

                        if (messages.length == 0) {
                            inboxArea.append("No emails found in the inbox.");
                        } else {
                            for (Message message : messages) {
                                try {
                                    // Retrieve message details safely
                                    String from = (message.getFrom() != null && message.getFrom().length > 0) ? message.getFrom()[0].toString() : "Unknown";
                                    String subject = message.getSubject() != null ? message.getSubject() : "No Subject";
                                    String sentDate = message.getSentDate() != null ? message.getSentDate().toString() : "Unknown Date";

                                    String emailDetails = "From: " + from +
                                            "\nSubject: " + subject +
                                            "\nDate: " + sentDate +
                                            "\n\n";

                                    System.out.println("Appending message: " + emailDetails);
                                    inboxArea.append(emailDetails);
                                } catch (Exception e) {
                                    System.out.println("Error processing message: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                } catch (FolderClosedException e) {
                    System.out.println("FolderClosedException: " + e.getMessage());
                    e.printStackTrace();
                    retry = true;
                    retryCount++;
                    System.out.println("Retrying... Attempt " + retryCount);

                    // Close and reconnect
                    try {
                        if (emailFolder != null && emailFolder.isOpen()) {
                            emailFolder.close(false);
                        }
                        if (store != null) {
                            store.close();
                        }
                    } catch (Exception closeEx) {
                        closeEx.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        inboxArea.setText("Failed to retrieve emails: " + e.getMessage());
                    });
                } finally {
                    try {
                        if (emailFolder != null && emailFolder.isOpen()) {
                            emailFolder.close(false);
                        }
                        if (store != null) {
                            store.close();
                        }
                    } catch (Exception closeEx) {
                        closeEx.printStackTrace();
                    }
                }
            }

            if (retryCount == maxRetries) {
                SwingUtilities.invokeLater(() -> inboxArea.setText("Failed to retrieve emails after " + maxRetries + " attempts."));
            }
        }).start();
    }







    private void addDefaultEmails() {
        String[] defaultEmails = {
                "From: john.doe@example.com\nSubject: Welcome!\n\nHello! Welcome to our email service.",
                "From: jane.smith@example.com\nSubject: Meeting Reminder\n\nDon't forget about our meeting tomorrow at 10 AM.",
                "From: support@example.com\nSubject: Account Verification\n\nPlease verify your account by clicking the link.",
                "From: news@example.com\nSubject: Monthly Newsletter\n\nCheck out our latest updates and articles."
        };

        for (String email : defaultEmails) {
            inboxArea.append(email + "\n\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new EmailClientGUI().setVisible(true);
            }
        });
    }
}

