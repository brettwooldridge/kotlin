import kotlin.Unit;
import pack.A;

import java.util.Collections;
import java.util.List;

class JavaClass {
    public List<A> getA() {
        A a = new A(1, "", "");
        return Collections.singletonList(a);
    }

    public void takeA(A a){}

    public void takeSAM(JavaSAM sam){}
}

public interface JavaSAM {
    void takeA(A a);
}