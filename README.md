# Simple OS Simulator

## Overview
This project is a simulation of a basic operating system in Java. It features:
- Round Robin Scheduling
- Process and Memory Management
- Mutex Locks and System Calls

## Code Structure

```plaintext
simple-os/
├── src/
│   ├── core/
│   │   ├── Kernel.java
│   │   ├── Memory.java
│   │   ├── SystemCall.java
│   │   └── OSLauncher.java
│   │   └── Process.java
│
│   ├── util/
│   │   ├── State.java
│   │   └── Pair.java
│
│   ├── sync/
│   │   └── Mutex.java
│
│   └── resources/
│       └── programs/
│           ├── program_1.txt
│           ├── program_2.txt
│           └── program_3.txt
│
├── .gitignore
├── README.md
```
