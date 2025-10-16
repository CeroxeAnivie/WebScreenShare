package fun.ceroxe.webScreenShare.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // Controller to serve the sender HTML page
    @GetMapping("/sender")
    public String senderPage() {
        return "sender"; // Points to src/main/resources/static/sender.html
    }

    // Controller to serve the receiver HTML page
    @GetMapping("/receiver")
    public String receiverPage() {
        return "receiver"; // Points to src/main/resources/static/receiver.html
    }
}