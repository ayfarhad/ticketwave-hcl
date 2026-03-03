# Infrastructure & Deployment Architecture - TicketWave Backend

**Document Version**: 1.0  
**Last Updated**: 2026-03-03  
**Status**: Active  
**Infrastructure Owner**: DevOps Team

---

## Table of Contents

1. [Infrastructure Overview](#1-infrastructure-overview)
2. [Kubernetes Deployment](#2-kubernetes-deployment)
3. [Database Architecture](#3-database-architecture)
4. [Caching & Session Management](#4-caching--session-management)
5. [Networking & Security](#5-networking--security)
6. [Monitoring & Observability](#6-monitoring--observability)
7. [CI/CD Pipeline](#7-cicd-pipeline)
8. [Disaster Recovery](#8-disaster-recovery)
9. [Performance Tuning](#9-performance-tuning)
10. [Cost Optimization](#10-cost-optimization)

---

## 1. Infrastructure Overview

### 1.1 Cloud Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│              Internet / CDN Layer                       │
│  (CloudFlare CDN, AWS CloudFront)                       │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTPS (TLS 1.3)
                       ▼
┌──────────────────────────────────────────────────────────┐
│           Application Load Balancer (ALB)               │
│  ┌──────────────────────────────────────────────────┐   │
│  │ - SSL/TLS Termination                           │   │
│  │ - Request routing (path/host-based)             │   │
│  │ - Health checks                                 │   │
│  │ - Auto-scaling based on metrics                │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────┬────────────────────────────────────────────┘
              │
   ┌──────────┼──────────┐
   │          │          │
   ▼          ▼          ▼
┌─────────────────────────────────────────────────────────┐
│        Kubernetes Cluster (EKS / AKS / GKE)            │
├─────────────────────────────────────────────────────────┤
│ ┌────────────────────────────────────────────────────┐  │
│ │          Control Plane (Managed)                   │  │
│ │  - API Server, Scheduler, Controller Manager       │  │
│ └────────────────────────────────────────────────────┘  │
│                                                         │
│ ┌──────────┬──────────┬──────────┬──────────┐          │
│ │  Worker  │  Worker  │  Worker  │  Worker  │          │
│ │  Node 1  │  Node 2  │  Node 3  │  Node N  │          │
│ │  (3 CPU) │  (3 CPU) │  (3 CPU) │  (3 CPU) │          │
│ │ (8GB RAM)│ (8GB RAM)│ (8GB RAM)│ (8GB RAM)│          │
│ │          │          │          │          │          │
│ │ ┌──────┐ │ ┌──────┐ │ ┌──────┐ │ ┌──────┐ │          │
│ │ │ Pod 1│ │ │ Pod 3│ │ │ Pod 5│ │ │ Pod 7│ │          │
│ │ │Java  │ │ │Java  │ │ │Java  │ │ │Java  │ │          │
│ │ │app   │ │ │app   │ │ │app   │ │ │app   │ │          │
│ │ └──────┘ │ └──────┘ │ └──────┘ │ └──────┘ │          │
│ │          │          │          │          │          │
│ │ ┌──────┐ │ ┌──────┐ │ ┌──────┐ │ ┌──────┐ │          │
│ │ │ Pod 2│ │ │ Pod 4│ │ │ Pod 6│ │ │ Pod 8│ │          │
│ │ │Java  │ │ │Java  │ │ │Java  │ │ │Java  │ │          │
│ │ │app   │ │ │app   │ │ │app   │ │ │app   │ │          │
│ │ └──────┘ │ └──────┘ │ └──────┘ │ └──────┘ │          │
│ │          │          │          │          │          │
│ └──────────┴──────────┴──────────┴──────────┘          │
│                                                         │
│  Kubernetes Services, ConfigMaps, Secrets, PVCs       │
└─────────────────────────────────────────────────────────┘
              │
   ┌──────────┼──────────┬────────────────┐
   │          │          │                │
   ▼          ▼          ▼                ▼
┌─────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐
│Database │ │Redis     │ │Message   │ │External    │
│Primary  │ │Cluster   │ │Queue     │ │Services    │
│         │ │(Cache)   │ │(Rabbit)  │ │(Payment)   │
│PostgreSQL│ │3-node    │ │Sentinel  │ │(Email)     │
│(RDS)    │ │Sentinel  │ │enabled   │ │(SMS)       │
│Multi-AZ │ │enabled   │ │          │ │            │
└─────────┘ └──────────┘ └──────────┘ └────────────┘
   │          │          │
   └──────────┴──────────┘
        │
        ▼
┌──────────────────────────┐
│ Backup Storage (S3/GCS)  │
│ - Daily snapshots        │
│ - Point-in-time recovery │
└──────────────────────────┘
```

### 1.2 Component Specifications

| Component | Type | Configuration | Rationale |
|-----------|------|---------------|-----------|
| **Load Balancer** | ALB | 2 AZs, Auto-scaling | HA, auto-fail-over, SSL termination |
| **Kubernetes** | EKS/AKS/GKE | 4 worker nodes (t3.medium) | Managed K8s, reduced ops burden |
| **Pods** | Java/Spring | 8 replicas (HPA 2-10) | Horizontal scaling, fault tolerance |
| **Database** | PostgreSQL | RDS Multi-AZ, 50GB, 1000 IOPS | Managed, automatic failover, backups |
| **Cache** | Redis | ElastiCache/Azure Cache, 3-node | HA, auto-replication, eviction policy |
| **Message Queue** | RabbitMQ | 3-node cluster, Sentinel | For async processing, durability |
| **Storage** | S3/GCS | Standard tier, 30-day retention | Backup, audit logs, media |

---

## 2. Kubernetes Deployment

### 2.1 Deployment Manifest

**File**: `k8s/deployment.yaml`
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ticketwave-backend
  namespace: production
spec:
  replicas: 3                          # Min replicas
  selector:
    matchLabels:
      app: ticketwave-backend
  template:
    metadata:
      labels:
        app: ticketwave-backend
        version: v1.0.0
    spec:
      containers:
      - name: app
        image: gcr.io/ticketwave/backend:1.0.0
        imagePullPolicy: IfNotPresent
        
        ports:
        - containerPort: 8080
          name: http
        
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: JAVA_OPTS
          value: "-Xmx512m -Xms512m -XX:+UseG1GC"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
        - name: REDIS_URL
          valueFrom:
            configMapKeyRef:
              name: redis-config
              key: url
        
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 2
        
        lifecycle:
          preStop:
            exec:
              command: ["/bin/sh", "-c", "sleep 15"]

      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - ticketwave-backend
              topologyKey: kubernetes.io/hostname
```

### 2.2 Service Manifest

**File**: `k8s/service.yaml`
```yaml
apiVersion: v1
kind: Service
metadata:
  name: ticketwave-backend
  namespace: production
spec:
  type: ClusterIP
  selector:
    app: ticketwave-backend
  ports:
  - port: 8080
    targetPort: 8080
    protocol: TCP
    name: http
  sessionAffinity: None
```

### 2.3 HorizontalPodAutoscaler

**File**: `k8s/hpa.yaml`
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ticketwave-backend-hpa
  namespace: production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ticketwave-backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 75
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30
```

### 2.4 ConfigMap & Secrets

**File**: `k8s/configmap.yaml`
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: redis-config
  namespace: production
data:
  url: "redis://redis-cluster:6379"
  cache-ttl: "3600"
  
---
apiVersion: v1
kind: Secret
metadata:
  name: db-credentials
  namespace: production
type: Opaque
data:
  url: base64_encoded_postgres_url
  username: base64_encoded_username
  password: base64_encoded_password
```

---

## 3. Database Architecture

### 3.1 PostgreSQL Setup (AWS RDS)

**Configuration**:
```
Engine Version: PostgreSQL 15.2
Instance Class: db.t3.medium (2 vCPU, 4GB RAM)
Storage: 50 GB, GP3 (1000 IOPS)
Multi-AZ: Enabled (Synchronous replication)
Backup Retention: 30 days
Backup Window: 03:00-04:00 UTC
Maintenance Window: Sunday 04:00-05:00 UTC
Enhanced Monitoring: Enabled (1-minute granularity)
Parameter Group: Custom (shared_buffers, effective_cache_size tuned)
```

### 3.2 Schema Initialization (Flyway)

**Location**: `src/main/resources/db/migration/`

**Files**:
- `V1__initial_schema.sql` - Tables, constraints, indexes
- `V2__add_audit_tables.sql` - Audit tables (future)
- `V3__add_indexes_performance.sql` - Performance indexes (future)

### 3.3 Performance Optimization

**Indexes**:
```sql
-- Query optimization indexes
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_pnr ON bookings(pnr);
CREATE INDEX idx_bookings_created_at ON bookings(created_at DESC);

CREATE INDEX idx_schedules_route_id ON schedules(route_id);
CREATE INDEX idx_seats_schedule_id ON seats(schedule_id);
CREATE INDEX idx_seats_status ON seats(status);
CREATE INDEX idx_seats_schedule_status ON seats(schedule_id, status);

CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_status ON payments(status);

CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- Full-text search indexes (future)
CREATE INDEX idx_search_routes_tsvector ON routes USING GIN(to_tsvector('english', origin || ' ' || destination));
```

**Autovacuum Settings**:
```sql
ALTER TABLE bookings SET (autovacuum_vacuum_scale_factor = 0.05);
ALTER TABLE seats SET (autovacuum_vacuum_scale_factor = 0.05);
ALTER TABLE payments SET (autovacuum_vacuum_scale_factor = 0.05);
```

### 3.4 Connection Pooling (HikariCP)

**Configuration**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20           # Max connections
      minimum-idle: 5                 # Min idle connections
      connection-timeout: 30000       # 30 seconds
      idle-timeout: 600000            # 10 minutes
      max-lifetime: 1800000           # 30 minutes
      auto-commit: true
      connection-test-query: "SELECT 1"
```

### 3.5 Replication Architecture

```
┌─────────────────────────────────────────┐
│       Primary (Write) PostgreSQL        │
│  Region: us-east-1a                    │
│  Accepts: Write + Read                 │
│  Replication: Synchronous              │
└────────────────┬────────────────────────┘
                 │
                 │ Stream Replication
                 │ wal_level = logical
                 │
    ┌────────────┼────────────┐
    │            │            │
    ▼            ▼            ▼
┌────────┐  ┌────────┐  ┌────────────┐
│Standby │  │Standby │  │Read Replica│
│ (RDS)  │  │ (RDS)  │  │  (Separate)│
│Zone 1b │  │Zone 1c │  │ For reports│
└────────┘  └────────┘  └────────────┘
    │            │           │
    └────────────┼───────────┘
                 │
    Failover: Auto promote standby to primary in <2 min
```

---

## 4. Caching & Session Management

### 4.1 Redis Cluster Setup

**AWS ElastiCache Configuration**:
```
Cluster Mode: Enabled (3 shards)
Node Type: cache.r6g.xlarge (4 vCPU, 26GB RAM)
Auto-failover: Enabled
Snapshot Retention: 5 days
Backup Window: 03:00-04:00 UTC
Parameter Group:
  - maxmemory-policy: allkeys-lru
  - timeout: 0
  - tcp-keepalive: 300
```

### 4.2 Cache Layers

**L1 Cache (Application Memory)**:
- Caffeine (Spring Cache)
- Short-lived: 5-10 minutes
- Use case: Frequently accessed user profiles

**L2 Cache (Redis)**:
- Distributed cache
- Medium-lived: 30 minutes - 1 hour
- Use case: Schedule search results, user roles

**L3 Cache (CDN)**:
- CloudFlare / CloudFront
- Long-lived: 24 hours
- Use case: Static content, API responses (GET /routes)

**Cache Invalidation Timeline**:
```
Event                  Time        Impact
──────────────────────────────────────────
User profile update    Immediate   L1 + L2 evicted
Booking confirmed      Immediate   Seat status updated
Schedule published     1 minute    L2 invalidated
Search result aging    1 hour      L2 TTL expires
Periodic cleanup       2 hours     Stale entries removed
```

### 4.3 Session Management

**Session Storage**:
```yaml
spring:
  session:
    store-type: redis
    redis:
      namespace: "ticketwave:session"
    timeout: 30m                      # 30 minutes session timeout
```

**Implementation**:
```java
@Configuration
@EnableSpringHttpSession
public class SessionConfig {
    @Bean
    public LettuceConnectionFactory connectionFactory() {
        return new LettuceConnectionFactory();
    }
}
```

---

## 5. Networking & Security

### 5.1 Network Topology

```
┌─────────────────────────────────────────┐
│         VPC (10.0.0.0/16)              │
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────────────────────────┐  │
│  │  Public Subnet (10.0.1.0/24)   │  │
│  │  - NAT Gateway                 │  │
│  │  - ALB (Internet-facing)       │  │
│  └─────────────────────────────────┘  │
│                                         │
│  ┌─────────────────────────────────┐  │
│  │ Private Subnet A (10.0.2.0/24)  │  │
│  │  - Worker Nodes                 │  │
│  │  - Application Pods             │  │
│  └─────────────────────────────────┘  │
│                                         │
│  ┌─────────────────────────────────┐  │
│  │ Private Subnet B (10.0.3.0/24)  │  │
│  │  - RDS (Multi-AZ)               │  │
│  │  - ElastiCache                  │  │
│  │  - RabbitMQ                     │  │
│  └─────────────────────────────────┘  │
│                                         │
└─────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  Internet Gateway                       │
│  (Routes public internet traffic)       │
└─────────────────────────────────────────┘
```

### 5.2 Security Groups

**ALB Security Group**:
```
Inbound:
  - 80 (HTTP) from 0.0.0.0/0 → Redirect to 443
  - 443 (HTTPS) from 0.0.0.0/0

Outbound:
  - All traffic to 0.0.0.0/0
```

**Worker Node Security Group**:
```
Inbound:
  - 8080 (App) from ALB security group
  - 22 (SSH) from Bastion security group (optional)
  - 6379 (Redis) from same group

Outbound:
  - All traffic to 0.0.0.0/0
```

**RDS Security Group**:
```
Inbound:
  - 5432 (PostgreSQL) from Worker Node SG

Outbound:
  - None (closed by default)
```

**ElastiCache Security Group**:
```
Inbound:
  - 6379 (Redis) from Worker Node SG

Outbound:
  - None (closed by default)
```

### 5.3 TLS/SSL Configuration

**Certificate Management**:
- Provider: AWS Certificate Manager (ACM)
- Domain: `api.ticketwave.com`
- Wildcard: `*.ticketwave.com`
- Auto-renewal: Enabled
- Validation: DNS validation

**TLS Policy**:
```
ALB Listener:
  - Protocol: HTTPS
  - Port: 443
  - Certificate: ACM
  - Security Policy: ELBSecurityPolicy-TLS13-1-2-2021-06
  - Minimum TLS: 1.2
```

---

## 6. Monitoring & Observability

### 6.1 Monitoring Stack

```
┌──────────────────────────────────────────┐
│     Application (Spring Boot)            │
│     - Micrometer metrics export          │
│     - Structured logging (SLF4J)         │
│     - OpenTelemetry instrumentation      │
└──────────────┬───────────────────────────┘
               │
    ┌──────────┼─────────────┐
    │          │             │
    ▼          ▼             ▼
┌────────┐ ┌──────────┐ ┌────────────┐
│Prometheus│ │CloudWatch│ │ Jaeger     │
│(Metrics)│ │(AWS Native)│ │(Tracing)   │
└─────┬──┘ └────┬─────┘ └───────┬────┘
      │         │               │
      └────┬────┴───────┬───────┘
           │            │
           ▼            ▼
       ┌─────────────────────────┐
       │ Alerting & Dashboards   │
       ├─────────────────────────┤
       │ - Grafana (Prometheus)  │
       │ - CloudWatch Dashboard  │
       │ - Alertmanager          │
       │ - PagerDuty            │
       └─────────────────────────┘
```

### 6.2 Key Metrics

**Application Metrics**:
```
ticketwave_http_request_count
  {method=POST, endpoint=/booking/initiate, status=201}

ticketwave_http_request_duration_seconds
  {endpoint=/search, quantile=0.95}

ticketwave_database_queries_total
  {operation=SELECT, table=bookings}

ticketwave_cache_hits_total
  {cache=schedule_search}

ticketwave_cache_misses_total
  {cache=schedule_search}

ticketwave_thread_pool_active
  {pool=dataSource}

ticketwave_active_connections
  {datasource=default}

ticketwave_jvm_memory_usage_bytes
  {area=heap, type=used}
```

### 6.3 Alerting Rules

**Alert Conditions**:
```yaml
Alert: HighErrorRate
Condition: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
Severity: Critical
Action: PagerDuty interrupt, Slack critical-alerts

Alert: DatabaseConnectionPoolExhausted
Condition: hikaricp_connections_active / hikaricp_connections_max > 0.9
Severity: Warning
Action: Slack warning, Auto-scaling trigger

Alert: HighMemoryUsage
Condition: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes > 0.85
Severity: Warning
Action: Slack, Consider pod restart

Alert: RedisCacheEviction
Condition: redis_evicted_keys_total > 100
Severity: Info
Action: Slack notification, Consider cache increase
```

### 6.4 Distributed Tracing

**Configuration**:
```yaml
management:
  tracing:
    sampling:
      probability: 0.1              # 10% sampling
  otlp:
    tracing:
      endpoint: "http://jaeger-collector:4318/v1/traces"
```

**Trace Details**:
Each request trace includes:
- Span: CorrelationIdFilter → JwtFilter → BookingController → Service → Repository → Database
- Timing: Latency at each layer
- Logs: Correlated by correlation ID
- Tags: User ID, booking ID, error details

---

## 7. CI/CD Pipeline

### 7.1 Pipeline Stages

**File**: `.github/workflows/deploy.yaml` or `Jenkinsfile`

```yaml
stages:
  1. BUILD
     - Checkout code
     - Run unit tests
     - Run integration tests
     - Build Docker image
     - Push to registry
  
  2. SECURITY SCAN
     - SonarQube analysis
     - Dependency check (OWASP)
     - Trivy container scan
     - Secrets detection
  
  3. STAGING DEPLOYMENT
     - Deploy to staging cluster
     - Run smoke tests
     - Run performance tests
     - Run security tests
  
  4. APPROVAL
     - Manual approval required
  
  5. PRODUCTION DEPLOYMENT
     - Blue/green deployment
     - Gradual rollout (10% → 50% → 100%)
     - Health checks
     - Smoke tests
  
  6. POST-DEPLOYMENT
     - Run end-to-end tests
     - Database migration (if needed)
     - Clear caches
     - Notify team
```

### 7.2 Docker Build Process

**Dockerfile** (Multi-stage):
```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-slim
WORKDIR /app
COPY --from=builder /app/target/ticketwave-*.jar ticketwave.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "ticketwave.jar"]
```

### 7.3 Blue/Green Deployment

```
┌──────────────────────────────────────────────┐
│           Load Balancer                      │
│      (100% traffic to Blue)                  │
└─────────────────┬────────────────────────────┘
                  │
        ┌─────────┴──────────┐
        │                    │
        ▼                    ▼
┌──────────────┐      ┌──────────────┐
│   Blue Env   │      │  Green Env   │
│  (Current)   │      │    (New)     │
│   v1.0.0     │      │   v1.0.1     │
│              │      │   Ready      │
│ - 8 pods     │      │ - 8 pods     │
│ - v1.0.0     │      │ - Running    │
│              │      │   tests      │
└──────────────┘      └──────────────┘
   (Active)              (Testing)
       │
       │ [Smoke tests pass]
       │
       ▼
   Switch LB → Green
   ├─ 10% traffic (canary)
   ├─ Monitor metrics
   ├─ 50% traffic (progressive)
   ├─ Monitor metrics
   └─ 100% traffic → Green is Blue
   
   Blue decommissioned
```

---

## 8. Disaster Recovery

### 8.1 RPO & RTO Targets

| Scenario | RPO | RTO | Strategy |
|----------|-----|-----|----------|
| **Database Failure** | 5 min | 2 min | RDS Multi-AZ auto-failover |
| **Pod Crash** | 0 min | 30 sec | K8s auto-restart |
| **Node Failure** | 0 min | 1 min | K8s pod rescheduling |
| **AZ Failure** | 0 min | 5 min | Multi-AZ deployment |
| **Region Failure** | 1 hour | 30 min | Cross-region snapshot restore |
| **Data Corruption** | 24 hour | 2 hour | Point-in-time restore |

### 8.2 Backup Strategy

**Database Backups**:
```
Automatic RDS Snapshots:
  - Frequency: Daily at 03:00 UTC
  - Retention: 30 days
  - Copies: Auto-copied to secondary region

Continuous Binary Log Replication:
  - Capture every transaction
  - Point-in-time recovery: Last 35 days
  - Backup window: 03:00-04:00 UTC

Manual Snapshots:
  - Before major releases
  - Before schema migrations
  - Monthly archival (S3 Glacier)
```

**Restore Procedures**:
```
1. RDS Point-in-Time Restore
   - New DB instance from snapshot
   - Update application config
   - Run smoke tests
   - Monitor for 30 minutes
   - Cutover to new instance

2. Cross-Region Failover
   - Trigger standby DB promotion
   - Update Route53 DNS (TTL: 60s)
   - Redirect application traffic
   - Restore to new secondary in primary region

3. Application State Recovery
   - Restore sessions from Redis cluster
   - Restore cache from backup
   - Replay recent transactions (if logged)
```

### 8.3 Disaster Recovery Test Schedule

```
Weekly:
  - RDS automated backup verification
  - Redis cluster failover test

Monthly:
  - Full database restore-to-staging
  - Application recovery simulation
  - DNS failover test

Quarterly:
  - Cross-region failover drill
  - Full system restoration test
  - Document lessons learned
```

---

## 9. Performance Tuning

### 9.1 JVM Tuning

**Production JVM Options**:
```
-Xmx1g                          # Max heap: 1GB
-Xms1g                          # Initial heap: 1GB
-XX:+UseG1GC                    # Garbage collector
-XX:MaxGCPauseMillis=200        # Max pause time
-XX:+UnlockDiagnosticVMOptions
-XX:G1SummarizeRSetStatsPeriod=1
-XX:+PrintGCDateStamps
-XX:+PrintGCDetails
-Xloggc:/var/log/jvm/gc.log
```

**Monitoring GC**:
```
Metrics:
  - GC pause time (P99 < 500ms)
  - Heap usage (75% threshold for alerts)
  - Full GC frequency (< 1 per hour)
  - Long pause events (> 1 second)
```

### 9.2 Database Query Optimization

**Query Patterns**:
```sql
-- ✓ Good: Use indexes
SELECT * FROM bookings WHERE user_id = ? AND status = ?;

-- ✗ Bad: No index, full table scan
SELECT * FROM bookings WHERE LOWER(pnr) = ?;

-- ✓ Good: Pagination
SELECT * FROM bookings WHERE user_id = ? LIMIT 50 OFFSET 0;

-- ✗ Bad: Large result set
SELECT * FROM bookings WHERE user_id = ?;

-- ✓ Good: Use batch operations
INSERT INTO booking_items (booking_id, seat_id) VALUES (?, ?), (?, ?), ...;

-- ✗ Bad: N+1 queries
for (Booking b : bookings) {
    for (BookingItem item : bookingRepository.findItems(b.id)) { ... }
}
```

### 9.3 Response Time Optimization

**Target Response Times** (P99):
```
Search Routes:              100ms  (cached)
Create Booking:            500ms  (transactional)
Confirm Booking:           700ms  (payment)
Initiate Payment:          200ms  (async to gateway)
User Login:                150ms  (JWT generation)
List Bookings:             200ms  (paginated)
```

**Profiling Tools**:
- JFR (Java Flight Recorder)
- YourKit Java Profiler
- JProfiler
- Async Profiler

---

## 10. Cost Optimization

### 10.1 Infrastructure Cost Breakdown

**Monthly Cost Estimate** (AWS):
```
Component                          Cost        Savings Opportunity
──────────────────────────────────────────────────────────────────
EKS Cluster (4 nodes, t3.medium)  $400        Reserved instances (-30%)
RDS PostgreSQL (db.t3.medium)     $150        Reserved instances (-33%)
ElastiCache Redis (3 xlarge)      $500        Multi-AZ (-20%)
ALB + Data transfer               $200        Compress responses
CloudWatch Monitoring             $100        Error sampling
NAT Gateway                       $100        VPC Endpoint (-50%)
──────────────────────────────────────────────────────────────────
Total Monthly                     $1,450      Potential: $850-900
```

### 10.2 Cost Optimization Strategies

**Strategy 1**: Reserved Instances (1-3 year commitment)
- **Savings**: 30-50% vs on-demand
- **Apply to**: RDS, ElastiCache, EC2 nodes
- **Estimated Savings**: $600/month

**Strategy 2**: Auto-scaling
- **Mechanism**: Scale down to 2 pods during low traffic (nights/weekends)
- **Savings**: 20% reduction on compute
- **Estimated Savings**: $80/month

**Strategy 3**: Data Transfer Optimization
- **Mechanism**: CloudFront CDN for static API responses
- **Savings**: 50-75% on cross-AZ traffic
- **Estimated Savings**: $100/month

**Strategy 4**: Database Right-sizing
- **Current**: db.t3.medium (2 vCPU, 4GB RAM)
- **Monitor**: Utilization for 3 months
- **Option**: Downsize to db.t3.small (-60% cost) if < 30% utilization

**Strategy 5**: Spot Instances for Non-Critical Workload
- **Mechanism**: Use Spot instances for worker nodes (-70% cost)
- **Risk**: Interruption (mitigate with pod disruption budgets)
- **Estimated Savings**: $150/month

### 10.3 Cost Monitoring Dashboard

**CloudWatch / Cost Explorer Metrics**:
```
- Daily cost trend
- Cost by service (EKS, RDS, ALB, etc.)
- Cost by environment (dev, staging, prod)
- Cost per user (divide total cost by MAU)
- Cost anomaly detection (alert if > 20% increase)
```

---

## Appendix: Infrastructure Checklists

### Pre-Production Deployment Checklist

- [ ] Load balancer configured with SSL/TLS
- [ ] Security groups configured (network isolation)
- [ ] RDS Multi-AZ enabled
- [ ] Automated backups configured (30-day retention)
- [ ] Redis cluster with auto-failover
- [ ] Kubernetes cluster with HPA (2-10 replicas)
- [ ] Pod disruption budget configured
- [ ] Health checks (liveness + readiness probes)
- [ ] Resource requests & limits set
- [ ] Secrets configured (no hardcoded passwords)
- [ ] ConfigMaps for environment-specific config
- [ ] Monitoring & alerting configured
- [ ] Log aggregation setup (CloudWatch / ELK)
- [ ] Distributed tracing enabled
- [ ] Database indexes optimized
- [ ] Cache warming strategy documented
- [ ] Disaster recovery plan documented
- [ ] Runbooks for common incidents
- [ ] Load testing completed (> 1000 concurrent users)
- [ ] Security audit completed (OWASP Top 10)

---

**Document Version**: 1.0  
**Last Updated**: 2026-03-03  
**Status**: Active  
**Infrastructure Owner**: DevOps Team  
**Review Cycle**: Quarterly
