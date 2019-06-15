## Akka Java Cluster Persistence Example

### Introduction

This is a Java, Maven, Akka project that demonstrates how to setup an
[Akka Cluster](https://doc.akka.io/docs/akka/current/index-cluster.html)
with an example implementation of
[Akka Persistence](https://doc.akka.io/docs/akka/current/persistence.html).

This project is one in a series of projects that starts with a simple Akka Cluster project and progressively builds up to examples of event sourcing and command query responsibility segregation.

The project series is composed of the following projects:
* [akka-java-cluster](https://github.com/mckeeh3/akka-java-cluster)
* [akka-java-cluster-aware](https://github.com/mckeeh3/akka-java-cluster-aware)
* [akka-java-cluster-singleton](https://github.com/mckeeh3/akka-java-cluster-singleton)
* [akka-java-cluster-sharding](https://github.com/mckeeh3/akka-java-cluster-sharding)
* [akka-java-cluster-persistence](https://github.com/mckeeh3/akka-java-cluster-persistence) (this project)
* [akka-java-cluster-persistence-query](https://github.com/mckeeh3/akka-java-cluster-persistence-query)

Each project can be cloned, built, and runs independently of the other projects.

This project contains an example implementation of Akka persistence. Here we will focus on the implementation details in this project. Please see the
[Akka documentation](https://doc.akka.io/docs/akka/current/persistence.html)
for a more detailed discussion about Akka Persistence.

### What is Akka Persistence

This project builds on the
[akka-java-cluster-sharding](https://github.com/mckeeh3/akka-java-cluster-sharding)
project. The `EntityActor` in the previous project, which only logged entity messages, is replaced with an `EntityPersistenceActor`, which adds the persistence of events that are derived from the command messages.


The `EntityPersistenceActor` actor handles the state of a specific bank account. Incoming command messages are either bank account deposits or withdrawals. These commands, which are requests to perform an entity state changing action, are persisted as historical events. Please see the
[Akka Persistence documentation](https://doc.akka.io/docs/akka/current/persistence.html#persistence)
for more details about Event Sourcing and Akka Persistence. Here we will focus on the specific parts of this example that are related to persisting events.

~~~java
@Override
public Receive createReceive() {
    return receiveBuilder()
            .match(EntityMessage.DepositCommand.class, this::deposit)
            .match(EntityMessage.WithdrawalCommand.class, this::withdrawal)
            .match(EntityMessage.Query.class, this::query)
            .matchEquals(ReceiveTimeout.getInstance(), t -> passivate())
            .build();
}
~~~

In the `createReceive()` method, shown above, is the routing of messages sent to the `EntityPersistenceActor`. Deposit and withdrawal command messages are routed to the appropriate methods.

~~~java
private void deposit(EntityMessage.DepositCommand depositCommand) {
    log.info("{} <- {}", depositCommand, sender());
    persist(tagCommand(depositCommand), taggedEvent -> handleDeposit(depositCommand, taggedEvent));
}
~~~

When a deposit command message is received, it is handled by the `deposit(...)` method; and this method invokes the inherited `persist(...)` method. The persist method stores events in an event store, such as a Cassandra table. In the above code snippet, the `tagCommand(...)` method creates events from commands. (Note: tags are covered in the next project that covers Akka persistence query.) A lambda is called after the event has been persisted. In this case, the `handleDeposit(...)` method is invoked.

~~~java
private void handleDeposit(EntityMessage.DepositCommand depositCommand, Tagged taggedEvent) {
    if (taggedEvent.payload() instanceof EntityMessage.DepositEvent) {
        EntityMessage.DepositEvent depositEvent = (EntityMessage.DepositEvent) taggedEvent.payload();
        update(depositEvent);
        log.info("{} {} {} -> {}", depositCommand, depositEvent, entity, sender());
        sender().tell(EntityMessage.CommandAck.from(depositCommand, depositEvent), self());
    }
}
~~~

The `handleDeposit` method updates the bank account entity. In this case, the deposit amount is added to the account balance. Also, an acknowledgment message is sent to the command message sender. Something is missing from this code, can you see it? Can you fix it?

~~~java
private void withdrawal(EntityMessage.WithdrawalCommand withdrawalCommand) {
    log.info("{} <- {}", withdrawalCommand, sender());
    persist(tagCommand(withdrawalCommand), taggedEvent -> handleWithdrawal(withdrawalCommand, taggedEvent));
}
~~~

~~~java
private void handleWithdrawal(EntityMessage.WithdrawalCommand withdrawalCommand, Tagged taggedEvent) {
    if (taggedEvent.payload() instanceof EntityMessage.WithdrawalEvent) {
        EntityMessage.WithdrawalEvent withdrawalEvent = (EntityMessage.WithdrawalEvent) taggedEvent.payload();
        update(withdrawalEvent);
        log.info("{} {} {} -> {}", withdrawalCommand, withdrawalEvent, entity, sender());
        sender().tell(EntityMessage.CommandAck.from(withdrawalCommand, withdrawalEvent), self());
    }
}
~~~

TODO

### Installation

~~~bash
git clone https://github.com/mckeeh3/akka-java-cluster-persistence.git
cd akka-java-cluster-persistence
mvn clean package
~~~

The Maven command builds the project and creates a self contained runnable JAR.

### Cassandra Installation

TODO make sure to mention creating all of the tables

### Run a cluster (Mac, Linux)

The project contains a set of scripts that can be used to start and stop individual cluster nodes or start and stop a cluster of nodes.

The main script `./akka` is provided to run a cluster of nodes or start and stop individual nodes.
Use `./akka node start [1-9] | stop` to start and stop individual nodes and `./akka cluster start [1-9] | stop` to start and stop a cluster of nodes.
The `cluster` and `node` start options will start Akka nodes on ports 2551 through 2559.
Both `stdin` and `stderr` output is sent to a file in the `/tmp` directory using the file naming convention `/tmp/<project-dir-name>-N.log`.

Start node 1 on port 2551 and node 2 on port 2552.
~~~bash
./akka node start 1
./akka node start 2
~~~

Stop node 3 on port 2553.
~~~bash
./akka node stop 3
~~~

Start a cluster of four nodes on ports 2551, 2552, 2553, and 2554.
~~~bash
./akka cluster start 4
~~~

Stop all currently running cluster nodes.
~~~bash
./akka cluster stop
~~~

You can use the `./akka cluster start [1-9]` script to start multiple nodes and then use `./akka node start [1-9]` and `./akka node stop [1-9]`
to start and stop individual nodes.

Use the `./akka node tail [1-9]` command to `tail -f` a log file for nodes 1 through 9.

The `./akka cluster status` command displays the status of a currently running cluster in JSON format using the
[Akka Management](https://developer.lightbend.com/docs/akka-management/current/index.html)
extension
[Cluster Http Management](https://developer.lightbend.com/docs/akka-management/current/cluster-http-management.html).

### Run a cluster (Windows, command line)

The following Maven command runs a signle JVM with 3 Akka actor systems on ports 2551, 2552, and a radmonly selected port.
~~~~bash
mvn exec:java
~~~~
Use CTRL-C to stop.

To run on specific ports use the following `-D` option for passing in command line arguements.
~~~~bash
mvn exec:java -Dexec.args="2551"
~~~~
The default no arguments is equilevalant to the following.
~~~~bash
mvn exec:java -Dexec.args="2551 2552 0"
~~~~
A common way to run tests is to start single JVMs in multiple command windows. This simulates running a multi-node Akka cluster.
For example, run the following 4 commands in 4 command windows.
~~~~bash
mvn exec:java -Dexec.args="2551" > /tmp/$(basename $PWD)-1.log
~~~~
~~~~bash
mvn exec:java -Dexec.args="2552" > /tmp/$(basename $PWD)-2.log
~~~~
~~~~bash
mvn exec:java -Dexec.args="0" > /tmp/$(basename $PWD)-3.log
~~~~
~~~~bash
mvn exec:java -Dexec.args="0" > /tmp/$(basename $PWD)-4.log
~~~~
This runs a 4 node Akka cluster starting 2 nodes on ports 2551 and 2552, which are the cluster seed nodes as configured and the `application.conf` file.
And 2 nodes on randomly selected port numbers.
The optional redirect `> /tmp/$(basename $PWD)-4.log` is an example for pushing the log output to filenames based on the project direcctory name.

For convenience, in a Linux command shell define the following aliases.

~~~~bash
alias p1='cd ~/akka-java/akka-java-cluster'
alias p2='cd ~/akka-java/akka-java-cluster-aware'
alias p3='cd ~/akka-java/akka-java-cluster-singleton'
alias p4='cd ~/akka-java/akka-java-cluster-sharding'
alias p5='cd ~/akka-java/akka-java-cluster-persistence'
alias p6='cd ~/akka-java/akka-java-cluster-persistence-query'

alias m1='clear ; mvn exec:java -Dexec.args="2551" > /tmp/$(basename $PWD)-1.log'
alias m2='clear ; mvn exec:java -Dexec.args="2552" > /tmp/$(basename $PWD)-2.log'
alias m3='clear ; mvn exec:java -Dexec.args="0" > /tmp/$(basename $PWD)-3.log'
alias m4='clear ; mvn exec:java -Dexec.args="0" > /tmp/$(basename $PWD)-4.log'
~~~~

The p1-6 alias commands are shortcuts for cd'ing into one of the six project directories.
The m1-4 alias commands start and Akka node with the appropriate port. Stdout is also redirected to the /tmp directory.
