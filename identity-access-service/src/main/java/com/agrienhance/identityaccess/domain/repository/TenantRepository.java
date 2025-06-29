package com.agrienhance.identityaccess.domain.repository;

import com.agrienhance.identityaccess.domain.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {}