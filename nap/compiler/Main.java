package compiler;

import ast.*;
import parser.*;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;

public class Main
{

  public static void main( String[] args ) throws Exception
  {
    // FIRST STEP: analysis
    // Creation of the stream of characters
    InputStream stream;
    CharStream input;
    CommonTokenStream tokens;
    ParseTree tree;
    Program program;

    if ( args.length == 0 )
    {
      stream = System.in;
    }
    else
    {
      stream = new FileInputStream( new File( args[0] ) );
    }

    input = CharStreams.fromStream( stream );

    // Creation of the lexer for nap programs
    napLexer lexer = new napLexer( input );

    tokens = new CommonTokenStream(lexer);

    // Creation of the parser for pico programs
    napParser parser = new napParser( tokens );

    // Parse the input: the result is a parse tree
    tree = parser.program();

    // Walk the parse tree in order to create an
    // abstract syntax tree
    napVisitor<Ast> buildAST = new BuildAST();

    program = (Program) buildAST.visit( tree );

    if ( args.length > 1 && args[1].equals("-p") )
    {
      System.out.println( program.accept( new PrettyPrinter(2) ) );
    }
    else if ( 1 < args.length && args[1].equals("-i") )
    {
    }
  }
}
