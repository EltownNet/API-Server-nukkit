package net.eltown.apiserver.components.handler.ticketsystem.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class Ticket {

    private final String creator;
    private String supporter;
    private String id;
    private String subject;
    private String section;
    private String priority;
    private List<String> messages;
    private String dateOpened;
    private String dateClosed;

}
