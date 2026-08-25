package ${{values.java_package_name}}.enums;

/**
 * Enum representing different types of identification documents
 * used across different countries in Latin America and globally
 */
public enum IdentificationType {
    /**
     * Documento Nacional de Identidad (Spain, Argentina, Peru, etc.)
     */
    DNI("DNI", "Documento Nacional de Identidad"),
    
    /**
     * Passport - International identification
     */
    PASSPORT("PASSPORT", "Passport"),
    
    /**
     * Social Security Number (United States)
     */
    SSN("SSN", "Social Security Number"),
    
    /**
     * Registro Federal de Contribuyentes (Mexico)
     */
    RFC("RFC", "Registro Federal de Contribuyentes"),
    
    /**
     * Clave Única de Registro de Población (Mexico)
     */
    CURP("CURP", "Clave Única de Registro de Población"),
    
    /**
     * Cadastro de Pessoas Físicas (Brazil)
     */
    CPF("CPF", "Cadastro de Pessoas Físicas"),
    
    /**
     * Clave Única de Identificación Tributaria (Argentina)
     */
    CUIT("CUIT", "Clave Única de Identificación Tributaria"),
    
    /**
     * Rol Único Tributario (Chile)
     */
    RUT("RUT", "Rol Único Tributario"),
    
    /**
     * Rol Único Nacional (Chile - alternative name)
     */
    RUN("RUN", "Rol Único Nacional"),
    
    /**
     * National Insurance Number (United Kingdom)
     */
    NINO("NINO", "National Insurance Number"),
    
    /**
     * Código de Identificación Fiscal (Spain - companies)
     */
    CIF("CIF", "Código de Identificación Fiscal"),
    
    /**
     * Employer Identification Number (United States - companies)
     */
    EIN("EIN", "Employer Identification Number"),
    
    /**
     * Generic Tax ID for other countries
     */
    TAX_ID("TAX_ID", "Tax Identification Number");
    
    private final String code;
    private final String description;
    
    IdentificationType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static IdentificationType fromCode(String code) {
        for (IdentificationType type : IdentificationType.values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown identification type code: " + code);
    }
}