package mailServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lauri Holopainen
 *
 */
public class MailServer {
    private List<Mail> mailbox = new ArrayList<>();

    public static void main(String[] args) {
        var server = new MailServer();
        while (true) {
            try {
                SmtpListener smtp = new SmtpListener(server);
                smtp.listenSmtp();
                Pop3Listener pop3 = new Pop3Listener(server); 
                pop3.listenPop3();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int countOctets() {
        var result = 0;
        for (Mail mail : mailbox) {
            result += mail.data.getBytes().length;
        }
        return result;
    }


    public void addMail(Mail mail) {
        mailbox.add(mail);        
    }

    public List<Mail> getMailbox() {
        return mailbox;
    }
}
