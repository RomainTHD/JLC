package fr.rthd.jlc.env.exception;

/**
 * Symbol already defined
 * @author RomainTHD
 */
public class SymbolAlreadyDefinedException extends EnvException {
    public SymbolAlreadyDefinedException(String symbolName) {
        super(String.format("Symbol `%s` already defined", symbolName));
    }
}
