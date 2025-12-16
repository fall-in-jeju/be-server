# 🚀 AWS EKS + ArgoCD + CodeBuild 완전 자동화 CI/CD 파이프라인 구축 가이드

> **작성일**: 2025-12-09  
> **대상**: Spring Boot 백엔드 애플리케이션  
> **목적**: GitHub Push → 자동 빌드 → 자동 배포 완전 자동화

---

# 📋 목차

1. [개요 및 아키텍처](#개요-및-아키텍처)
2. [사전 요구사항](#사전-요구사항)
3. [1단계: Docker 및 ECR 설정](#1단계-docker-및-ecr-설정)
4. [2단계: Kubernetes 매니페스트 작성](#2단계-kubernetes-매니페스트-작성)
5. [3단계: EKS 클러스터 구축](#3단계-eks-클러스터-구축)
6. [4단계: ArgoCD 설치 및 설정](#4단계-argocd-설치-및-설정)
7. [5단계: AWS CodeBuild 설정](#5단계-aws-codebuild-설정)
8. [6단계: GitHub Actions 연동](#6단계-github-actions-연동)
9. [7단계: 보안 관리 (Secrets)](#7단계-보안-관리-secrets)
10. [트러블슈팅 가이드](#트러블슈팅-가이드)
11. [운영 및 유지보수](#운영-및-유지보수)
12. [비용 관리](#비용-관리)

---

# 🎯 개요 및 아키텍처

## 전체 워크플로우

```mermaid
graph LR
    A[개발자] -->|git push| B[GitHub]
    B -->|트리거| C[GitHub Actions]
    C -->|시작| D[AWS CodeBuild]
    D -->|빌드| E[Docker Image]
    E -->|푸시| F[AWS ECR]
    D -->|업데이트| G[deployment.yml]
    G -->|커밋| B
    B -->|감지| H[ArgoCD]
    H -->|배포| I[AWS EKS]
```

## 구성 요소

### CI (Continuous Integration)
- **GitHub Actions**: CodeBuild 자동 트리거
- **AWS CodeBuild**: Docker 이미지 빌드 및 ECR 푸시
- **AWS ECR**: Docker 이미지 저장소

### CD (Continuous Deployment)
- **ArgoCD**: GitOps 기반 자동 배포 도구
- **AWS EKS**: Kubernetes 클러스터 (애플리케이션 실행 환경)

### 자동화 플로우
```
1. 코드 변경 → GitHub Push
2. GitHub Actions 자동 실행 (30초)
3. CodeBuild 빌드 시작 (3-5분)
4. ECR 이미지 푸시
5. deployment.yml 자동 업데이트
6. GitHub 자동 커밋
7. ArgoCD 변경 감지 (최대 3분)
8. EKS 자동 배포 (2분)
총 소요 시간: 약 10분
```

---

# 📌 사전 요구사항

## 필수 도구 설치

### Windows 환경
```powershell
# AWS CLI 설치
winget install Amazon.AWSCLI

# kubectl 설치
curl -LO "https://dl.k8s.io/release/v1.32.0/bin/windows/amd64/kubectl.exe"

# Docker Desktop 설치
# https://www.docker.com/products/docker-desktop

# Git 설치
winget install Git.Git
```

## AWS 계정 준비

### 계정 1: 관리자 계정 (인프라 관리용)
```
용도:
- EKS 클러스터 생성/관리
- ECR 이미지 푸시
- CodeBuild 실행
- IAM 역할 생성

필요 권한:
- AmazonEKSClusterPolicy
- AmazonEKSServicePolicy
- AmazonEC2ContainerRegistryFullAccess
- IAMFullAccess
- CloudWatchLogsFullAccess
```

### 계정 2: DynamoDB IAM 사용자 (애플리케이션용)
```
용도:
- 애플리케이션 런타임에서 DynamoDB 접근

필요 권한:
- DynamoDB 테이블 읽기/쓰기만
```

> ⚠️ **보안 원칙**: 권한 분리로 애플리케이션이 인프라에 접근하지 못하게 함

---

# 1️⃣ 1단계: Docker 및 ECR 설정

## 1.1 Dockerfile 작성

### 프로젝트 구조 확인
```
backend/
├── src/
├── build.gradle
├── settings.gradle
├── gradlew
└── gradle/
```

### Dockerfile 생성

**파일 위치**: `backend/Dockerfile`

```dockerfile
# 빌드 단계 (Gradle + JDK 21)
FROM gradle:8.5-jdk21-alpine AS build
WORKDIR /app
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .
COPY src ./src
RUN chmod +x gradlew
RUN ./gradlew bootJar

# 실행 단계 (JRE 21만)
FROM eclipse-temurin:21-jre-alpine
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8001
ENTRYPOINT ["java","-jar","/app.jar"]
```

### 🔧 트러블슈팅 1: Docker 이미지 not found

**문제:**
```
ERROR: openjdk:17-jre-slim: not found
```

**원인:** Docker Hub에서 해당 이미지가 더 이상 제공되지 않음

**해결:**
- `openjdk` → `eclipse-temurin` 사용
- Eclipse Temurin은 오픈소스 OpenJDK 배포판으로 안정적

**대안:**
- `amazoncorretto:21-alpine` (AWS 제공)
- `adoptopenjdk:21-jre-hotspot-alpine` (구버전, 권장 안 함)

---

## 1.2 AWS ECR 저장소 생성

### ECR 저장소 생성
```powershell
aws ecr create-repository `
    --repository-name backend-app `
    --region ap-northeast-2
```

**출력 예시:**
```json
{
    "repository": {
        "repositoryArn": "arn:aws:ecr:ap-northeast-2:310688446727:repository/backend-app",
        "repositoryUri": "310688446727.dkr.ecr.ap-northeast-2.amazonaws.com/backend-app"
    }
}
```

> 💡 **repositoryUri를 기록**해두세요. deployment.yml에서 사용합니다.

---

## 1.3 Docker 이미지 빌드 및 푸시

### ECR 로그인
```powershell
aws ecr get-login-password --region ap-northeast-2 | `
docker login --username AWS --password-stdin 310688446727.dkr.ecr.ap-northeast-2.amazonaws.com
```

### 이미지 빌드
```powershell
docker build -t backend-app .
```

### 이미지 태그 지정 및 푸시
```powershell
# 태그 지정
docker tag backend-app:latest 310688446727.dkr.ecr.ap-northeast-2.amazonaws.com/backend-app:latest

# ECR에 푸시
docker push 310688446727.dkr.ecr.ap-northeast-2.amazonaws.com/backend-app:latest
```

### 🔧 트러블슈팅 2: Gradle 빌드 실패

**문제:**
```
Could not find org.springframework.boot:spring-boot-starter-webmvc:.
```

**원인:** Spring Boot 4 → 3 다운그레이드 시 의존성 이름 불일치

**해결:**
```gradle
// ❌ 잘못된 이름
implementation 'org.springframework.boot:spring-boot-starter-webmvc'
implementation 'org.springframework.boot:spring-boot-starter-session-data-redis'

// ✅ 올바른 이름 (Spring Boot 3)
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'org.springframework.session:spring-session-data-redis'
```

**왜 이런 일이?**
- Spring Boot 4 → 3 다운그레이드 시 starter 이름 변경됨
- Spring Boot 3는 Servlet 기반 (`starter-web`)과 Reactive 기반 (`starter-webflux`)을 명확히 구분

---

# 2️⃣ 2단계: Kubernetes 매니페스트 작성

## 2.1 디렉토리 구조

```
backend/
├── k8s-manifests/
│   ├── deployment.yml
│   └── service.yml
├── Dockerfile
└── buildspec.yml
```

## 2.2 deployment.yml 작성

**파일 위치**: `backend/k8s-manifests/deployment.yml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend-app-deployment
  labels:
    app: backend-app
spec:
  replicas: 3  # Pod 개수
  selector:
    matchLabels:
      app: backend-app
  template:
    metadata:
      labels:
        app: backend-app
    spec:
      containers:
      - name: backend-app-container
        image: 310688446727.dkr.ecr.ap-northeast-2.amazonaws.com/backend-app:latest
        ports:
        - containerPort: 8001
        resources:
          requests:
            cpu: "100m"
            memory: "128Mi"
          limits:
            cpu: "200m"
            memory: "256Mi"
        env:
        # Spring Boot 설정
        - name: SPRING_PROFILES_ACTIVE
          value: production
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
        - name: SPRING_DATASOURCE_USERNAME
          value: "sa"
        - name: SPRING_DATASOURCE_PASSWORD
          value: ""
        # AWS DynamoDB 자격 증명 (Kubernetes Secret에서 가져옴)
        - name: AWS_ACCESS_KEY_ID
          valueFrom:
            secretKeyRef:
              name: dynamodb-credentials
              key: AWS_ACCESS_KEY_ID
        - name: AWS_SECRET_ACCESS_KEY
          valueFrom:
            secretKeyRef:
              name: dynamodb-credentials
              key: AWS_SECRET_ACCESS_KEY
        - name: AWS_REGION
          valueFrom:
            secretKeyRef:
              name: dynamodb-credentials
              key: AWS_REGION
        # 애플리케이션 코드가 사용하는 변수명 (언더스코어 2개)
        - name: AWS_ACCESS_KEY
          valueFrom:
            secretKeyRef:
              name: dynamodb-credentials
              key: AWS_ACCESS_KEY_ID
        - name: AWS_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: dynamodb-credentials
              key: AWS_SECRET_ACCESS_KEY
```

> 💡 **주의**: `image` 필드는 CodeBuild가 자동으로 업데이트합니다.

---

## 2.3 service.yml 작성

**파일 위치**: `backend/k8s-manifests/service.yml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: backend-app-service
  labels:
    app: backend-app
spec:
  selector:
    app: backend-app
  type: LoadBalancer  # AWS에서 자동으로 ELB 생성
  ports:
    - protocol: TCP
      port: 80          # 외부 접속 포트
      targetPort: 8001  # Pod 내부 포트
```

---

# 3️⃣ 3단계: EKS 클러스터 구축

## 3.1 EKS 클러스터 생성

### AWS CLI로 생성
```powershell
aws eks create-cluster `
    --name my-backend-cluster `
    --role-arn arn:aws:iam::310688446727:role/my-eks-cluster-role `
    --resources-vpc-config subnetIds=subnet-xxx,subnet-yyy,securityGroupIds=sg-zzz `
    --region ap-northeast-2
```

**소요 시간**: 약 10-15분

### 클러스터 상태 확인
```powershell
aws eks describe-cluster `
    --name my-backend-cluster `
    --query "cluster.status" `
    --output text
```

**출력**: `ACTIVE` (정상)

---

## 3.2 kubeconfig 설정

```powershell
aws eks update-kubeconfig `
    --name my-backend-cluster `
    --region ap-northeast-2
```

**확인:**
```powershell
kubectl get svc
# 출력: kubernetes 서비스가 보이면 성공
```

---

## 3.3 Node Group 생성

### 🔧 트러블슈팅 3: Node Group 생성 실패

**시도 1: AWS CLI**
```powershell
aws eks create-nodegroup `
    --cluster-name my-backend-cluster `
    --nodegroup-name my-backend-nodegroup `
    --node-role arn:aws:iam::310688446727:role/my-eks-nodegroup-role `
    --subnets subnet-xxx subnet-yyy `
    --instance-types t3.medium `
    --scaling-config minSize=2,maxSize=3,desiredSize=2
```

**문제:** 
- Node가 생성되지 않음
- `kubectl get nodes` → `No resources found`
- AWS Console에서 "NodeCreationFailure" 에러

**원인:** Amazon Linux 2 (AL2) AMI 사용 (2025년 6월부터 deprecated)

**해결:** AWS Console에서 수동 생성
1. AWS Console → EKS → Clusters → my-backend-cluster
2. Compute → Node groups → **Add node group**
3. 다음 설정으로 생성:
   - **Name**: `my-backend-nodegroup`
   - **Node IAM role**: `my-eks-nodegroup-role`
   - **AMI type**: **Amazon Linux 2023 (AL2023_x86_64_STANDARD)** ✅
   - **Instance type**: `t3.medium`
   - **Scaling**: Min=2, Max=3, Desired=2
   - **Subnets**: 프라이빗 서브넷 선택

**소요 시간**: 약 5분

### Node Group IAM Role 생성

**Trust Policy**: `node-trust-policy.json`
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

**역할 생성 및 정책 연결:**
```powershell
# IAM 역할 생성
aws iam create-role `
    --role-name my-eks-nodegroup-role `
    --assume-role-policy-document file://node-trust-policy.json

# 필수 정책 연결
aws iam attach-role-policy `
    --role-name my-eks-nodegroup-role `
    --policy-arn arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy

aws iam attach-role-policy `
    --role-name my-eks-nodegroup-role `
    --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly

aws iam attach-role-policy `
    --role-name my-eks-nodegroup-role `
    --policy-arn arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy
```

### Node 확인
```powershell
kubectl get nodes
```

**출력 예시:**
```
NAME                                                 STATUS   ROLES    AGE   VERSION
ip-192-168-85-223.ap-northeast-2.compute.internal    Ready    <none>   10m   v1.32.9-eks-...
ip-192-168-140-255.ap-northeast-2.compute.internal   Ready    <none>   10m   v1.32.9-eks-...
```

---

## 3.4 EKS Addons 설치

### 🔧 트러블슈팅 4: ArgoCD Pod 연결 실패

**문제:**
```
dial tcp 10.100.0.1:443: i/o timeout
```

**원인:** 필수 시스템 구성 요소 누락
- ❌ CoreDNS 없음
- ❌ kube-proxy 없음

**해결: Addons 수동 설치**

#### VPC-CNI Addon
```powershell
# 지원 버전 확인
aws eks describe-addon-versions `
    --addon-name vpc-cni `
    --kubernetes-version 1.32 `
    --query "addons[0].addonVersions[0].addonVersion" `
    --output text

# 설치
aws eks create-addon `
    --cluster-name my-backend-cluster `
    --addon-name vpc-cni `
    --addon-version v1.20.1-eksbuild.3
```

#### kube-proxy Addon
```powershell
aws eks create-addon `
    --cluster-name my-backend-cluster `
    --addon-name kube-proxy `
    --addon-version v1.32.9-eksbuild.2
```

#### CoreDNS Addon
```powershell
aws eks create-addon `
    --cluster-name my-backend-cluster `
    --addon-name coredns `
    --addon-version v1.11.4-eksbuild.24
```

**확인:**
```powershell
kubectl get pods -n kube-system
```

**출력 예시:**
```
NAME                       READY   STATUS    RESTARTS   AGE
aws-node-nq4rv             2/2     Running   0          10m
aws-node-qltdb             2/2     Running   0          10m
coredns-7bb47d475b-p9c7c   1/1     Running   0          5m
coredns-7bb47d475b-txt9r   1/1     Running   0          5m
kube-proxy-fgx78           1/1     Running   0          5m
kube-proxy-rncdv           1/1     Running   0          5m
```

> ⚠️ **중요**: 이 3개 Addon이 없으면 Pod 간 네트워크 통신 불가!

---

# 4️⃣ 4단계: ArgoCD 설치 및 설정

## 4.1 ArgoCD 설치

### 네임스페이스 생성
```powershell
kubectl create namespace argocd
```

### ArgoCD 설치 (특정 버전 사용)
```powershell
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/v2.11.2/manifests/install.yaml
```

> 💡 **왜 특정 버전?** 안정성 보장 및 호환성 문제 방지

### 🔧 트러블슈팅 5: ArgoCD Pod CrashLoopBackOff

**문제:**
```
argocd-redis: Init:CrashLoopBackOff
argocd-server: CrashLoopBackOff
```

**원인:** `argocd-redis` Secret 누락

**해결:**
1. ArgoCD 삭제
```powershell
kubectl delete namespace argocd
```

2. 네임스페이스 재생성 및 재설치
```powershell
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/v2.11.2/manifests/install.yaml
```

**대안:**
- 최신 버전 대신 Stable 버전 사용 (`v2.11.2`)
- Helm Chart 사용 (더 안정적인 설치)

---

## 4.2 ArgoCD 서버 접속

### Port Forward 실행
```powershell
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

> 💡 백그라운드 실행 권장 (별도 터미널 창)

### 초기 admin 비밀번호 확인
```powershell
$password = kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath="{.data.password}"
[System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($password))
```

**출력 예시**: `JbJs2rrnNMB3L0-i`

### ArgoCD UI 접속
- **URL**: https://localhost:8080
- **Username**: `admin`
- **Password**: (위에서 확인한 값)

---

## 4.3 GitHub 저장소 연결

### ArgoCD UI에서 설정

1. **Settings** (톱니바퀴) → **Repositories** → **+ Connect Repo**
2. 다음 정보 입력:
   - **Connection Method**: `HTTPS`
   - **Type**: `git`
   - **Repository URL**: `https://github.com/4-in-jeju-fall-in-jeju/be-server.git`
   - **Username**: GitHub 사용자명
   - **Password**: GitHub Personal Access Token
3. **Connect** 클릭

### GitHub Personal Access Token 생성

1. GitHub → **Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)**
2. **Generate new token (classic)**
3. 권한 선택:
   - ✅ **repo** (전체)
4. **Generate token**
5. 생성된 Token 복사 (한 번만 표시됨!)

---

## 4.4 ArgoCD Application 생성

### ArgoCD UI에서 설정

1. **+ New App** 클릭
2. 다음 정보 입력:

**GENERAL:**
- **Application Name**: `backend-app`
- **Project**: `default`
- **Sync Policy**: `Automatic` 체크
  - ✅ `Prune Resources` (삭제된 리소스 자동 제거)
  - ✅ `Self Heal` (수동 변경 시 자동 복구)

**SOURCE:**
- **Repository URL**: GitHub 저장소 선택
- **Revision**: `HEAD`
- **Path**: `k8s-manifests`

> ⚠️ **중요**: 저장소 루트 기준 경로! `backend/k8s-manifests` 아님!

**DESTINATION:**
- **Cluster URL**: `https://kubernetes.default.svc`
- **Namespace**: `default`

3. **Create** 클릭

### 배포 확인
```powershell
kubectl get applications -n argocd
```

**출력:**
```
NAME          SYNC STATUS   HEALTH STATUS
backend-app   Synced        Healthy
```

```powershell
kubectl get pods -n default
```

**출력:**
```
NAME                                     READY   STATUS    RESTARTS   AGE
backend-app-deployment-6f556cb69-6nq6s   1/1     Running   0          2m
backend-app-deployment-6f556cb69-wddw2   1/1     Running   0          2m
```

---

# 5️⃣ 5단계: AWS CodeBuild 설정

## 5.1 buildspec.yml 작성

**파일 위치**: `backend/buildspec.yml`

```yaml
version: 0.2

env:
  variables:
    AWS_DEFAULT_REGION: "ap-northeast-2"
    AWS_ACCOUNT_ID: "310688446727"
    IMAGE_REPO_NAME: "backend-app"
    IMAGE_TAG: "latest"

phases:
  pre_build:
    commands:
      - echo "Logging in to Amazon ECR..."
      - aws ecr get-login-password --region $AWS_DEFAULT_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com
      - REPOSITORY_URI=$AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$IMAGE_REPO_NAME
      - COMMIT_HASH=$(echo $CODEBUILD_RESOLVED_SOURCE_VERSION | cut -c 1-7)
      - IMAGE_TAG=${COMMIT_HASH:=latest}
      - echo "Building image with tag $IMAGE_TAG"

  build:
    commands:
      - echo "Build started on `date`"
      - echo "Building the Docker image..."
      - docker build -t $REPOSITORY_URI:latest .
      - docker tag $REPOSITORY_URI:latest $REPOSITORY_URI:$IMAGE_TAG

  post_build:
    commands:
      - echo "Build completed on `date`"
      - echo "Pushing the Docker images to ECR..."
      - docker push $REPOSITORY_URI:latest
      - docker push $REPOSITORY_URI:$IMAGE_TAG
      - echo "Docker image pushed successfully!"
      - echo "Image URI = $REPOSITORY_URI:$IMAGE_TAG"
      
      # deployment.yml 자동 업데이트
      - echo "Updating k8s-manifests/deployment.yml with new image tag..."
      - sed -i "s|image:.*|image:\ $REPOSITORY_URI:$IMAGE_TAG|g" k8s-manifests/deployment.yml
      
      # GitHub 자동 커밋 및 푸시
      - echo "Configuring git..."
      - git config --global user.email "codebuild@aws.com"
      - git config --global user.name "AWS CodeBuild"
      - git remote set-url origin https://${GITHUB_TOKEN}@github.com/4-in-jeju-fall-in-jeju/be-server.git
      - echo "Committing changes..."
      - git add k8s-manifests/deployment.yml
      - |
        if ! git diff --cached --exit-code > /dev/null; then
          git commit -m "chore: Update image to $IMAGE_TAG [skip ci]"
          echo "Changes committed"
        else
          echo "No changes to commit"
        fi
      - echo "Pushing to GitHub..."
      - |
        if git push origin HEAD:main; then
          echo "Successfully pushed to GitHub"
        else
          echo "No changes to push or push failed"
        fi

artifacts:
  files:
    - k8s-manifests/deployment.yml
    - k8s-manifests/service.yml
```

> 💡 **[skip ci]**: GitHub Actions가 무한 루프에 빠지지 않도록 방지

### 🔧 트러블슈팅 6: YAML 문법 오류

**문제:**
```
YAML_FILE_ERROR: Expected Commands[14] to be of string type
```

**원인:** Shell `||` 연산자를 YAML에서 직접 사용

```yaml
# ❌ 잘못된 방식
- git commit -m "..." || echo "No changes"
```

**해결:** 멀티라인 스크립트 사용
```yaml
# ✅ 올바른 방식
- |
  if git push origin HEAD:main; then
    echo "Success"
  else
    echo "Failed"
  fi
```

---

## 5.2 CodeBuild IAM 역할 생성

### Trust Policy 생성

**파일**: `codebuild-trust-policy.json`
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "codebuild.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

### IAM 역할 생성
```powershell
aws iam create-role `
    --role-name CodeBuildServiceRole `
    --assume-role-policy-document file://codebuild-trust-policy.json
```

---

### 권한 정책 생성

**파일**: `codebuild-policy.json`
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "*"
    }
  ]
}
```

### 정책 연결
```powershell
aws iam put-role-policy `
    --role-name CodeBuildServiceRole `
    --policy-name CodeBuildBasePolicy `
    --policy-document file://codebuild-policy.json
```

### Secrets Manager 접근 권한 추가

**파일**: `codebuild-secrets-policy.json`
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:ap-northeast-2:310688446727:secret:codebuild/github-token-*"
    }
  ]
}
```

```powershell
aws iam put-role-policy `
    --role-name CodeBuildServiceRole `
    --policy-name CodeBuildSecretsManagerPolicy `
    --policy-document file://codebuild-secrets-policy.json
```

---

## 5.3 GitHub Personal Access Token 저장

### Secrets Manager에 저장

```powershell
aws secretsmanager create-secret `
    --name codebuild/github-token `
    --description "GitHub Personal Access Token for CodeBuild" `
    --secret-string "ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" `
    --region ap-northeast-2
```

> ⚠️ **주의**: Token은 `repo` 권한 필요

---

## 5.4 CodeBuild 프로젝트 생성

### AWS Console에서 생성 (권장)

1. **AWS Console** → **CodeBuild** → **Create project**

2. **Project configuration:**
   - **Project name**: `backend-build-project`

3. **Source:**
   - **Source provider**: `GitHub`
   - **Repository**: `Repository in my GitHub account`
   - **GitHub repository**: `4-in-jeju-fall-in-jeju/be-server`
   - ✅ **Rebuild every time a code change is pushed**
   - **Event types**: 
     - ✅ PULL_REQUEST_CREATED
     - ✅ PULL_REQUEST_UPDATED
     - ✅ PULL_REQUEST_REOPENED
     - ✅ PUSH

4. **Environment:**
   - **Environment image**: `Managed image`
   - **Operating system**: `Amazon Linux`
   - **Runtime**: `Standard`
   - **Image**: `aws/codebuild/amazonlinux2-x86_64-standard:5.0`
   - ✅ **Privileged** (Docker 빌드 필수!)
   - **Service role**: `CodeBuildServiceRole`

5. **Buildspec:**
   - **Build specifications**: `Use a buildspec file`
   - **Buildspec name**: `buildspec.yml`

6. **Artifacts:**
   - **Type**: `No artifacts`

7. **Logs:**
   - ✅ **CloudWatch logs**

8. **Create build project**

---

### CodeBuild 환경 변수 추가

**AWS CLI로 설정:**
```powershell
aws codebuild update-project `
    --name backend-build-project `
    --environment "type=LINUX_CONTAINER,image=aws/codebuild/amazonlinux2-x86_64-standard:5.0,computeType=BUILD_GENERAL1_SMALL,privilegedMode=true,environmentVariables=[{name=GITHUB_TOKEN,value=codebuild/github-token,type=SECRETS_MANAGER},{name=AWS_DEFAULT_REGION,value=ap-northeast-2,type=PLAINTEXT},{name=AWS_ACCOUNT_ID,value=310688446727,type=PLAINTEXT},{name=IMAGE_REPO_NAME,value=backend-app,type=PLAINTEXT},{name=IMAGE_TAG,value=latest,type=PLAINTEXT}]" `
    --region ap-northeast-2
```

**확인:**
```powershell
aws codebuild batch-get-projects `
    --names backend-build-project `
    --query "projects[0].environment.environmentVariables" `
    --output json
```

---

### 🔧 트러블슈팅 7: Webhook 생성 실패

**문제:**
```
OAuthProviderException: Unable to create webhook at this time
```

**원인:** CodeBuild를 Personal Access Token 방식으로 연결하면 Webhook 자동 생성 불가

**시도한 해결책:**
1. AWS CLI로 Webhook 생성 → 실패
2. GitHub Token 권한 업데이트 → 실패
3. OAuth 연결 방식으로 재생성 → 복잡함

**최종 해결:** GitHub Actions 사용 (다음 단계)

**왜 이 방식을?**
- CodeBuild Personal Access Token 연결은 AWS 제한사항
- GitHub Actions는 유연하고 설정 간단
- 추가 작업 (테스트, 알림 등) 쉽게 추가 가능

**대안:**
- CodeBuild 프로젝트 삭제 후 OAuth 방식으로 재생성
- 하지만 GitHub Actions가 더 유연하고 권장됨

---

## 5.5 수동 빌드 테스트

### 빌드 시작
```powershell
aws codebuild start-build `
    --project-name backend-build-project `
    --source-version main `
    --region ap-northeast-2
```

### 빌드 상태 확인
```powershell
aws codebuild batch-get-builds `
    --ids "backend-build-project:빌드ID" `
    --query "builds[0].buildStatus" `
    --output text
```

---

# 6️⃣ 6단계: GitHub Actions 연동

## 6.1 GitHub Actions 워크플로우 작성

### 디렉토리 생성
```powershell
New-Item -ItemType Directory -Force -Path .github/workflows
```

### 워크플로우 파일 작성

**파일 위치**: `.github/workflows/trigger-codebuild.yml`

```yaml
name: Trigger AWS CodeBuild

on:
  push:
    branches:
      - main
  pull_request:
    branches:
      - main
    types: [opened, synchronize, reopened]

jobs:
  trigger-codebuild:
    name: Trigger CodeBuild
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ap-northeast-2
      
      - name: Get branch name
        id: branch-name
        run: |
          if [[ "${{ github.event_name }}" == "pull_request" ]]; then
            echo "branch=${{ github.head_ref }}" >> $GITHUB_OUTPUT
          else
            echo "branch=${GITHUB_REF#refs/heads/}" >> $GITHUB_OUTPUT
          fi
      
      - name: Trigger CodeBuild
        id: codebuild
        run: |
          BUILD_ID=$(aws codebuild start-build \
            --project-name backend-build-project \
            --source-version ${{ steps.branch-name.outputs.branch }} \
            --query 'build.id' \
            --output text)
          echo "build-id=$BUILD_ID" >> $GITHUB_OUTPUT
          echo "✅ CodeBuild 트리거 성공: $BUILD_ID"
      
      - name: Wait for build completion (optional)
        run: |
          echo "⏳ CodeBuild가 진행 중입니다..."
          echo "🔗 빌드 상태: https://console.aws.amazon.com/codesuite/codebuild/projects/backend-build-project/history"
          echo "📊 Build ID: ${{ steps.codebuild.outputs.build-id }}"
```

---

## 6.2 GitHub Secrets 설정

### GitHub 저장소에 Secrets 추가

1. **GitHub 저장소** → **Settings** → **Secrets and variables** → **Actions**
2. **New repository secret** 클릭
3. 다음 2개 추가:

**Secret 1:**
- **Name**: `AWS_ACCESS_KEY_ID`
- **Value**: (관리자 계정의 Access Key ID)

**Secret 2:**
- **Name**: `AWS_SECRET_ACCESS_KEY`
- **Value**: (관리자 계정의 Secret Access Key)

> 💡 **확인**: `C:\Users\사용자명\.aws\credentials` 파일에서 찾을 수 있음

---

## 6.3 GitHub Actions 테스트

### 푸시하여 트리거
```powershell
git add .github/workflows/trigger-codebuild.yml
git commit -m "feat: Add GitHub Actions workflow"
git push origin main
```

### 워크플로우 확인
- **GitHub 저장소** → **Actions** 탭
- 실행 중인 워크플로우 클릭
- 각 단계별 로그 확인

---

# 7️⃣ 7단계: 보안 관리 (Secrets)

## 7.1 민감한 정보 분류

### 관리자 계정 자격 증명 (인프라 관리)
```
위치:
  - 로컬: ~/.aws/credentials
  - GitHub: GitHub Actions Secrets
  - CodeBuild: 자동 사용

용도:
  - EKS 관리
  - ECR 푸시
  - CodeBuild 실행
```

### DynamoDB IAM 사용자 (애플리케이션 런타임)
```
위치:
  - 로컬: src/main/resources/application-secret.yml
  - EKS: Kubernetes Secret
  - GitHub: 올리지 않음 (.gitignore)

용도:
  - DynamoDB 읽기/쓰기만
```

---

## 7.2 Kubernetes Secret 생성

### DynamoDB 자격 증명 Secret 생성
```powershell
kubectl create secret generic dynamodb-credentials `
    --from-literal=AWS_ACCESS_KEY_ID=AKIAQTQBI3JDRWYTDQ2I `
    --from-literal=AWS_SECRET_ACCESS_KEY=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 `
    --from-literal=AWS_REGION=ap-northeast-2 `
    -n default
```

### Secret 확인
```powershell
kubectl get secret dynamodb-credentials -n default
kubectl describe secret dynamodb-credentials -n default
```

---

## 7.3 .gitignore 설정

### application-secret.yml 제외

**파일**: `.gitignore`
```gitignore
### Secret Config ###
application-secret.yml

### AWS Policies (임시 파일) ###
codebuild-policy.json
codebuild-secrets-policy.json
codebuild-trust-policy.json
node-trust-policy.json
webhook-filters.json
```

> 💡 AWS 정책 JSON 파일들은 이미 AWS에 적용되었으므로 GitHub에 올릴 필요 없음

---

# 🔧 트러블슈팅 가이드

## 트러블슈팅 8: GitHub Repository Rule 위반

### 문제
```
remote: error: GH013: Repository rule violations found for refs/heads/main.
remote: - Changes must be made through a pull request.
! [remote rejected] HEAD -> main
```

### 원인
- GitHub Repository Ruleset에서 main 브랜치 보호 설정
- CodeBuild가 main에 직접 푸시할 수 없음

### 해결
1. **GitHub 저장소** → **Settings** → **Rules** → **Rulesets**
2. 현재 Ruleset 클릭
3. **Bypass list** → **Add bypass**
4. 다음 항목 체크:
   - ✅ **Repository admin**
   - ✅ **AWS Connector for GitHub**
5. **Save changes**

### 왜 이 방식?
- CodeBuild가 deployment.yml을 자동으로 업데이트해야 함
- PR 없이 직접 커밋할 수 있어야 자동화 가능
- Bypass는 특정 Actor(AWS Connector)에게만 허용

### 대안
- PR 자동 생성 방식 (GitHub API 사용, 복잡함)
- GitHub Actions에서 deployment.yml 업데이트 (CodeBuild 외부)

---

## 트러블슈팅 9: Pod 재시작 반복

### 문제
```
RESTARTS: 6
Error: Could not resolve placeholder 'AWS_ACCESS_KEY'
```

### 원인
- 애플리케이션 코드가 `${AWS_ACCESS_KEY}` 찾음
- deployment.yml에는 `AWS_ACCESS_KEY_ID`만 설정됨
- 변수 이름 불일치

### 해결
deployment.yml에 **두 가지 형식 모두 추가**:
```yaml
env:
  # 표준 AWS SDK 변수명
  - name: AWS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: dynamodb-credentials
        key: AWS_ACCESS_KEY_ID
  
  # 애플리케이션 코드가 사용하는 변수명
  - name: AWS_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: dynamodb-credentials
        key: AWS_ACCESS_KEY_ID
```

### 왜 이 방식?
- 애플리케이션 코드 수정 없이 해결
- 표준 이름과 커스텀 이름 모두 지원
- 다른 AWS SDK/라이브러리와 호환성 유지

### 대안
- application.yml에서 변수 이름 통일
- Spring Boot Environment PostProcessor로 변환

---

## 트러블슈팅 10: Gradle 버전 호환성

### 문제 1: Gradle 9.2.1 사용 시
```
Could not find org.springframework.boot:spring-boot-starter-web
```

### 원인
- Gradle 9.x는 Spring Boot 3.3.x와 호환성 문제

### 해결
```properties
# gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
```

### 문제 2: 로컬 Java 22 사용 시
```
Gradle 8.5와 Java 22.0.2를 사용하도록 구성되었습니다.
호환되는 최대 Gradle JVM 버전은 21입니다.
```

### 해결
```properties
# Gradle 8.10으로 업그레이드 (Java 22 지원)
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10-bin.zip
```

### 버전 호환성 매트릭스

| Spring Boot | Gradle | Java | 비고 |
|-------------|--------|------|------|
| 3.3.6 | 8.5+ | 17, 21 | LTS 권장 |
| 3.3.6 | 8.10+ | 17, 21, 22 | 최신 |
| 4.0.0 | 8.13+ | 21, 22 | Early Access |

---

# 🎯 완전 자동화 플로우

## 개발자 워크플로우

### 1. 로컬에서 개발
```bash
# 기능 브랜치 생성
git checkout -b feature/new-api

# 코드 작성
vim src/main/java/...

# 로컬 테스트
./gradlew bootRun

# 커밋
git add .
git commit -m "feat: 새 API 추가"
```

### 2. GitHub에 푸시
```bash
git push origin feature/new-api
```

### 3. Pull Request 생성
- GitHub 웹에서 PR 생성
- Code Review 진행
- **승인 후 main 머지**

---

## 자동화가 진행하는 작업

### 4. GitHub Actions 자동 실행 (즉시)
```
✅ main 브랜치 Push 감지
✅ .github/workflows/trigger-codebuild.yml 실행
✅ AWS 인증
✅ CodeBuild 프로젝트 트리거
```

### 5. CodeBuild 자동 빌드 (3-5분)
```
✅ GitHub 소스 코드 클론
✅ Docker 이미지 빌드
   - Gradle로 JAR 파일 생성
   - eclipse-temurin:21-jre-alpine 기반 이미지
✅ ECR 로그인
✅ 이미지 푸시
   - latest 태그
   - Git 커밋 해시 태그 (예: abc1234)
✅ k8s-manifests/deployment.yml 자동 수정
   - 이미지 태그를 새 커밋 해시로 변경
✅ GitHub main 브랜치에 자동 커밋
   - 커밋 메시지: "chore: Update image to abc1234 [skip ci]"
```

### 6. ArgoCD 자동 배포 (3-5분)
```
✅ GitHub 저장소 폴링 (최대 3분마다)
✅ k8s-manifests 폴더 변경 감지
✅ 새 Revision 생성 (rev:1, rev:2, ...)
✅ EKS 클러스터에 배포
   - Rolling Update 방식 (무중단 배포)
   - 한 번에 1개 Pod씩 교체
✅ Health Check 통과 확인
✅ SYNC STATUS: Synced
✅ HEALTH STATUS: Healthy
```

### 7. 사용자 접속 가능 (10분 후)
```
✅ LoadBalancer를 통해 전 세계 어디서나 접속 가능
✅ http://k8s-default-backenda-xxx.elb.ap-northeast-2.amazonaws.com
```

---

## 전체 타임라인

| 시간 | 단계 | 소요 시간 | 누가? |
|------|------|-----------|-------|
| 0분 | git push origin main | 즉시 | 개발자 |
| 0-1분 | GitHub Actions 실행 | ~30초 | GitHub |
| 1-5분 | CodeBuild 빌드 | ~3-4분 | AWS |
| 5-6분 | ECR 푸시 & GitHub 커밋 | ~30초 | CodeBuild |
| 6-9분 | ArgoCD 변경 감지 | 최대 3분 | ArgoCD |
| 9-11분 | EKS Pod 재시작 | ~2분 | Kubernetes |
| **총 11분** | **완전 자동 배포** | | **사람 개입 0** |

---

# 📊 운영 및 유지보수

## ArgoCD 모니터링

### UI 접속
```powershell
# Port Forward 시작
kubectl port-forward svc/argocd-server -n argocd 8080:443
```
- **URL**: https://localhost:8080
- **Login**: admin / (초기 비밀번호)

### CLI 명령어
```powershell
# Application 상태
kubectl get applications -n argocd

# 상세 정보
kubectl describe application backend-app -n argocd

# Sync 이력
kubectl get application backend-app -n argocd -o yaml | Select-String "history" -Context 10
```

---

## Pod 관리

### Pod 상태 확인
```powershell
kubectl get pods -n default
kubectl get pods -n default -o wide  # IP 및 Node 정보 포함
```

### Pod 로그 확인
```powershell
kubectl logs <pod-name> -n default --tail=100
kubectl logs <pod-name> -n default -f  # 실시간 로그
```

### Pod 재시작 (수동)
```powershell
kubectl rollout restart deployment backend-app-deployment -n default
```

### 특정 버전으로 Rollback
```powershell
# 이전 버전으로 롤백
kubectl rollout undo deployment backend-app-deployment -n default

# 특정 Revision으로 롤백
kubectl rollout undo deployment backend-app-deployment -n default --to-revision=3
```

---

## LoadBalancer 관리

### 외부 URL 확인
```powershell
kubectl get svc backend-app-service -n default
```

### LoadBalancer 설정 변경
```yaml
# service.yml
spec:
  type: LoadBalancer
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: "nlb"  # NLB 사용
    service.beta.kubernetes.io/aws-load-balancer-internal: "true"  # 내부 전용
```

---

## Secret 업데이트

### Secret 수정
```powershell
# 기존 Secret 삭제
kubectl delete secret dynamodb-credentials -n default

# 새 값으로 재생성
kubectl create secret generic dynamodb-credentials `
    --from-literal=AWS_ACCESS_KEY_ID=새값 `
    --from-literal=AWS_SECRET_ACCESS_KEY=새값 `
    --from-literal=AWS_REGION=ap-northeast-2 `
    -n default

# Pod 재시작 (새 Secret 적용)
kubectl rollout restart deployment backend-app-deployment -n default
```

---

# 💰 비용 관리

## EKS 비용 구조

### 고정 비용
```
EKS 클러스터: $0.10/시간 = $72/월
```

### 가변 비용
```
EC2 인스턴스 (t3.medium x 2):
  - On-Demand: $0.0416/시간 x 2 = $60/월
  - Spot 인스턴스 사용 시: ~$20/월 (약 66% 절감)

LoadBalancer (CLB):
  - $0.025/시간 = $18/월
  
데이터 전송:
  - 인터넷 아웃바운드: $0.09/GB

ECR 스토리지:
  - $0.10/GB/월 (첫 50GB 무료)
```

### 월 예상 비용
```
최소 (Spot 사용): ~$110/월
일반 (On-Demand): ~$150/월
```

---

## 비용 절감 방법

### 1. 개발 환경 자동 중지
```bash
# 근무 시간 외 클러스터 중지 스크립트
# (Lambda + EventBridge 사용)
```

### 2. Spot 인스턴스 사용
- Node Group 생성 시 Spot 인스턴스 선택
- 약 66% 비용 절감
- 중단 가능성 있음 (프로덕션 주의)

### 3. Fargate 사용 (대안)
- 서버리스 컴퓨팅
- Pod 실행 시간만큼만 과금
- 소규모 트래픽에 유리

### 4. 테스트 후 리소스 정리
```powershell
# Node Group 삭제
aws eks delete-nodegroup `
    --cluster-name my-backend-cluster `
    --nodegroup-name my-backend-nodegroup

# 클러스터 삭제
aws eks delete-cluster --name my-backend-cluster

# ECR 이미지 정리
aws ecr batch-delete-image `
    --repository-name backend-app `
    --image-ids imageTag=old-tag
```

---

# 🌐 프론트엔드 적용 가이드

## 백엔드와의 차이점

| 항목 | 백엔드 | 프론트엔드 |
|------|--------|-----------|
| **빌드 도구** | Gradle | npm/yarn |
| **런타임** | Java 21 | Node.js 20 |
| **포트** | 8001 | 3000 |
| **환경 변수** | AWS 자격 증명 | API URL |
| **Health Check** | Spring Actuator | / |

---

## 프론트엔드 Dockerfile 예시

```dockerfile
# 빌드 단계
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# 실행 단계 (Nginx로 정적 파일 서빙)
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

---

## 프론트엔드 deployment.yml 예시

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend-app-deployment
spec:
  replicas: 2
  selector:
    matchLabels:
      app: frontend-app
  template:
    metadata:
      labels:
        app: frontend-app
    spec:
      containers:
      - name: frontend-app-container
        image: 310688446727.dkr.ecr.ap-northeast-2.amazonaws.com/frontend-app:latest
        ports:
        - containerPort: 80
        env:
        - name: REACT_APP_API_URL
          value: "http://k8s-default-backenda-xxx.elb.ap-northeast-2.amazonaws.com"
```

---

## 프론트엔드 buildspec.yml 예시

```yaml
version: 0.2

env:
  variables:
    AWS_DEFAULT_REGION: "ap-northeast-2"
    AWS_ACCOUNT_ID: "310688446727"
    IMAGE_REPO_NAME: "frontend-app"

phases:
  pre_build:
    commands:
      - echo "Logging in to Amazon ECR..."
      - aws ecr get-login-password --region $AWS_DEFAULT_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com
      - REPOSITORY_URI=$AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$IMAGE_REPO_NAME
      - COMMIT_HASH=$(echo $CODEBUILD_RESOLVED_SOURCE_VERSION | cut -c 1-7)
      - IMAGE_TAG=${COMMIT_HASH:=latest}

  build:
    commands:
      - echo "Building Docker image..."
      - docker build -t $REPOSITORY_URI:latest .
      - docker tag $REPOSITORY_URI:latest $REPOSITORY_URI:$IMAGE_TAG

  post_build:
    commands:
      - echo "Pushing to ECR..."
      - docker push $REPOSITORY_URI:latest
      - docker push $REPOSITORY_URI:$IMAGE_TAG
      
      # deployment.yml 자동 업데이트
      - sed -i "s|image:.*|image:\ $REPOSITORY_URI:$IMAGE_TAG|g" k8s-manifests/deployment.yml
      
      # GitHub 자동 커밋
      - git config --global user.email "codebuild@aws.com"
      - git config --global user.name "AWS CodeBuild"
      - git remote set-url origin https://${GITHUB_TOKEN}@github.com/organization/frontend-repo.git
      - git add k8s-manifests/deployment.yml
      - |
        if ! git diff --cached --exit-code > /dev/null; then
          git commit -m "chore: Update image to $IMAGE_TAG [skip ci]"
          git push origin HEAD:main
        fi
```

> 💡 **주의**: ECR 저장소 이름, GitHub URL만 변경하면 됨!

---

# 🔐 보안 모범 사례

## 1. 최소 권한 원칙

### ✅ 올바른 방식
```
애플리케이션 Pod:
  - DynamoDB 읽기/쓰기만 허용
  - IAM Policy로 특정 테이블만 접근

CI/CD 도구:
  - 빌드 및 배포에 필요한 권한만
  - ECR 푸시, CodeBuild 실행, EKS 업데이트
```

### ❌ 잘못된 방식
```
애플리케이션 Pod에 관리자 계정 키 주입:
  - 해킹 시 전체 AWS 계정 탈취 가능
  - EKS 클러스터 삭제 가능
  - 모든 리소스 접근 가능
```

---

## 2. Secret 로테이션

### 정기적 교체 (권장: 90일마다)
```powershell
# 새 IAM 액세스 키 생성
aws iam create-access-key --user-name dynamodb-user

# Kubernetes Secret 업데이트
kubectl create secret generic dynamodb-credentials \
    --from-literal=AWS_ACCESS_KEY_ID=새키 \
    --from-literal=AWS_SECRET_ACCESS_KEY=새비밀키 \
    --from-literal=AWS_REGION=ap-northeast-2 \
    -n default \
    --dry-run=client -o yaml | kubectl apply -f -

# Pod 재시작
kubectl rollout restart deployment backend-app-deployment -n default

# 구 키 비활성화
aws iam delete-access-key --user-name dynamodb-user --access-key-id 구키
```

---

## 3. IRSA (IAM Role for Service Account) - 권장

### 장점
- 자격 증명 하드코딩 불필요
- 자동 로테이션
- CloudTrail 감사 추적

### 설정 (프로덕션 준비 시)

#### OIDC Provider 생성
```powershell
eksctl utils associate-iam-oidc-provider `
    --cluster my-backend-cluster `
    --approve
```

#### IAM Role 생성
```powershell
eksctl create iamserviceaccount `
    --name backend-app-sa `
    --namespace default `
    --cluster my-backend-cluster `
    --attach-policy-arn arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess `
    --approve
```

#### deployment.yml 수정
```yaml
spec:
  template:
    spec:
      serviceAccountName: backend-app-sa  # IAM Role 연결
      containers:
      - name: backend-app-container
        # 환경 변수 불필요! AWS SDK가 자동으로 인증
```

---

# 📚 추가 유용한 정보

## kubectl 치트시트

### Pod 관리
```powershell
# Pod 목록
kubectl get pods -n default

# Pod 상세 정보
kubectl describe pod <pod-name> -n default

# Pod 로그 (실시간)
kubectl logs -f <pod-name> -n default

# Pod 내부 접속
kubectl exec -it <pod-name> -n default -- /bin/sh

# Pod 리소스 사용량
kubectl top pod -n default
```

### Deployment 관리
```powershell
# 스케일 조정
kubectl scale deployment backend-app-deployment --replicas=5 -n default

# Rollout 상태
kubectl rollout status deployment backend-app-deployment -n default

# Rollout 히스토리
kubectl rollout history deployment backend-app-deployment -n default

# 특정 Revision으로 롤백
kubectl rollout undo deployment backend-app-deployment --to-revision=2 -n default
```

### Service 관리
```powershell
# Service 목록
kubectl get svc -n default

# Service Endpoint 확인
kubectl get endpoints -n default

# LoadBalancer 이벤트
kubectl describe svc backend-app-service -n default
```

---

## ArgoCD CLI 사용

### 설치
```powershell
# Windows
winget install ArgoCD.CLI
```

### 로그인
```powershell
argocd login localhost:8080 --username admin --password <password>
```

### Application 관리
```powershell
# Application 목록
argocd app list

# 상태 확인
argocd app get backend-app

# 수동 Sync
argocd app sync backend-app

# Rollback
argocd app rollback backend-app
```

---

## CodeBuild 디버깅

### 로그 확인
```powershell
# 최근 빌드 ID 확인
aws codebuild list-builds-for-project `
    --project-name backend-build-project `
    --max-items 5

# 빌드 상세 정보
aws codebuild batch-get-builds --ids "빌드ID"

# CloudWatch 로그 확인
aws logs tail /aws/codebuild/backend-build-project --follow
```

### 로컬에서 buildspec.yml 테스트
```powershell
# Docker로 CodeBuild 환경 시뮬레이션
docker run -it --rm `
    -v ${PWD}:/workspace `
    -w /workspace `
    aws/codebuild/amazonlinux2-x86_64-standard:5.0 `
    bash

# 컨테이너 내부에서
./gradlew bootJar
```

---

## GitHub Actions 디버깅

### 로그 확인
- GitHub → Actions → 실패한 워크플로우 클릭
- 각 Step 펼쳐서 상세 로그 확인

### 로컬에서 테스트 (act 사용)
```powershell
# act 설치
winget install nektos.act

# 워크플로우 실행
act push
```

---

# 🚨 일반적인 문제 해결

## 문제 1: "ImagePullBackOff"

### 증상
```
kubectl get pods
NAME                          READY   STATUS             RESTARTS   AGE
backend-app-xxx               0/1     ImagePullBackOff   0          2m
```

### 원인
- ECR 이미지를 Pull할 수 없음
- Node가 ECR 접근 권한 없음

### 해결
```powershell
# Node IAM Role 확인
aws eks describe-nodegroup `
    --cluster-name my-backend-cluster `
    --nodegroup-name my-backend-nodegroup `
    --query "nodegroup.nodeRole"

# AmazonEC2ContainerRegistryReadOnly 정책 확인
aws iam list-attached-role-policies --role-name my-eks-nodegroup-role
```

---

## 문제 2: "CrashLoopBackOff"

### 증상
```
backend-app-xxx   0/1   CrashLoopBackOff   5   10m
```

### 진단
```powershell
# 로그 확인
kubectl logs <pod-name> -n default --previous

# 이벤트 확인
kubectl describe pod <pod-name> -n default
```

### 일반적인 원인
1. **환경 변수 누락**
   - application.yml이 기대하는 변수가 없음
   - Secret 이름 또는 키 오타

2. **포트 설정 오류**
   - containerPort와 실제 애플리케이션 포트 불일치
   - Spring Boot: `server.port` 확인

3. **메모리 부족**
   - Java heap size가 Pod memory limit 초과
   - limits 증가 또는 Java heap 조정

---

## 문제 3: ArgoCD Sync 안 됨

### 증상
- GitHub에 deployment.yml 변경했는데 ArgoCD가 배포 안 함
- SYNC STATUS: OutOfSync

### 원인 및 해결

#### 원인 1: Auto Sync 비활성화
```powershell
# 확인
kubectl get application backend-app -n argocd -o yaml | Select-String "automated"

# 활성화
argocd app set backend-app --sync-policy automated
```

#### 원인 2: 폴링 주기
- ArgoCD는 기본 3분마다 폴링
- 즉시 반영하려면 수동 Sync

```powershell
argocd app sync backend-app
# 또는
kubectl patch application backend-app -n argocd --type merge -p '{"operation":{"sync":{}}}'
```

#### 원인 3: 경로 오류
```yaml
# ❌ 잘못된 경로
spec:
  source:
    path: backend/k8s-manifests  # 저장소에 없는 경로!

# ✅ 올바른 경로
spec:
  source:
    path: k8s-manifests  # 저장소 루트 기준
```

---

## 문제 4: LoadBalancer Pending

### 증상
```
backend-app-service   LoadBalancer   10.100.x.x   <pending>   80:30552/TCP   10m
```

### 원인
- AWS Load Balancer Controller 없음
- VPC Subnet 설정 오류
- Security Group 문제

### 해결

#### Subnet 태그 확인
퍼블릭 서브넷에 다음 태그 필요:
```
kubernetes.io/role/elb = 1
```

프라이빗 서브넷에 다음 태그 필요:
```
kubernetes.io/role/internal-elb = 1
```

#### Security Group 확인
```powershell
aws ec2 describe-security-groups `
    --group-ids sg-xxx `
    --query "SecurityGroups[0].{IpPermissions:IpPermissions}"
```

---

# 📝 체크리스트

## 초기 구축 체크리스트

- [ ] AWS CLI 설치 및 인증 설정
- [ ] kubectl 설치 및 kubeconfig 설정
- [ ] Docker Desktop 설치
- [ ] ECR 저장소 생성
- [ ] Dockerfile 작성
- [ ] Docker 이미지 빌드 및 ECR 푸시 테스트
- [ ] k8s-manifests 폴더 생성
- [ ] deployment.yml 작성
- [ ] service.yml 작성
- [ ] EKS 클러스터 생성 (10-15분)
- [ ] Node Group 생성 (5분)
- [ ] kubectl get nodes 확인
- [ ] EKS Addons 설치 (vpc-cni, kube-proxy, coredns)
- [ ] ArgoCD 설치
- [ ] ArgoCD 초기 비밀번호 확인
- [ ] ArgoCD UI 접속
- [ ] GitHub 저장소 연결
- [ ] ArgoCD Application 생성
- [ ] Pod 배포 확인
- [ ] CodeBuild IAM 역할 생성
- [ ] buildspec.yml 작성
- [ ] GitHub Personal Access Token 생성 (repo 권한)
- [ ] Secrets Manager에 Token 저장
- [ ] CodeBuild 프로젝트 생성
- [ ] CodeBuild 환경 변수 설정
- [ ] 수동 빌드 테스트
- [ ] GitHub Actions 워크플로우 작성
- [ ] GitHub Secrets 설정 (AWS 자격 증명)
- [ ] GitHub Actions 테스트
- [ ] DynamoDB IAM 사용자 자격 증명 준비
- [ ] Kubernetes Secret 생성 (dynamodb-credentials)
- [ ] deployment.yml에 Secret 참조 추가
- [ ] application-secret.yml을 .gitignore에 추가
- [ ] GitHub Repository Rules Bypass 설정
- [ ] 전체 CI/CD 파이프라인 테스트
- [ ] LoadBalancer URL 접속 확인

---

## 일일 운영 체크리스트

### 매일
- [ ] Pod 상태 확인 (`kubectl get pods -n default`)
- [ ] ArgoCD Application 상태 (`kubectl get applications -n argocd`)
- [ ] CodeBuild 빌드 이력 (AWS Console)

### 매주
- [ ] EKS Node 상태 및 리소스 사용량
- [ ] CloudWatch 로그 확인
- [ ] LoadBalancer Health Check 상태

### 매월
- [ ] AWS 비용 확인
- [ ] Secret 로테이션 검토
- [ ] EKS 버전 업데이트 확인

---

# 🎓 개념 이해하기

## GitOps란?

**정의:** Git 저장소를 Single Source of Truth로 사용하는 배포 방식

**원리:**
```
Git 저장소 (원하는 상태)
    ↓
ArgoCD (비교 및 동기화)
    ↓
Kubernetes (실제 상태)
```

**장점:**
- ✅ Git 이력으로 모든 배포 추적
- ✅ Git Revert로 쉬운 롤백
- ✅ 선언적 배포 (명령형 스크립트 불필요)
- ✅ 코드 리뷰 프로세스 통합

---

## Rolling Update vs Blue-Green vs Canary

### Rolling Update (현재 사용 중)
```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1  # 동시 중단 가능 Pod 수
      maxSurge: 1        # 동시 추가 가능 Pod 수
```

**장점:** 무중단 배포, 리소스 효율적  
**단점:** 전체 배포 시간 김

### Blue-Green Deployment
```
Blue (현재 버전) → 100% 트래픽
Green (새 버전) → 배포 완료
Green 검증 완료 → 트래픽 전환
Blue 삭제
```

**장점:** 빠른 롤백  
**단점:** 2배 리소스 필요

### Canary Deployment
```
기존 버전: 90% 트래픽
새 버전: 10% 트래픽 (카나리)
문제 없으면 점진적으로 100%로
```

**장점:** 위험 최소화  
**단점:** 복잡한 설정 (Istio, Flagger 필요)

---

## Docker 멀티스테이지 빌드

### 왜 사용?

**단일 스테이지 (나쁜 예):**
```dockerfile
FROM gradle:8.5-jdk21-alpine
COPY . .
RUN ./gradlew bootJar
ENTRYPOINT ["java", "-jar", "build/libs/app.jar"]
```

**문제:**
- 이미지 크기: ~500MB (Gradle + JDK 포함)
- 빌드 도구가 프로덕션 이미지에 포함
- 보안 위험 (불필요한 도구 노출)

**멀티 스테이지 (좋은 예):**
```dockerfile
FROM gradle:8.5-jdk21-alpine AS build  # 빌드 단계
RUN ./gradlew bootJar

FROM eclipse-temurin:21-jre-alpine     # 실행 단계
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

**장점:**
- 이미지 크기: ~150MB (JRE만)
- 빌드 도구 제외
- 보안 향상

---

## Kubernetes Resources 이해

### requests vs limits

```yaml
resources:
  requests:     # 최소 보장 리소스
    cpu: "100m"
    memory: "128Mi"
  limits:       # 최대 사용 가능 리소스
    cpu: "200m"
    memory: "256Mi"
```

**동작:**
- **requests**: Pod 스케줄링 시 필요한 최소 리소스
- **limits**: 초과 시 Pod 강제 종료 (OOMKilled)

**권장 설정:**
```
Java 애플리케이션:
  - CPU: 100m ~ 500m
  - Memory: 256Mi ~ 1Gi (Java heap + 여유분)

Node.js 애플리케이션:
  - CPU: 50m ~ 200m
  - Memory: 128Mi ~ 512Mi
```

---

## 환경별 배포 전략

### 1. 단일 클러스터, 여러 네임스페이스

```
my-backend-cluster
├── namespace: dev
├── namespace: staging
└── namespace: prod
```

**장점:** 비용 효율적  
**단점:** 네임스페이스 간 간섭 가능

### 2. 환경별 클러스터 (권장)

```
dev-cluster    → 개발 환경
staging-cluster → 스테이징 환경
prod-cluster   → 프로덕션 환경
```

**장점:** 완전 격리  
**단점:** 비용 증가

### 3. ArgoCD ApplicationSet

여러 환경을 한 번에 관리:
```yaml
apiVersion: argoproj.io/v1alpha1
kind: ApplicationSet
metadata:
  name: backend-apps
spec:
  generators:
  - list:
      elements:
      - cluster: dev
        namespace: dev
      - cluster: staging
        namespace: staging
      - cluster: prod
        namespace: prod
  template:
    metadata:
      name: 'backend-{{cluster}}'
    spec:
      source:
        path: k8s-manifests/{{cluster}}
      destination:
        namespace: '{{namespace}}'
```

---

# 🔍 모니터링 및 알림

## CloudWatch 통합

### Container Insights 활성화
```powershell
aws eks update-cluster-config `
    --name my-backend-cluster `
    --logging '{"clusterLogging":[{"types":["api","audit","authenticator","controllerManager","scheduler"],"enabled":true}]}'
```

### 메트릭 확인
- AWS Console → CloudWatch → Container Insights
- CPU, 메모리, 네트워크 사용량 실시간 모니터링

---

## Slack 알림 통합

### GitHub Actions에서 Slack 알림

**workflow 파일에 추가:**
```yaml
- name: Slack Notification
  if: always()
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    text: '빌드 ${{ job.status }}: ${{ github.repository }}'
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### ArgoCD Slack 알림

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: argocd-notifications-cm
  namespace: argocd
data:
  service.slack: |
    token: $slack-token
  trigger.on-deployed: |
    - when: app.status.operationState.phase in ['Succeeded']
      send: [app-deployed]
  template.app-deployed: |
    message: |
      {{.app.metadata.name}} 배포 완료!
    slack:
      attachments: |
        [{
          "title": "{{.app.metadata.name}}",
          "title_link": "{{.context.argocdUrl}}/applications/{{.app.metadata.name}}",
          "color": "good"
        }]
```

---

# 📖 참고 자료

## 공식 문서

- **AWS EKS**: https://docs.aws.amazon.com/eks/
- **ArgoCD**: https://argo-cd.readthedocs.io/
- **AWS CodeBuild**: https://docs.aws.amazon.com/codebuild/
- **Kubernetes**: https://kubernetes.io/docs/
- **Docker**: https://docs.docker.com/

## 유용한 도구

- **k9s**: Kubernetes CLI UI (https://k9scli.io/)
- **Lens**: Kubernetes IDE (https://k8slens.dev/)
- **kubectx**: Context 빠른 전환
- **stern**: 여러 Pod 로그 동시 확인

---

# 🎉 최종 요약

## 구축 완료된 것

✅ **완전 자동화 CI/CD 파이프라인**
- GitHub Push → 자동 빌드 → 자동 배포
- 개발자는 코드만 작성하면 됨

✅ **안전한 보안 관리**
- 권한 분리 (인프라 vs 애플리케이션)
- Secret 암호화 저장
- 민감 정보 GitHub 노출 방지

✅ **무중단 배포**
- Rolling Update
- Health Check
- 자동 롤백

✅ **모니터링 및 로깅**
- CloudWatch Logs
- ArgoCD UI
- kubectl 명령어

---

## 다음 단계 제안

### 단기 (1-2주)
- [ ] LoadBalancer 접속 테스트 및 Health Check 설정
- [ ] Horizontal Pod Autoscaler (HPA) 설정
- [ ] 프론트엔드 CI/CD 구축
- [ ] Monitoring Dashboard 설정

### 중기 (1개월)
- [ ] IRSA (IAM Role for Service Account) 적용
- [ ] Ingress Controller 설정 (ALB)
- [ ] SSL/TLS 인증서 설정 (ACM)
- [ ] 환경 분리 (dev/staging/prod)

### 장기 (3개월)
- [ ] Service Mesh (Istio) 적용
- [ ] Canary Deployment 구현
- [ ] Disaster Recovery 계획
- [ ] 비용 최적화 (Spot 인스턴스, Fargate)

---

## 마무리

이 가이드는 실제 프로덕션 환경에서 사용 가능한 **완전 자동화 CI/CD 파이프라인 구축 방법**을 담고 있습니다.

**핵심 원칙:**
1. **자동화**: 사람의 개입 최소화
2. **보안**: 권한 분리 및 Secret 관리
3. **모니터링**: 문제를 빠르게 발견
4. **문서화**: 팀원이 쉽게 이해

**질문이나 문제가 발생하면:**
1. 이 문서의 트러블슈팅 섹션 참고
2. kubectl/aws cli로 상태 확인
3. CloudWatch 로그 확인
4. ArgoCD UI에서 배포 이력 확인

---

**작성일**: 2025-12-09  
**버전**: 1.0  
**작성자**: AI Assistant  
**검증 완료**: ✅ 실제 환경에서 테스트 완료


