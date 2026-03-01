package com.mailreader.mail;

import jakarta.mail.MessagingException;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MailMcpTools {

    private final MailService mailService;

    public MailMcpTools(MailService mailService) {
        this.mailService = mailService;
    }

    @Tool(description = "List the most recent emails in the inbox. Returns index, subject, from address, and sent date for each email.")
    public String listRecentMails(
            @ToolParam(description = "Maximum number of emails to return, defaults to 10", required = false) Integer maxCount
    ) {
        int limit = maxCount != null && maxCount > 0 ? Math.min(maxCount, 100) : 10;
        try {
            List<MailService.MailSummary> mails = mailService.listRecentMails(limit);
            return formatSummaries(mails);
        } catch (MessagingException e) {
            return "Error listing mails: " + e.getMessage();
        }
    }

    @Tool(description = "Search emails whose subject contains the given text. Returns index, subject, from address, and sent date for each match.")
    public String listMailsBySubject(
            @ToolParam(description = "Text to search for in email subjects") String subjectMatch,
            @ToolParam(description = "Maximum number of emails to return, defaults to 20", required = false) Integer maxCount
    ) {
        int limit = maxCount != null && maxCount > 0 ? Math.min(maxCount, 100) : 20;
        try {
            List<MailService.MailSummary> mails = mailService.listMailsBySubject(subjectMatch, limit);
            return formatSummaries(mails);
        } catch (MessagingException e) {
            return "Error searching mails by subject: " + e.getMessage();
        }
    }

    @Tool(description = "Read the full body of an email by its 1-based index (use listRecentMails or listMailsBySubject first to get the index).")
    public String readMail(
            @ToolParam(description = "1-based index of the email to read") Integer index
    ) {
        if (index == null || index < 1) {
            return "Invalid index: must be >= 1";
        }
        try {
            return mailService.readMail(index);
        } catch (MessagingException e) {
            return "Error reading mail: " + e.getMessage();
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    @Tool(description = "Send a new email to one or more recipients with the specified subject and plain-text body.")
    public String sendMail(
            @ToolParam(description = "Recipient email address (To)") String to,
            @ToolParam(description = "Comma-separated CC email addresses", required = false) String cc,
            @ToolParam(description = "Subject line of the email") String subject,
            @ToolParam(description = "Plain text body of the email") String body
    ) {
        try {
            String[] ccArray = (cc != null && !cc.isBlank())
                    ? Arrays.stream(cc.split(",")).map(String::trim).toArray(String[]::new)
                    : null;
            mailService.sendMail(to, ccArray, subject, body);
            return "Email sent successfully to " + to + (ccArray != null ? " (CC: " + cc + ")" : "");
        } catch (MessagingException e) {
            return "Error sending email: " + e.getMessage();
        }
    }

    @Tool(description = "Send a reply email to a given address with the specified subject and plain-text body.")
    public String replyToMail(
            @ToolParam(description = "Recipient email address") String to,
            @ToolParam(description = "Subject line for the reply, e.g. Re: original subject", required = false) String subject,
            @ToolParam(description = "Plain text body of the reply") String body
    ) {
        try {
            mailService.reply(to, subject != null ? subject : "Re: ", body);
            return "Reply sent successfully to " + to;
        } catch (MessagingException e) {
            return "Error sending reply: " + e.getMessage();
        }
    }

    private static String formatSummaries(List<MailService.MailSummary> mails) {
        if (mails.isEmpty()) return "No emails found.";
        return mails.stream()
                .map(m -> String.format("[%d] From: %s | Subject: %s | Sent: %s",
                        m.getIndex(), m.getFrom(), m.getSubject(), m.getSentDate()))
                .collect(Collectors.joining("\n"));
    }
}
