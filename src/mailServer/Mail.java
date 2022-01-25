package mailServer;

import java.util.List;

public class Mail {
    public String data;
    public String sender;
    public List<String> recepients;
    
    public Mail(String data, String sender, List<String> rcpts) {
        this.data = data;
        this.sender = sender;
        this.recepients = rcpts;
    }
}
