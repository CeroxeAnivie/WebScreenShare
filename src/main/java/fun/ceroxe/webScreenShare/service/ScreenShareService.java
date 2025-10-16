package fun.ceroxe.webScreenShare.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ScreenShareService {
    private String password = null; // Store the optional password, null if none set
    public final Map<String, String> sessionRoles = new ConcurrentHashMap<>(); // Maps session ID to role ("sender" or "receiver")
    private volatile boolean sharingActive = false; // Tracks if sharing is currently active

    public void setPassword(String password) {
        // Set password to null if the provided string is null or empty
        if (password !=null && !password.isEmpty()){
            this.password=password;
        }else{
            this.password=null;
        }
    }
    public String getPassword() {
        return password;
    }

    public boolean checkPassword(String providedPassword) {
        // If no password is set (password is null), authentication always passes
        if (password == null) {
            return true;
        }
        // If password is set, compare it with the provided one
        // Handle case where providedPassword is null (shouldn't happen with UI, but be safe)
        return password.equals(providedPassword != null ? providedPassword : "");
    }

    public void setAuthenticated(String sessionId, String role) {
        sessionRoles.put(sessionId, role);
    }

    public boolean isAuthenticated(String sessionId) {
        return sessionRoles.containsKey(sessionId);
    }

    public String getRole(String sessionId) {
        return sessionRoles.get(sessionId);
    }

    public void setSharingStatus(boolean active) {
        this.sharingActive = active;
    }

    public boolean isSharingActive() {
        return this.sharingActive;
    }

    // Returns the sender's session ID, or null if no sender is present
    public String getSenderSessionId() {
        return sessionRoles.entrySet().stream()
                .filter(entry -> "sender".equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    // Returns a list of receiver session IDs
    public List<String> getReceiverSessionIds() {
        return sessionRoles.entrySet().stream()
                .filter(entry -> "receiver".equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // For a given session, returns the target(s) based on its role
    public List<String> getTargetSessionIds(String sessionId) {
        String role = getRole(sessionId);
        if ("sender".equals(role)) {
            // Sender wants to send to all receivers
            return getReceiverSessionIds(); // This should return a *snapshot* of current receivers
        } else if ("receiver".equals(role)) {
            // Receiver wants to receive from the sender
            String senderId = getSenderSessionId();
            if (senderId != null && !senderId.equals(sessionId)) { // Ensure receiver doesn't target itself
                return List.of(senderId);
            }
        }
        return List.of(); // Return empty list if no targets found
    }

    public void cleanupSession(String sessionId) {
        sessionRoles.remove(sessionId);
        // If the removed session was the sender, sharing stops
        if ("sender".equals(getRole(sessionId))) {
            setSharingStatus(false);
        }
    }
}