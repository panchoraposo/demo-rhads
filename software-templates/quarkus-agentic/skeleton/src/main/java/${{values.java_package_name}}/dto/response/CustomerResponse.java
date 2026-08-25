package ${{values.java_package_name}}.dto.response;

import ${{values.java_package_name}}.enums.CustomerType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public class CustomerResponse {
    
    public Long id;
    public String identificacion;
    public String tipoIdentificacion;
    public String nombre;
    public String apellido;
    public String nombreCompleto;
    public LocalDate fechaNacimiento;
    public String email;
    public String telefono;
    public String telefonoAlternativo;
    public String direccion;
    public String ciudad;
    public String estadoProvincia;
    public String codigoPostal;
    public Long paisId;
    public CustomerType tipoCliente;
    public BigDecimal scoreCrediticio;
    public String nivelRiesgo;
    public LocalDate fechaRegistro;
    public Boolean activo;
    public Long sucursalId;
    public Long ejecutivoId;
    public Map<String, Object> metadata;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}