package mailServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lauri Holopainen
 *
 */
public class MailServer {
    private List<MailBox> mailboxes = new ArrayList<>();

    public static void main(String[] args) {
        var server = new MailServer();
        var mailBox = new MailBox("INBOX"); // Add default mailbox
        mailBox.addAttribute("Inbox");
        server.addMailBox(mailBox);
        server.addMail(new Mail("Test mail 1.\nOn Two rows?", "example@example.com",
                List.of("customer@example.com", "customer2@example.com")));
        while (true) {
            try {
                SmtpListener smtp = new SmtpListener(server);
                smtp.listenSmtp();
                Pop3Listener pop3 = new Pop3Listener(server);
                pop3.listenPop3();
                ImapListener imap = new ImapListener(server);
                imap.listenImap();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addMailBox(MailBox mailBox) {
        mailboxes.add(mailBox);
    }

    public int countOctets(int mailbox) {
        var result = 0;
        for (Mail mail : mailboxes.get(mailbox).getMails()) {
            result += mail.data.getBytes().length;
        }
        return result;
    }

    public void addMail(Mail mail) {
        addMail(mail, 0);
    }

    public void addMail(Mail mail, int mailBox) {
        mailboxes.get(mailBox).add(mail);
    }

    public List<Mail> getMails() {
        return getMails(0);
    }

    public List<Mail> getMails(int mailBox) {
        return mailboxes.get(mailBox).getMails();
    }

    public List<MailBox> getMailBoxes() {
        return mailboxes;
    }
}
