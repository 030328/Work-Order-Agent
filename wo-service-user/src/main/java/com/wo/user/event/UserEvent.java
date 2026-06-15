package com.wo.user.event;

import com.wo.user.entity.SysUser;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Application event fired when a user is created, updated, or deleted.
 * Can be consumed by other services via Spring's event mechanism.
 */
@Getter
public class UserEvent extends ApplicationEvent {

    public enum EventType {
        CREATED, UPDATED, DELETED, LOGIN
    }

    private final EventType eventType;
    private final SysUser user;

    public UserEvent(Object source, EventType eventType, SysUser user) {
        super(source);
        this.eventType = eventType;
        this.user = user;
    }
}
