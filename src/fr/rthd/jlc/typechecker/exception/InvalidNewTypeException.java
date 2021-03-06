package fr.rthd.jlc.typechecker.exception;

import fr.rthd.jlc.TypeCode;
import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when a primitive is used on the `new` keyword
 * @author RomainTHD
 */
public class InvalidNewTypeException extends TypeException {
    public InvalidNewTypeException(@NotNull TypeCode type) {
        super(String.format(
            "Cannot use primitive type `%s` with the `new` keyword",
            type.getRealName()
        ));
    }
}
