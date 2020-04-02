package compiler;
import type.Type; /* don't know if this is the right import */
import java.util.*;

/*
TODO: this function is a big ol mess...

For a signature I'm not sure how to handle the parameters in the symbol tables
If the parameters are pass by value then when they're used it just
  uses the value
If parameters are passed by reference then they should be in some sort of symbol
  table themselves? Where would they go? In the first local block?
*/

// Signature class for function declarations
  // similar to W2 signature class
  // needs to have public constructors/methods to handle user defined signatures
  // needs to handle pass by reference arguments in functions
public class Signature
{
  // public List< Pair< Pair< String, Type >, Boolean >> arguments;

  // not sure if this should be hashmap or not....
  public HashMap< String, Pair< Type, Boolean > > arguments;
  public Optional< Type > returnType;

  // not sure about the constructor fields....
  public Signature( List< Pair< Pair< String, Type >, Boolean >> args,
                    Type returnType )
  {
    HashMap< String, Pair< Type, Boolean > > tempArgs = new HashMap<>();
    Pair< Type, Boolean > value;
    Type type;
    Boolean bool;
    String funcName;

    for( Pair< Pair< String, Type >, Boolean > a : args )
    {
      type = a.getKey().getValue();
      bool = a.getValue();
      funcName = a.getKey().getKey();

      value = Pair( type, bool );
      tempArgs.put( funcName, value );
    }
    this.arguments = tempArgs;
    this.returnType = Optional.of( returnType );
  }

  public Signature( List< Pair< Pair< String, Type >, Boolean >> args )
  {
    HashMap< String, Pair< Type, Boolean > > tempArgs = new HashMap<>();
    Pair< Type, Boolean > value;
    Type type;
    Boolean bool;
    String funcName;

    for( Pair< Pair< String, Type >, Boolean > a : args )
    {
      type = a.getKey().getValue();
      bool = a.getValue();
      funcName = a.getKey().getKey();

      value = Pair( type, bool );
      tempArgs.put( funcName, value );
    }
    this.arguments = tempArgs;
    this.returnType = Optional.empty();
  }

  // type checking methods here
}
