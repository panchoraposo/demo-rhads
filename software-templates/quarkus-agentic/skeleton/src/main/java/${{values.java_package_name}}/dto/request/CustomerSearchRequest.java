package ${{values.java_package_name}}.dto.request;

import ${{values.java_package_name}}.enums.CustomerType;

import java.math.BigDecimal;

public class CustomerSearchRequest {
    
    public String search;
    public CustomerType tipoCliente;
    public String ciudad;
    public Long paisId;
    public Long sucursalId;
    public Long ejecutivoId;
    public String nivelRiesgo;
    public BigDecimal scoreMin;
    public BigDecimal scoreMax;
    public Boolean activo;
}