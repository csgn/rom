# r ◦ m

In-Memory MapReduce implementation in Scala

# Components

1. App: Beginning of the mapreduce program. User defines its own configuration
and mapper/reducer functions.

2. Reactor (is a Task): It reads and splits input files, forks coordinator and
workers, and returns the result to the user.

3. Coordinator (is a Task): It assigns tasks to workers. Act like a messenger
between other workers. It returns the result to the reactor.

4. Worker (is a Task): It runs map and reduce functions that givens by the user at App.

5. Task: It is a fiber(thread) and has a lifecycle like 'idle', 'run', 'stop'.

# Execution Steps

1. User creates App with configuration and functions.
```
object Main extends rom.App {
    def run: IO[ExitCode] = for {
        spec = rom.Spec(
            "input" -> (
                "format" -> "text",
                "filePattern" -> "log-*.txt"
            ),
            "numOfFibers" -> 2
        )
        mapper = rom.Mapper("WordCounter") { ... }
        reducer = rom.Reducer("Adder") { ... }
        proc = rom.Proc(spec, mapper, reducer)
        result <- proc.run
    } yield IO.println(result).as(ExitCode.Success)
}
```

# License
```
MIT License

Copyright (c) 2024 csgn

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
