package upem.jarret.http;

import java.io.IOException;
import java.util.Objects;

public class HTTPException extends IOException {

    private static final long serialVersionUID = -1810727803680020453L;

    public HTTPException() {
        super();
    }

    public HTTPException(String s) {
        super(Objects.requireNonNull(s));
    }

    public static void ensure(boolean b, String string) throws HTTPException {
        if (!b)
            throw new HTTPException(Objects.requireNonNull(string));

    }
}
