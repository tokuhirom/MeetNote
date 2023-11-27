# MeetNote

![MeetNote](icon.png)

Streamline your online meetings by integrating recording, transcription, and summarization into a single process. MeetNote offers you the ability to:

* Auto-record voices from your online meetings
* Transcribe the recorded voice into accessible text
* Summarize the transcriptions for a quick review

## Supported platforms

* Mac OS Ventura or later
* Java 17+

## Permissions

To get the list of windows, this application requires the "Accessibility" permission.

## Dependencies

### Lame

    brew install lame

Lame is utilized for converting voice files to mp3.

### Blackhole

Install BlackHole to record both voice and microphone. To do so, run:

    brew install blackhole-2ch

And set them up as:

```mermaid
graph TB
    A[Mic]
    B[Online Meeting]
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

Speaker configuration:

![img.png](img.png)

Zoom Configuration:

![img_1.png](img_1.png)

Audio MIDI setup:

![img_3.png](img_3.png)

![img_2.png](img_2.png)

## Build

Build the app by running the following command:

    ./gradlew packageDmg

## User License

The MIT License (MIT)

Copyright © 2023 Tokuhiro Matsuno, http://64p.org/ <tokuhirom@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the “Software”), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
