package at.aau.serg.websocketdemoserver.gamelogic.board;

import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
public record Connection(int to, TicketType transport) {}