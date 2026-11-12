package com.enterprise.iam.identity.domain;

import com.enterprise.iam.common.jpa.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "iam_user_attribute")
public class IamUserAttribute extends AuditedEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "attr_code", nullable = false, length = 64)
    private String attrCode;

    @Column(name = "value_text")
    private String valueText;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getAttrCode() {
        return attrCode;
    }

    public void setAttrCode(String attrCode) {
        this.attrCode = attrCode;
    }

    public String getValueText() {
        return valueText;
    }

    public void setValueText(String valueText) {
        this.valueText = valueText;
    }
}
