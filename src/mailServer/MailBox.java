package mailServer;

import java.util.ArrayList;
import java.util.List;

public class MailBox {
    private List<Mail> mails = new ArrayList<>();
    private String name;
    private List<String> attributes = new ArrayList<>();

    public MailBox(String name) {
        this.name = name;
    }

    public void add(Mail mail) {
        mails.add(mail);
    }

    public List<Mail> getMails() {
        return mails;
    }

    public String getName() {
        return name;
    }

    public void addAttribute(String attribute) {
        attributes.add(attribute);
    }

    public String getAttributesString() {
        var result = "";
        for (String attribute: attributes) {
            result += String.format("\\%s ", attribute);
        }
        return result.trim();
    }
    
}
