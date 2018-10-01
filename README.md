# The Go-Back-N (GBN) Protocol

### Get Started

1. Compile the program
```
$ make
```

2. On the host __host1__:
```
$ ./nEmulator-linux386 <port1> <host2> <port4> <port3> <host1> <port2> <delay> <discard> <verbose>
```

3. On the host __host2__:
```
$ java receiver <host1> <port3> <port4> <output_file>
```

4. On the host __host3__:
```
$ java sender <host1> <port1> <port2> <input_file>
```

### Tested Machines

- host1 `ubuntu1604-002.student.cs.uwaterloo.ca`
- host2 `ubuntu1604-004.student.cs.uwaterloo.ca`
- host3 `ubuntu1604-006.student.cs.uwaterloo.ca`

### Versions

- Dev Environment: `macOS High Sierra 10.13.5`
- Java Version: `openjdk version "1.8.0_171"`
- Compiler version: `javac 1.8.0_171`
