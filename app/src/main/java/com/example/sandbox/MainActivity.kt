package com.example.sandbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.subcircuit.ExperimentalSubCircuitApi
import com.slack.circuit.subcircuit.LocalSubCircuit
import com.slack.circuit.subcircuit.SubCircuit

@OptIn(ExperimentalSubCircuitApi::class)
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val circuit =
      Circuit.Builder()
        .addPresenterFactory(presenterFactory)
        .addUiFactory(uiFactory)
        .build()

    val subCircuit =
      SubCircuit.builder()
        .addPresenterFactory(subPresenterFactory)
        .addUiFactory(subUiFactory)
        .build()

    setContent {
      MaterialTheme {
        Surface {
          CircuitCompositionLocals(circuit) {
            CompositionLocalProvider(LocalSubCircuit provides subCircuit) {
              val backStack = rememberSaveableBackStack(root = HomeScreen)
              val navigator = rememberCircuitNavigator(backStack)
              NavigableCircuitContent(navigator, backStack, Modifier)
            }
          }
        }
      }
    }
  }
}
