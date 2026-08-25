package ${{values.java_package_name}}.dto.response;

import ${{values.java_package_name}}.enums.CustomerType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class CustomerSummaryResponse {
    
    // Customer Basic Info
    public Long id;
    public String identificacion;
    public String tipoIdentificacion;
    public String nombreCompleto;
    public String email;
    public String telefono;
    public String ciudad;
    public String estadoProvincia;
    public Long paisId;
    public CustomerType tipoCliente;
    public Boolean activo;
    public LocalDate fechaRegistro;
    
    // Credit Information
    public BigDecimal scoreCrediticio;
    public String nivelRiesgo;
    public String evaluacionCrediticia;
    public LocalDateTime ultimaActualizacionScore;
    
    // Account Information
    public Integer totalCuentas;
    public Integer cuentasActivas;
    public BigDecimal saldoTotal;
    public List<AccountSummary> cuentas;
    
    // Transaction Summary
    public Integer transaccionesUltimos30Dias;
    public BigDecimal montoTransaccionesUltimos30Dias;
    public LocalDateTime ultimaTransaccion;
    
    // Loan Information
    public Integer prestamosActivos;
    public BigDecimal deudaTotal;
    public BigDecimal proximoPago;
    public LocalDate fechaProximoPago;
    
    // Relationship Information
    public Long ejecutivoId;
    public String nombreEjecutivo;
    public Long sucursalId;
    public String nombreSucursal;
    public Integer antiguedadMeses;
    
    // Alerts and Notes
    public List<String> alertas;
    public List<String> notasImportantes;
    public Map<String, Object> metadata;
    
    // Audit Info
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDateTime ultimaActualizacion;
    
    // Inner class for account summary
    public static class AccountSummary {
        public Long accountId;
        public String numeroCuenta;
        public String tipoCuenta;
        public BigDecimal saldo;
        public Boolean activa;
        public LocalDate fechaApertura;
    }
}