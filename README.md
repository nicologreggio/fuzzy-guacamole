# fuzzy-guacamole
Advanced Topics in Programming Languages project @ UniPd w/ Kynetics

![Fuzzy guacamole illustation (AI made digital art)](docs/Fuzzy2.jpg)
> Design and Implementation of a Publish/Subscribe System

---

# Introduction
The scope of this project is the implementation of a system capable of searching regular expression in huge text files. The seeking operations are split into several units to make the overall search faster and more efficient. For the sake of this implementation everything runs locally on a single machine, but given the architecture the application may easily become distributed, with each unit running on a separate machine increasing this way the computational power hence the efficiency as well.

---

# Technologies adopted
## Docker
It is an open platform for developing, shipping, and running applications. It allows to define images that describe step by step how an environment should be made and configured. There is a wide set of availabile images in the [docker hub](https://hub.docker.com/) which can usually be a good place to find what you need or at least a starting point. Instantiating images gives you containers, actual environments in which you can operate without worrying to corrupt your local machine.

### Compose
It is a tool developed to help define and share multi-container applications via [YAML](https://yaml.org/) syntax.

## MQTT
MQTT (MQ Telemetry Transport) is a lightweight, publish-subscribe, machine to machine network protocol. It is primarily designed for connections with remote locations that have devices with resource constraints or limited network bandwidth. The protocol is event driven and connects devices using the publish /subscribe pattern. The sender (Publisher) and the receiver (Subscriber) communicate via Topics and are decoupled from each other. The connection between them is handled by the MQTT broker which filters all incoming messages and distributes them correctly to the Subscribers.

The [mosquitto](https://mosquitto.org/) broker has been adopted for this project.

## Scala
Scala is a strong statically typed general-purpose programming language which supports both object-oriented programming and functional programming.
Its source code can be compiled to Java bytecode and run on a Java virtual machine (JVM), leading to optimal interoperability. Given the overhead required to run it, it has been chosen to develop the central part.

## Rust
Rust is a multi-paradigm, general-purpose programming language which emphasizes performance, type safety, and concurrency. Rust also enforces memory safety (i.e.: all references point to valid memory) without requiring the use of a garbage collector or reference counting, mechanisms present in other memory-safe languages. Since it compiles to machine executable code it has been chosen for the units, so that they can be lightweight.

---

# Analysis & design
## Use cases
### MQTT communication
![MQTT communication use case diagram](docs/useCases/MqttCommunication.png)
Here we can see basically what the actors of an MQTT system do. As we anticipated earlier we have the broker which handles the connection with all the clients and is responsible to triage all the messages to the correct subscribers. While a connected client can publish messages to one or more topics or subscribe to as many topic as it needs.

### Regex search
![Regex search use case diagram](docs/useCases/RegexFinding.png)
Here we can see the business logic of our application. Let's meet our actors:
- **Library**: Contains all the available files. In this implementation it is a shared local volume, but it might as well be a web server, or another online resource;
- **Main server**: 
  - It receives the user choice regarding the regex and the file in which to search for it;
  - it handles the splitting of the file based on the number of actors, and it communicates them in which part to operate;
  - it gathers the results from each unit presenting them to the final User as they come;
- **Unit**: Several running at the same time, each one receives the instructions telling it on which file to operate (that's why it interacts with the library as well, so that the broker doesn't need to pointlessly send huge files, as it's not made for that purpose) and the regex to search. It then sends its findings back to the main server via the broker of course;
- **User**: The actual person interacting with the system, it can browse the list of files and pick one in which to search for a regex it can input.

## Architecture
![Architetcture diagram of the application](docs/Architecture.png)
In this diagram we can see all the components of the application and their interaction. Starting from the bottom we see that the end user can interact with the application via a shell script, which allows it to pick the file it likes and input the regex to search. The script then is in charge of launching the entire compose environment, in which the different part of the application run.
There are three different kind of containers:
1. **mqtt broker**: *(based on eclipse-mosquitto image)* it just needs to be run, after that the broker will be there doing its job
2. **scala**: *(based on a custom image, more in the implementation)* handles the core business logic
3. **rust**: *(based on a custom image)* there can be several ones of this kind, being them the unit doing the chunk-search job. As discussed previously migrating from this local setup to a distributed one with each machine running an instance of this kind is straightforward, since the communication is always done via the broker on a network.

The scala service receives the informations about the file and the regex via the environment, set by the launching script. At this point it fetches the line count of the file from the library and computes the length of each chunk to process based on the number of available units (which it knows again from the environment). Initially it subscribes to the topics `new_client` and `results`. The former will be to acknowledge and process the connection of a new unit while the latter to gather the results from the units which will then be printed.

When a rust unit starts it connects to the broker and publish its own id (set by the environment) in the `new_client` topic, then it subscribes to the topic `unit_id`. At this point it will receive on "its own" topic the information from the server required to start the search. Then it fetches the required chunk from the library and seek for matches of the regex. Upon completion it publishes its results to the `results` topic.

Finally the user can see the results thanks to the fact that the script, after executing compose in background, it attaches to the scala container stdout.

---

# Implementation
## About the files library
The repo contains only some files, the bigger ones are ignored to save space. Large files can be found [here](https://github.com/logpai/loghub). In any case to add file it is enough to put it in the [library folder](library/)


