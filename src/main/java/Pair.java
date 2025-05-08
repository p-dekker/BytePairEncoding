import java.util.Objects;

public class Pair {
    Token first, second;

    Pair(Token first, Token second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return first + "-" + second;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair pair)) return false;
        return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    public Token merge() {
        return new Token(first, second);
    }
}