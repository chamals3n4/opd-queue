package lk.opdqueue.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lk.opdqueue.enums.DepartmentType;
import java.time.LocalDateTime;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepartmentType departmentType;

    @Column(nullable = false)
    private int maxQueueCapacity;

    @Column(nullable = false)
    private int currentQueueCount;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        currentQueueCount = 0;
        isActive = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public DepartmentType getDepartmentType() { return departmentType; }
    public void setDepartmentType(DepartmentType departmentType) { this.departmentType = departmentType; }

    public int getMaxQueueCapacity() { return maxQueueCapacity; }
    public void setMaxQueueCapacity(int maxQueueCapacity) { this.maxQueueCapacity = maxQueueCapacity; }

    public int getCurrentQueueCount() { return currentQueueCount; }
    public void setCurrentQueueCount(int currentQueueCount) { this.currentQueueCount = currentQueueCount; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
