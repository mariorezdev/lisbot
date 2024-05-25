package dev.seariver.command;

import dev.seariver.NewMessage;

public class ListCommand implements Command {

    @Override
    public void execute(NewMessage event) {

        var response = "Received command: %s".formatted(event.text());

        event.response(response);
    }

    @Override
    public String alias() {
        return "/l";
    }
}
