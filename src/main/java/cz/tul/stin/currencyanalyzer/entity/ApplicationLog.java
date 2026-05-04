package cz.tul.stin.currencyanalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_logs")
public class ApplicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String level;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false, length = 1000)
    private String message;

    @Lob
    private String detail;

    protected ApplicationLog() {
    }

    public ApplicationLog(
            LocalDateTime createdAt,
            String level,
            String source,
            String message,
            String detail
    ) {
        this.createdAt = createdAt;
        this.level = level;
        this.source = source;
        this.message = message;
        this.detail = detail;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getLevel() {
        return level;
    }

    public String getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    public String getDetail() {
        return detail;
    }
}
