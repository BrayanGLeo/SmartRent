package cl.duoc.mssmartrentbff.dto;

public class GraphUserDto {
    private String id;
    private String fullName;
    private String email;
    private String tenantId;
    private String company;
    private String role;
    private String status;
    private String createdAt;

    public GraphUserDto() {}

    public GraphUserDto(String id, String fullName, String email, String tenantId, String company, String role, String status, String createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.tenantId = tenantId;
        this.company = company;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
