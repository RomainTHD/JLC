package fr.rthd.jlc.optimizer;

import fr.rthd.jlc.env.FunType;

import java.util.HashSet;
import java.util.Set;

/**
 * Functions optimizer
 */
public class FunTypeOptimizer extends FunType {
    /**
     * Set of all functions using this
     */
    private final Set<FunType> _usedBy;

    /**
     * Constructor
     * @param funType Base function
     */
    public FunTypeOptimizer(FunType funType) {
        super(funType);
        this._usedBy = new HashSet<>();
    }

    /**
     * Add a function using this. For example, if `f` is a function that call
     * `g`, then `f` is added to `g`'s usage set.
     * @param user Function using this
     */
    public void addUsageIn(FunTypeOptimizer user) {
        this._usedBy.add(user);
        // To avoid infinite recursion or deep transversal, we increase space
        //  complexity by merging sets.
        this._usedBy.addAll(user._usedBy);
    }

    /**
     * Check whether this function is used by the main function or not
     * @return If this function is used by the main function
     */
    public boolean isUsedByMain() {
        return this._usedBy.stream().anyMatch(FunType::isMain);
    }
}
