package com.enterprise.iam.identity.service;

import com.enterprise.iam.audit.AuditService;
import com.enterprise.iam.common.exception.BusinessException;
import com.enterprise.iam.common.exception.ConflictException;
import com.enterprise.iam.common.exception.NotFoundException;
import com.enterprise.iam.identity.domain.IamOrg;
import com.enterprise.iam.identity.domain.OrgStatus;
import com.enterprise.iam.identity.repo.IamOrgRepository;
import com.enterprise.iam.identity.web.dto.CreateOrgRequest;
import com.enterprise.iam.identity.web.dto.OrgResponse;
import com.enterprise.iam.identity.web.dto.OrgTreeNode;
import com.enterprise.iam.identity.web.dto.UpdateOrgRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OrgService {

    private final IamOrgRepository orgRepository;
    private final AuditService auditService;

    public OrgService(IamOrgRepository orgRepository, AuditService auditService) {
        this.orgRepository = orgRepository;
        this.auditService = auditService;
    }

    @Transactional
    public OrgResponse create(CreateOrgRequest request) {
        if (orgRepository.existsByCode(request.code().trim())) {
            throw new ConflictException("org_code_taken", "Organization code is already taken");
        }
        IamOrg org = new IamOrg();
        org.setId(UUID.randomUUID());
        org.setCode(request.code().trim());
        org.setName(request.name().trim());
        org.setStatus(OrgStatus.ACTIVE);
        org.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        org.setParentId(request.parentId());
        org.setPath(computePath(request.parentId(), org.getId()));
        org = orgRepository.save(org);
        auditService.record("org.create", "create", "org", org.getId(), "success",
                Map.of("code", org.getCode()));
        return OrgResponse.from(org);
    }

    @Transactional(readOnly = true)
    public OrgResponse get(UUID id) {
        return OrgResponse.from(requireOrg(id));
    }

    @Transactional(readOnly = true)
    public List<OrgResponse> listChildren(UUID parentId) {
        if (parentId != null) {
            requireOrg(parentId);
            return orgRepository.findByParentIdOrderBySortOrderAscNameAsc(parentId).stream()
                    .map(OrgResponse::from)
                    .toList();
        }
        return orgRepository.findByParentIdIsNullOrderBySortOrderAscNameAsc().stream()
                .map(OrgResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrgTreeNode> tree() {
        List<IamOrg> all = orgRepository.findAllByOrderByPathAsc();
        Map<UUID, OrgTreeNode> nodes = new LinkedHashMap<>();
        List<OrgTreeNode> roots = new ArrayList<>();
        for (IamOrg org : all) {
            nodes.put(org.getId(), OrgTreeNode.from(org));
        }
        for (IamOrg org : all) {
            OrgTreeNode node = nodes.get(org.getId());
            if (org.getParentId() == null || !nodes.containsKey(org.getParentId())) {
                roots.add(node);
            } else {
                nodes.get(org.getParentId()).children().add(node);
            }
        }
        sortTree(roots);
        return roots;
    }

    @Transactional
    public OrgResponse update(UUID id, UpdateOrgRequest request) {
        IamOrg org = requireOrg(id);
        if (request.name() != null) {
            if (!StringUtils.hasText(request.name())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_org_name", "Name cannot be blank");
            }
            org.setName(request.name().trim());
        }
        if (request.status() != null) {
            org.setStatus(request.status());
        }
        if (request.sortOrder() != null) {
            org.setSortOrder(request.sortOrder());
        }
        if (request.parentId() != null || request.clearParent()) {
            UUID newParent = request.clearParent() ? null : request.parentId();
            move(org, newParent);
        }
        org = orgRepository.save(org);
        auditService.record("org.update", "update", "org", org.getId(), "success",
                Map.of("status", org.getStatus().code()));
        return OrgResponse.from(org);
    }

    private void move(IamOrg org, UUID newParentId) {
        if (newParentId != null && newParentId.equals(org.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_parent", "An organization cannot be its own parent");
        }
        if (newParentId != null) {
            IamOrg parent = requireOrg(newParentId);
            if (parent.getPath().startsWith(org.getPath())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_parent", "Cannot move an organization under its descendant");
            }
        }
        String oldPath = org.getPath();
        String newPath = computePath(newParentId, org.getId());
        org.setParentId(newParentId);
        org.setPath(newPath);
        List<IamOrg> descendants = orgRepository.findByPathStartingWith(oldPath);
        for (IamOrg child : descendants) {
            if (child.getId().equals(org.getId())) {
                continue;
            }
            child.setPath(newPath + child.getPath().substring(oldPath.length()));
            orgRepository.save(child);
        }
    }

    private String computePath(UUID parentId, UUID id) {
        if (parentId == null) {
            return "/" + id + "/";
        }
        IamOrg parent = requireOrg(parentId);
        return parent.getPath() + id + "/";
    }

    private IamOrg requireOrg(UUID id) {
        return orgRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("org_not_found", "Organization not found"));
    }

    private void sortTree(List<OrgTreeNode> nodes) {
        nodes.sort(Comparator.comparingInt(OrgTreeNode::sortOrder).thenComparing(OrgTreeNode::name));
        nodes.forEach(node -> sortTree(node.children()));
    }
}
