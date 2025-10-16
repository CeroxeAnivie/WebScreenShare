package fun.ceroxe.webScreenShare.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import fun.ceroxe.webScreenShare.service.ScreenShareService;
import fun.ceroxe.webScreenShare.WebScreenShareApplication; // Import for logDebug
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignalingWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // Cache candidates per SENDER session ID, targeting specific RECEIVER session IDs
    // Key: senderId, Value: Map<Key: receiverId, Value: List of candidate messages>
    private final Map<String, Map<String, List<Map<String, Object>>>> pendingCandidatesPerReceiver = new ConcurrentHashMap<>();
    private final ScreenShareService screenShareService;

    @Autowired
    public SignalingWebSocketHandler(ScreenShareService screenShareService) {
        this.screenShareService = screenShareService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        WebScreenShareApplication.logDebug("New WebSocket connection: " + session.getId());
        // Do not add to sessions map until authenticated
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = (String) message.getPayload();
        WebScreenShareApplication.logDebug("Received message from " + session.getId() + ": " + payload);

        // Check if session is authenticated
        if (!screenShareService.isAuthenticated(session.getId())) {
            // Expecting initial authentication message
            Map<String, Object> receivedMessage = objectMapper.readValue(payload, Map.class);
            if ("auth".equals(receivedMessage.get("type"))) {
                String providedPassword = (String) receivedMessage.get("password");
                if (screenShareService.checkPassword(providedPassword)) {
                    String role = (String) receivedMessage.get("role"); // "sender" or "receiver"
                    String existingSenderId = screenShareService.getSenderSessionId();

                    // --- ENFORCE SINGLE SENDER ---
                    if ("sender".equals(role) && existingSenderId != null && sessions.containsKey(existingSenderId)) {
                        // Check if the existing sender is still connected
                        WebSocketSession existingSenderSession = sessions.get(existingSenderId);
                        if (existingSenderSession != null && existingSenderSession.isOpen()) {
                            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "auth_error", "message", "Another sender is already connected."))));
                            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Another sender is already connected."));
                            WebScreenShareApplication.logDebug("Attempted to connect a second sender, rejecting new session " + session.getId());
                            return; // Exit after closing
                        } else {
                            // Existing sender session is closed, remove its role
                            screenShareService.cleanupSession(existingSenderId);
                        }
                    }
                    // --------------------------

                    screenShareService.setAuthenticated(session.getId(), role);
                    sessions.put(session.getId(), session);

                    // --- NEW LOGIC: Send pending candidates to new receiver ---
                    if ("receiver".equals(role)) {
                        String senderId = screenShareService.getSenderSessionId();
                        if (senderId != null) {
                            // Check if there are pending candidates cached for this specific sender-receiver pair
                            Map<String, List<Map<String, Object>>> cachedCandidatesMap = pendingCandidatesPerReceiver.get(senderId);
                            if (cachedCandidatesMap != null) {
                                List<Map<String, Object>> cachedCandidatesForThisReceiver = cachedCandidatesMap.get(session.getId()); // Use receiver's session ID as key
                                if (cachedCandidatesForThisReceiver != null && !cachedCandidatesForThisReceiver.isEmpty()) {
                                    WebScreenShareApplication.logDebug("Found " + cachedCandidatesForThisReceiver.size() + " cached candidates for sender " + senderId + " targeting receiver " + session.getId() + ", sending them now.");
                                    for (Map<String, Object> candidate : cachedCandidatesForThisReceiver) {
                                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(candidate)));
                                    }
                                    // Remove the sent candidates from cache for this specific receiver
                                    cachedCandidatesMap.remove(session.getId());
                                } else {
                                    WebScreenShareApplication.logDebug("No cached candidates found for sender " + senderId + " targeting receiver " + session.getId() + ".");
                                }
                            } else {
                                WebScreenShareApplication.logDebug("No cached candidates map found for sender " + senderId + ".");
                            }
                            // Note: No need to send cached offer here. Receiver will request it.
                        } else {
                            WebScreenShareApplication.logDebug("No sender found when receiver " + session.getId() + " connected.");
                        }
                    }
                    // --------------------------------------------------------------

                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "auth_success", "role", role))));
                    WebScreenShareApplication.logDebug("Session " + session.getId() + " authenticated as " + role);

                    // Print current session counts
                    int senderCount = (int) screenShareService.sessionRoles.entrySet().stream().filter(e -> "sender".equals(e.getValue())).count();
                    int receiverCount = (int) screenShareService.sessionRoles.entrySet().stream().filter(e -> "receiver".equals(e.getValue())).count();
                    WebScreenShareApplication.logDebug("Current sessions - Senders: " + senderCount + ", Receivers: " + receiverCount);

                } else {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "auth_error", "message", "Invalid password"))));
                    session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid password"));
                    return; // Exit after closing
                }
            } else {
                // If not an auth message, reject
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "error", "message", "Authentication required"))));
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Authentication required"));
                return;
            }
        } else {
            // Handle authenticated messages
            String role = screenShareService.getRole(session.getId());
            Map<String, Object> receivedMessage = objectMapper.readValue(payload, Map.class);
            String type = (String) receivedMessage.get("type");

            if ("offer".equals(type)) {
                // --- CRITICAL FIX: Handle Offer message from sender correctly ---
                // Sender sends offer -> Relay it to the specific receiver indicated by receiverId in the message.
                if ("sender".equals(role)) {
                    String senderId = session.getId();
                    // Extract the receiverId from the message payload sent by the sender
                    Object receiverIdObj = receivedMessage.get("receiverId");
                    if (receiverIdObj instanceof String) {
                        String receiverId = (String) receiverIdObj;
                        WebScreenShareApplication.logDebug("Sender " + senderId + " sending offer to specific receiver " + receiverId + ".");

                        // Find the target receiver's session
                        WebSocketSession targetReceiverSession = sessions.get(receiverId);
                        if (targetReceiverSession != null && targetReceiverSession.isOpen()) {
                            // Forward the offer message payload directly to the receiver
                            targetReceiverSession.sendMessage(new TextMessage(payload));
                            WebScreenShareApplication.logDebug("Successfully forwarded offer from sender " + senderId + " to receiver " + receiverId + ".");
                        } else {
                            WebScreenShareApplication.logDebug("Target receiver session " + receiverId + " not found or not open. Cannot forward offer.");
                            // Optionally, you could cache the offer here for a short time if the receiver is expected to connect soon,
                            // but typically it's better to let the sender retry or the receiver request again.
                        }
                    } else {
                        System.err.println("Received 'offer' message from sender " + senderId + " without a valid 'receiverId' string. Message: " + payload);
                        // Malformed message, log error
                    }
                } else {
                    WebScreenShareApplication.logDebug("Received offer from non-sender session " + session.getId());
                }
                // --- END CRITICAL FIX ---

            } else if ("answer".equals(type)) {
                // Receiver sends answer -> Relay to sender, including receiver ID
                if ("receiver".equals(role)) {
                    String receiverId = session.getId(); // The session ID of the receiver who sent the answer
                    List<String> targetIds = screenShareService.getTargetSessionIds(receiverId); // Get the sender
                    WebScreenShareApplication.logDebug("Receiver " + receiverId + " sending answer. Targeting senders: " + targetIds);
                    // Modify the message to include the receiverId before forwarding to sender
                    Map<String, Object> messageWithReceiverId = new java.util.HashMap<>(receivedMessage);
                    messageWithReceiverId.put("receiverId", receiverId); // Add the receiver ID

                    for (String targetId : targetIds) {
                        if (sessions.containsKey(targetId)) {
                            WebSocketSession targetSession = sessions.get(targetId);
                            if (targetSession.isOpen()) {
                                targetSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(messageWithReceiverId)));
                            } else {
                                WebScreenShareApplication.logDebug("Target sender session " + targetId + " is not open, cannot send answer.");
                            }
                        } else {
                            WebScreenShareApplication.logDebug("Target sender session " + targetId + " not found in sessions map, cannot send answer.");
                        }
                    }
                } else {
                    WebScreenShareApplication.logDebug("Received answer from non-receiver session " + session.getId());
                }
            } else if ("candidate".equals(type)) {
                // Relay candidate messages
                List<String> targetIds = screenShareService.getTargetSessionIds(session.getId());
                WebScreenShareApplication.logDebug("Session " + session.getId() + " (role: " + role + ") sending candidate. Targeting: " + targetIds);

                if ("sender".equals(role)) {
                    // Sender sends candidate -> Relay to receivers
                    if (!targetIds.isEmpty()) {
                        boolean sentToAtLeastOne = false;
                        for (String targetId : targetIds) {
                            if (sessions.containsKey(targetId)) {
                                WebSocketSession targetSession = sessions.get(targetId);
                                if (targetSession.isOpen()) {
                                    targetSession.sendMessage(new TextMessage(payload));
                                    sentToAtLeastOne = true;
                                } else {
                                    WebScreenShareApplication.logDebug("Target receiver session " + targetId + " is not open, cannot send candidate.");
                                }
                            } else {
                                WebScreenShareApplication.logDebug("Target receiver session " + targetId + " not found in sessions map, cannot send candidate.");
                            }
                        }
                        // If no targets were available or open, cache the candidate (only for sender -> receivers)
                        // Cache per sender -> receiver pair
                        if (!sentToAtLeastOne) {
                            String senderId = session.getId();
                            List<String> potentialTargets = screenShareService.getTargetSessionIds(senderId); // Get all receivers
                            WebScreenShareApplication.logDebug("No target receiver sessions found for candidate from sender " + senderId + ". Caching candidate for potential receivers: " + potentialTargets);
                            for (String receiverId : potentialTargets) {
                                pendingCandidatesPerReceiver.computeIfAbsent(senderId, k -> new ConcurrentHashMap<>())
                                        .computeIfAbsent(receiverId, k -> new ArrayList<>())
                                        .add(receivedMessage);
                            }
                            WebScreenShareApplication.logDebug("Cache size for sender-receiver pairs after caching: " + pendingCandidatesPerReceiver.get(senderId).size());
                        }
                    } else {
                        WebScreenShareApplication.logDebug("No target sessions found for candidate from " + session.getId());
                    }
                } else if ("receiver".equals(role)) {
                    // Receiver sends candidate -> Relay to sender, including receiver ID
                    String receiverId = session.getId(); // The session ID of the receiver who sent the candidate
                    // Modify the message to include the receiverId before forwarding to sender
                    Map<String, Object> messageWithReceiverId = new java.util.HashMap<>(receivedMessage);
                    messageWithReceiverId.put("receiverId", receiverId); // Add the receiver ID

                    for (String targetId : targetIds) { // targetIds should be the sender
                        if (sessions.containsKey(targetId)) {
                            WebSocketSession targetSession = sessions.get(targetId);
                            if (targetSession.isOpen()) {
                                targetSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(messageWithReceiverId)));
                            } else {
                                WebScreenShareApplication.logDebug("Target sender session " + targetId + " is not open, cannot send candidate.");
                            }
                        } else {
                            WebScreenShareApplication.logDebug("Target sender session " + targetId + " not found in sessions map, cannot send candidate.");
                        }
                    }
                }
            } else if ("start_share".equals(type) && "sender".equals(role)) {
                screenShareService.setSharingStatus(true);
                // Optionally notify receivers if they are already connected
                List<String> receiverIds = screenShareService.getReceiverSessionIds();
                WebScreenShareApplication.logDebug("Sender " + session.getId() + " started sharing. Notifying receivers: " + receiverIds);
                for (String receiverId : receiverIds) {
                    if (sessions.containsKey(receiverId)) {
                        sessions.get(receiverId).sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "sender_started"))));
                    }
                }
            } else if ("stop_share".equals(type) && "sender".equals(role)) {
                screenShareService.setSharingStatus(false);
                // Optionally notify receivers
                List<String> receiverIds = screenShareService.getReceiverSessionIds();
                WebScreenShareApplication.logDebug("Sender " + session.getId() + " stopped sharing. Notifying receivers: " + receiverIds);
                for (String receiverId : receiverIds) {
                    if (sessions.containsKey(receiverId)) {
                        sessions.get(receiverId).sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "sender_stopped"))));
                    }
                }
            } else if ("request_offer".equals(type) && "receiver".equals(role)) {
                // Receiver explicitly requests an offer (e.g., after connecting or refreshing)
                String receiverId = session.getId(); // Get the ID of the requesting receiver
                String senderId = screenShareService.getSenderSessionId();
                WebScreenShareApplication.logDebug("Receiver " + receiverId + " requested offer. Notifying sender " + senderId);
                if (senderId != null && sessions.containsKey(senderId)) {
                    WebSocketSession senderSession = sessions.get(senderId);
                    if (senderSession.isOpen()) {
                        // Inform the sender that a receiver wants an offer.
                        // Send a specific message to the sender to trigger re-offer, including the requesting receiver's ID.
                        // The sender.html will listen for this message type and the receiverId.
                        WebScreenShareApplication.logDebug("Sending 'receiver_requests_offer' message (for receiver " + receiverId + ") to sender " + senderId);
                        // Send the receiver ID within the message payload
                        senderSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "receiver_requests_offer", "receiverId", receiverId))));
                    } else {
                        WebScreenShareApplication.logDebug("Sender session " + senderId + " is not open, cannot notify for request.");
                    }
                } else {
                    WebScreenShareApplication.logDebug("No sender found to notify about receiver " + receiverId + " request.");
                }
            }
        }
    }


    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("Transport error for session " + session.getId() + ": " + exception.getMessage());
        cleanupSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        WebScreenShareApplication.logDebug("WebSocket connection closed: " + session.getId() + ", status: " + status);
        cleanupSession(session);
    }

    private void cleanupSession(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        // Remove any pending candidates associated with this session as sender or receiver
        // Remove as sender
        pendingCandidatesPerReceiver.remove(sessionId);
        // Remove as receiver from all sender caches
        // Iterate over all sender caches and remove entries for this receiver ID
        for (Map<String, List<Map<String, Object>>> receiverCandidateMap : pendingCandidatesPerReceiver.values()) {
            receiverCandidateMap.remove(sessionId); // 'sessionId' here is the receiver ID being removed
        }

        screenShareService.cleanupSession(sessionId); // Notify service to clear role mappings

        // Print current session counts after cleanup
        int senderCount = (int) screenShareService.sessionRoles.entrySet().stream().filter(e -> "sender".equals(e.getValue())).count();
        int receiverCount = (int) screenShareService.sessionRoles.entrySet().stream().filter(e -> "receiver".equals(e.getValue())).count();
        WebScreenShareApplication.logDebug("Session " + sessionId + " cleaned up. Current sessions - Senders: " + senderCount + ", Receivers: " + receiverCount);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}