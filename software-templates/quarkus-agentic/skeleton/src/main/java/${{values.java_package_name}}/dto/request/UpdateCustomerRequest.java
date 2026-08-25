package ${{values.java_package_name}}.dto.request;

import ${{values.java_package_name}}.enums.CustomerType;

import java.time.LocalDate;
import java.util.Map;

public class UpdateCustomerRequest {
    
    public String nombre;
    public String apellido;
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
    public Map<String, Object> metadata;
}