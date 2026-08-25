package ${{values.java_package_name}}.enums;

/**
 * Enum representing the risk level of a customer
 */
public enum RiskLevel {
    /**
     * Low risk - Excellent credit history and financial stability
     */
    BAJO("Bajo"),
    
    /**
     * Medium risk - Good credit history with minor concerns
     */
    MEDIO("Medio"),
    
    /**
     * High risk - Poor credit history or financial instability
     */
    ALTO("Alto"),
    
    /**
     * Very High risk - Critical credit issues or fraud concerns
     */
    MUY_ALTO("Muy Alto");
    
    private final String displayName;
    
    RiskLevel(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static RiskLevel fromDisplayName(String displayName) {
        for (RiskLevel level : RiskLevel.values()) {
            if (level.displayName.equalsIgnoreCase(displayName)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown risk level: " + displayName);
    }
}