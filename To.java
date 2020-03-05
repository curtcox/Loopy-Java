import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/**
 See docs and examples at https://github.com/curtcox/Loopy-Java/
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
                X arg = xs[i];
                args.add(arg);
                newInstanceWithArg(this,arg);
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
                newInstanceWithArg(this,arg);
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
                newInstanceWithArg(this,arg);
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
                newInstanceWithArg(this,arg);
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
     * We do this in order to run whatever code is in the subclass instance
     * initializer again.  It can refer to x, which will have the given value.
     */
    private void newInstanceWithArg(To to,Object x) {
        try {
            newInstanceWithArgWrapper0(to,x);
        } catch (IllegalArgumentException e) {
            String message = x + " not valid for constructor " + extending + " taking "+ Arrays.asList(formals);
            throw new IllegalArgumentException(message,e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            setChanged();
            notifyObservers(x);
        }
    }

    private To newInstanceWithArgWrapper0(To to,Object x)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        Field[] declaredFields = to.getClass().getDeclaredFields();
        Object[] params = new Object[formals.length];
        boolean hasThis = declaredFields.length==1;
        boolean varArgs = extending.isVarArgs();
        ArgWrapper wrapped = new ArgWrapper(x,extending,formals);
        Object arg = varArgs ? new Object[] {wrapped} : wrapped;
        if (hasThis) {
            params[0] = declaredFields[0].get(to);
            params[1] = arg;
        } else {
            params[0] = arg;
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
        newInstanceWithArg(this,x);
        this.notifyObservers(x);
        return x;
    }

    /**
     * Run this To loop.
     */
    public void run() {
        for (Object x : args) {
            newInstanceWithArg(this,x);
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

}
