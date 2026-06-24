#  Idan & Ido Graph Computation Web
youtubelink to the demo
https://youtu.be/mu75kSw4Xuw

---

## Background

The project implements a **Publish/Subscribe** (pub/sub) system for graph-based computations:

- **Topics** — data channels. Each topic stores the last value published to it.
- **Agents** — modules that subscribe to input topics, compute a result, and publish it to an output topic.
- **Computation Graph** — visualizes the data flow: user → input topics → agents → output topics.

The interface is divided into three panels:

| Panel | Purpose |
|-------|---------|
| **Control (left)** | Upload a config file, build a graph from expressions, publish values |
| **Graph (center)** | Live SVG diagram of the computation graph |
| **Topics (right)** | Table of all topics with their current value and validity status |

### Available Agents

| Class | Operation |
|-------|-----------|
| `test.PlusAgent` | Sum: `A + B` |
| `test.IncAgent` | Increment: `A + 1` |
| `test.MultiplyAgent` | Product: `A × B` |
| `test.AverageAgent` | Average: `(A + B) / 2` |
| `test.MaxAgent` | Maximum: `max(A, B)` |
| `test.MinAgent` | Minimum: `min(A, B)` |

---

## Installation

### Prerequisites

- **Java JDK 11+** — for compilation and runtime
- **Browser** — Chrome / Firefox / Edge

---

## Run Commands

### 1. Compile (once, or after any code change)

```
javac -sourcepath . -d out test\*.java
```

### 2. Start the server

```
java -cp out test.Main
```

You should see:
```
Server started on http://localhost:8080
Open: http://localhost:8080/app/index.html
```

### 3. Open the app

Navigate to:

```
http://localhost:8080/app/index.html
```

### 4. Stop the server

Press **Ctrl+C** in the terminal.

---

## Basic Usage

### Loading a Configuration

1. In the **Control** panel click **Choose File** and select a `.conf` file from `config_files/`.
2. Click **Upload & Build Graph** — the graph appears in the center panel.

**Ready-made demo configs:**

| File | Description |
|------|-------------|
| `simple_math.conf` | `A + B = C`, then `C + 1 = D` |
| `advanced_math.conf` | Sum, product, and average of X and Y |
| `max_min.conf` | Max, min, and average of P and Q |


### Building a Graph from Expressions

Click the **✏ Expressions** tab and type, for example:
```
C = A + B
D = inc(C)
E = max(A, C)
```
Then click **Build Graph**.

### Publishing Values

- **Single:** topic `A`, value `5`
- **Multiple:** topics `A,B,C`, values `1,2,3` — publishes A=1, B=2, C=3 at once

> **Note:** You can only publish to topics that are defined in the currently loaded config.

---

## Project Structure

```
finalproject/
├── test/               ← Java source code (package test)
├── html_files/         ← HTML / CSS / JS files
├── config_files/       ← Configuration files (.conf)
└── out/                ← Compiled .class files (created on build)
```

---

## Config File Format

Three lines per agent:

```
test.PlusAgent
A,B
C
```

Line 1: fully-qualified class name · Line 2: input topics · Line 3: output topic
