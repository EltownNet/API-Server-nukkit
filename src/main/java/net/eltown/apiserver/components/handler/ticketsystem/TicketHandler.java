package net.eltown.apiserver.components.handler.ticketsystem;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.ticketsystem.data.Ticket;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;

public class TicketHandler {

    private final Server server;
    private final TicketProvider provider;
    private final TinyRabbitListener tinyRabbitListener;

    @SneakyThrows
    public TicketHandler(final Server server) {
        this.server = server;
        this.tinyRabbitListener = new TinyRabbitListener("localhost");
        this.provider = new TicketProvider(server);
        this.startCallbacking();
    }

    public void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.receive(delivery -> {
                final String[] d = delivery.getData();
                switch (TicketCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_TAKE_TICKET:
                        final Ticket ticket1 = this.provider.getTicket(d[2]);
                        if (ticket1.getSupporter().equals("null")) {
                            this.provider.setTicketSupporter(d[2], d[1]);
                        }
                        break;
                    case REQUEST_SEND_MESSAGE:
                        this.provider.addNewTicketMessage(d[3], d[1], d[2]);
                        break;
                    case REQUEST_SET_PRIORITY:
                        this.provider.setTicketPriority(d[3], d[2]);
                        break;
                    case REQUEST_CLOSE_TICKET:
                        this.provider.closeTicket(d[2]);
                        break;
                }
            }, "API/Ticketsystem/Receive", "tickets.receive");
        });

        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.callback((request -> {
                final String[] d = request.getData();
                switch (TicketCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_OPEN_TICKET:
                        this.provider.getTickets(d[1], tickets -> {
                            if (tickets.size() >= 5) {
                                request.answer(TicketCalls.CALLBACK_TOO_MANY_TICKETS.name(), "null");
                            } else {
                                this.provider.createTicket(d[1], d[2], d[3], d[4], d[5]);
                                request.answer(TicketCalls.CALLBACK_NULL.name(), "null");
                            }
                        });
                        break;
                    case REQUEST_MY_TICKETS:
                        this.provider.getTickets(d[1], tickets -> {
                            if (tickets.size() == 0) {
                                request.answer(TicketCalls.CALLBACK_NO_TICKETS.name(), "null");
                            } else {
                                final LinkedList<String> list = new LinkedList<>();
                                for (Ticket e : tickets) {
                                    final StringBuilder builder = new StringBuilder();
                                    e.getMessages().forEach(f -> {
                                        builder.append(f).append("~~~");
                                    });
                                    final String messages = builder.substring(0, builder.length() - 3);
                                    list.add(e.getCreator() + ">>" + e.getSupporter() + ">>" + e.getId() + ">>" + e.getSubject() + ">>" + e.getSection()  + ">>" + e.getPriority() + ">>" + messages + ">>" + e.getDateOpened() + ">>" + e.getDateClosed());
                                }
                                request.answer(TicketCalls.CALLBACK_MY_TICKETS.name(), list.toArray(new String[0]));
                            }
                        });
                        break;
                    case REQUEST_OPEN_TICKETS:
                        final LinkedHashSet<Ticket> tickets = this.provider.getOpenTickets();
                        if (tickets.size() == 0) {
                            request.answer(TicketCalls.CALLBACK_OPEN_TICKETS.name(), "null");
                        } else {
                            final LinkedList<String> list = new LinkedList<>();
                            for (Ticket e : tickets) {
                                final StringBuilder builder = new StringBuilder();
                                e.getMessages().forEach(f -> {
                                    builder.append(f).append("~~~");
                                });
                                final String messages = builder.substring(0, builder.length() - 3);
                                list.add(e.getCreator() + ">>" + e.getSupporter() + ">>" + e.getId() + ">>" + e.getSubject() + ">>" + e.getSection()  + ">>" + e.getPriority() + ">>" + messages + ">>" + e.getDateOpened() + ">>" + e.getDateClosed());
                            }
                            request.answer(TicketCalls.CALLBACK_OPEN_TICKETS.name(), list.toArray(new String[0]));
                        }
                        break;
                    case REQUEST_MY_SUPPORT_TICKETS:
                        final LinkedHashSet<Ticket> mySupportTickets = this.provider.getMySupportTickets(d[1]);
                        if (mySupportTickets.size() == 0) {
                            request.answer(TicketCalls.CALLBACK_MY_SUPPORT_TICKETS.name(), "null");
                        } else {
                            final LinkedList<String> list = new LinkedList<>();
                            for (Ticket e : mySupportTickets) {
                                final StringBuilder builder = new StringBuilder();
                                e.getMessages().forEach(f -> {
                                    builder.append(f).append("~~~");
                                });
                                final String messages = builder.substring(0, builder.length() - 3);
                                list.add(e.getCreator() + ">>" + e.getSupporter() + ">>" + e.getId() + ">>" + e.getSubject() + ">>" + e.getSection()  + ">>" + e.getPriority() + ">>" + messages + ">>" + e.getDateOpened() + ">>" + e.getDateClosed());
                            }
                            request.answer(TicketCalls.CALLBACK_MY_SUPPORT_TICKETS.name(), list.toArray(new String[0]));
                        }
                        break;
                }
            }), "API/Ticketsystem/Callback", "tickets.callback");
        });
    }

}

