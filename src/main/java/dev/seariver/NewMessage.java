package dev.seariver;

import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.standard.TextMessage;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class NewMessage {

    private final MessageInfo info;
    private TextMessage textMessage;
    private boolean isSenderName = false;
    private final Map<String, String> names = new HashMap<>();
    private String response = "";

    public NewMessage(MessageInfo info) {
        this.info = info;
        if (info.message().content() instanceof TextMessage textMessage) {
            this.textMessage = textMessage;
        }
        names.put(senderSlug(), senderNormalized());
    }

    public String text() {

        if (null == textMessage) {
            return "";
        }

        return textMessage.text().trim();
    }

    public Jid chatJid() {
        return info.parentJid();
    }

    public Jid senderJid() {
        return info.senderJid();
    }

    public String senderName() {

        if (info instanceof ChatMessageInfo chatMessageInfo) {
            if (chatMessageInfo.pushName().isPresent()) {
                return chatMessageInfo.pushName().get();
            }

            return chatMessageInfo.senderName();
        }

        return info.senderJid().user();
    }

    public String senderNormalized() {
        return normalize(senderName());
    }

    public String senderSlug() {
        return slug(senderName());
    }

    public String senderPhone() {
        return info.senderJid().toPhoneNumber();
    }

    public boolean isSenderName() {
        return isSenderName;
    }

    public void isSenderName(boolean value) {
        isSenderName = value;
    }

    public void names(String name) {
        names.put(slug(name), normalize(name));
    }

    public Map<String, String> names() {
        return names;
    }

    public String response() {
        return response;
    }

    public void response(String text) {
        this.response = text;
    }

    public String normalize(String text) {
        // Normalize text
        var normalized = Normalizer.normalize(text, Normalizer.Form.NFKD);

        // Removes non ASCII chars
        var pattern = Pattern.compile("[^\\p{ASCII}]");
        var plainText = pattern.matcher(normalized).replaceAll("").trim();
        // Removes non alfa-numeric
        var cleanText = plainText.replaceAll("[^a-zA-Z\\s]", "");

        // Replace blank spaces
        return cleanText
            .replaceAll("\\s{2,}", " ");
    }

    public String slug(String text) {

        var lowerCased = normalize(text).toLowerCase();

        // remove accents
        var noAccents = lowerCased.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // replace space by underscore
        var slug = noAccents.replaceAll("\\s+", "_");
        // remove start and end underscore
        slug = slug.replaceAll("^_+|_+$", "");

        return slug;
    }
}
