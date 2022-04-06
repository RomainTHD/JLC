package fr.rthd.jlc.typecheck.exception;

/**
 * No such function
 * @author RomainTHD
 */
public class NoSuchFunctionException extends TypeException {
    public NoSuchFunctionException(String varName) {
        super(String.format("No such function `%s`", varName));
    }
}
