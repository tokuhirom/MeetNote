This is a Kotlin Multiplatform project targeting Desktop.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

## Dependencies

  lame
  
## Blackhole

    brew install blackhole-2ch

する。

で、以下のようにつなげる。

```mermaid
graph TB
    A[Mic]
    B[Zoom]
    C[Aggregate Device]
    D[Multi Output]
    E[Blackhole]
    F[MeetNote]
    G[Speaker]
    
    A -.-> B
    A -.-> C
    B --> D
    D --> E
    E --> C
    C --> F
    D --> G
    
    class A,G device;
    classDef device fill:#f9d,stroke:#333,stroke-width:4px;
```

