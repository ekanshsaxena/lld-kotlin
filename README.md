# Low-Level Design — Kotlin

Collection of low-level design (LLD) implementations in Kotlin, covering classic system design interview problems and design patterns.

## Implementations

| Problem | Pattern | File |
|---------|---------|------|
| Amazon Locker | State Machine, Strategy | `amazonLocker.kt` |
| Elevator System | State Machine, Scheduling | `elevator.kt` |
| Parking Lot | Factory, Strategy | `parkingLot.kt` |
| Connect 4 Game | Observer, Game Loop | `connect4game.kt` |

## Design Patterns

| Pattern | File |
|---------|------|
| Observer | `observer.kt` |
| Strategy | `strategy.kt` |
| Decorator | `decorator.kt` |

## Key Principles

- SOLID principles throughout
- Interface-driven design for extensibility
- Immutable state where possible
- Clean separation of concerns

## Usage

Each file is self-contained and runnable with `kotlinc`:

```bash
kotlinc parkingLot.kt -include-runtime -d parkingLot.jar && java -jar parkingLot.jar
```
