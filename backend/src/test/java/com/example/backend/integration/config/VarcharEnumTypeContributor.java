package com.example.backend.integration.config;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

/**
 * Overrides the NAMED_ENUM JDBC type to use VARCHAR semantics in H2.
 *
 * Entities use @JdbcTypeCode(SqlTypes.NAMED_ENUM) for PostgreSQL native enum support.
 * H2 does not support this type natively, so this contributor re-registers the
 * NAMED_ENUM type code to behave as VARCHAR (storing the enum name as a string).
 *
 * Loaded via SPI: META-INF/services/org.hibernate.boot.model.TypeContributor
 */
public class VarcharEnumTypeContributor implements TypeContributor {

    @Override
    public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        typeContributions.contributeJdbcType(new VarcharJdbcType() {
            @Override
            public int getDefaultSqlTypeCode() {
                return SqlTypes.NAMED_ENUM;
            }

            @Override
            public int getJdbcTypeCode() {
                return SqlTypes.NAMED_ENUM;
            }
        });
    }
}
