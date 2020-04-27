package ir.com;

public class Label extends Command{
    private int label;
    private static int lastLabel;
    private Optional<String> name;

    @Override
    public String toString()
    {
      if( name.isPresent() )
      {
        return name.get() + ": ";
      }

      return "L" + label + ": ";

    }

    public Label() {
        this.label = lastLabel;
        lastLabel += 1;
        this.name = Optional.empty();
    }

    puclic Label( String name )
    {
      this.label = -1;
      this.name = Optional.of( name );
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
