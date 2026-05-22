package internet.magazine.magazine.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "catalog_import_settings")
public class CatalogImportSettings {

    private static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(length = 500)
    private String apiBaseUrl;

    @Column(length = 500)
    private String accessToken;

    @Column(nullable = false)
    private Integer pageSize = 250;

    @Column(nullable = false)
    private Integer maxItems = 0;

    @Column(nullable = false)
    private Long requestIntervalMs = 1200L;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = SINGLETON_ID;
        }
        if (pageSize == null || pageSize < 0) {
            pageSize = 250;
        }
        if (maxItems == null || maxItems < 0) {
            maxItems = 0;
        }
        if (requestIntervalMs == null || requestIntervalMs < 0) {
            requestIntervalMs = 1200L;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        if (pageSize == null || pageSize < 0) {
            pageSize = 250;
        }
        if (maxItems == null || maxItems < 0) {
            maxItems = 0;
        }
        if (requestIntervalMs == null || requestIntervalMs < 0) {
            requestIntervalMs = 1200L;
        }
    }

    public static long singletonId() {
        return SINGLETON_ID;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(Integer maxItems) {
        this.maxItems = maxItems;
    }

    public Long getRequestIntervalMs() {
        return requestIntervalMs;
    }

    public void setRequestIntervalMs(Long requestIntervalMs) {
        this.requestIntervalMs = requestIntervalMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
