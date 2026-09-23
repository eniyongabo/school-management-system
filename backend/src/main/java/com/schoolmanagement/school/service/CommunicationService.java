package com.schoolmanagement.school.service;

import static com.schoolmanagement.school.dto.SchoolDtos.*;
import static org.springframework.http.HttpStatus.*;

import com.schoolmanagement.identity.*;
import com.schoolmanagement.school.entity.*;
import com.schoolmanagement.school.repository.SchoolRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CommunicationService {

    private final SchoolRepository db;
    private final CurrentUser current;
    private final SchoolAccess access;
    private final UserRepository users;

    public CommunicationService(SchoolRepository d, CurrentUser c, SchoolAccess a, UserRepository u) {
        db = d;
        current = c;
        access = a;
        users = u;
    }

    public View announce(AnnouncementInput p) {
        var u = current.require("ADMINISTRATOR", "TEACHER");
        if (p.classId() == null && !u.has("ADMINISTRATOR")) throw new ResponseStatusException(
            FORBIDDEN,
            "Teachers may post to assigned classes only"
        );
        var a = new Announcement();
        a.author = u;
        a.schoolClass = p.classId() == null ? null : db.get(SchoolClass.class, p.classId());
        if (a.schoolClass != null) access.classWrite(a.schoolClass.id);
        a.title = p.title();
        a.body = p.body();
        a.priority = p.priority();
        a.publishedAt = Instant.now();
        db.save(a);
        for (var recipient : users.findAll()) {
            if (
                !recipient.active || (a.schoolClass != null && !access.classVisible(recipient, a.schoolClass))
            ) continue;
            var n = new Notification();
            n.recipient = recipient;
            n.announcement = a;
            n.title = a.title;
            n.createdAt = Instant.now();
            db.save(n);
        }
        return announcementView(a);
    }

    public List<View> announcements() {
        return db
            .query(Announcement.class, "select a from Announcement a order by a.publishedAt desc")
            .stream()
            .filter(a -> a.schoolClass == null || access.classVisible(a.schoolClass))
            .map(this::announcementView)
            .toList();
    }

    private View announcementView(Announcement a) {
        return view(
            "id",
            a.id,
            "title",
            a.title,
            "body",
            a.body,
            "priority",
            a.priority,
            "classId",
            a.schoolClass == null ? null : a.schoolClass.id,
            "className",
            a.schoolClass == null ? "School-wide" : a.schoolClass.name,
            "author",
            a.author.firstName + " " + a.author.lastName,
            "publishedAt",
            a.publishedAt
        );
    }

    public List<View> notifications() {
        var u = current.get();
        return db
            .query(
                Notification.class,
                "select n from Notification n where n.recipient.id=?1 order by n.createdAt desc",
                u.id
            )
            .stream()
            .filter(
                n -> n.announcement.schoolClass == null || access.classVisible(n.announcement.schoolClass)
            )
            .map(n ->
                view(
                    "id",
                    n.id,
                    "announcementId",
                    n.announcement.id,
                    "title",
                    n.title,
                    "body",
                    n.announcement.body,
                    "readAt",
                    n.readAt,
                    "createdAt",
                    n.createdAt
                )
            )
            .toList();
    }

    public void readNotification(long id) {
        var n = db.get(Notification.class, id);
        if (!n.recipient.id.equals(current.get().id)) throw SchoolAccess.hidden();
        n.readAt = Instant.now();
    }

    public List<View> contacts() {
        var u = current.get();
        return users
            .findAll()
            .stream()
            .filter(other -> access.canMessage(u, other))
            .map(other ->
                view(
                    "id",
                    other.id,
                    "name",
                    other.firstName + " " + other.lastName,
                    "roles",
                    other.roles
                        .stream()
                        .map(r -> r.name)
                        .toList()
                )
            )
            .toList();
    }

    public View send(MessageInput p) {
        var u = current.get();
        var other = db.get(UserAccount.class, p.recipientId());
        if (!access.canMessage(u, other)) throw new ResponseStatusException(
            FORBIDDEN,
            "This conversation is not permitted"
        );
        var m = new Message();
        m.sender = u;
        m.recipient = other;
        m.body = p.body();
        m.sentAt = Instant.now();
        db.save(m);
        return messageView(m);
    }

    public List<View> messages() {
        var u = current.get();
        return db
            .query(
                Message.class,
                "select m from Message m where m.sender.id=?1 or m.recipient.id=?1 order by m.sentAt",
                u.id
            )
            .stream()
            .map(this::messageView)
            .toList();
    }

    private View messageView(Message m) {
        return view(
            "id",
            m.id,
            "senderId",
            m.sender.id,
            "sender",
            m.sender.firstName + " " + m.sender.lastName,
            "recipientId",
            m.recipient.id,
            "recipient",
            m.recipient.firstName + " " + m.recipient.lastName,
            "body",
            m.body,
            "sentAt",
            m.sentAt,
            "readAt",
            m.readAt
        );
    }

    public void readMessage(long id) {
        var m = db.get(Message.class, id);
        if (!m.recipient.id.equals(current.get().id)) throw SchoolAccess.hidden();
        m.readAt = Instant.now();
    }

    public View event(Long id, EventInput p) {
        current.require("ADMINISTRATOR");
        if (p.endAt().isBefore(p.startAt())) throw new ResponseStatusException(
            BAD_REQUEST,
            "Event end must follow start"
        );
        var e = id == null ? new CalendarEvent() : db.get(CalendarEvent.class, id);
        e.title = p.title();
        e.description = p.description();
        e.category = p.category();
        e.startAt = p.startAt();
        e.endAt = p.endAt();
        e.allDay = p.allDay();
        e.author = current.get();
        if (id == null) db.save(e);
        return eventView(e);
    }

    public List<View> events() {
        return db
            .query(CalendarEvent.class, "select e from CalendarEvent e order by e.startAt")
            .stream()
            .map(this::eventView)
            .toList();
    }

    public void deleteEvent(long id) {
        current.require("ADMINISTRATOR");
        db.delete(db.get(CalendarEvent.class, id));
    }

    private View eventView(CalendarEvent e) {
        return view(
            "id",
            e.id,
            "title",
            e.title,
            "description",
            e.description,
            "category",
            e.category,
            "startAt",
            e.startAt,
            "endAt",
            e.endAt,
            "allDay",
            e.allDay
        );
    }
}
