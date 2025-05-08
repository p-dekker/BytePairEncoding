import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Vocabulary {
    final private int maxSize;
    final private Set<Token> set;

    public Vocabulary(int maxSize) {
        this.maxSize = maxSize;
        set = new HashSet<>();
    }

    public boolean add(Token t) {
        set.add(t);
        return set.size() < maxSize;
    }

    public boolean addAll(Collection<Token> c) {
        set.addAll(c);
        return set.size() < maxSize;
    }
}
