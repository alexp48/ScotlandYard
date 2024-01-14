package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import javax.annotation.Nonnull;
import java.util.ArrayList;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		// TODO
		return new MyModel(new MyGameStateFactory().build(setup, mrX, detectives), ImmutableSet.of());
	}

	private static final class MyModel implements Model {
		private Board.GameState modelState;
		private ImmutableSet<Observer> observers;

		private MyModel(final Board.GameState gameState, ImmutableSet<Observer> observers) {
			this.modelState = gameState;
			this.observers = observers;
		}

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return modelState;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			if (observer == null) throw new NullPointerException();

			for (final var o : observers) {
				if (o == observer)
					throw new IllegalArgumentException();
			}

			final var newObservers = new ArrayList<>(observers);
			newObservers.add(observer);
			observers = ImmutableSet.copyOf(newObservers);
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if (observer == null) throw new NullPointerException();

			boolean okObserver = false;
			for (final var o : observers) {
				if (o == observer) {
					okObserver = true;
					break;
				}
			}
			if (!okObserver) throw new IllegalArgumentException();

			final var newObservers = new ArrayList<>(observers);
			newObservers.remove(observer);
			observers = ImmutableSet.copyOf(newObservers);
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return observers;
		}

		@Override
		public void chooseMove(@Nonnull Move move) {
			modelState = modelState.advance(move);
			var event = modelState.getWinner().isEmpty() ? Observer.Event.MOVE_MADE : Observer.Event.GAME_OVER;
			for (Observer o : observers) o.onModelChanged(modelState, event);
		}
	}
}
