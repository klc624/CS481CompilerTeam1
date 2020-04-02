// IMPORTS

// class for block hierarchy
  // Map< Block, Block > (parent, child)
public class HierarchyMap
{
  // Map to store relationships between parent/child symbol tables
  HashMap< Block, Block > hMap;

  // constructor initialize
  public HierarchyMap( )
  {
    this.hMap = new HashMap<>();
  }

  // constructor given first pair
  public HierarchyMap( Block child, Block parent )
  {
    HashMap< Block, Block > map = new HashMap<>();
    map.put( child, parent );
    this.hMap = map;
  }

  // method to add a parent child binding
  public void addBinding( Block child, Block parent )
  {
    this.hMap.put( child, parent );
  }
}
