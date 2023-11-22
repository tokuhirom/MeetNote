# MeetNote

Complete Zoom recording, transcription, and summarization all at once


## Supported platforms

  * Mac OS Ventura or later
  * Java 17+

## Dependencies

### Lame

    brew install lame

音声ファイルを mp3 にするために lame を利用する。
  
### Blackhole

音声とマイクを両方録音するために以下のようにして BlackHole を入れる。

    brew install blackhole-2ch

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

