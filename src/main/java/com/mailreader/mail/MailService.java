package com.mailreader.mail;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final String INBOX = "INBOX";
    private static final String PROTOCOL_IMAPS = "imaps";

    @Value("${mail.reader.imap-host:imap.gmail.com}")
    private String imapHost;

    @Value("${mail.reader.imap-port:993}")
    private int imapPort;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public List<MailSummary> listRecentMails(int maxCount) throws MessagingException {
        return listMails(null, maxCount);
    }

    public List<MailSummary> listMailsBySubject(String subjectPattern, int maxCount) throws MessagingException {
        return listMails(new SubjectTerm(subjectPattern), maxCount);
    }

    private List<MailSummary> listMails(SearchTerm searchTerm, int maxCount) throws MessagingException {
        List<MailSummary> result = new ArrayList<>();
        try (Store store = openStore()) {
            Folder inbox = store.getFolder(INBOX);
            inbox.open(Folder.READ_ONLY);

            Message[] messages = searchTerm != null
                    ? inbox.search(searchTerm)
                    : inbox.getMessages();

            sortNewestFirst(messages);

            int limit = Math.min(maxCount, messages.length);
            for (int i = 0; i < limit; i++) {
                result.add(toSummary(messages[i], i + 1));
            }
            inbox.close(false);
        }
        return result;
    }

    public String readMail(int oneBasedIndex) throws MessagingException {
        try (Store store = openStore()) {
            Folder inbox = store.getFolder(INBOX);
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.getMessages();
            sortNewestFirst(messages);

            if (oneBasedIndex < 1 || oneBasedIndex > messages.length) {
                throw new IllegalArgumentException(
                        "Invalid message index: " + oneBasedIndex + " (valid: 1-" + messages.length + ")");
            }

            Message msg = messages[oneBasedIndex - 1];
            String from = addressesToString(msg.getFrom());
            String subject = msg.getSubject() != null ? msg.getSubject() : "(no subject)";
            String body = getTextContent(msg);

            inbox.close(false);
            return "From: " + from + "\nSubject: " + subject + "\n\n" + body;
        }
    }

    public void sendMail(String to, String[] cc, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(username);
        helper.setTo(to);
        if (cc != null && cc.length > 0) {
            helper.setCc(cc);
        }
        helper.setSubject(subject != null ? subject : "(no subject)");
        helper.setText(body, false);
        mailSender.send(message);
    }

    public void reply(String to, String subject, String body) throws MessagingException {
        sendMail(to, null, subject, body);
    }

    private Store openStore() throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", PROTOCOL_IMAPS);
        props.put("mail.imap.host", imapHost);
        props.put("mail.imap.port", String.valueOf(imapPort));
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.ssl.trust", "*");

        Session session = Session.getInstance(props);
        Store store = session.getStore(PROTOCOL_IMAPS);
        store.connect(imapHost, username, password);
        return store;
    }

    private static void sortNewestFirst(Message[] messages) {
        Arrays.sort(messages, (a, b) -> {
            try {
                Date da = a.getSentDate();
                Date db = b.getSentDate();
                if (da != null && db != null) return db.compareTo(da);
            } catch (MessagingException ignored) {
            }
            return 0;
        });
    }

    private static MailSummary toSummary(Message msg, int index) throws MessagingException {
        MailSummary s = new MailSummary();
        s.setIndex(index);
        try {
            s.setSubject(MimeUtility.decodeText(msg.getSubject() != null ? msg.getSubject() : "(no subject)"));
        } catch (Exception e) {
            s.setSubject(msg.getSubject());
        }
        s.setFrom(addressesToString(msg.getFrom()));
        s.setSentDate(msg.getSentDate() != null ? msg.getSentDate().toString() : null);
        s.setReceivedDate(msg.getReceivedDate() != null ? msg.getReceivedDate().toString() : null);
        return s;
    }

    private static String addressesToString(Address[] addresses) {
        if (addresses == null || addresses.length == 0) return "";
        return Arrays.stream(addresses)
                .map(a -> a instanceof InternetAddress ia ? ia.getAddress() : a.toString())
                .reduce((x, y) -> x + ", " + y)
                .orElse("");
    }

    private static String getTextContent(Message message) throws MessagingException {
        try {
            Object content = message.getContent();
            if (content instanceof String s) return s;
            if (content instanceof Multipart mp) {
                for (int i = 0; i < mp.getCount(); i++) {
                    BodyPart part = mp.getBodyPart(i);
                    String ct = part.getContentType();
                    if (ct != null && ct.toLowerCase().contains("text/plain")) {
                        return part.getContent().toString();
                    }
                }
                for (int i = 0; i < mp.getCount(); i++) {
                    BodyPart part = mp.getBodyPart(i);
                    String ct = part.getContentType();
                    if (ct != null && ct.toLowerCase().contains("text/")) {
                        return part.getContent().toString();
                    }
                }
            }
            return content != null ? content.toString() : "";
        } catch (IOException e) {
            throw new MessagingException("Failed to read message content", e);
        }
    }

    public static class MailSummary {
        private int index;
        private String subject;
        private String from;
        private String sentDate;
        private String receivedDate;

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getSentDate() { return sentDate; }
        public void setSentDate(String sentDate) { this.sentDate = sentDate; }
        public String getReceivedDate() { return receivedDate; }
        public void setReceivedDate(String receivedDate) { this.receivedDate = receivedDate; }
    }
}
