import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class Token {
    private byte[] values;

    public int length() {
        return values.length;
    }

    public Token(Token a, Token b) {
        values = new byte[a.length() + b.length()];
        System.arraycopy(a.values, 0, values, 0, a.length());
        System.arraycopy(b.values, 0, values, a.length(), b.length());
    }

    public Token(byte value) {
        values = new byte[]{value};
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Token token)) return false;
        return Objects.deepEquals(values, token.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        return new String(values, StandardCharsets.UTF_8);
    }
}