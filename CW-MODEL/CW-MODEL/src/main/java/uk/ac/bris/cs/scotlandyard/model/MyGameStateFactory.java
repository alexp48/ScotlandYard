package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

	private static final class MyGameState implements GameState {
		private final int endGameRoundCnt = 1000;
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Piece> winner;

		private MyGameState(final GameSetup setup,
							final ImmutableSet<Piece> remaining,
							final ImmutableList<LogEntry> log,
							final Player mrX,
							final List<Player> detectives) {

			if (setup.rounds.isEmpty()) throw new IllegalArgumentException();
			if (mrX == null) throw new NullPointerException();
			if (detectives == null) throw new NullPointerException();

			for (final var d : detectives)
				if (d == null) throw new NullPointerException();

			if (!mrX.isMrX()) throw new IllegalArgumentException();

			for (final var d : detectives)
				if (d.isMrX()) throw new IllegalArgumentException();

			for (int i = 0; i < detectives.size(); i++)
				for (int j = i + 1; j < detectives.size(); j++)
					if (detectives.get(i).location() == detectives.get(j).location() ||
							detectives.get(i).piece() == detectives.get(j).piece()) throw new IllegalArgumentException();

			for (final var d : detectives)
				if (d.has(ScotlandYard.Ticket.SECRET) ||
						d.has(ScotlandYard.Ticket.DOUBLE)) throw new IllegalArgumentException();

			if (setup.graph.nodes().size() == 0) throw new IllegalArgumentException();

			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
		}

		@Nonnull
		@Override
		public GameSetup getSetup() {
			return setup;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {
			final var players = new ArrayList<Piece>();
			players.add(mrX.piece());
			for (final var d : detectives)
				players.add(d.piece());
			return ImmutableSet.copyOf(players);
		}

		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			for (final var d : detectives) {
				if (d.piece() == detective) return Optional.of(d.location());
			}
			return Optional.empty();
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			class getTickets implements TicketBoard {
				@Override
				public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
					if (piece.isMrX()) return mrX.tickets().get(ticket);

					for (final var d : detectives) {
						if (d.piece() == piece)
							return d.tickets().get(ticket);
					}
					return 0;
				}
			}

			getTickets tickets = new getTickets();
			if (piece.isMrX())
				if (mrX.tickets().size() != 0)
					return Optional.of(tickets);
				else
					return Optional.empty();

			for (final var d : detectives) {
				if (d.piece() == piece) {
					if (d.tickets().size() != 0)
						return Optional.of(tickets);
				}
			}
			return Optional.empty();
		}

		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			winner = null;
			final var detectiveSet = new ArrayList<Piece>();
			for (final var d : detectives) detectiveSet.add(d.piece());

			for (final var d : detectives) {
				if (d.location() == mrX.location()) {
					winner = ImmutableSet.copyOf(detectiveSet);
					return winner;
				}
			}

			if (setup.rounds.size() == endGameRoundCnt) winner = ImmutableSet.of(mrX.piece());

			boolean mrXcanMove = false;
			boolean detectivesCanMove = false;
			for(final var p: detectives) {
				if (!makeSingleMoves(setup, detectives, p, p.location()).isEmpty()) {
					detectivesCanMove = true;
					break;
				}
			}
			if(!detectivesCanMove) winner= ImmutableSet.of(mrX.piece());

			if (!makeSingleMoves(setup, detectives, mrX, mrX.location()).isEmpty() || !makeDoubleMoves(setup, detectives, mrX, mrX.location()).isEmpty())
				mrXcanMove = true;

			if (remaining.contains(mrX.piece())) {
				if(!detectivesCanMove && !mrXcanMove) {
					winner = ImmutableSet.of(mrX.piece());
				}
				else {
					if (!mrXcanMove)
						winner = ImmutableSet.copyOf(detectiveSet);
				}
			}
			else {
				if (remaining.size() == detectives.size()) {
					if (!detectivesCanMove)
						winner = ImmutableSet.of(mrX.piece());
				}
			}

			if (winner == null) return ImmutableSet.of();
			else return winner;
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			if (winner != null) return ImmutableSet.of();
			final var moveSet = new ArrayList<Move>();
			if (this.remaining.contains(mrX.piece())) {
				moveSet.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
				moveSet.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
			}
			for (final var d : detectives) {
				if (this.remaining.contains(d.piece())) {
					moveSet.addAll(makeSingleMoves(setup, detectives, d, d.location()));
					moveSet.addAll(makeDoubleMoves(setup, detectives, d, d.location()));
				}
			}

			return ImmutableSet.copyOf(moveSet);
		}

		@Nonnull
		@Override
		public GameState advance(Move move) {
			Player currentPlayer = null;
			if (mrX.piece() == move.commencedBy())
				currentPlayer = mrX;
			else {
				for (final var d : detectives)
					if (d.piece() == move.commencedBy()) {
						currentPlayer = d;
						break;
					}
			}

			Player finalCurrentPlayer = currentPlayer;
			currentPlayer = move.visit(new Move.Visitor<>() {
				@Override
				public Player visit(Move.SingleMove move) {
					assert finalCurrentPlayer != null;

					boolean okTicket = false;
					for (ScotlandYard.Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(finalCurrentPlayer.location(), move.destination, ImmutableSet.of()))) {
						if (t.requiredTicket() == move.ticket)
							okTicket = true;
					}
					if (move.ticket == ScotlandYard.Ticket.SECRET) okTicket = true;
					if (!okTicket) throw new IllegalArgumentException();

					if (finalCurrentPlayer != mrX) {
						mrX = new Player(mrX.piece(), mrX.give(move.ticket).tickets(), mrX.location());
						if (setup.rounds.size() == log.size() && remaining.size() == 1) {
							List<Boolean> newRounds = new ArrayList<>();
							for (int i = 1; i <= endGameRoundCnt; i++)
								newRounds.add(false);
							setup = new GameSetup(setup.graph, (ImmutableList.copyOf(newRounds)));
						}
					} else {
						List<LogEntry> newLog = new ArrayList<>(log);
						if (setup.rounds.get(log.size())) {
							newLog.add(LogEntry.reveal(move.ticket, move.destination));
						} else {
							newLog.add(LogEntry.hidden(move.ticket));
						}
						log = ImmutableList.copyOf(newLog);
					}
					return new Player(move.commencedBy(), finalCurrentPlayer.use(move.ticket).tickets(), move.destination);
				}

				@Override
				public Player visit(Move.DoubleMove move) {
					assert finalCurrentPlayer != null;

					List<LogEntry> newLog = new ArrayList<>(log);

					if (setup.rounds.get(log.size())) {
						newLog.add(LogEntry.reveal(move.ticket1, move.destination1));
					} else {
						newLog.add(LogEntry.hidden(move.ticket1));
					}

					if (setup.rounds.get(log.size() + 1)) {
						newLog.add(LogEntry.reveal(move.ticket2, move.destination2));
					} else {
						newLog.add(LogEntry.hidden(move.ticket2));
					}

					log = ImmutableList.copyOf(newLog);

					return new Player(move.commencedBy(), finalCurrentPlayer.use(move.ticket1).use(move.ticket2).use(ScotlandYard.Ticket.DOUBLE).tickets(), move.destination2);
				}
			});

			List<Player> newDetectives = new ArrayList<>();

			if (mrX.piece() == move.commencedBy()) {
				mrX = currentPlayer;
				newDetectives.addAll(detectives);
			}
			else {
				for (var d : detectives) {
					if (d.piece() == move.commencedBy())
						newDetectives.add(currentPlayer);
					else
						newDetectives.add(d);
				}
			}

			var newRemaining = new ArrayList<Piece>();

			for (var p : this.remaining) {
				if (p != currentPlayer.piece())
					newRemaining.add(p);
			}

			if (newRemaining.size() == 0) {
				if (currentPlayer == mrX) {
					for (var d : detectives) {
						newRemaining.add(d.piece());
					}
				} else {
					newRemaining.add(mrX.piece());
				}
			}

			remaining = ImmutableSet.copyOf(newRemaining);

			if (getAvailableMoves().isEmpty())
				remaining = ImmutableSet.of(mrX.piece());

			return new MyGameState(setup, remaining, log, mrX, newDetectives);
		}
	}

	private static boolean isOccupiedByDetectives(List<Player> detectives, int destination) {
		boolean occupied = false;
		for (final var d : detectives)
			if (d.location() == destination) {
				occupied = true;
				break;
			}
		return occupied;
	}

	private static ImmutableSet<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
		final var singleMoves = new ArrayList<Move.SingleMove>();
		for (int destination : setup.graph.adjacentNodes(source)) {

			// TODO: find out if destination is occupied by a detective
			if (isOccupiedByDetectives(detectives, destination)) continue;

			for (ScotlandYard.Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()))) {
				if (player.has(t.requiredTicket())) {
					singleMoves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
				}
			}

			// TODO: add moves to the destination via a Secret ticket if there are any left with the player
			if (player.has(ScotlandYard.Ticket.SECRET))
				singleMoves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination));
		}
		return ImmutableSet.copyOf(singleMoves);
	}

	private static ImmutableSet<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
		if (setup.rounds.size() <= 1) return ImmutableSet.of();
		if (!player.has(ScotlandYard.Ticket.DOUBLE)) return ImmutableSet.of();

		final var doubleMoves = new ArrayList<Move.DoubleMove>();
		for (int destination1 : setup.graph.adjacentNodes(source)) {

			if (isOccupiedByDetectives(detectives, destination1)) continue;

			for (ScotlandYard.Transport t1 : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of()))) {
				if (player.has(t1.requiredTicket())) {
					for (int destination2 : setup.graph.adjacentNodes(destination1)) {
						if (isOccupiedByDetectives(detectives, destination2)) continue;
						for (ScotlandYard.Transport t2 : Objects.requireNonNull(setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of()))) {
							if (player.has(t2.requiredTicket())) {
								if (t1.requiredTicket() == t2.requiredTicket() && !player.hasAtLeast(t1.requiredTicket(), 2)) continue;
								doubleMoves.add(new Move.DoubleMove(player.piece(), source, t1.requiredTicket(), destination1, t2.requiredTicket(), destination2));
								if (player.has(ScotlandYard.Ticket.SECRET))
									doubleMoves.add(new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination1, t2.requiredTicket(), destination2));
							}
						}
						if (player.has(ScotlandYard.Ticket.SECRET)) {
							doubleMoves.add(new Move.DoubleMove(player.piece(), source, t1.requiredTicket(), destination1, ScotlandYard.Ticket.SECRET, destination2));
							if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 2))
								doubleMoves.add(new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination1, ScotlandYard.Ticket.SECRET, destination2));
						}
					}
				}
			}
		}
		return ImmutableSet.copyOf(doubleMoves);
	}
}