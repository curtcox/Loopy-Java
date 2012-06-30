package temp;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;

/**
 The "new To" loop provides a looping alternative that is often slightly more
 concise than the for loop.  It allows you to replace
<pre>
    for (Object x : new Integer[] {8,6,7,5,3,0,9}) { print("Jenny:" + x); };
</pre>
 * with 
<pre>
    new To(8,6,7,5,3,0,9) {{ print("Jenny:" + x); }};
</pre>
<p>
Unlike, the standard for loop, the new To loop also accepts IteratorS and EnumerationS
<pre>
    new To(System.getProperties().keys()) {{ print(x); }};
</pre>
A To is an object that implements several interfaces, so that adapters aren't
required in order use it in a variety of ways.  Because it is a Collection,
it can be executed again later, along with additional code, using a for loop.
<pre>
    for (Object x : to) {
        // do additional stuff to x, here
    }
</pre>
If no additional action is to be taken on the subsequent runs, it can simply
be run as a Runnable, or called as a Callable.  No matter how the loop is executed
again, any registered ObserverS are notified.  Thus, in addition to being nested
like for loops, To loops can be wired together for interleaved execution.

<pre>
    To tick = new To(1,2,3) {{ print("tick:" + x); }};
    To tock = new To(4,5,6) {{ print("tock:" + x); }};

    tick.addObserver(tock);

    tick.run();
</pre>
 *
 * <p>
 Unfortunately, despite these advantages, the current implementation
 has several glaring limitations.
 * <ul>
 * <li> Special care must be taken to handle empty loops
 * <li> Only supports To loops that are static classes
 * <li> It has much more overhead and is much slower than an equivalent for loop
 * </ul>
 * 
 */
public class To<X>
    extends Observable
    implements Iterator<X>, Collection<X>, Enumeration<X>, Runnable, Callable, Observer
{

    /**
     * The iterator we defer to
     */
    private Iterator<X> iterator;

    /**
     * The loop variable value
     */
    public final X x;

    /**
     * The arguments for this To
     */
    private final List<X> args = new ArrayList();

    /**
     * The constructor for the (generally anonymous) class that extends this one.
     */
    private final Constructor<?> extending;

    /**
     * The formal parameters of the extending constructor.
     */
    private final Class[] formals;

    /**
     * The type of argument that was passed in the constructor
     */
    private final Type type;

    /**
     * The types of arguments that can be passed in the constructor.
     */
    private enum Type { VARARG, ITERATOR, ITERABLE, ENUMERATION; }
    /**
     * This wraps an arg, so that it can be passed through the constructor.
     */
    private static final class ArgWrapper implements Iterator, Iterable, Enumeration {
        /**
         * The single value we hold.
         */
        final Object x;
        final Constructor<?> extending;
        final Class[] formals;

        ArgWrapper(Object x, Constructor<?> extending, Class[] formals) {
            this.x = x;
            this.extending = extending;
            this.formals = formals;
        }

        public Iterator       iterator() { return this; }
        public boolean hasMoreElements() { return true; }
        public Object      nextElement() { return x; }
        public boolean         hasNext() { return true; }
        public Object             next() { return x; }
        public void             remove() {}
    } // ArgWrapper

    /**
     * Override this constructor for looping through individually specified values.
     */
    protected To(X... xs) {
        type = Type.VARARG;
        if (xs==null || xs.length==0) {
            throw badArgs(xs);
        }

        // ArgWrapper
        // This was created by the last if block in this constructor.
        // Reflectively create a new normal instance.
        if (xs[0] instanceof ArgWrapper) {
            ArgWrapper wrapper = (ArgWrapper) xs[0];
            x = (X) wrapper.x;
            extending = wrapper.extending;
            formals   = wrapper.formals;
            return;
        }
        extending = extendingConstructor();
        formals = extending.getParameterTypes();

        // single value -- we can handle this
        if (xs.length == 1) {
            x = xs[0];
            return;
        }

        // Multiple values
        // This is the constructor our subclass will use.
        if (xs.length > 1) {
            for (int i=0; i<xs.length-1; i++) {
                X arg = (X) xs[i];
                args.add(arg);
                newInstanceWithArg(arg);
            }
            x = xs[xs.length - 1];
            args.add(x);
            return;
        }
        throw badArgs(xs);
    }

    /**
     * Override this constructor for looping through individually specified values.
     */
    protected To(Iterable<X> iterable) {
        type = Type.ITERABLE;
        if (iterable==null) {
            throw badArgs();
        }
        extending = extendingConstructor();
        formals = extending.getParameterTypes();
        // ArgWrapper
        // This was created by the last if block in this constructor.
        // Reflectively create a new normal instance.
        if (iterable instanceof ArgWrapper) {
            ArgWrapper wrapper = (ArgWrapper) iterable;
            x = (X) wrapper.x;
            return;
        }

        // must not be empty
        Iterator i = iterable.iterator();
        if (!i.hasNext()) {
            throw badArgs();
        }

        X arg = (X) i.next();
        // single value -- we can handle this
        if (!i.hasNext()) {
            x =  arg;
            args.add(arg);
            return;
        }

        // Multiple values
        // This is the constructor our subclass will use.
        for (; i.hasNext(); arg = (X) i.next()) {
            args.add(arg);
            if (i.hasNext()) {
                newInstanceWithArg(arg);
            }
        }
        args.add(arg);
        x = arg;
    }

    /**
     * Override this constructor for looping through individually specified values.
     */
    protected To(Iterator<X> i) {
        type = Type.ITERATOR;
        if (i==null) {
            throw badArgs();
        }
        // ArgWrapper
        // This was created by the last if block in this constructor.
        // Reflectively create a new normal instance.
        if (i instanceof ArgWrapper) {
            ArgWrapper wrapper = (ArgWrapper) i;
            x = (X) wrapper.x;
            extending = wrapper.extending;
            formals   = wrapper.formals;
            return;
        }
        extending = extendingConstructor();
        formals = extending.getParameterTypes();

        // must not be empty
        if (!i.hasNext()) {
            throw badArgs();
        }

        X arg = i.next();
        // single value -- we can handle this
        if (!i.hasNext()) {
            x = arg;
            args.add(arg);
            return;
        }

        // Multiple values
        // This is the constructor our subclass will use.
        for (; i.hasNext(); arg = i.next()) {
            args.add(arg);
            if (i.hasNext()) {
                newInstanceWithArg(arg);
            }
        }
        args.add(arg);
        x = arg;
    }

    /**
     * Override this constructor for looping through individually specified values.
     */
    protected To(Enumeration<X> e) {
        type = Type.ENUMERATION;
        if (e==null) {
            throw badArgs();
        }
        // ArgWrapper
        // This was created by the last if block in this constructor.
        // Reflectively create a new normal instance.
        if (e instanceof ArgWrapper) {
            ArgWrapper wrapper = (ArgWrapper) e;
            x = (X) wrapper.x;
            extending = wrapper.extending;
            formals   = wrapper.formals;
            return;
        }
        extending = extendingConstructor();
        formals = extending.getParameterTypes();

        // must not be empty
        if (!e.hasMoreElements()) {
            throw badArgs();
        }

        X arg = e.nextElement();
        // single value -- we can handle this
        if (!e.hasMoreElements()) {
            x = arg;
            args.add(arg);
            return;
        }

        // Multiple values
        // This is the constructor our subclass will use.
        for (; e.hasMoreElements(); arg = e.nextElement()) {
            args.add(arg);
            if (e.hasMoreElements()) {
                newInstanceWithArg(arg);
            }
        }
        args.add(arg);
        x = arg;
    }

    private static IllegalArgumentException badArgs(Object... xs) {
        return new IllegalArgumentException("" + xs);
    }

    /**
     * Use the same constructor that is invoking us, to create a new instance.
     * We do this in order to run whatever code in the subclass instance
     * initializer again.  It can refer to x, which will have the given value.
     */
    private void newInstanceWithArg(Object x) {
        try {
            newInstanceWithArgWrapper0(x);
        } catch (IllegalArgumentException e) {
            String message = x + " not valid for constructor " + extending + " taking "+ Arrays.asList(formals);
            throw new IllegalArgumentException(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            setChanged();
            notifyObservers(x);
        }
    }

    private To newInstance0(Object x)
        throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object[] params = new Object[formals.length];
        params[0] = x;
        return (To) extending.newInstance(params);
    }

    private To newInstanceWithArgWrapper0(Object x)
        throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object[] params = new Object[formals.length];
        params[0] = new ArgWrapper(x,extending,formals);
        if (extending.isVarArgs()) {
            params = new Object[] {params};
        }
        return (To) extending.newInstance(params);
    }

    /**
     * Return the constructor of the class that is extending this one.
     */
    private static Constructor<?> extendingConstructor() {
        try {
            return extendingConstructor0();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static Constructor<?> extendingConstructor0() throws ClassNotFoundException {
        Class<? extends To> c = (Class<To>) Class.forName(extendingClass());
        Constructor<?> con = c.getDeclaredConstructors()[0];
        return con;
    }
    /**
     * Return the (usually anonymous) class that extends this one.
     */
    private static String extendingClass() {
        for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
            String c = e.getClassName();
            if (isExtendingClass(c)) {
                return c;
            }
        }
        throw new IllegalStateException();
    }

    private static boolean isExtendingClass(String c) {
        return  c !=null &&
              ! c.equals(To.class.getName()) &&
              ! c.equals(Thread.class.getName());
    }

    /*
     * The next set of methods are for Iteration and Enumeration.
     */
    public boolean hasMoreElements() { return hasNext(); }
    public X           nextElement() { return next(); }
    public boolean         hasNext() {
        if (iterator==null) {
            iterator();
        }
        return iterator.hasNext();
    }
    final public       void remove() { iterator.remove(); }
    public X                  next() {
        X x = (X) iterator.next();
        newInstanceWithArg(x);
        this.notifyObservers(x);
        return x;
    }

    /**
     * Run this To loop.
     */
    public void run() {
        for (Object x : args) {
            newInstanceWithArg(x);
        }
    }

    /**
     * Call this To loop;
     */
    public Object call() {
        run();
        return null;
    }

    /**
     * Return an iterator that can be used to iterate through this loop.
     */
    public Iterator<X> iterator() {
        iterator = args.iterator();
        return this;
    }

    // the next several methods are from Collection
    public int size() {
        return args.size();
    }

    public boolean isEmpty() {
        return args.isEmpty();
    }

    public boolean contains(Object o) {
        return args.contains(o);
    }

    public Object[] toArray() {
        return args.toArray();
    }

    public Object[] toArray(Object[] a) {
        return args.toArray(a);
    }

    public boolean add(X e) {
        boolean flag = args.add(e);
        notifyObservers(e);
        return flag;
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean containsAll(Collection c) {
        return args.containsAll(c);
    }

    public boolean addAll(Collection<? extends X> c) {
        boolean flag = args.addAll(c);
        for (Object o : c) {
            notifyObservers(o);
        }
        return flag;
    }

    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void update(Observable o, Object arg) {
        if (!hasNext()) {
            iterator();
        }
        next();
    }

    /**
     * @param args the command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        List list1 = new ArrayList();
        list1.add(1);

        To iterable1 = new To(list1)                             {{print("iterable:" +x); }};
        To iterator1 = new To(list1.iterator())                  {{print("iterator:" +x); }};
        To enumeration1 = new To(Collections.enumeration(list1)) {{print("enumeration:" +x); }};

        List list2 = new ArrayList();
        list2.add(1);
        list2.add(2);

        To iterable2 = new To(list2)                             {{print("iterable:" +x); }};
        To iterator2 = new To(list2.iterator())                  {{print("iterator:" +x); }};
        To enumeration2 = new To(Collections.enumeration(list2)) {{print("enumeration:" +x); }};

        new To("bar","baz") {{ print("2:" +x);  }};
        new To(1,2,3)       {{ print("3:" +x);  }};
        new To(1,2,3,4)     {{ print("4:" + x); }};
        new To(8,6,7,5,3,0,9)   {{ print("Jenny:" + x); }};
        for (Object x : Arrays.asList(new Integer[] {8,6,7,5,3,0,9})) { print("Jenny:" + x); };

        new To(System.getProperties().keys()) {{ print(x); }};

        for (Object o :new To(1,2){}) {}

        new To(iterable1,iterator1,enumeration1) {{
           ((To)x).run();
        }};
        new To(iterable2,iterator2,enumeration2) {{
           ((To)x).run();
        }};

        To<To> to = new To(iterable2,iterator2,enumeration2) {{
           ((To)x).run();
        }};

        for (To x : to) {
            for (Object y : x) {
                print("In for " + y);
            }
        }

        To tick = new To(1,2,3) {{ print("tick:" + x); }};
        To tock = new To(4,5,6) {{ print("tock:" + x); }};

        tick.addObserver(tock);

        print("Convoluted");

        tick.run();

        print("Done");
        System.exit(0);
    }

    static void print(Object x) {
        System.out.println( x );
    }
}
