# Loopy Java - a looping class of dubious value

This repository contains an implementation of the Java "To" loop construct.
It shows how Java instance initializers could be abused into being a looping
construct long before lambdas.

The "new To" loop provides a looping alternative that is often slightly more
concise than the for loop.  It allows you to replace

```
 for (Object x : new Integer[] {8,6,7,5,3,0,9}) { print("Jenny:" + x); };
```

with

```
 new To(8,6,7,5,3,0,9) {{ print("Jenny:" + x); }};
```

Unlike, the standard for loop, the new To loop also accepts IteratorS and EnumerationS

```
 new To(System.getProperties().keys()) {{ print(x); }};
```

A To is an object that implements several interfaces, so that adapters aren't
required in order use it in a variety of ways.  Because it is a Collection,
it can be executed again later, along with additional code, using a for loop.

```
 for (Object x : to) {
 // do additional stuff to x, here
 }
```

If no additional action is to be taken on the subsequent runs, it can simply
be run as a Runnable, or called as a Callable.  No matter how the loop is executed
again, any registered ObserverS are notified.  Thus, in addition to being nested
like for loops, To loops can be wired together for interleaved execution.

```
 To tick = new To(1,2,3) {{ print("tick:" + x); }};
 To tock = new To(4,5,6) {{ print("tock:" + x); }};

 tick.addObserver(tock);

 tick.run();
```

Using this code for anything serious is probably a bad idea.
If you still want to use it, just copy the [To](To.java) class into your
code and use it like the examples above.

