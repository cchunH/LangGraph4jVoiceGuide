package com.msjl.langgraph4j.voiceguide.service;

import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideStreamEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class VoiceGuideStreamSessionService {

    private static final long SSE_TIMEOUT_MS = 30L * 60L * 1000L;

    private final ConcurrentHashMap<String, SessionBucket> sessions = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String sessionId) {
        SessionBucket bucket = sessions.computeIfAbsent(sessionId, key -> new SessionBucket());
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        bucket.emitters.add(emitter);
        emitter.onCompletion(() -> bucket.emitters.remove(emitter));
        emitter.onTimeout(() -> {
            bucket.emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(error -> bucket.emitters.remove(emitter));

        for (VoiceGuideStreamEvent event : bucket.backlog) {
            send(emitter, event);
        }
        if (bucket.completed) {
            emitter.complete();
        }
        return emitter;
    }

    public void publish(String sessionId, VoiceGuideStreamEvent event) {
        SessionBucket bucket = sessions.computeIfAbsent(sessionId, key -> new SessionBucket());
        bucket.backlog.add(event);
        for (SseEmitter emitter : bucket.emitters) {
            if (!send(emitter, event)) {
                bucket.emitters.remove(emitter);
            }
        }
    }

    public void complete(String sessionId) {
        SessionBucket bucket = sessions.computeIfAbsent(sessionId, key -> new SessionBucket());
        bucket.completed = true;
        for (SseEmitter emitter : bucket.emitters) {
            emitter.complete();
        }
        bucket.emitters.clear();
    }

    public List<VoiceGuideStreamEvent> listBufferedEvents(String sessionId) {
        SessionBucket bucket = sessions.get(sessionId);
        if (bucket == null) {
            return List.of();
        }
        return new ArrayList<>(bucket.backlog);
    }

    private boolean send(SseEmitter emitter, VoiceGuideStreamEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(event.getEventType().name().toLowerCase())
                    .data(event));
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static final class SessionBucket {
        private final List<VoiceGuideStreamEvent> backlog = new CopyOnWriteArrayList<>();
        private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
        private volatile boolean completed;
    }
}
