package com.school.management.chat;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.socket.*;
import java.util.concurrent.*;
import java.time.Instant;

@Component
public class ChatSocketSessions implements DisposableBean {
 private final ConcurrentMap<String,WebSocketSession> sessions=new ConcurrentHashMap<>();
 private final ConcurrentMap<String,ScheduledFuture<?>> deadlines=new ConcurrentHashMap<>();
 private final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"chat-token-expiry");t.setDaemon(true);return t;});
 public void opened(WebSocketSession session){sessions.put(session.getId(),session);}
 public void closed(String id){sessions.remove(id);var task=deadlines.remove(id);if(task!=null)task.cancel(false);}
 public void expire(String id,Instant expires){if(id==null||expires==null)return;var old=deadlines.put(id,timer.schedule(()->{var session=sessions.get(id);if(session!=null)try{session.close(CloseStatus.POLICY_VIOLATION);}catch(Exception ignored){}closed(id);},Math.max(0,expires.toEpochMilli()-System.currentTimeMillis()),TimeUnit.MILLISECONDS));if(old!=null)old.cancel(false);}
 @Override public void destroy(){timer.shutdownNow();}
}
