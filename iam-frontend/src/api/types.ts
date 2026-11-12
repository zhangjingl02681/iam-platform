export interface UserProfile {
  id: string;
  username: string;
  employeeNo?: string | null;
  displayName: string;
  email?: string | null;
  mobile?: string | null;
  status: string;
  primaryOrgId?: string | null;
  lastLoginAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
  assignments?: Assignment[];
  attributes?: AttributeValue[];
  roles?: Role[];
}

export interface Assignment {
  id: string;
  orgId: string;
  positionId?: string | null;
  primary: boolean;
  validFrom?: string | null;
  validTo?: string | null;
}

export interface AttributeValue {
  attrCode: string;
  valueText?: string | null;
}

export interface Me {
  profile: UserProfile;
  mustChangePassword: boolean;
  admin: boolean;
  authorities: string[];
}

export interface Application {
  id: string;
  code: string;
  name: string;
  status: string;
  ownerOrgId?: string | null;
  description?: string | null;
  launchUrl?: string | null;
  icon?: string | null;
  accentColor?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface Session {
  id: string;
  userId: string;
  username?: string | null;
  ip?: string | null;
  userAgent?: string | null;
  amr?: string | null;
  createdAt: string;
  lastSeenAt: string;
  idleExpiresAt: string;
  absExpiresAt: string;
  status: string;
  revokedAt?: string | null;
  current: boolean;
}

export interface OrgNode {
  id: string;
  parentId?: string | null;
  code: string;
  name: string;
  path?: string;
  status: string;
  sortOrder: number;
  children?: OrgNode[];
  createdAt?: string;
  updatedAt?: string;
}

export interface AuditEvent {
  id: string;
  occurredAt: string;
  eventType: string;
  actorType: string;
  actorId?: string | null;
  objectType?: string | null;
  objectId?: string | null;
  action: string;
  result: string;
  ip?: string | null;
  userAgent?: string | null;
  requestId?: string | null;
  sessionId?: string | null;
  clientId?: string | null;
  detail?: Record<string, unknown> | null;
  eventHash?: string;
}

export interface ImportJob {
  id: string;
  type: string;
  fileRef?: string | null;
  status: string;
  total?: number | null;
  successCount?: number | null;
  failedCount?: number | null;
  errorReport?: string | null;
  createdBy?: string | null;
  createdAt: string;
  finishedAt?: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiError {
  status: number;
  title?: string;
  detail?: string;
  code?: string;
}

export interface Role {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Entitlement {
  id: string;
  applicationId: string;
  subjectType: "USER" | "ORG" | "ROLE" | string;
  subjectId: string;
  createdAt?: string;
}

export interface Policy {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  status: string;
  createdBy?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface PolicyVersion {
  id: string;
  policyId: string;
  version: number;
  language?: string | null;
  document: string;
  checksum?: string | null;
  status: string;
  publishedAt?: string | null;
  comment?: string | null;
  createdAt?: string;
}
