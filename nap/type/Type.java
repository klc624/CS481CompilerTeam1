package type;

abstract public class Type
{
    public abstract <T> T accept(Visitor<T> visitor);
}
