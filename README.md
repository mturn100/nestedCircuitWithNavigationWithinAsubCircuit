# nestedCircuitWithNavigationWithinAsubCircuit

Small Android sample for [Circuit](https://slackhq.github.io/circuit/) showing a
[SubCircuit](https://slackhq.github.io/circuit/circuitx/subcircuit/) that never navigates itself,
but hosts a nested circuit that does.

## Idea

- The **parent circuit** owns the `Navigator`. It hoists navigation: the subcircuit asks, the parent navigates.
- The **subcircuit** has no `Navigator`. It only emits events through `outerEventSink`.
- Inside the subcircuit's UI lives a **nested circuit** with its own backstack and `Navigator`. It navigates independently of the parent.

```
HomeScreen (parent circuit, Navigator A)
└── SubCircuitContent(PromoCardScreen)       no Navigator, outerEventSink only
    ├── button ──outerEventSink──▶ PromoCardEvent.OpenDetail
    │                              └─ parent handles it: navigatorA.goTo(DetailScreen)
    └── NavigableCircuitContent (nested circuit, Navigator B)
        └── TipScreen(1) ─ Next tip ─▶ TipScreen(2) ─ …    navigatorB.goTo / pop
```

## What happens when you run it

| Action | Result |
|---|---|
| Tap **Next tip** / **Prev tip** | The nested circuit moves between tips. The parent screen doesn't change. |
| Tap **Hoisted nav: open Detail in parent** | The event goes out of the subcircuit, and the parent navigates to `DetailScreen`. |
| Back from Detail | Returns to Home. The nested circuit keeps its backstack. |

## Code map

Everything is in `app/src/main/java/com/example/sandbox/`.

| File | Contents |
|---|---|
| `Screens.kt` | Parent (`HomeScreen`, `DetailScreen`), subcircuit (`PromoCardScreen`, `PromoCardEvent`, presenter, UI), nested `TipScreen`, and all factories |
| `MainActivity.kt` | Builds `Circuit` and `SubCircuit`, provides `LocalSubCircuit`, hosts the parent `NavigableCircuitContent` |

Key pieces:

- `PromoCardScreen : SubScreen<PromoCardEvent>`: marker for the embedded component, not a navigation record.
- `PromoCardPresenter : SubPresenter<...>`: maps a UI click to `outerEventSink(PromoCardEvent.OpenDetail(...))`.
- `HomeUi`: renders `SubCircuitContent(...)` and maps `PromoCardEvent` to `HomeState.Event`. The presenter then calls `navigator.goTo(...)`.
- `PromoCardUi`: creates its own `rememberSaveableBackStack` and `rememberCircuitNavigator` for the nested circuit.
- The nested `TipScreen`s are registered in the same `Circuit` instance, so the nested circuit resolves them through the ambient `CircuitCompositionLocals`.

## Notes

- `SubCircuit` is `@ExperimentalSubCircuitApi`, so the code opts in.
- Factories are wired by hand to keep the sample small. In a real app, use `@SubCircuitInject` / `@CircuitInject` code generation.
- The nested `NavigableCircuitContent` handles back first while its stack has more than one entry.
- Screens are `Screen, Parcelable` with `@Parcelize`, since `Screen` isn't Parcelable on Android in Circuit 0.39.0.

## Versions

Circuit 0.39.0 (`circuit-foundation`, `circuitx-subcircuit`), Kotlin 2.4.20, Compose BOM 2026.09.00, AGP 9.4.1.

## Build and run

```
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Requires JDK 11+ and an Android SDK with API 37. Set `sdk.dir` in `local.properties`, which Android Studio does for you.
