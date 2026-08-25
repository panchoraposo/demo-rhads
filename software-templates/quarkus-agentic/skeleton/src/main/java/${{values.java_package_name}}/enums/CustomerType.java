package ${{values.java_package_name}}.enums;

/**
 * Enum representing the type of customer in NeuralBank system
 */
public enum CustomerType {
    /**
     * Individual/Personal customer
     */
    PERSONAL,
    
    /**
     * Business customer (small to medium enterprises)
     */
    EMPRESARIAL,
    
    /**
     * Corporate customer (large corporations)
     */
    CORPORATIVO
}