package com.wsmith.dosingcontrol.model;

import com.wsmith.dosingcontrol.converter.ChicagoInstantConverter;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dosing_formulas")
public class DosingFormula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "chemical_type", nullable = false, length = 20)
    private ChemicalType chemicalType;

    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormulaStatus status;

    @Column(name = "base_dose", nullable = false, precision = 10, scale = 4)
    private BigDecimal baseDose;

    @Column(name = "min_dose", nullable = false, precision = 10, scale = 4)
    private BigDecimal minDose;

    @Column(name = "max_dose", nullable = false, precision = 10, scale = 4)
    private BigDecimal maxDose;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "approved_by")
    private String approvedBy;

    @Convert(converter = ChicagoInstantConverter.class)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Convert(converter = ChicagoInstantConverter.class)
    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "formula", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<FormulaInput> inputs = new ArrayList<>();

    protected DosingFormula() {
        // JPA
    }

    public DosingFormula(ChemicalType chemicalType, Integer version, FormulaStatus status,
                          BigDecimal baseDose, BigDecimal minDose, BigDecimal maxDose,
                          String createdBy) {
        this.chemicalType = chemicalType;
        this.version = version;
        this.status = status;
        this.baseDose = baseDose;
        this.minDose = minDose;
        this.maxDose = maxDose;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    /** Keeps both sides of the parent/child relationship in sync. */
    public void addInput(FormulaInput input) {
        inputs.add(input);
        input.setFormula(this);
    }

    public void removeInput(FormulaInput input) {
        inputs.remove(input);
        input.setFormula(null);
    }

    // --- getters / setters ---

    public Long getId() { return id; }

    public ChemicalType getChemicalType() { return chemicalType; }
    public void setChemicalType(ChemicalType chemicalType) { this.chemicalType = chemicalType; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public FormulaStatus getStatus() { return status; }
    public void setStatus(FormulaStatus status) { this.status = status; }

    public BigDecimal getBaseDose() { return baseDose; }
    public void setBaseDose(BigDecimal baseDose) { this.baseDose = baseDose; }

    public BigDecimal getMinDose() { return minDose; }
    public void setMinDose(BigDecimal minDose) { this.minDose = minDose; }

    public BigDecimal getMaxDose() { return maxDose; }
    public void setMaxDose(BigDecimal maxDose) { this.maxDose = maxDose; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<FormulaInput> getInputs() { return inputs; }
}
