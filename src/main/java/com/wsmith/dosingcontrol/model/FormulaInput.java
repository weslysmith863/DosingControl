package com.wsmith.dosingcontrol.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "formula_inputs")
public class FormulaInput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formula_id", nullable = false)
    private DosingFormula formula;

    @Column(name = "reading_tag", nullable = false)
    private String readingTag;

    @Column(nullable = false, precision = 12, scale = 6)
    private BigDecimal coefficient;

    protected FormulaInput() {
        // JPA
    }

    public FormulaInput(String readingTag, BigDecimal coefficient) {
        this.readingTag = readingTag;
        this.coefficient = coefficient;
    }

    public Long getId() { return id; }

    public DosingFormula getFormula() { return formula; }
    public void setFormula(DosingFormula formula) { this.formula = formula; }

    public String getReadingTag() { return readingTag; }
    public void setReadingTag(String readingTag) { this.readingTag = readingTag; }

    public BigDecimal getCoefficient() { return coefficient; }
    public void setCoefficient(BigDecimal coefficient) { this.coefficient = coefficient; }
}
