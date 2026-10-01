package com.example.sandbox

import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.slack.circuit.subcircuit.ExperimentalSubCircuitApi
import com.slack.circuit.subcircuit.SubCircuitContent
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubPresenter
import com.slack.circuit.subcircuit.SubPresenterFactory
import com.slack.circuit.subcircuit.SubScreen
import com.slack.circuit.subcircuit.SubUi
import com.slack.circuit.subcircuit.SubUiFactory
import kotlinx.parcelize.Parcelize

// ---------------------------------------------------------------------------------------------
// Parent circuit: owns the Navigator. Hosts the subcircuit and handles its hoisted nav events.
// ---------------------------------------------------------------------------------------------

@Parcelize data object HomeScreen : Screen, Parcelable

@Parcelize data class DetailScreen(val source: String) : Screen, Parcelable

data class HomeState(val eventSink: (Event) -> Unit) : CircuitUiState {
  sealed interface Event {
    data class OpenDetail(val source: String) : Event
  }
}

fun homePresenter(navigator: Navigator): Presenter<HomeState> = presenterOf {
  HomeState { event ->
    when (event) {
      // Navigation requested by the subcircuit lands here — the PARENT navigates.
      is HomeState.Event.OpenDetail -> navigator.goTo(DetailScreen(event.source))
    }
  }
}

@OptIn(ExperimentalSubCircuitApi::class)
fun homeUi(): Ui<HomeState> = ui { state, modifier ->
  Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Parent circuit (hoists navigation)")
    SubCircuitContent(
      screen = PromoCardScreen,
      outerEventSink = { event ->
        when (event) {
          is PromoCardEvent.OpenDetail -> state.eventSink(HomeState.Event.OpenDetail(event.from))
        }
      },
    )
  }
}

data class DetailState(val source: String, val onBack: () -> Unit) : CircuitUiState

fun detailPresenter(screen: DetailScreen, navigator: Navigator): Presenter<DetailState> =
  presenterOf { DetailState(screen.source, onBack = navigator::pop) }

fun detailUi(): Ui<DetailState> = ui { state, modifier ->
  Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Detail screen, opened from: ${state.source}")
    Button(onClick = state.onBack) { Text("Back") }
  }
}

// ---------------------------------------------------------------------------------------------
// Subcircuit: no Navigator. Emits PromoCardEvent through outerEventSink.
// Inside its UI lives a NESTED full circuit with its own backstack + navigator.
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalSubCircuitApi::class)
data object PromoCardScreen : SubScreen<PromoCardEvent>

sealed interface PromoCardEvent : SubCircuitOuterEvent {
  data class OpenDetail(val from: String) : PromoCardEvent
}

data class PromoCardState(val eventSink: (UiEvent) -> Unit) : SubCircuitUiState {
  sealed interface UiEvent {
    data object OpenDetailClicked : UiEvent
  }
}

@OptIn(ExperimentalSubCircuitApi::class)
class PromoCardPresenter : SubPresenter<PromoCardEvent, PromoCardState> {
  @Composable
  override fun present(outerEventSink: (PromoCardEvent) -> Unit): PromoCardState =
    PromoCardState { event ->
      when (event) {
        PromoCardState.UiEvent.OpenDetailClicked ->
          outerEventSink(PromoCardEvent.OpenDetail(from = "promo card"))
      }
    }
}

@OptIn(ExperimentalSubCircuitApi::class)
class PromoCardUi : SubUi<PromoCardState> {
  @Composable
  override fun Content(state: PromoCardState, modifier: Modifier) {
    Card(modifier.fillMaxWidth()) {
      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Subcircuit (no Navigator)")
        OutlinedButton(onClick = { state.eventSink(PromoCardState.UiEvent.OpenDetailClicked) }) {
          Text("Hoisted nav: open Detail in parent")
        }

        // Nested circuit with its own backstack: navigates independently of the parent.
        val backStack = rememberSaveableBackStack(root = TipScreen(1))
        val nestedNavigator = rememberCircuitNavigator(backStack)
        NavigableCircuitContent(
          navigator = nestedNavigator,
          backStack = backStack,
          modifier = Modifier.fillMaxWidth().height(140.dp),
        )
      }
    }
  }
}

// ---------------------------------------------------------------------------------------------
// Nested circuit screens (regular Circuit screens, navigated inside the subcircuit).
// ---------------------------------------------------------------------------------------------

@Parcelize data class TipScreen(val index: Int) : Screen, Parcelable

data class TipState(val index: Int, val eventSink: (Event) -> Unit) : CircuitUiState {
  sealed interface Event {
    data object Next : Event
    data object Back : Event
  }
}

fun tipPresenter(screen: TipScreen, navigator: Navigator): Presenter<TipState> = presenterOf {
  TipState(screen.index) { event ->
    when (event) {
      TipState.Event.Next -> navigator.goTo(TipScreen(screen.index + 1))
      TipState.Event.Back -> navigator.pop()
    }
  }
}

fun tipUi(): Ui<TipState> = ui { state, modifier ->
  Column(modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Nested circuit: tip #${state.index}")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(onClick = { state.eventSink(TipState.Event.Next) }) { Text("Next tip") }
      if (state.index > 1) {
        OutlinedButton(onClick = { state.eventSink(TipState.Event.Back) }) { Text("Prev tip") }
      }
    }
  }
}

// ---------------------------------------------------------------------------------------------
// Factories
// ---------------------------------------------------------------------------------------------

val presenterFactory =
  Presenter.Factory { screen: Screen, navigator: Navigator, _: CircuitContext ->
    when (screen) {
      HomeScreen -> homePresenter(navigator)
      is DetailScreen -> detailPresenter(screen, navigator)
      is TipScreen -> tipPresenter(screen, navigator)
      else -> null
    }
  }

val uiFactory =
  Ui.Factory { screen: Screen, _: CircuitContext ->
    when (screen) {
      HomeScreen -> homeUi()
      is DetailScreen -> detailUi()
      is TipScreen -> tipUi()
      else -> null
    }
  }

@OptIn(ExperimentalSubCircuitApi::class)
val subPresenterFactory = SubPresenterFactory { screen ->
  when (screen) {
    PromoCardScreen -> PromoCardPresenter()
    else -> null
  }
}

@OptIn(ExperimentalSubCircuitApi::class)
val subUiFactory = SubUiFactory { screen ->
  when (screen) {
    PromoCardScreen -> PromoCardUi()
    else -> null
  }
}
