package dev.seariver;

import dev.seariver.model.Event;
import dev.seariver.model.Person;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.System.out;

public class Repository {

    private final DataSource datasource;

    public Repository(DataSource datasource) {
        this.datasource = datasource;
    }

    public void listNextEvent(NewMessage newMessage) {

        newMessage.response("Sem eventos programados");

        var nextEvent = findNextEvent(newMessage);

        if (nextEvent.isEmpty()) return;

        var event = nextEvent.get();

        var persons = findPersonList(event.id());

        var personList = IntStream
            .range(0, persons.size())
            .mapToObj(i -> {
                var row = newMessage.senderJid().toString().equals(persons.get(i).senderJid())
                    ? "*%02d - %s*"
                    : "%02d - %s";
                return row.formatted(i + 1, persons.get(i).name());
            })
            .collect(Collectors.joining("\n"));

        var response = event.template()
            .replace("#ID", String.valueOf(event.id()))
            .replace("#WEEK_DAY", event.weekDay())
            .replace("#DATE", event.date())
            .replace("#START_AT", event.start())
            .replace("#END_AT", event.end())
            .replace("#PERSON_LIST", personList);

        newMessage.response(response);
    }

    public void addPersonOnNextEvent(NewMessage newMessage) {

        newMessage.response("Sem eventos programados");

        var nextEvent = findNextEvent(newMessage);

        if (nextEvent.isEmpty()) return;

        var event = nextEvent.get();

        var sql = """
            INSERT INTO person 
            (event_id, sender_jid, slug, name, created_at, updated_at) 
            values (?, ?, ?, ?, ?, ?)""";

        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, event.id());
            stmt.setString(2, newMessage.senderJid().toString());
            stmt.setString(3, Person.slugify(newMessage.senderName()));
            stmt.setString(4, newMessage.senderName());
            stmt.setTimestamp(5, Timestamp.from(Instant.now()));
            stmt.setTimestamp(6, Timestamp.from(Instant.now()));
            stmt.executeUpdate();
        } catch (JdbcSQLIntegrityConstraintViolationException e) {
            out.println(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        listNextEvent(newMessage);
    }

    public boolean isRegisteredChat(NewMessage newMessage) {

        var result = false;

        String sql = "SELECT jid FROM chat_group WHERE jid = ?";

        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, newMessage.chatJid().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result = true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public Optional<Event> findNextEvent(NewMessage newMessage) {

        Optional<Event> result = Optional.empty();

        String sql = """
            SELECT
                id,
                chat_group_jid,
                event_date,
                start_at,
                end_at,
                template,
                created_at,
                updated_at
            FROM event 
            WHERE chat_group_jid = ? 
            AND event_date >= CURRENT_DATE
            ORDER BY event_date ASC
            LIMIT 1""";

        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, newMessage.chatJid().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result = Optional.of(new Event(
                        rs.getInt("id"),
                        rs.getString("chat_group_jid"),
                        rs.getDate("event_date").toLocalDate(),
                        rs.getTime("start_at").toLocalTime(),
                        rs.getTime("end_at").toLocalTime(),
                        rs.getString("template"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public List<Person> findPersonList(int eventId) {

        var result = new ArrayList<Person>();

        String sql = """
            SELECT
                id,
                event_id,
                sender_jid,
                slug,
                name,
                created_at,
                updated_at
            FROM person
            WHERE event_id = ?
            ORDER BY created_at ASC""";

        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new Person(
                        rs.getInt("id"),
                        rs.getInt("event_id"),
                        rs.getString("sender_jid"),
                        rs.getString("slug"),
                        rs.getString("name"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }
}
