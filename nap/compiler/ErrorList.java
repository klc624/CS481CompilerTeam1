package compiler;

import java.util.LinkedList;
import java.util.List;

class ErrorList
{
    protected List<String> errors;

    public boolean has_errors() {
        return !errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    public void printErrors(){
        for (String error : errors)
            System.out.println(error);

        System.out.println("Total Errors: " + errors.size() );
    }

    public ErrorList() {
        errors = new LinkedList<>();
    }
}
