package balls;

public class Tuple<T, U> {

    public T t;
    public U u;

    public Tuple(T t, U u) {
        this.t = t;
        this.u = u;
    }

    @Override
    public String toString() {
        return "Tuple{" +
                "t=" + t +
                ", u=" + u +
                '}';
    }
}
