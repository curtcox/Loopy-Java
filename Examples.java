import java.util.*;

class Examples {

    static class StaticInner {
        void p(Object x) {
            System.out.println( x );
        }
        void run() {

            List list1 = new ArrayList();
            list1.add(1);

            new To(list1)                             {{p("iterable:" +x); }};
            new To(list1.iterator())                  {{p("iterator:" +x); }};
            new To(Collections.enumeration(list1)) {{p("enumeration:" +x); }};

            List list2 = new ArrayList();
            list2.add(1);
            list2.add(2);

            new To(list2)                             {{p("iterable:" +x); }};
            new To(list2.iterator())                  {{p("iterator:" +x); }};
            new To(Collections.enumeration(list2)) {{p("enumeration:" +x); }};

            new To("bar","baz") {{ print("2:" +x);  }};
            new To(1,2,3)       {{ print("3:" +x);  }};
            new To(1,2,3,4)     {{ print("4:" + x); }};
            new To(8,6,7,5,3,0,9)   {{ print("Jenny:" + x); }};
            for (Object x : Arrays.asList(new Integer[] {8,6,7,5,3,0,9})) { p("Jenny:" + x); };

            new To(System.getProperties().keys()) {{ p(x); }};

        }
    }

    class InstanceInner {
        void q(Object x) {
            System.out.println( x );
        }
        void run() {

            List list1 = new ArrayList();
            list1.add(1);

            new To(list1)                             {{q("iterable:" +x); }};
            new To(list1.iterator())                  {{q("iterator:" +x); }};
            new To(Collections.enumeration(list1)) {{q("enumeration:" +x); }};

            List list2 = new ArrayList();
            list2.add(1);
            list2.add(2);

            new To(list2)                             {{q("iterable:" +x); }};
            new To(list2.iterator())                  {{q("iterator:" +x); }};
            new To(Collections.enumeration(list2)) {{q("enumeration:" +x); }};

            new To("bar","baz") {{ q("2:" +x);  }};
            new To(1,2,3)       {{ q("3:" +x);  }};
            new To(1,2,3,4)     {{ q("4:" + x); }};
            new To(8,6,7,5,3,0,9)   {{ q("Jenny:" + x); }};
            for (Object x : Arrays.asList(new Integer[] {8,6,7,5,3,0,9})) { q("Jenny:" + x); };

            new To(System.getProperties().keys()) {{ q(x); }};

        }
    }

    static void staticExamples() {

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
    }

    static void run() {
        staticExamples();
        new StaticInner().run();
        Examples examples = new Examples();
        Examples.InstanceInner instanceInner = examples.new InstanceInner();
        instanceInner.run();
    }

    static void print(Object x) {
        System.out.println( x );
    }

    public static void main(String[] args) {
        run();
    }

}
